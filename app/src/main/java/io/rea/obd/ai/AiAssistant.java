package io.rea.obd.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.ReadingGuide;
import io.rea.obd.ui.I18n;
import io.rea.obd.ui.LocalizedDialog;
import io.rea.obd.ui.UiKit;
import org.json.JSONArray;
import org.json.JSONObject;

/** A case-scoped, bounded chat about one saved reading. No access to adapter commands. */
public final class AiAssistant implements AutoCloseable {
    private static final String CONSENT_PREFS = "ai";
    private final Activity activity;
    private final DiagnosticCase diagnosticCase;
    private final AnalysisRequest source;
    private final AiConversation conversation;
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
    private AlertDialog dialog;
    private ScrollView scroll;
    private LinearLayout body;
    private EditText question;
    private boolean busy;
    private boolean closed;
    private String pending = "";
    private String error = "";
    private boolean keyProblem;

    private static String t(String pt, String en) { return I18n.english() ? en : pt; }

    public AiAssistant(Activity activity, DiagnosticCase diagnosticCase) throws Exception {
        this.activity = activity;
        this.diagnosticCase = diagnosticCase;
        source = AnalysisRequest.from(diagnosticCase);
        conversation = new AiConversation(activity, diagnosticCase.id, diagnosticCase.revision, source.refs());
    }

    public void show() {
        scroll = new ScrollView(activity);
        body = new UiKit(activity).column();
        int pad = new UiKit(activity).dp(16);
        body.setPadding(pad, pad / 2, pad, pad);
        scroll.addView(body);
        dialog = new LocalizedDialog(activity).setTitle(t("Conversar com a IA", "Talk to the AI"))
                .setView(scroll).setPositiveButton(t("Fechar", "Close"), null).create();
        dialog.setOnDismissListener(ignored -> close());
        render("");
        dialog.show();
    }

