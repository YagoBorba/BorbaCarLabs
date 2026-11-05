package io.rea.obd.ai;

/** AI providers the app can call directly with the user's own API key. */
public enum AiProvider {
    GEMINI("gemini", "Gemini (Google)", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "https://aistudio.google.com/apikey"),
    CLAUDE("claude", "Claude (Anthropic)", "claude-sonnet-5-5", "claude-haiku-5-5", "https://platform.claude.com/settings/keys");

    public final String id;
    public final String label;
    public final String defaultModel;
    /** Used when the default model is overloaded and the user did not pick a model. */
    public final String fallbackModel;
    public final String keyPage;

    AiProvider(String id, String label, String defaultModel, String fallbackModel, String keyPage) {
        this.id = id;
        this.label = label;
        this.defaultModel = defaultModel;
        this.fallbackModel = fallbackModel;
        this.keyPage = keyPage;
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
