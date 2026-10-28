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
        return this.serverUrl.matches("https?://\\S+") && this.token.length() >= 16;
    }

    public static boolean save(Context context, String serverUrl, String token) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server_url", serverUrl.trim())
                .putString("token", token.trim())
                .commit();
    }
}
