package io.rea.obd.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.UiKit;

/** Bring-your-own-key setup: provider, API key, optional model, and a free key check. */
public final class AiSetupDialog {
    private AiSetupDialog() {}
    private static String t(String pt, String en) { return I18n.english() ? en : pt; }

    public static void show(Activity activity) {
        show(activity, AiSettings.load(activity).provider);
    }

    private static void show(Activity activity, AiProvider provider) {
        UiKit ui = new UiKit(activity);
        AiSettings saved = AiSettings.load(activity, provider);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = ui.column();
        body.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        scroll.addView(body);
        AlertDialog[] holder = new AlertDialog[1];

        ui.label(body, t("Use a sua própria chave de API. O app envia o caso que você revisar direto ao provedor, pela internet do celular. Não precisa de computador nem servidor.",
                "Use your own API key. The app sends the case you review straight to the provider over the phone's internet. No computer or server needed."), 14, UiKit.MUTED, false);
        ui.row(body, t("Provedor: ", "Provider: ") + provider.label, () -> new LocalizedDialog(activity)
                .setTitle(t("Escolha o provedor", "Choose the provider"))
                .setItems(new String[]{AiProvider.GEMINI.label, AiProvider.CLAUDE.label}, (d, which) -> {
                    holder[0].dismiss();
                    show(activity, which == 0 ? AiProvider.GEMINI : AiProvider.CLAUDE);
                }).show());
        ui.label(body, provider == AiProvider.GEMINI
                ? t("Crie a chave no Google AI Studio. Costuma haver uma cota gratuita; no plano gratuito o Google pode usar os dados enviados para melhorar os produtos dele.",
                        "Create the key in Google AI Studio. There is usually a free quota; on the free tier Google may use submitted data to improve its products.")
                : t("Crie a chave no Claude Console. A API usa créditos pré-pagos; uma assinatura do app Claude não vale como chave de API.",
                        "Create the key in the Claude Console. The API uses prepaid credits; a Claude app subscription is not an API key."), 13, UiKit.MUTED, false);
        ui.row(body, provider == AiProvider.GEMINI ? t("Criar chave no Google AI Studio", "Create a key in Google AI Studio")
                : t("Criar chave no Claude Console", "Create a key in the Claude Console"), () -> open(activity, provider.keyPage));

        EditText key = ui.input(body, t("Chave de API", "API key"), "", "ai-api-key");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setHint(saved.configured()
                ? t("Chave salva terminando em ", "Saved key ending in ") + saved.keyHint() + t(" · vazio mantém", " · leave empty to keep")
                : t("Cole a chave aqui", "Paste the key here"));

        LinearLayout advanced = ui.disclosure(body, t("Avançado", "Advanced"));
        EditText model = ui.input(advanced, t("Modelo (vazio usa ", "Model (empty uses ") + provider.defaultModel + ")", saved.model, "ai-model");
        EditText workspace = null;
        if (provider == AiProvider.CLAUDE) {
            workspace = ui.input(advanced, t("ID do workspace (só para chaves sem workspace)", "Workspace ID (only for keys without a workspace)"),
                    saved.workspace, "ai-workspace");
        }
        final EditText workspaceField = workspace;

        TextView status = ui.label(body, t("O teste confere a chave e o modelo sem gerar texto, então não consome créditos.",
                "The test checks the key and model without generating text, so it uses no credits."), 13, UiKit.MUTED, false);
        AlertDialog dialog = new LocalizedDialog(activity).setTitle(t("Conectar IA", "Connect AI")).setView(scroll)
                .setNegativeButton(t("Fechar", "Close"), null).create();
        holder[0] = dialog;

        Button test = ui.button(body, t("Salvar e testar chave", "Save and test key"), true, () -> { });
        test.setOnClickListener(view -> {
            try {
                AiSettings.save(activity, provider, key.getText().toString(), model.getText().toString(),
                        workspaceField == null ? "" : workspaceField.getText().toString());
                key.setText("");
                AiSettings current = AiSettings.load(activity, provider);
                key.setHint(t("Chave salva terminando em ", "Saved key ending in ") + current.keyHint() + t(" · vazio mantém", " · leave empty to keep"));
                status.setText(t("Testando…", "Testing…"));
                test.setEnabled(false);
                new Thread(() -> {
                    String text;
                    try {
                        String name = AnalysisClient.verify(current, current.key());
                        text = t("Chave aceita pelo provedor · modelo ", "Key accepted by the provider · model ") + name
                                + t(". Pode conversar com a IA nas investigações.", ". You can now talk to the AI from an investigation.");
                    } catch (Exception e) { text = I18n.t(AnalysisClient.userMessage(e)); }
                    final String message = text;
                    activity.runOnUiThread(() -> {
                        if (!dialog.isShowing() || activity.isDestroyed()) return;
                        status.setText(message);
                        test.setEnabled(true);
                    });
                }, "ai-key-test").start();
            } catch (IllegalArgumentException e) { status.setText(I18n.t(e.getMessage())); }
        });
        ui.row(body, t("Remover a chave deste aparelho", "Remove the key from this device"), () -> {
            boolean removed = AiSettings.removeKey(activity, provider);
            key.setHint(t("Cole a chave aqui", "Paste the key here"));
            status.setText(removed ? t("Chave removida.", "Key removed.") : t("Não foi possível remover.", "Could not remove."));
        });
        ui.row(body, t("Apagar todas as conversas locais", "Delete all local conversations"), () ->
                new LocalizedDialog(activity).setMessage(t("Apagar as conversas com IA deste aparelho? Os casos e relatórios permanecem.",
                        "Delete the AI conversations on this device? Cases and reports are preserved."))
                    .setNegativeButton(t("Cancelar", "Cancel"), null).setPositiveButton(t("Apagar", "Delete"), (d, w) -> {
                        boolean deleted = activity.getSharedPreferences("ai_conversations", android.content.Context.MODE_PRIVATE).edit().clear().commit();
                        status.setText(deleted ? t("Conversas apagadas.", "Conversations deleted.") : t("Não foi possível apagar.", "Could not delete."));
                    }).show());
        ui.label(body, t("A chave fica criptografada neste celular e só é enviada ao provedor escolhido. O uso pode gerar custos na sua conta do provedor.",
                "The key is encrypted on this phone and only sent to the chosen provider. Usage may be billed to your provider account."), 12, UiKit.MUTED, false);
        dialog.show();
    }

    private static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            new LocalizedDialog(activity).setMessage(url).setPositiveButton(t("Fechar", "Close"), null).show();
        }
    }
}
