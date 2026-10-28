package io.rea.obd.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

public final class AnalysisClient {
    private static final int MAX_RESPONSE = 64 * 1024;

    private AnalysisClient() {
    }

    public static JSONObject analyze(String serverUrl, String token, JSONObject request) throws IOException, JSONException {
        URL url = new URL(serverUrl.replaceAll("/+$", "") + "/v1/analyze");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(120_000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            String body = read(status < 400 ? connection.getInputStream() : connection.getErrorStream());
            if (status != 200) {
                throw new IOException(failure(status, body));
            }
            return new JSONObject(body);
        } finally {
            connection.disconnect();
        }
    }

    private static String failure(int status, String body) {
        switch (status) {
            case 401:
                return "O servidor de IA recusou o token. Confira o token em Ajustes.";
            case 429:
                return "Limite de análises de demonstração atingido no servidor.";
            case 422:
                return "A IA não aceitou analisar este caso.";
            default:
                String detail = "";
                try {
                    detail = new JSONObject(body).optString("error");
                } catch (JSONException ignored) {
                }
                return "Servidor de IA indisponível (HTTP " + status + ")" + (detail.isEmpty() ? "." : ": " + detail);
        }
    }

    private static String read(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        try (InputStream stream = input) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
                if (buffer.size() > MAX_RESPONSE) {
                    throw new IOException("Resposta do servidor de IA grande demais");
                }
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
