package io.rea.obd.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Calls Gemini or Claude directly over HTTPS with the user's own API key.
 * The key goes only in a request header to the chosen provider, never in a URL or a message.
 */
public final class AnalysisClient {
    static final String NO_KEY = "Configure uma chave de API em Ajustes › Análise por IA.";
    static final String KEY_UNREADABLE = "Não foi possível ler a chave salva. Cole a chave novamente em Ajustes › Análise por IA.";
    static final String OUT_OF_CONTRACT = "O caso não está no formato esperado para envio à IA.";
    static final String INVALID_KEY = "O provedor recusou a chave de API. Confira a chave em Ajustes › Análise por IA.";
    static final String MODEL_UNAVAILABLE = "O modelo configurado não está disponível para esta chave. Deixe o campo Modelo vazio para usar o padrão.";
    static final String RATE_LIMITED = "Limite de uso da sua chave atingido no provedor. Aguarde um pouco ou confira o plano da sua conta.";
    static final String OVERLOADED = "O provedor de IA está sobrecarregado ou indisponível. Tente novamente em instantes.";
    static final String REFUSED = "A IA não aceitou analisar este caso.";
    static final String INCOMPLETE = "Resposta da IA incompleta. Tente novamente.";
    static final String TOO_LARGE = "Resposta da IA grande demais.";
    static final String TIMEOUT = "A IA não respondeu a tempo. Tente novamente em instantes.";
    static final String OFFLINE = "Sem conexão com o provedor de IA. Confira a internet do celular.";
    static final String WRONG_FORMAT = "A resposta da IA não está no formato esperado.";
    static final String INTERRUPTED = "A comunicação com o provedor de IA foi interrompida. Tente novamente.";

    private static final int MAX_RESPONSE = 512 * 1024;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** Overridden by tests to point at a local fake. */
    static String geminiBase = "https://generativelanguage.googleapis.com/v1beta";
    static String claudeBase = "https://api.anthropic.com/v1";

    public static final class Failure extends IOException {
        Failure(String message) {
            super(message);
        }
    }

    private AnalysisClient() {
    }

    /** A Portuguese message for the user; callers translate it with I18n.t. */
    public static String userMessage(Exception exception) {
        if (exception instanceof Failure || exception instanceof AnalysisResult.ContractException) {
            return exception.getMessage();
        }
        if (exception instanceof SocketTimeoutException) {
            return TIMEOUT;
        }
        if (exception instanceof ConnectException || exception instanceof NoRouteToHostException || exception instanceof UnknownHostException) {
            return OFFLINE;
        }
        if (exception instanceof JSONException) {
            return WRONG_FORMAT;
        }
        if (exception instanceof IOException) {
            return INTERRUPTED;
        }
        return "Não foi possível concluir a análise por IA.";
    }

    /** Sends the reviewed request and returns {"model": ..., "analysis": {...}}. */
    public static JSONObject analyze(AiSettings settings, String key, JSONObject request) throws IOException, JSONException {
        JSONObject kase = AiPrompt.caseFor(request);
        String model = settings.modelOrDefault();
        try {
            return ask(settings, key, model, kase);
        } catch (Failure failure) {
            // A busy default model should not end the conversation: try the provider's lighter model once.
            if (!OVERLOADED.equals(failure.getMessage()) || !settings.model.isEmpty()) {
                throw failure;
            }
            return ask(settings, key, settings.provider.fallbackModel, kase);
        }
    }

    private static JSONObject ask(AiSettings settings, String key, String model, JSONObject kase) throws IOException, JSONException {
        return settings.provider == AiProvider.CLAUDE ? claude(settings, key, model, kase) : gemini(key, model, kase);
    }

    /** Checks the key and model without generating text, so it uses no tokens. Returns the model name. */
    public static String verify(AiSettings settings, String key) throws IOException, JSONException {
        String model = settings.modelOrDefault();
        if (settings.provider == AiProvider.CLAUDE) {
            JSONObject info = exchange(claudeBase + "/models/" + model, "GET", claudeHeaders(settings, key), null);
            return info.optString("id", model);
        }
        JSONObject info = exchange(geminiBase + "/models/" + model, "GET", geminiHeaders(key), null);
        return info.optString("name", model).replaceFirst("^models/", "");
    }

