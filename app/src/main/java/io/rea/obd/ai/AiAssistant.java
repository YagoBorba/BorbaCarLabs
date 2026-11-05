package io.rea.obd.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.ReadingGuide;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.UiKit;
import org.json.JSONArray;
import org.json.JSONObject;

/** A case-scoped, bounded conversation. No access to adapter commands. */
public final class AiAssistant implements AutoCloseable {
    private final Activity activity;
    private final DiagnosticCase diagnosticCase;
    private final AnalysisRequest source;
    private final AiConversation conversation;
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
    private AlertDialog dialog;
    private LinearLayout body;
    private EditText question;
    private boolean busy;
    private boolean closed;
    private String error = "";

    private static String t(String pt, String en) { return I18n.english() ? en : pt; }

    public AiAssistant(Activity activity, DiagnosticCase diagnosticCase) throws Exception {
        this.activity = activity;
        this.diagnosticCase = diagnosticCase;
        source = AnalysisRequest.from(diagnosticCase);
        conversation = new AiConversation(activity, diagnosticCase.id, diagnosticCase.revision, source.refs());
    }

    public void show() {
        ScrollView scroll = new ScrollView(activity);
        body = new UiKit(activity).column();
        int pad = new UiKit(activity).dp(16);
        body.setPadding(pad, pad, pad, pad);
        scroll.addView(body);
        dialog = new LocalizedDialog(activity).setTitle(t("Assistente da investigação", "Case assistant"))
                .setView(scroll).setPositiveButton(t("Fechar", "Close"), null).create();
        dialog.setOnDismissListener(ignored -> close());
        render("");
        dialog.show();
    }

    private void render(String draft) {
        body.removeAllViews();
        UiKit ui = new UiKit(activity);
        ui.eyebrow(body, t("CASO · REVISÃO ", "CASE · REVISION ") + diagnosticCase.revision);
        ui.label(body, source.body.optBoolean("demonstration_data")
                ? t("Dados simulados. Nenhum carro está sendo consultado.", "Simulated data. No car is being queried.")
                : t("Conversa sobre esta captura salva; não acompanha o carro ao vivo.", "Conversation about this saved capture; it does not monitor the car live."), 13, UiKit.AMBER, false);
        ui.label(body, t("Pergunte sobre os códigos, os dados que faltam e como conversar com a oficina. As respostas ficam neste aparelho e não alteram o laudo.",
                "Ask about codes, missing data and what to discuss with a workshop. Replies stay on this device and do not change the report."), 14, UiKit.MUTED, false);
        try {
            JSONArray turns = conversation.turns();
            for (int i = 0; i < turns.length(); i++) {
                JSONObject turn = turns.getJSONObject(i);
                LinearLayout card = ui.card(body, false);
                ui.label(card, turn.getString("question"), 16, UiKit.TEXT, true);
                AnalysisResult result = AnalysisResult.parse(turn.getJSONObject("response"), source.refs());
                ui.label(card, result.model, 12, UiKit.ACCENT, true);
                ui.label(card, result.summary, 15, UiKit.TEXT, false);
                LinearLayout detail = ui.disclosure(card, t("Hipóteses, evidências e limites", "Hypotheses, evidence and limits"));
                ui.label(detail, result.display(source.labels), 14, UiKit.TEXT, false);
            }
        } catch (Exception e) { error = t("Não foi possível abrir a conversa.", "Could not open the conversation."); }
        if (!error.isEmpty()) ui.label(body, error, 14, UiKit.AMBER, false);
        if (busy) ui.label(body, t("A IA está preparando a resposta…", "The AI is preparing a reply…"), 15, UiKit.ACCENT, true);
        ui.row(body, t("Sugestões de perguntas", "Suggested questions"), () -> new LocalizedDialog(activity)
                .setTitle(t("Escolha uma pergunta", "Choose a question"))
                .setItems(suggestions(), (d, index) -> question.setText(suggestions()[index])).show()).setEnabled(!busy);
        question = ui.input(body, t("Sua pergunta", "Your question"), draft, "ai-question");
        question.setSingleLine(false);
        question.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        question.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        question.setEnabled(!busy);
        ui.button(body, t("Enviar para a IA", "Send to AI"), true, () -> prepare(false)).setEnabled(!busy);
        ui.button(body, t("Usar minha IA · compartilhar pergunta", "Use my AI · share question"), false, () -> prepare(true)).setEnabled(!busy);
        ui.row(body, t("Conectar ou testar IA", "Connect or test AI"), () -> AiSetupDialog.show(activity)).setEnabled(!busy);
        ui.row(body, t("Guia offline · sem IA", "Offline guide · no AI"), this::offline);
        ui.row(body, t("Limpar esta conversa", "Clear this conversation"), () -> new LocalizedDialog(activity)
                .setMessage(t("Apagar somente as perguntas e respostas desta revisão?", "Delete only the questions and replies for this revision?"))
                .setNegativeButton(t("Cancelar", "Cancel"), null).setPositiveButton(t("Limpar", "Clear"), (d, w) -> {
                    try { conversation.clear(); error = ""; render(""); } catch (Exception e) { error = I18n.t(e.getMessage()); render(""); }
                }).show()).setEnabled(!busy);
    }

