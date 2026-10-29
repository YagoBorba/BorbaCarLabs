package io.rea.obd.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

public class AnalysisClientTest {
    private ServerSocket server;

    private String serve(int status, String body) throws IOException {
        ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        server = socket;
        Thread thread = new Thread(() -> {
            try (Socket client = socket.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                int length = 0;
                for (String line = reader.readLine(); line != null && !line.isEmpty(); line = reader.readLine()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        length = Integer.parseInt(line.substring(15).trim());
                    }
                }
                for (int i = 0; i < length; i++) {
                    reader.read();
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                OutputStream out = client.getOutputStream();
                out.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                        + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(bytes);
                out.flush();
            } catch (IOException ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
        return "http://127.0.0.1:" + socket.getLocalPort() + "/";
    }

    @After
    public void stop() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    private static String failure(String url) {
        try {
            AnalysisClient.analyze(url, "token", new JSONObject());
            fail("deveria falhar");
            return null;
        } catch (IOException | JSONException e) {
            return AnalysisClient.userMessage(e);
        }
    }

    @Test
    public void bodyThatIsNotJsonIsReportedAsWrongFormat() throws IOException {
        assertEquals("A resposta do servidor de IA não está no formato esperado.", failure(serve(200, "<html>proxy</html>")));
    }

    @Test
    public void emptyBodyIsReportedAsWrongFormat() throws IOException {
        assertEquals("A resposta do servidor de IA não está no formato esperado.", failure(serve(200, "")));
    }

    @Test
    public void oversizedBodyIsRejected() throws IOException {
        char[] big = new char[70 * 1024];
        Arrays.fill(big, 'a');
        assertEquals("Resposta do servidor de IA grande demais.", failure(serve(200, "{\"x\":\"" + new String(big) + "\"}")));
    }

    @Test
    public void serverErrorShowsItsReason() throws IOException {
        assertEquals("Servidor de IA indisponível (HTTP 502): Resposta da IA fora do contrato",
                failure(serve(502, "{\"error\":\"Resposta da IA fora do contrato\"}")));
        stop();
        assertEquals("Servidor de IA indisponível (HTTP 504): A IA demorou demais para responder",
                failure(serve(504, "{\"error\":\"A IA demorou demais para responder\"}")));
    }

    @Test
    public void serverErrorWithoutJsonStillGivesAClearMessage() throws IOException {
        assertEquals("Servidor de IA indisponível (HTTP 500).", failure(serve(500, "Internal Server Error")));
    }

    @Test
    public void tokenAndQuotaErrorsHaveTheirOwnMessages() throws IOException {
        assertEquals("O servidor de IA recusou o token. Confira o token em Ajustes.", failure(serve(401, "{}")));
        stop();
        assertEquals("Limite de análises de demonstração atingido no servidor.", failure(serve(429, "{}")));
    }

    @Test
    public void unreachableServerIsNotDescribedAsTheAdapter() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        assertEquals("Não foi possível alcançar o servidor de IA. Confira o endereço em Ajustes e se o celular está na mesma rede do servidor.",
                failure("http://127.0.0.1:" + port));
    }

    @Test
    public void answerOutsideTheContractKeepsItsReason() {
        try {
            AnalysisResult.parse(new JSONObject("{\"model\":\"m\",\"analysis\":{\"summary\":\"s\",\"hypotheses\":[{\"title\":\"t\",\"reasoning\":\"r\","
                    + "\"evidence\":[\"X9\"],\"confidence\":\"baixa\",\"next_check\":\"n\"}],\"missing_information\":[],\"safety_note\":\"\"}}"),
                    new HashSet<>(Arrays.asList("D1")));
            fail();
        } catch (JSONException e) {
            assertEquals("A IA citou uma evidência que não foi enviada: X9", AnalysisClient.userMessage(e));
        }
    }

    @Test
    public void answerMissingTheAnalysisIsReportedAsWrongFormat() {
        try {
            AnalysisResult.parse(new JSONObject("{\"model\":\"m\"}"), new HashSet<>(Arrays.asList("D1")));
            fail();
        } catch (JSONException e) {
            assertEquals("A resposta do servidor de IA não está no formato esperado.", AnalysisClient.userMessage(e));
        }
    }
}