    private void render(String draft) {
        body.removeAllViews();
        UiKit ui = new UiKit(activity);
        AiSettings settings = AiSettings.load(activity);
        ui.label(body, (source.body.optBoolean("demonstration_data")
                ? t("Dados simulados · ", "Simulated data · ")
                : t("Leitura salva · ", "Saved reading · ")) + settings.provider.label
                + t(" · as respostas são hipóteses, não um diagnóstico confirmado.", " · replies are hypotheses, not a confirmed diagnosis."), 12, UiKit.MUTED, false);

        int answered = 0;
        java.util.Set<String> asked = new java.util.HashSet<>();
        try {
            JSONArray turns = conversation.turns();
            answered = turns.length();
            for (int i = 0; i < turns.length(); i++) {
                JSONObject turn = turns.getJSONObject(i);
                asked.add(turn.getString("question"));
                bubble(ui, turn.getString("question"), true);
                AnalysisResult result = AnalysisResult.parse(turn.getJSONObject("response"), source.refs());
                // The header already says the data is simulated; repeating it on every reply reads like a robot.
                String answer = i == 0 ? result.summary : result.summary.replaceFirst("^(Dados simulados|Simulated data):\\s*", "");
                LinearLayout reply = bubble(ui, answer, false);
                if (!result.safetyNote.isEmpty()) ui.label(reply, "⚠ " + result.safetyNote, 14, UiKit.AMBER, true);
                if (result.hasDetails()) {
                    LinearLayout detail = ui.disclosure(reply, t("Possíveis causas e o que verificar", "Possible causes and what to check"));
                    ui.label(detail, result.details(source.labels), 14, UiKit.TEXT, false);
                }
            }
        } catch (Exception e) { error = t("Não foi possível abrir a conversa anterior. Use \"Limpar conversa\" em Mais opções.",
                "Could not open the previous conversation. Use \"Clear conversation\" in More options."); }

        if (answered == 0 && !busy) {
            bubble(ui, t("Oi! Posso explicar os códigos desta leitura, dizer o que costuma causar cada um e o que perguntar na oficina. Toque numa sugestão ou escreva a sua pergunta.",
                    "Hi! I can explain the codes in this reading, what usually causes them and what to ask the workshop. Tap a suggestion or type your question."), false);
        }
        if (busy) {
            bubble(ui, pending, true);
            bubble(ui, t("Pensando…", "Thinking…"), false).setAlpha(0.7f);
        }
        if (!error.isEmpty()) {
            LinearLayout problem = bubble(ui, error, false);
            ((TextView) problem.getChildAt(0)).setTextColor(ui.palette.color(UiKit.AMBER));
            if (keyProblem) {
                ui.button(problem, t("Configurar a chave", "Set up the key"), true, this::openSetup);
            } else if (!pending.isEmpty()) {
                String retry = pending;
                ui.button(problem, t("Tentar de novo", "Try again"), true, () -> send(retry));
            }
        }

        if (!busy) {
            String[] suggestions = answered == 0 ? suggestions() : followUps();
            LinearLayout chips = ui.column();
            body.addView(chips);
            for (String suggestion : suggestions) {
                if (asked.contains(suggestion)) continue;
                Button chip = ui.button(chips, suggestion, false, () -> send(suggestion));
                chip.setTextSize(14);
                chip.setMinHeight(ui.dp(40));
                chip.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            }
        }

        question = ui.input(body, t("Sua pergunta", "Your question"), draft, "ai-question");
        question.setHint(t("Ex.: isso é grave? Posso continuar rodando?", "E.g. is this serious? Can I keep driving?"));
        question.setSingleLine(false);
        question.setMaxLines(4);
        question.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        question.setImeOptions(EditorInfo.IME_ACTION_SEND);
        question.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        question.setEnabled(!busy);
        ui.button(body, t("Enviar", "Send"), true, () -> send(question.getText().toString())).setEnabled(!busy);

        LinearLayout more = ui.disclosure(body, t("Mais opções", "More options"));
        ui.row(more, t("Usar minha IA · compartilhar pergunta", "Use my AI · share question"), this::share).setEnabled(!busy);
        ui.row(more, t("Conectar ou testar IA", "Connect or test AI"), this::openSetup).setEnabled(!busy);
        ui.row(more, t("Guia offline · sem IA", "Offline guide · no AI"), this::offline);
        ui.row(more, t("Ver os dados enviados à IA", "See the data sent to the AI"), this::showData);
        ui.row(more, t("Limpar esta conversa", "Clear this conversation"), () -> new LocalizedDialog(activity)
                .setMessage(t("Apagar as perguntas e respostas desta leitura?", "Delete the questions and replies for this reading?"))
                .setNegativeButton(t("Cancelar", "Cancel"), null).setPositiveButton(t("Limpar", "Clear"), (d, w) -> {
                    try { conversation.clear(); error = ""; pending = ""; render(""); } catch (Exception e) { error = I18n.t(e.getMessage()); render(""); }
                }).show()).setEnabled(!busy);
        if (answered > 0 || busy || !error.isEmpty()) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    /** A chat bubble: the user's on the right with the accent tint, the AI's on the left on a card. */
    private LinearLayout bubble(UiKit ui, String text, boolean mine) {
        LinearLayout box = ui.column();
        box.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(4));
        GradientDrawable shape = new GradientDrawable();
        float big = ui.dp(18), small = ui.dp(4);
        shape.setCornerRadii(mine ? new float[]{big, big, small, small, big, big, big, big}
                : new float[]{small, small, big, big, big, big, big, big});
        shape.setColor(mine ? (ui.palette.accent & 0x00FFFFFF) | 0x33000000 : ui.palette.card);
        shape.setStroke(ui.dp(1), mine ? (ui.palette.accent & 0x00FFFFFF) | 0x66000000 : ui.palette.line);
        box.setBackground(shape);
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextSize(15);
        label.setTextColor(ui.palette.text);
        label.setTypeface(Typeface.create(mine ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        label.setLineSpacing(ui.dp(2), 1.0f);
        label.setTextIsSelectable(!mine);
        label.setPadding(0, 0, 0, ui.dp(6));
        box.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = mine ? Gravity.END : Gravity.START;
        params.bottomMargin = ui.dp(10);
        if (mine) params.leftMargin = ui.dp(40); else params.rightMargin = ui.dp(24);
        body.addView(box, params);
        return box;
    }

    public static String[] suggestions() {
        return new String[]{t("O que estes códigos significam?", "What do these codes mean?"),
                t("Isso é grave? Posso continuar rodando?", "Is this serious? Can I keep driving?"),
                t("O que devo perguntar à oficina?", "What should I ask the workshop?")};
    }

    private static String[] followUps() {
        return new String[]{t("Qual a causa mais provável?", "What is the most likely cause?"),
                t("Quanto costuma custar para resolver?", "How much does it usually cost to fix?")};
    }

    private void send(String typed) {
        if (busy || closed) return;
        String draft = typed == null ? "" : typed.trim();
        if (draft.isEmpty()) {
            question.setError(t("Escreva uma pergunta ou toque numa sugestão.", "Type a question or tap a suggestion."));
            return;
        }
        if (!AiSettings.load(activity).configured()) {
            error = t("Para conversar, configure primeiro a sua chave de API (Gemini tem cota grátis).",
                    "To chat, first set up your API key (Gemini has a free quota).");
            keyProblem = true;
            pending = draft;
            render(draft);
            return;
        }
        final JSONObject request;
        try {
            request = conversation.request(source, draft);
        } catch (Exception e) {
            error = I18n.t(e.getMessage());
            keyProblem = false;
            render(draft);
            return;
        }
        if (consented()) {
            start(draft, request);
            return;
        }
        AiProvider provider = AiSettings.load(activity).provider;
        new LocalizedDialog(activity).setTitle(t("Antes da primeira pergunta", "Before your first question"))
                .setMessage(t("Para responder, o app envia ao " + provider.label + " os códigos e leituras desta investigação, a marca e o modelo do carro, os sintomas informados e a sua pergunta.\n\n"
                                + "Nome do veículo, placa, e-mails e telefones são removidos antes do envio. O filtro não detecta todos os nomes ou endereços, então evite escrevê-los na pergunta.",
                        "To answer, the app sends " + provider.label + " this investigation's codes and readings, the car's make and model, the reported symptoms and your question.\n\n"
                                + "The vehicle name, plate, e-mails and phone numbers are removed first. The filter does not catch every name or address, so avoid typing them."))
                .setNegativeButton(t("Cancelar", "Cancel"), null)
                .setNeutralButton(t("Ver dados", "See data"), (d, w) -> showData())
                .setPositiveButton(t("Entendi, enviar", "Got it, send"), (d, w) -> {
                    activity.getSharedPreferences(CONSENT_PREFS, Context.MODE_PRIVATE).edit().putBoolean("consent_" + provider.id, true).apply();
                    start(draft, request);
                }).show();
    }

    private boolean consented() {
        AiProvider provider = AiSettings.load(activity).provider;
        return activity.getSharedPreferences(CONSENT_PREFS, Context.MODE_PRIVATE).getBoolean("consent_" + provider.id, false);
    }

    private void start(String draft, JSONObject request) {
        if (busy || closed) return;
        busy = true;
        error = "";
        keyProblem = false;
        pending = request.optString("question", draft);
        render("");
        AiSettings settings = AiSettings.load(activity);
        worker.execute(() -> {
            String failure = "";
            boolean badKey = false;
            try {
                JSONObject response = AnalysisClient.analyze(settings, settings.key(), request);
                conversation.append(pending, response);
            } catch (Exception e) {
                String message = AnalysisClient.userMessage(e);
                badKey = message.equals(AnalysisClient.INVALID_KEY) || message.equals(AnalysisClient.NO_KEY) || message.equals(AnalysisClient.KEY_UNREADABLE);
                failure = I18n.t(message);
            }
            final String problem = failure;
            final boolean keyIssue = badKey;
            activity.runOnUiThread(() -> {
                busy = false;
                if (closed || activity.isDestroyed()) return;
                error = problem;
                keyProblem = keyIssue;
                if (problem.isEmpty()) pending = "";
                render("");
            });
        });
    }

    private void openSetup() {
        AiSetupDialog.show(activity, () -> {
            if (closed || activity.isDestroyed()) return;
            if (keyProblem && AiSettings.load(activity).configured()) { error = ""; keyProblem = false; }
            render(question == null ? "" : question.getText().toString());
        });
    }

    private void share() {
        try {
            String draft = question.getText().toString().trim();
            if (draft.isEmpty()) draft = suggestions()[0];
            final JSONObject request = conversation.request(source, draft);
            new LocalizedDialog(activity).setTitle(t("Revisar envio", "Review sharing"))
                    .setMessage(t("Confira o que será compartilhado com o app de IA que você escolher. O filtro não detecta todos os nomes ou endereços.\n\n",
                            "Review what will be shared with the AI app you choose. The filter does not detect every name or address.\n\n") + request.toString(2))
                    .setNegativeButton(t("Cancelar", "Cancel"), null)
                    .setPositiveButton(t("Compartilhar", "Share"), (d, w) -> {
                        Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, AiConversation.externalPrompt(request));
                        activity.startActivity(Intent.createChooser(intent, t("Escolher minha IA", "Choose my AI")));
                    }).show();
        } catch (Exception e) { error = I18n.t(e.getMessage()); keyProblem = false; render(question.getText().toString()); }
    }

    private void showData() {
        try {
            String draft = question == null ? "" : question.getText().toString().trim();
            JSONObject request = conversation.request(source, draft.isEmpty() ? suggestions()[0] : draft);
            new LocalizedDialog(activity).setTitle(t("Dados enviados à IA", "Data sent to the AI"))
                    .setMessage(request.toString(2)).setPositiveButton(t("Fechar", "Close"), null).show();
        } catch (Exception e) { error = I18n.t(e.getMessage()); keyProblem = false; render(""); }
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