    public static String[] suggestions() {
        return new String[]{t("O que estes códigos significam em linguagem simples?", "What do these codes mean in plain language?"),
                t("Quais informações faltam para investigar melhor?", "What information is missing for a better investigation?"),
                t("O que devo perguntar à oficina?", "What should I ask the workshop?"),
                t("O que esta leitura permite concluir e o que permanece incerto?", "What can this reading establish and what remains uncertain?")};
    }

    private void prepare(boolean external) {
        if (busy) return;
        try {
            String draft = question.getText().toString().trim();
            if (draft.isEmpty()) draft = suggestions()[0];
            final JSONObject request = conversation.request(source, draft);
            if (!external && !AiSettings.load(activity).configured()) { AiSetupDialog.show(activity); return; }
            String notice = t("Confira exatamente o que será compartilhado. O filtro não detecta todos os nomes ou endereços. A resposta é uma hipótese, não diagnóstico confirmado.\n\n",
                    "Review exactly what will be shared. The filter does not detect every name or address. Replies are hypotheses, not confirmed diagnoses.\n\n");
            new LocalizedDialog(activity).setTitle(t("Revisar envio", "Review sharing"))
                    .setMessage(notice + request.toString(2)).setNegativeButton(t("Cancelar", "Cancel"), null)
                    .setPositiveButton(external ? t("Compartilhar", "Share") : t("Enviar", "Send"), (d, w) -> {
                        if (external) {
                            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, AiConversation.externalPrompt(request));
                            activity.startActivity(Intent.createChooser(intent, t("Escolher minha IA", "Choose my AI")));
                        } else send(request);
                    }).show();
        } catch (Exception e) { error = I18n.t(e.getMessage()); render(question.getText().toString()); }
    }

    private void send(JSONObject request) {
        if (busy || closed) return;
        busy = true;
        error = "";
        String draft = request.optString("question");
        render(draft);
        AiSettings settings = AiSettings.load(activity);
        worker.execute(() -> {
            String failure = "";
            try {
                JSONObject response = AnalysisClient.analyze(settings, settings.key(), request);
                conversation.append(draft, response);
            } catch (Exception e) { failure = I18n.t(AnalysisClient.userMessage(e)); }
            final String problem = failure;
            activity.runOnUiThread(() -> {
                busy = false;
                if (closed || activity.isDestroyed()) return;
                error = problem;
                render(problem.isEmpty() ? "" : draft);
            });
        });
    }

    private void offline() {
        StringBuilder text = new StringBuilder(t("Guia por regras e catálogo local, sem modelo de IA.\n\n", "Rules and local catalog, without an AI model.\n\n"));
        for (ReadingGuide.Code code : ReadingGuide.codes(diagnosticCase.dtc)) {
            text.append(code.code).append(": ").append(I18n.t(code.meaning == null ? ReadingGuide.NO_MEANING : code.meaning)).append("\n\n");
        }
        text.append(I18n.t(ReadingGuide.SCOPE));
        new LocalizedDialog(activity).setTitle(t("Guia offline · sem IA", "Offline guide · no AI")).setMessage(text)
                .setPositiveButton(t("Fechar", "Close"), null).show();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        worker.shutdownNow();
    }
}
