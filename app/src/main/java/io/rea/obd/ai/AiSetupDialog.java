package io.rea.obd.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.UiKit;

public final class AiSetupDialog {
    private AiSetupDialog() {}
    private static String t(String pt, String en) { return I18n.english() ? en : pt; }

    public static void show(Activity activity) {
        UiKit ui = new UiKit(activity);
        LinearLayout body = ui.column();
        body.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        ui.label(body, t("No computador: npm run setup e npm start. Depois cole o convite exibido. A chave da IA fica no computador.",
                "On your computer: npm run setup and npm start. Then paste the displayed invitation. The AI key stays on the computer."), 14, UiKit.MUTED, false);
        EditText invite = ui.input(body, t("Convite do servidor", "Server invitation"), "", "ai-invitation");
        invite.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AiSettings saved = AiSettings.load(activity);
        LinearLayout advanced = ui.disclosure(body, t("Configuração manual", "Manual setup"));
        EditText address = ui.input(advanced, t("Endereço", "Address"), saved.serverUrl, "ai-server-url");
        EditText token = ui.input(advanced, t("Token de acesso", "Access token"), saved.token, "ai-token");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView status = ui.label(body, t("Teste a conexão antes da apresentação. O teste não chama a IA.",
                "Test the connection before presenting. This test does not call the AI."), 13, UiKit.MUTED, false);
        AlertDialog dialog = new LocalizedDialog(activity).setTitle(t("Conectar IA", "Connect AI")).setView(body)
                .setNegativeButton(t("Fechar", "Close"), null).create();
        ui.button(body, t("Salvar e testar conexão", "Save and test connection"), true, () -> {
            try {
                String[] values = invite.getText().toString().trim().isEmpty()
                        ? new String[]{address.getText().toString().trim(), token.getText().toString().trim()}
                        : AiSettings.invitation(invite.getText().toString());
                if (!AiSettings.valid(values[0], values[1])) throw new IllegalArgumentException(t("Confira o convite ou endereço e token.", "Check the invitation or address and token."));
                if (!AiSettings.save(activity, values[0], values[1])) throw new IllegalStateException(t("Não foi possível salvar.", "Could not save."));
                status.setText(t("Testando…", "Testing…"));
                new Thread(() -> {
                    String text;
                    try {
                        org.json.JSONObject result = AnalysisClient.status(values[0], values[1]);
                        if (!result.optBoolean("ok")) throw new java.io.IOException();
                        text = t("Servidor conectado: ", "Server connected: ") + result.optString("model")
                                + t(". O provedor será consultado quando você enviar uma pergunta.", ". The provider is contacted when you send a question.");
                    } catch (Exception e) { text = I18n.t(AnalysisClient.userMessage(e)); }
                    final String message = text;
                    activity.runOnUiThread(() -> { if (dialog.isShowing() && !activity.isDestroyed()) status.setText(message); });
                }, "ai-connection-test").start();
            } catch (Exception e) { status.setText(I18n.t(e.getMessage())); }
        });
        ui.row(body, t("Apagar todas as conversas locais", "Delete all local conversations"), () ->
                new LocalizedDialog(activity).setMessage(t("Apagar as conversas com IA deste aparelho? Os casos e relatórios permanecem.",
                        "Delete the AI conversations on this device? Cases and reports are preserved."))
                    .setNegativeButton(t("Cancelar", "Cancel"), null).setPositiveButton(t("Apagar", "Delete"), (d, w) -> {
                        boolean deleted = activity.getSharedPreferences("ai_conversations", android.content.Context.MODE_PRIVATE).edit().clear().commit();
                        status.setText(deleted ? t("Conversas apagadas.", "Conversations deleted.") : t("Não foi possível apagar.", "Could not delete."));
                    }).show());
        dialog.show();
    }
}
