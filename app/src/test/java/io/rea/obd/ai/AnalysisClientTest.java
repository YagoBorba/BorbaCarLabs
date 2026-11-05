package io.rea.obd.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Talks to a local fake that answers like the Gemini and Claude APIs; no real provider is called. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AnalysisClientTest {
    private static final String KEY = "test-key-0123456789abcdef";
    private ServerSocket server;
    private final Map<String, String> headers = new HashMap<>();
    private volatile String requestLine = "";
    private volatile String requestBody = "";
    private String originalGemini;
    private String originalClaude;

    @Before
    public void setUp() {
        originalGemini = AnalysisClient.geminiBase;
        originalClaude = AnalysisClient.claudeBase;
        KeyVault.backend = TestVault.INSTANCE;
        AnalysisClient.overloadRetries = 0;
    }

    @After
    public void stop() throws IOException {
        AnalysisClient.geminiBase = originalGemini;
        AnalysisClient.claudeBase = originalClaude;
        if (server != null) {
            server.close();
        }
    }

    @After
    public void restoreRetries() {
        AnalysisClient.overloadRetries = 1;
        AnalysisClient.readTimeoutMs = 35_000;
    }

    private String serve(int status, String body) throws IOException {
        return serve(new int[]{status}, new String[]{body});
    }

    /** Answers one connection per entry, in order; counts them in {@link #requests}. */
    private String serve(int[] statuses, String[] bodies) throws IOException {
        ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        server = socket;
        headers.clear();
        requests.set(0);
        Thread thread = new Thread(() -> {
            for (int turn = 0; turn < statuses.length; turn++) {
                answer(socket, statuses[turn], bodies[turn]);
            }
        });
        thread.setDaemon(true);
        thread.start();
        String base = "http://127.0.0.1:" + socket.getLocalPort();
        AnalysisClient.geminiBase = base + "/v1beta";
        AnalysisClient.claudeBase = base + "/v1";
        return base;
    }

    private final java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();

    private void answer(ServerSocket socket, int status, String body) {
            if (status == 0) { // a model that never answers in time: hold the connection open, keep accepting
                try {
                    Socket silent = socket.accept();
                    requests.incrementAndGet();
                    Thread hold = new Thread(() -> {
                        try { Thread.sleep(2000); silent.close(); } catch (InterruptedException | IOException ignored) { }
                    });
                    hold.setDaemon(true);
                    hold.start();
                } catch (IOException ignored) {
                }
                return;
            }
            try (Socket client = socket.accept()) {
                requests.incrementAndGet();
                // Content-Length counts bytes, so the body is read as bytes (accented text is longer in UTF-8).
                InputStream input = client.getInputStream();
                requestLine = readLine(input);
                int length = 0;
                for (String line = readLine(input); !line.isEmpty(); line = readLine(input)) {
                    int colon = line.indexOf(':');
                    String name = line.substring(0, colon).trim().toLowerCase();
                    String value = line.substring(colon + 1).trim();
                    headers.put(name, value);
                    if (name.equals("content-length")) {
                        length = Integer.parseInt(value);
                    }
                }
                byte[] content = new byte[length];
                int read = 0;
                while (read < length) {
                    int n = input.read(content, read, length - read);
                    if (n < 0) break;
                    read += n;
                }
                requestBody = new String(content, 0, read, StandardCharsets.UTF_8);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                OutputStream out = client.getOutputStream();
                out.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                        + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(bytes);
                out.flush();
            } catch (IOException ignored) {
            }
    }

    @Test
    public void aMomentaryOverloadIsRetriedOnce() throws Exception {
        AnalysisClient.overloadRetries = 1;
        serve(new int[]{503, 200}, new String[]{"{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}",
                geminiReply(analysisJson("D1", "media"), "STOP")});
        JSONObject response = AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request());
        assertEquals(2, requests.get());
        assertEquals("gemini-test", response.getString("model"));
    }

    @Test
    public void aBusyDefaultModelFallsBackToTheLighterOne() throws Exception {
        serve(new int[]{503, 200}, new String[]{"{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}",
                geminiReply(analysisJson("D1", "media"), "STOP")});
        AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request());
        assertEquals(2, requests.get());
        assertTrue(requestLine.contains("/models/" + AiProvider.GEMINI.fallbackModels[0] + ":generateContent"));
    }

    @Test
    public void slowAndRateLimitedModelsHandOverToTheNextOne() throws Exception {
        AnalysisClient.readTimeoutMs = 300;
        String quota = "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"quota\"}}";
        serve(new int[]{0, 429, 200}, new String[]{"", quota, geminiReply(analysisJson("D1", "media"), "STOP")});
        JSONObject response = AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request());
        assertEquals(3, requests.get());
        assertTrue(requestLine.contains("/models/" + AiProvider.GEMINI.fallbackModels[1] + ":generateContent"));
        assertEquals("gemini-test", response.getString("model"));
    }

    @Test
    public void aModelChosenByTheUserIsNotSwapped() throws Exception {
        serve(503, "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}");
        AiSettings.save(RuntimeEnvironment.getApplication(), AiProvider.GEMINI, KEY, "gemini-3.5-flash", "");
        try {
            AnalysisClient.analyze(AiSettings.load(RuntimeEnvironment.getApplication(), AiProvider.GEMINI), KEY, request());
            fail();
        } catch (AnalysisClient.Failure expected) {
            assertEquals(AnalysisClient.OVERLOADED, expected.getMessage());
        }
        assertEquals(1, requests.get());
    }

    /** Reads one CRLF-terminated header line; returns "" at the end of the headers or of the stream. */
    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int b = input.read(); b != -1 && b != '\n'; b = input.read()) {
            if (b != '\r') {
                line.write(b);
            }
        }
        return new String(line.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private static AiSettings settings(AiProvider provider, String workspace) {
        AiSettings.save(context(), provider, KEY, "", workspace);
        return AiSettings.load(context(), provider);
    }

    private static JSONObject request() throws JSONException {
        return new JSONObject()
                .put("schema", AnalysisRequest.SCHEMA)
                .put("language", "pt-BR")
                .put("vehicle", new JSONObject().put("make", "Chevrolet"))
                .put("read_complete", true)
                .put("demonstration_data", true)
                .put("observations", new JSONArray().put(new JSONObject().put("ref", "D1").put("type", "dtc").put("code", "P0301")))
                .put("question", "O que significa?")
                .put("conversation", new JSONArray());
    }

    private static String analysisJson(String ref, String confidence) {
        return "{\"summary\":\"Dados simulados: falha de ignição no cilindro 1.\",\"hypotheses\":[{\"title\":\"Vela ou bobina\","
                + "\"reasoning\":\"P0301 indica falha no cilindro 1.\",\"evidence\":[\"" + ref + "\"],\"confidence\":\"" + confidence + "\","
                + "\"next_check\":\"Inspecionar vela e bobina.\"}],\"missing_information\":[\"Sintomas\"],\"safety_note\":\"\"}";
    }

    private static String geminiReply(String text, String finish) throws JSONException {
        return new JSONObject().put("modelVersion", "gemini-test")
                .put("candidates", new JSONArray().put(new JSONObject().put("finishReason", finish)
                        .put("content", new JSONObject().put("role", "model").put("parts", new JSONArray()
                                .put(new JSONObject().put("text", "pensando").put("thought", true))
                                .put(new JSONObject().put("text", text)))))).toString();
    }

    private static String claudeReply(String text, String stop) throws JSONException {
        return new JSONObject().put("model", "claude-test").put("stop_reason", stop)
                .put("content", new JSONArray().put(new JSONObject().put("type", "text").put("text", text))).toString();
    }

    private String failure(AiProvider provider) {
        try {
            AnalysisClient.analyze(settings(provider, ""), KEY, request());
            fail("deveria falhar");
            return null;
        } catch (IOException | JSONException e) {
            return AnalysisClient.userMessage(e);
        }
    }

    @Test
    public void geminiSendsTheKeyInAHeaderAndReturnsAValidatedAnalysis() throws Exception {
        serve(200, geminiReply(analysisJson("D1", "Média"), "STOP"));
        JSONObject response = AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request());
        assertTrue(requestLine.startsWith("POST /v1beta/models/" + AiProvider.GEMINI.defaultModel + ":generateContent "));
        assertFalse("a chave não pode ir na URL", requestLine.contains(KEY));
        assertEquals(KEY, headers.get("x-goog-api-key"));
        JSONObject sent = new JSONObject(requestBody);
        assertTrue(sent.getJSONObject("systemInstruction").toString().contains("Não invente referências"));
        assertEquals("application/json", sent.getJSONObject("generationConfig").getString("responseMimeType"));
        assertFalse(sent.getJSONObject("generationConfig").getJSONObject("responseJsonSchema").has("additionalProperties"));
        JSONObject kase = new JSONObject(sent.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"));
        assertEquals("O que significa?", kase.getString("question"));
        assertFalse(kase.has("schema"));
        assertEquals("gemini-test", response.getString("model"));
        AnalysisResult result = AnalysisResult.parse(response, new HashSet<>(Arrays.asList("D1")));
        assertEquals("media", result.hypotheses.get(0).confidence);
        assertFalse(result.summary.contains("pensando"));
    }

    @Test
    public void claudeUsesStructuredOutputsAndTheOptionalWorkspace() throws Exception {
        serve(200, claudeReply(analysisJson("D1", "alta"), "end_turn"));
        JSONObject response = AnalysisClient.analyze(settings(AiProvider.CLAUDE, "wrkspc_test"), KEY, request());
        assertTrue(requestLine.startsWith("POST /v1/messages "));
        assertEquals(KEY, headers.get("x-api-key"));
        assertEquals("2023-06-01", headers.get("anthropic-version"));
        assertEquals("wrkspc_test", headers.get("anthropic-workspace-id"));
        JSONObject sent = new JSONObject(requestBody);
        assertEquals(AiProvider.CLAUDE.defaultModel, sent.getString("model"));
        JSONObject format = sent.getJSONObject("output_config").getJSONObject("format");
        assertEquals("json_schema", format.getString("type"));
        assertFalse(format.getJSONObject("schema").getBoolean("additionalProperties"));
        assertEquals("claude-test", response.getString("model"));
        AnalysisResult.parse(response, new HashSet<>(Arrays.asList("D1")));
    }

    @Test
    public void englishCasesAskForEnglishReplies() throws Exception {
        serve(200, claudeReply(analysisJson("D1", "baixa"), "end_turn"));
        AnalysisClient.analyze(settings(AiProvider.CLAUDE, ""), KEY, request().put("language", "en"));
        assertTrue(new JSONObject(requestBody).getString("system").contains("escreva todos os textos em inglês"));
        assertFalse(headers.containsKey("anthropic-workspace-id"));
    }

    @Test
    public void verifyingTheKeyDoesNotGenerateText() throws Exception {
        serve(200, "{\"name\":\"models/gemini-3.5-flash-lite\",\"displayName\":\"Gemini\"}");
        assertEquals("gemini-3.5-flash-lite", AnalysisClient.verify(settings(AiProvider.GEMINI, ""), KEY));
        assertTrue(requestLine.startsWith("GET /v1beta/models/gemini-3.5-flash-lite "));
        stop();
        serve(200, "{\"id\":\"claude-sonnet-5-5\",\"type\":\"model\"}");
        assertEquals("claude-sonnet-5-5", AnalysisClient.verify(settings(AiProvider.CLAUDE, ""), KEY));
        assertTrue(requestLine.startsWith("GET /v1/models/claude-sonnet-5-5 "));
    }

    @Test
    public void rejectedKeysHaveTheirOwnMessage() throws IOException {
        serve(400, "{\"error\":{\"code\":400,\"message\":\"API key not valid. Please pass a valid API key.\",\"status\":\"INVALID_ARGUMENT\","
                + "\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}");
        assertEquals(AnalysisClient.INVALID_KEY, failure(AiProvider.GEMINI));
        stop();
        serve(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}");
        assertEquals(AnalysisClient.INVALID_KEY, failure(AiProvider.CLAUDE));
    }

    @Test
    public void quotaModelAndOverloadErrorsAreExplained() throws IOException {
        String quota = "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"quota\"}}";
        serve(new int[]{429, 429, 429}, new String[]{quota, quota, quota});
        assertEquals(AnalysisClient.RATE_LIMITED, failure(AiProvider.GEMINI));
        assertEquals("every free model is tried before reporting the limit", 3, requests.get());
        stop();
        serve(404, "{\"type\":\"error\",\"error\":{\"type\":\"not_found_error\",\"message\":\"model: x\"}}");
        assertEquals(AnalysisClient.MODEL_UNAVAILABLE, failure(AiProvider.CLAUDE));
        stop();
        String overloaded = "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}";
        serve(new int[]{529, 529}, new String[]{overloaded, overloaded});
        assertEquals(AnalysisClient.OVERLOADED, failure(AiProvider.CLAUDE));
        assertEquals("the lighter model is tried before giving up", 2, requests.get());
    }

    @Test
    public void otherProviderErrorsShowTheirReasonWithoutTheKey() throws IOException {
        serve(400, "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Your credit balance is too low.\"}}");
        String message = failure(AiProvider.CLAUDE);
        assertEquals("O provedor recusou o pedido (HTTP 400): Your credit balance is too low.", message);
        assertFalse(message.contains(KEY));
        stop();
        serve(400, "not json");
        assertEquals("O provedor recusou o pedido (HTTP 400).", failure(AiProvider.GEMINI));
    }

    @Test
    public void refusalsTruncationAndBadJsonAreReported() throws Exception {
        serve(200, claudeReply("", "refusal"));
        assertEquals(AnalysisClient.REFUSED, failure(AiProvider.CLAUDE));
        stop();
        serve(200, geminiReply("{\"summary\":", "MAX_TOKENS"));
        assertEquals(AnalysisClient.INCOMPLETE, failure(AiProvider.GEMINI));
        stop();
        serve(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        assertEquals(AnalysisClient.REFUSED, failure(AiProvider.GEMINI));
        stop();
        serve(200, geminiReply("<html>proxy</html>", "STOP"));
        assertEquals(AnalysisClient.WRONG_FORMAT, failure(AiProvider.GEMINI));
        stop();
        serve(200, "<html>proxy</html>");
        assertEquals(AnalysisClient.WRONG_FORMAT, failure(AiProvider.CLAUDE));
    }

    @Test
    public void oversizedRepliesAreRejected() throws IOException {
        char[] big = new char[600 * 1024];
        Arrays.fill(big, 'a');
        serve(200, "{\"x\":\"" + new String(big) + "\"}");
        assertEquals(AnalysisClient.TOO_LARGE, failure(AiProvider.GEMINI));
    }

    @Test
    public void unreachableProviderIsDescribedAsAConnectionProblem() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        AnalysisClient.geminiBase = "http://127.0.0.1:" + port + "/v1beta";
        assertEquals(AnalysisClient.OFFLINE, failure(AiProvider.GEMINI));
    }

    @Test
    public void casesOutsideTheContractAreNotSent() throws Exception {
        serve(200, "{}");
        JSONObject duplicated = request();
        duplicated.getJSONArray("observations").put(new JSONObject().put("ref", "D1"));
        try {
            AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, duplicated);
            fail();
        } catch (AnalysisClient.Failure e) {
            assertEquals(AnalysisClient.OUT_OF_CONTRACT, e.getMessage());
        }
        try {
            AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request().put("question", ""));
            fail();
        } catch (AnalysisClient.Failure e) {
            assertEquals(AnalysisClient.OUT_OF_CONTRACT, e.getMessage());
        }
        assertEquals("", requestBody);
    }

    @Test
    public void evidenceIsLimitedToTheRefsThatWereSent() throws Exception {
        serve(200, geminiReply(analysisJson("X9", "baixa"), "STOP"));
        JSONObject response = AnalysisClient.analyze(settings(AiProvider.GEMINI, ""), KEY, request());
        JSONObject items = new JSONObject(requestBody).getJSONObject("generationConfig").getJSONObject("responseJsonSchema")
                .getJSONObject("properties").getJSONObject("hypotheses").getJSONObject("items")
                .getJSONObject("properties").getJSONObject("evidence").getJSONObject("items");
        assertEquals("[\"D1\"]", items.getJSONArray("enum").toString());
        AnalysisResult result = AnalysisResult.parse(response, new HashSet<>(Arrays.asList("D1")));
        assertTrue(result.hypotheses.get(0).evidence.isEmpty());
    }

    @Test
    public void pastedKeysLoseSpacesLineBreaksAndQuotes() {
        assertEquals("AQ.abc-123_XYZ", AiSettings.cleanKey("  \"AQ.abc-\n123_XYZ\" \n"));
    }

    @Test
    public void answerMissingTheAnalysisIsReportedAsWrongFormat() {
        try {
            AnalysisResult.parse(new JSONObject("{\"model\":\"m\"}"), new HashSet<>(Arrays.asList("D1")));
            fail();
        } catch (JSONException e) {
            assertEquals(AnalysisClient.WRONG_FORMAT, AnalysisClient.userMessage(e));
        }
    }
}
