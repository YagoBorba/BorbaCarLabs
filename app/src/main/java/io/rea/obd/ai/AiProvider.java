package io.rea.obd.ai;

/** AI providers the app can call directly with the user's own API key. */
public enum AiProvider {
    GEMINI("gemini", "Gemini (Google)", "gemini-3.5-flash-lite", "https://aistudio.google.com/apikey",
            "gemini-3.1-flash-lite", "gemini-3-flash-preview"),
    CLAUDE("claude", "Claude (Anthropic)", "claude-sonnet-5-5", "https://platform.claude.com/settings/keys",
            "claude-haiku-5-5");

    public final String id;
    public final String label;
    public final String defaultModel;
    public final String keyPage;
    /**
     * Tried in order when the default model is busy, slow or out of free quota and the user did not pick
     * a model. Free-tier limits are per model, so another model usually answers right away.
     */
    public final String[] fallbackModels;

    AiProvider(String id, String label, String defaultModel, String keyPage, String... fallbackModels) {
        this.id = id;
        this.label = label;
        this.defaultModel = defaultModel;
        this.keyPage = keyPage;
        this.fallbackModels = fallbackModels;
    }

    public static AiProvider from(String id) {
        for (AiProvider provider : values()) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return GEMINI;
    }
}
