package io.rea.obd.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import io.rea.obd.diagnostics.DtcFormatter;
import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.model.DataOrigin;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DiagnosisScreen {
    private DiagnosisScreen() {
    }

    public static final class Headline {
        public final String title;
        public final String detail;
        public final boolean clean;
        public final boolean hasCodes;
        public final boolean milOn;

        Headline(DtcReadResult read) {
            Set<String> codes = new LinkedHashSet<>();
            for (DtcObservation observation : read.observations()) {
                codes.add(observation.code);
            }
            boolean mil = false;
            for (DtcReadResult.MilObservation observation : read.mil.observations) {
                mil |= observation.illuminated;
            }
            this.milOn = mil;
            this.hasCodes = !codes.isEmpty();
            this.clean = codes.isEmpty() && !read.incomplete();
            if (!codes.isEmpty()) {
                this.title = Format.count(codes.size(), "código de falha encontrado", "códigos de falha encontrados");
                this.detail = String.join(" · ", codes);
            } else if (read.incomplete()) {
                this.title = "Leitura incompleta";
                this.detail = "Nem todas as consultas responderam. Veja os detalhes ou leia novamente.";
            } else {
                this.title = "Nenhum código de falha";
                this.detail = "A ECU respondeu a todas as consultas sem registrar códigos.";
            }
        }
    }

    public static Headline headline(DtcReadResult read) {
        return new Headline(read);
    }

    public static LinearLayout verdict(LinearLayout parent, DtcReadResult read, boolean compact) {
        UiKit ui = new UiKit(parent.getContext());
        Headline headline = headline(read);
        boolean clean = headline.clean;
        boolean milOn = headline.milOn;
        String title = headline.title;
        String detail = headline.detail;

        LinearLayout card = ui.card(parent, !clean);
        LinearLayout header = new LinearLayout(parent.getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(badge(ui, clean), new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)));
        LinearLayout heading = ui.column();
        heading.setPadding(ui.dp(14), 0, 0, 0);
        header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        String when = (read.origin == DataOrigin.SIMULATED ? "DEMONSTRAÇÃO · " : "LEITURA ") + Format.when(read.completedAtEpochMillis);
        ui.eyebrow(heading, when.toUpperCase(java.util.Locale.ROOT));
        ui.label(heading, title, compact ? 19 : 22, UiKit.TEXT, true).setPadding(0, 0, 0, 0);

        ui.label(card, detail, 15, headline.hasCodes ? UiKit.TEXT : UiKit.MUTED, headline.hasCodes).setPadding(0, ui.dp(12), 0, ui.dp(4));
        if (milOn) {
            ui.label(card, "Luz de injeção (MIL) acesa no painel", 14, UiKit.AMBER, true);
        }
        if (headline.hasCodes && read.incomplete()) {
            ui.label(card, "Algumas consultas não terminaram; pode haver outros códigos.", 13, UiKit.AMBER, false);
        }
        if (!compact) {
            ui.label(card, "Cobre o diagnóstico de emissões (OBD2). Módulos como ABS e airbag não são lidos.", 12, UiKit.MUTED, false);
        }
        return card;
    }

    private static TextView badge(UiKit ui, boolean clean) {
        TextView badge = new TextView(ui.context);
        badge.setText(clean ? "✓" : "!");
        badge.setTextSize(22.0f);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        int tone = clean ? ui.palette.accent : ui.palette.attention;
        badge.setTextColor(tone);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor((tone & 0x00FFFFFF) | 0x26000000);
        badge.setBackground(circle);
        badge.setImportantForAccessibility(TextView.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return badge;
    }

    public static void shortcuts(LinearLayout linearLayout, Runnable runnable, Runnable runnable2) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        LinearLayout column = uiKit.column();
        boolean sideBySide = linearLayout.getResources().getConfiguration().fontScale <= 1.3f;
        column.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        linearLayout.addView(column, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button button = uiKit.button(column, "Ao vivo", false, runnable);
        Button button2 = uiKit.button(column, "Investigações e histórico", false, runnable2);
        if (sideBySide) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            layoutParams.setMargins(0, 0, uiKit.dp(8), uiKit.dp(16));
            button.setLayoutParams(layoutParams);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            layoutParams2.bottomMargin = uiKit.dp(16);
            button2.setLayoutParams(layoutParams2);
        }
    }

    public static void progress(LinearLayout linearLayout, int i, String guidedStatus) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        uiKit.label(linearLayout, guidedStatus, 14, UiKit.TEXT, true);
        if (i < 2) {
            ProgressBar progressBar = new ProgressBar(linearLayout.getContext(), null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setIndeterminate(false);
            progressBar.setMax(2);
            progressBar.setProgress(i);
            progressBar.setProgressTintList(ColorStateList.valueOf(uiKit.palette.accent));
            progressBar.setContentDescription(i + " de 2 grupos de consultas encerrados");
            linearLayout.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, uiKit.dp(8)));
        }
    }

    public static void results(LinearLayout linearLayout, DtcReadResult dtcReadResult) {
        if (dtcReadResult == null) {
            return;
        }
        UiKit uiKit = new UiKit(linearLayout.getContext());
        uiKit.section(linearLayout, "Resultado por serviço", "Uma resposta sem códigos não exclui outros problemas no veículo.");
        for (DtcReadResult.ServiceResult serviceResult : dtcReadResult.services) {
            LinearLayout card = uiKit.card(linearLayout, false);
            uiKit.eyebrow(card, DtcFormatter.category(serviceResult.category));
            uiKit.label(card, (serviceResult.status.outcome != DtcReadResult.Outcome.POSITIVE && serviceResult.observations.isEmpty()) ? "—" : Integer.toString(serviceResult.observations.size()), 28, UiKit.TEXT, true);
            uiKit.label(card, DtcFormatter.outcome(serviceResult.status.outcome), 14, UiKit.MUTED, false);
            uiKit.label(card, serviceResult.status.completedAtEpochMillis == null ? "Sem horário de leitura" : "Lida " + Format.when(serviceResult.status.completedAtEpochMillis.longValue()), 12, UiKit.MUTED, false);
        }
    }
}