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
        show(activity, null);
    }

    /** {@code onClose} runs when the dialog closes, so the caller can pick up a newly saved key. */
    public static void show(Activity activity, Runnable onClose) {
        show(activity, AiSettings.load(activity).provider, onClose);
    }

    private static void show(Activity activity, AiProvider provider, Runnable onClose) {
        UiKit ui = new UiKit(activity);
        AiSettings saved = AiSettings.load(activity, provider);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = ui.column();
        body.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        scroll.addView(body);
        AlertDialog[] holder = new AlertDialog[1];
        boolean[] switching = new boolean[1];

        ui.label(body, t("Use a sua própria chave de API. O app envia o caso direto ao provedor, pela internet do celular. Não precisa de computador nem servidor.",
                "Use your own API key. The app sends the case straight to the provider over the phone's internet. No computer or server needed."), 14, UiKit.MUTED, false);
        ui.row(body, t("Provedor: ", "Provider: ") + provider.label, () -> new LocalizedDialog(activity)
                .setTitle(t("Escolha o provedor", "Choose the provider"))
                .setItems(new String[]{AiProvider.GEMINI.label, AiProvider.CLAUDE.label}, (d, which) -> {
                    switching[0] = true;
                    holder[0].dismiss();
                    show(activity, which == 0 ? AiProvider.GEMINI : AiProvider.CLAUDE, onClose);
                }).show());

        LinearLayout statusCard = ui.card(body, saved.configured());
        TextView keyState = ui.label(statusCard, "", 15, UiKit.TEXT, true);
        TextView status = ui.label(statusCard, "", 14, UiKit.MUTED, false);
        showKeyState(keyState, saved);
        status.setText(saved.configured()
                ? t("Toque em \"Testar chave\" para confirmar que ela funciona. O teste não gera texto e não gasta créditos.",
                        "Tap \"Test key\" to confirm it works. The test generates no text and uses no credits.")
                : t("Cole a chave abaixo e toque em \"Salvar e testar\".", "Paste the key below and tap \"Save and test\"."));

        ui.label(body, provider == AiProvider.GEMINI
                ? t("Crie a chave no Google AI Studio. Costuma haver uma cota gratuita; no plano gratuito o Google pode usar os dados enviados para melhorar os produtos dele.",
                        "Create the key in Google AI Studio. There is usually a free quota; on the free tier Google may use submitted data to improve its products.")
                : t("Crie a chave no Claude Console. A API usa créditos pré-pagos; uma assinatura do app Claude não vale como chave de API.",
                        "Create the key in the Claude Console. The API uses prepaid credits; a Claude app subscription is not an API key."), 13, UiKit.MUTED, false);
        ui.row(body, provider == AiProvider.GEMINI ? t("Criar chave no Google AI Studio", "Create a key in Google AI Studio")
                : t("Criar chave no Claude Console", "Create a key in the Claude Console"), () -> open(activity, provider.keyPage));

        EditText key = ui.input(body, saved.configured() ? t("Trocar a chave (opcional)", "Replace the key (optional)") : t("Chave de API", "API key"), "", "ai-api-key");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setHint(t("Cole a chave aqui", "Paste the key here"));

        LinearLayout advanced = ui.disclosure(body, t("Avançado", "Advanced"));
        EditText model = ui.input(advanced, t("Modelo (vazio usa ", "Model (empty uses ") + provider.defaultModel + ")", saved.model, "ai-model");
        EditText workspace = null;
        if (provider == AiProvider.CLAUDE) {
            workspace = ui.input(advanced, t("ID do workspace (só para chaves sem workspace)", "Workspace ID (only for keys without a workspace)"),
                    saved.workspace, "ai-workspace");
        }
        final EditText workspaceField = workspace;

        AlertDialog dialog = new LocalizedDialog(activity).setTitle(t("Conectar IA", "Connect AI")).setView(scroll)
                .setNegativeButton(t("Fechar", "Close"), null).create();
        holder[0] = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (!switching[0] && onClose != null) onClose.run();
        });

        Button test = ui.button(body, saved.configured() ? t("Testar chave", "Test key") : t("Salvar e testar", "Save and test"), true, () -> { });
        key.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                boolean typed = s.toString().trim().length() > 0 || !AiSettings.load(activity, provider).configured();
                test.setText(typed ? t("Salvar e testar", "Save and test") : t("Testar chave", "Test key"));
            }
        });
        test.setOnClickListener(view -> {
            AiSettings current;
            try {
                AiSettings.save(activity, provider, key.getText().toString(), model.getText().toString(),
                        workspaceField == null ? "" : workspaceField.getText().toString());
                current = AiSettings.load(activity, provider);
            } catch (IllegalArgumentException e) {
                status.setText("✗ " + I18n.t(e.getMessage()));
                status.setTextColor(ui.palette.color(UiKit.AMBER));
                return;
            }
            key.setText("");
            showKeyState(keyState, current);
            status.setText(t("Chave salva. Testando com o provedor…", "Key saved. Testing with the provider…"));
            status.setTextColor(ui.palette.muted);
            test.setEnabled(false);
            new Thread(() -> {
                String text;
                boolean ok;
                try {
                    String name = AnalysisClient.verify(current, current.key());
                    text = t("✓ Tudo certo! A chave funciona com o modelo ", "✓ All set! The key works with model ") + name
                            + t(". Feche esta janela e converse com a IA.", ". Close this window and talk to the AI.");
                    ok = true;
                } catch (Exception e) {
                    text = "✗ " + I18n.t(AnalysisClient.userMessage(e));
                    ok = false;
                }
                final String message = text;
                final boolean success = ok;
                activity.runOnUiThread(() -> {
                    if (!dialog.isShowing() || activity.isDestroyed()) return;
                    status.setText(message);
                    status.setTextColor(success ? ui.palette.accent : ui.palette.color(UiKit.AMBER));
                    test.setEnabled(true);
                    test.setText(t("Testar chave", "Test key"));
                });
            }, "ai-key-test").start();
        });
        ui.row(body, t("Remover a chave deste aparelho", "Remove the key from this device"), () -> {
            boolean removed = AiSettings.removeKey(activity, provider);
            showKeyState(keyState, AiSettings.load(activity, provider));
            test.setText(t("Salvar e testar", "Save and test"));
            status.setText(removed ? t("Chave removida.", "Key removed.") : t("Não foi possível remover.", "Could not remove."));
            status.setTextColor(ui.palette.muted);
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

    private static void showKeyState(TextView view, AiSettings settings) {
        view.setText(settings.configured()
                ? t("✓ Chave salva", "✓ Key saved") + (settings.keyHint().isEmpty() ? "" : t(" · termina em ", " · ends in ") + settings.keyHint())
                : t("Nenhuma chave salva ainda", "No key saved yet"));
    }

    private static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            new LocalizedDialog(activity).setMessage(url).setPositiveButton(t("Fechar", "Close"), null).show();
        }
    }
}
