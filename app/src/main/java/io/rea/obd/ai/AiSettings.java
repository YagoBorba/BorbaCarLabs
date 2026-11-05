package io.rea.obd.ai;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

/**
 * Provider, optional model and the user's own API key. The key is stored encrypted
 * (see {@link KeyVault}) and is only sent to the chosen provider.
 */
public final class AiSettings {
    private static final String PREFS = "ai";
    private static final Pattern MODEL = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,79}");
    private static final Pattern WORKSPACE = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final Pattern KEY = Pattern.compile("[\\x21-\\x7E]{20,300}");

    public final AiProvider provider;
    /** Model override; empty means the provider's default model. */
    public final String model;
    /** Claude only: workspace ID for keys that are not scoped to a workspace. */
    public final String workspace;
    private final String sealedKey;
    private final String keyHint;

    private AiSettings(AiProvider provider, String model, String workspace, String sealedKey, String keyHint) {
        this.provider = provider;
        this.model = model;
        this.workspace = workspace;
        this.sealedKey = sealedKey;
        this.keyHint = keyHint;
    }

    public static AiSettings load(Context context) {
        return load(context, null);
    }

    /** Settings for {@code provider}, or for the saved provider when null. */
    public static AiSettings load(Context context, AiProvider provider) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AiProvider chosen = provider != null ? provider : AiProvider.from(prefs.getString("provider", AiProvider.GEMINI.id));
        return new AiSettings(chosen,
                prefs.getString("model_" + chosen.id, ""),
                prefs.getString("workspace_" + chosen.id, ""),
                prefs.getString("key_" + chosen.id, ""),
                prefs.getString("key_hint_" + chosen.id, ""));
    }

    public boolean configured() {
        return !this.sealedKey.isEmpty();
    }

    public String modelOrDefault() {
        return this.model.isEmpty() ? this.provider.defaultModel : this.model;
    }

    /** Last characters of the saved key, for display only. */
    public String keyHint() {
        return this.keyHint;
    }

    /** Decrypts the saved key. Call off the main thread when possible. */
    public String key() throws AnalysisClient.Failure {
        if (this.sealedKey.isEmpty()) {
            throw new AnalysisClient.Failure(AnalysisClient.NO_KEY);
        }
        try {
            return KeyVault.open(this.sealedKey);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new AnalysisClient.Failure(AnalysisClient.KEY_UNREADABLE);
        }
    }

    public static boolean validKey(String key) {
        return key != null && KEY.matcher(key).matches();
    }

    public static boolean validModel(String model) {
        return model != null && (model.isEmpty() || MODEL.matcher(model).matches());
    }

    public static boolean validWorkspace(String workspace) {
        return workspace != null && (workspace.isEmpty() || WORKSPACE.matcher(workspace).matches());
    }

    /**
     * Saves the provider choice, model and workspace. A non-empty {@code key} replaces the saved key;
     * an empty one keeps it. Throws IllegalArgumentException with a user-facing message.
     */
    public static void save(Context context, AiProvider provider, String key, String model, String workspace) {
        String cleanKey = key == null ? "" : key.trim();
        String cleanModel = model == null ? "" : model.trim();
        String cleanWorkspace = workspace == null ? "" : workspace.trim();
        if (!cleanKey.isEmpty() && !validKey(cleanKey)) {
            throw new IllegalArgumentException("Chave de API inválida. Copie a chave inteira, sem espaços.");
        }
        if (!validModel(cleanModel)) {
            throw new IllegalArgumentException("Nome de modelo inválido. Deixe vazio para usar o padrão.");
        }
        if (!validWorkspace(cleanWorkspace)) {
            throw new IllegalArgumentException("ID de workspace inválido.");
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (cleanKey.isEmpty() && prefs.getString("key_" + provider.id, "").isEmpty()) {
            throw new IllegalArgumentException("Cole a chave de API do provedor escolhido.");
        }
        SharedPreferences.Editor editor = prefs.edit()
                .remove("server_url")
                .remove("token")
                .putString("provider", provider.id)
                .putString("model_" + provider.id, cleanModel)
                .putString("workspace_" + provider.id, cleanWorkspace);
        if (!cleanKey.isEmpty()) {
            try {
                editor.putString("key_" + provider.id, KeyVault.seal(cleanKey))
                        .putString("key_hint_" + provider.id, cleanKey.substring(cleanKey.length() - 4));
            } catch (GeneralSecurityException | RuntimeException e) {
                throw new IllegalArgumentException("Não foi possível proteger a chave neste aparelho.");
            }
        }
        if (!editor.commit()) {
            throw new IllegalArgumentException("Não foi possível salvar.");
        }
    }

    public static boolean removeKey(Context context, AiProvider provider) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("key_" + provider.id)
                .remove("key_hint_" + provider.id)
                .commit();
    }
}