    private static Map<String, String> geminiHeaders(String key) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-goog-api-key", key);
        return headers;
    }

    private static Map<String, String> claudeHeaders(AiSettings settings, String key) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", key);
        headers.put("anthropic-version", ANTHROPIC_VERSION);
        if (!settings.workspace.isEmpty()) {
            headers.put("anthropic-workspace-id", settings.workspace);
        }
        return headers;
    }

    private static JSONObject gemini(String key, String model, JSONObject kase) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", AiPrompt.system(kase)))))
                .put("contents", new JSONArray().put(new JSONObject().put("role", "user")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", kase.toString())))))
                .put("generationConfig", new JSONObject()
                        .put("responseMimeType", "application/json")
                        .put("responseJsonSchema", AiPrompt.schema(false, kase)));
        JSONObject reply = exchange(geminiBase + "/models/" + model + ":generateContent", "POST", geminiHeaders(key), body);
        JSONArray candidates = reply.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            JSONObject feedback = reply.optJSONObject("promptFeedback");
            throw new Failure(feedback != null && feedback.has("blockReason") ? REFUSED : WRONG_FORMAT);
        }
        JSONObject candidate = candidates.getJSONObject(0);
        String finish = candidate.optString("finishReason", "STOP");
        if ("MAX_TOKENS".equals(finish)) {
            throw new Failure(INCOMPLETE);
        }
        if (!"STOP".equals(finish)) {
            throw new Failure(REFUSED);
        }
        StringBuilder text = new StringBuilder();
        JSONObject content = candidate.optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        for (int i = 0; parts != null && i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (!part.optBoolean("thought") && part.has("text")) {
                text.append(part.getString("text"));
            }
        }
        return AiPrompt.response(reply.optString("modelVersion", model), text.toString());
    }

    private static JSONObject claude(AiSettings settings, String key, String model, JSONObject kase) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("max_tokens", 4000)
                .put("system", AiPrompt.system(kase))
                .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", kase.toString())))
                .put("output_config", new JSONObject().put("format", new JSONObject()
                        .put("type", "json_schema").put("schema", AiPrompt.schema(true, kase))));
        JSONObject reply = exchange(claudeBase + "/messages", "POST", claudeHeaders(settings, key), body);
        String stop = reply.optString("stop_reason");
        if ("refusal".equals(stop)) {
            throw new Failure(REFUSED);
        }
        if ("max_tokens".equals(stop)) {
            throw new Failure(INCOMPLETE);
        }
        StringBuilder text = new StringBuilder();
        JSONArray content = reply.getJSONArray("content");
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.getJSONObject(i);
            if ("text".equals(block.optString("type"))) {
                text.append(block.getString("text"));
            }
        }
        return AiPrompt.response(reply.optString("model", model), text.toString());
    }

    /** Free tiers often answer 500/503 for a moment; one quiet retry hides most of those blips. Tests set it to 0. */
    static int overloadRetries = 1;

    private static JSONObject exchange(String address, String method, Map<String, String> headers, JSONObject request) throws IOException, JSONException {
        for (int attempt = 0; ; attempt++) {
            try {
                return exchangeOnce(address, method, headers, request);
            } catch (Failure failure) {
                if (attempt >= overloadRetries || !OVERLOADED.equals(failure.getMessage())) {
                    throw failure;
                }
                try {
                    Thread.sleep(1500L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
            }
        }
    }

    private static JSONObject exchangeOnce(String address, String method, Map<String, String> headers, JSONObject request) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(90_000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setDoOutput(request != null);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (request != null) {
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(request.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            String body = read(status < 400 ? connection.getInputStream() : connection.getErrorStream());
            if (status != 200) {
                throw new Failure(failure(status, body));
            }
            return new JSONObject(body);
        } finally {
            connection.disconnect();
        }
    }

    /** Maps provider errors (Gemini or Claude JSON shapes) to a clear message without echoing secrets. */
    static String failure(int status, String body) {
        String detail = "";
        boolean keyRejected = false;
        try {
            JSONObject error = new JSONObject(body).optJSONObject("error");
            if (error != null) {
                detail = error.optString("message");
                String type = error.optString("type") + " " + error.optString("status");
                JSONArray details = error.optJSONArray("details");
                for (int i = 0; details != null && i < details.length(); i++) {
                    JSONObject item = details.optJSONObject(i);
                    if (item != null && "API_KEY_INVALID".equals(item.optString("reason"))) {
                        keyRejected = true;
                    }
                }
                keyRejected |= type.contains("authentication_error") || detail.toLowerCase(java.util.Locale.ROOT).contains("api key not valid");
            }
        } catch (JSONException ignored) {
        }
        if (status == 401 || status == 403 || keyRejected) {
            return INVALID_KEY;
        }
        switch (status) {
            case 404:
                return MODEL_UNAVAILABLE;
            case 429:
                return RATE_LIMITED;
            case 408:
            case 504:
                return TIMEOUT;
            case 500:
            case 502:
            case 503:
            case 529:
                return OVERLOADED;
            default:
                detail = detail.replaceAll("\\s+", " ").trim();
                if (detail.length() > 200) {
                    detail = detail.substring(0, 200) + "…";
                }
                return "O provedor recusou o pedido (HTTP " + status + ")" + (detail.isEmpty() ? "." : ": " + detail);
        }
    }

    private static String read(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        try (InputStream stream = input) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > MAX_RESPONSE) {
                    throw new Failure(TOO_LARGE);
                }
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
