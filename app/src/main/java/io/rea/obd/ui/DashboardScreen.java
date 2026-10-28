package io.rea.obd.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.ChartView;
import io.rea.obd.R;
import io.rea.obd.telemetry.DashboardState;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.vehicle.VehicleProfile;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DashboardScreen {
    public interface Actions {
        void connection();

        void demo();

        void export();

        void garage();

        void recording();

        void sensors();
    }

    public static final class Views {
        public ChartView chart;
        public TextView chartContext;
        public TextView chartRange;
        public TextView chartSelection;
        public TextView coverage;
        private DashboardState lastState;
        public TextView recordingInfo;
        public TextView voltage;
        public TextView voltageInfo;
        public final Map<Integer, TextView> values = new LinkedHashMap();
        public final Map<Integer, TextView> qualities = new LinkedHashMap();
        private final Map<Integer, TextView> origins = new HashMap();
        private final Map<Integer, TextView> times = new HashMap();
        private final Map<Integer, TextView> details = new HashMap();

        public void bind(DashboardState dashboardState, List<ChartView.Point> list, String text3, String rpmHistorySessionId, String voltage, String text, String text5) {
            String string;
            this.lastState = dashboardState;
            for (Map.Entry<Integer, TextView> entry : this.values.entrySet()) {
                DashboardState.Reading reading = entry.getKey().intValue() == 12 ? dashboardState.rpm : dashboardState.readings.get(entry.getKey());
                entry.getValue().setText(reading.value);
                this.qualities.get(entry.getKey()).setText(reading.quality);
                this.origins.get(entry.getKey()).setText(reading.origin);
                this.origins.get(entry.getKey()).setVisibility(reading.origin.equals("Dado simulado") ? View.GONE : View.VISIBLE);
                this.times.get(entry.getKey()).setText(reading.time);
                entry.getValue().setContentDescription(reading.sensor.name + ": " + reading.value + " " + reading.sensor.unit + ". " + reading.quality + ". " + reading.origin);
                this.details.get(entry.getKey()).setContentDescription("Abrir detalhes de " + reading.sensor.name + ". " + reading.value + " " + reading.sensor.unit + ". " + reading.quality + ". " + reading.origin + ". " + reading.time);
            }
            this.coverage.setText(dashboardState.coverage);
            this.voltage.setText(voltage);
            this.voltageInfo.setText(text);
            this.recordingInfo.setText(dashboardState.recording + "\n" + text5);
            boolean flag = dashboardState.collecting && dashboardState.rpm.selected && dashboardState.rpm.status == TelemetrySample.Status.VALID;
            TextView textView = this.chartContext;
            if (list.isEmpty()) {
                string = "SEM AMOSTRAS · aguardando leituras";
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(flag ? "AMOSTRAS DA SESSÃO" : "HISTÓRICO DA SESSÃO · sem atualização atual");
                builder.append(" · ");
                builder.append(text3);
                string = builder.toString();
            }
            textView.setText(string);
            this.chart.data(list, "rpm");
            TextView chartRange = this.chartRange;
            StringBuilder builder2 = new StringBuilder();
            builder2.append(this.chart.summary());
            builder2.append(rpmHistorySessionId.isEmpty() ? "" : "\nSessão de aquisição: " + rpmHistorySessionId);
            chartRange.setText(builder2.toString());
        }
    }

    public static Views render(LinearLayout linearLayout, VehicleProfile vehicleProfile, DashboardState dashboardState, String status, boolean busy, boolean enabled, String name, final Actions actions) {
        String text;
        String string;
        String text2;
        String text3;
        boolean flag;
        LinearLayout card;
        UiKit uiKit = new UiKit(linearLayout.getContext());
        final Views views = new Views();
        uiKit.label(linearLayout, "DADOS E CONTEXTO", 12, UiKit.ACCENT, true);
        uiKit.label(linearLayout, "Ao vivo", 28, UiKit.TEXT, true);
        if (vehicleProfile == null) {
            string = "Adicione um perfil na Garagem para associar suas gravações.";
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(vehicleProfile.name);
            if (vehicleProfile.description().isEmpty()) {
                text = "";
            } else {
                text = " · " + vehicleProfile.description();
            }
            builder.append(text);
            string = builder.toString();
        }
        uiKit.label(linearLayout, string, 16, UiKit.TEXT, true);
        boolean flag2 = false;
        LinearLayout card2 = uiKit.card(linearLayout, false);
        uiKit.label(card2, dashboardState.mode == DashboardState.Mode.DEMONSTRATION ? "CENÁRIO LOCAL" : "CONEXÃO", 12, dashboardState.mode == DashboardState.Mode.DEMONSTRATION ? UiKit.AMBER : UiKit.ACCENT, true);
        uiKit.label(card2, status, 16, UiKit.TEXT, true);
        DashboardState.Mode mode = dashboardState.mode;
        if (busy) {
            text2 = mode == DashboardState.Mode.CONNECTING ? "Conectando…" : "Operação em andamento…";
        } else {
            text2 = (mode == DashboardState.Mode.CONNECTED || dashboardState.mode == DashboardState.Mode.DEMONSTRATION) ? "Desconectar" : "Conectar adaptador";
        }
        Objects.requireNonNull(actions);
        uiKit.button(card2, text2, true, () -> actions.connection()).setEnabled(!busy);
        boolean flag3 = dashboardState.mode == DashboardState.Mode.CONNECTED || dashboardState.mode == DashboardState.Mode.DEMONSTRATION;
        if (flag3) {
            linearLayout.removeView(card2);
            uiKit.label(linearLayout, status, 13, UiKit.MUTED, false);
        }
        views.coverage = uiKit.label(linearLayout, "", 14, UiKit.MUTED, false);
        LinearLayout card3 = uiKit.card(linearLayout, true);
        sensor(card3, dashboardState.rpm, views, uiKit, true);
        views.chartContext = uiKit.label(card3, "", 12, UiKit.AMBER, true);
        views.chart = new ChartView(linearLayout.getContext());
        views.chart.setTag("rpm-chart");
        float fontScale = linearLayout.getResources().getConfiguration().fontScale;
        card3.addView(views.chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, uiKit.dp(fontScale > 1.3f ? 210 : 175)));
        views.chartRange = uiKit.label(card3, "", 12, UiKit.MUTED, false);
        views.chartSelection = uiKit.label(card3, "Toque na curva ou use os controles para explorar as capturas.", 13, UiKit.TEXT, false);
        views.chart.setSelectionListener(new ChartView.SelectionListener() {
            @Override
            public final void selected(ChartView.Point point) {
                DashboardScreen.showChartSelection(views, point);
            }
        });
        uiKit.button(card3, "Amostra anterior", false, () -> views.chart.moveSelection(-1));
        uiKit.button(card3, "Próxima amostra", false, () -> views.chart.moveSelection(1));
        uiKit.label(linearLayout, "SENSORES SELECIONADOS", 12, UiKit.ACCENT, true);
        boolean flag4 = linearLayout.getResources().getConfiguration().screenWidthDp >= 380 && fontScale <= 1.3f;
        Iterator<DashboardState.Reading> it = dashboardState.readings.values().iterator();
        LinearLayout column = null;
        int i = 0;
        while (it.hasNext()) {
            DashboardState.Reading next = it.next();
            if (next.sensor.pid != 12) {
                if (flag4) {
                    int value = i % 2;
                    if (value == 0) {
                        column = uiKit.column();
                        column.setOrientation(flag2 ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                        linearLayout.addView(column, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    LinearLayout card4 = uiKit.card(column, false);
                    LinearLayout column2 = column;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    layoutParams.bottomMargin = uiKit.dp(16);
                    if (value == 0) {
                        layoutParams.rightMargin = uiKit.dp(10);
                    }
                    card4.setLayoutParams(layoutParams);
                    card = card4;
                    column = column2;
                    flag = false;
                } else {
                    flag4 = flag4;
                    flag = flag2;
                    card = uiKit.card(linearLayout, flag);
                }
                sensor(card, next, views, uiKit, flag);
                i++;
            } else {
                flag4 = flag4;
                it = it;
            }
            flag4 = flag4;
            it = it;
            flag2 = false;
        }
        if (i == 0) {
            uiKit.label(linearLayout, "Nenhum sensor complementar selecionado. Use Selecionar sensores para mudar o lote.", 14, UiKit.MUTED, false);
        }
        LinearLayout card5 = uiKit.card(linearLayout, false);
        uiKit.label(card5, "TENSÃO NO ADAPTADOR", 12, UiKit.MUTED, true);
        views.voltage = uiKit.label(card5, "—", 28, UiKit.TEXT, true);
        views.voltageInfo = uiKit.label(card5, "", 13, UiKit.MUTED, false);
        uiKit.label(card5, "Alimentação do adaptador, não um teste de saúde da bateria. Não confundir com o PID de tensão do módulo.", 13, UiKit.MUTED, false);
        if (flag3) {
            linearLayout.addView(card2);
        }
        LinearLayout card6 = uiKit.card(linearLayout, false);
        uiKit.label(card6, "COLETA E GRAVAÇÃO", 12, UiKit.ACCENT, true);
        views.recordingInfo = uiKit.label(card6, "", 13, UiKit.MUTED, false);
        uiKit.label(card6, "A aquisição pausa fora deste painel ou do primeiro plano. Cada sinal conserva seu próprio horário; ausência nunca vira zero.", 14, UiKit.MUTED, false);
        Objects.requireNonNull(actions);
        uiKit.button(card6, "Selecionar sensores", false, () -> actions.sensors());
        boolean recordingOpen = dashboardState.recordingOpen;
        String text4 = recordingOpen ? "Parar gravação CSV" : "Iniciar gravação CSV";
        Objects.requireNonNull(actions);
        uiKit.button(card6, text4, true, () -> actions.recording()).setEnabled(recordingOpen || dashboardState.mode == DashboardState.Mode.CONNECTED || dashboardState.mode == DashboardState.Mode.DEMONSTRATION);
        if (enabled) {
            text3 = "Último CSV: " + name + "\nO arquivo mantém seu contexto original e pode pertencer a outro perfil.";
        } else {
            text3 = "Nenhum CSV anterior disponível.";
        }
        uiKit.label(card6, text3, 13, UiKit.MUTED, false);
        Objects.requireNonNull(actions);
        uiKit.button(card6, "Exportar último CSV", false, () -> actions.export()).setEnabled(enabled);
        return views;
    }

    static void showChartSelection(Views views, ChartView.Point point) {
        String text;
        TextView textView = views.chartSelection;
        if (point == null) {
            text = "Nenhuma captura selecionada.";
        } else {
            text = Format.when(point.time) + " · " + String.format(Locale.getDefault(), "%.0f rpm", Double.valueOf(point.value)) + " · amostra histórica selecionada";
        }
        textView.setText(text);
    }

    private static void sensor(final LinearLayout linearLayout, final DashboardState.Reading reading, final Views views, final UiKit uiKit, boolean flag) {
        final int i = reading.sensor.pid;
        TextView label = uiKit.label(linearLayout, reading.sensor.name, flag ? 18 : 15, UiKit.TEXT, true);
        label.setTag("sensor-details-" + i);
        label.setMinHeight(uiKit.dp(48));
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setFocusable(true);
        Drawable mutate = linearLayout.getContext().getDrawable(R.drawable.ic_info).mutate();
        mutate.setTint(uiKit.palette.accent);
        mutate.setBounds(0, 0, uiKit.dp(20), uiKit.dp(20));
        label.setCompoundDrawablesRelative(null, null, mutate, null);
        label.setCompoundDrawablePadding(uiKit.dp(8));
        views.details.put(Integer.valueOf(i), label);
        views.values.put(Integer.valueOf(i), uiKit.label(linearLayout, "—", flag ? 40 : 30, UiKit.TEXT, true));
        uiKit.label(linearLayout, reading.sensor.unit, 14, UiKit.MUTED, false);
        views.qualities.put(Integer.valueOf(i), uiKit.label(linearLayout, "", 13, UiKit.AMBER, true));
        views.origins.put(Integer.valueOf(i), uiKit.label(linearLayout, "", 12, UiKit.MUTED, false));
        views.times.put(Integer.valueOf(i), uiKit.label(linearLayout, "", 12, UiKit.MUTED, false));
        label.setOnClickListener((view) -> DashboardScreen.showSensorDetail(views, reading, i, uiKit, linearLayout, view));
    }

    static void showSensorDetail(Views views, DashboardState.Reading reading, int i, UiKit uiKit, LinearLayout linearLayout, View view) {
        if (views.lastState != null) {
            DashboardState dashboardState = views.lastState;
            reading = i == 12 ? dashboardState.rpm : dashboardState.readings.get(Integer.valueOf(i));
        }
        LinearLayout column = uiKit.column();
        column.setPadding(uiKit.dp(18), uiKit.dp(12), uiKit.dp(18), uiKit.dp(12));
        column.setBackgroundColor(uiKit.palette.bg);
        Iterator<String> it = reading.details().iterator();
        while (it.hasNext()) {
            uiKit.label(column, it.next(), 14, UiKit.TEXT, false);
        }
        ScrollView scrollView = new ScrollView(linearLayout.getContext());
        scrollView.addView(column);
        new AlertDialog.Builder(linearLayout.getContext()).setTitle("Detalhes da captura").setView(scrollView).setPositiveButton("Fechar", null).show();
    }
}