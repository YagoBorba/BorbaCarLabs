package io.rea.obd.ai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AiSettings {
    private static final String PREFS = "ai";

    public final String serverUrl;
    public final String token;

    private AiSettings(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.token = token;
    }

    public static AiSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new AiSettings(prefs.getString("server_url", ""), prefs.getString("token", ""));
    }

    public boolean configured() {
        return valid(this.serverUrl, this.token);
    }

    public static boolean valid(String address, String token) {
        try {
            java.net.URI uri = new java.net.URI(address);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && token.length() >= 16 && token.length() <= 256 && !token.matches("(?s).*\\s.*");
        } catch (Exception e) { return false; }
    }

    public static String[] invitation(String text) {
        String[] parts = text.trim().split("#", -1);
        if (parts.length != 2 || !valid(parts[0], parts[1])) {
            throw new IllegalArgumentException("Convite inválido. Copie o endereço completo exibido pelo servidor.");
        }
        return parts;
    }

    public static boolean save(Context context, String serverUrl, String token) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server_url", serverUrl.trim())
                .putString("token", token.trim())
                .commit();
    }
}
