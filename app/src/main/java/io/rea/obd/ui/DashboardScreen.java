package io.rea.obd.ui;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.rea.obd.ChartView;
import io.rea.obd.R;
import io.rea.obd.telemetry.DashboardState;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.vehicle.VehicleProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashboardScreen {
    public static final String SUMMARY = "live-summary";
    public static final String EXPANDED = "live-expanded";
    public static final String TILE = "sensor-tile-";

    public interface Actions {
        void connection();

        void demo();

        void expand(int pid);

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
        public int expanded;
        private DashboardState lastState;
        public TextView recordingInfo;
        public TextView voltage;
        public TextView voltageInfo;
        public final Map<Integer, TextView> values = new LinkedHashMap();
        public final Map<Integer, TextView> qualities = new LinkedHashMap();
        private final Map<Integer, View> tiles = new HashMap();
        private TextView value;
        private TextView quality;
        private TextView origin;
        private TextView time;
        private TextView details;

        public void bind(DashboardState dashboardState, List<ChartView.Point> list, String text3, String historySessionId, String voltage, String text, String text5) {
            String string;
            this.lastState = dashboardState;
            UiKit uiKit = new UiKit(this.chart.getContext());
            for (Map.Entry<Integer, TextView> entry : this.values.entrySet()) {
                DashboardState.Reading reading = reading(dashboardState, entry.getKey().intValue());
                entry.getValue().setText(I18n.t(reading.value));
                TextView quality = this.qualities.get(entry.getKey());
                String warning = alert(reading);
                quality.setText(I18n.t(warning != null ? warning : reading.status == TelemetrySample.Status.VALID ? "" : reading.quality));
                quality.setTextColor(uiKit.palette.color(warning != null || reading.status != TelemetrySample.Status.VALID ? UiKit.AMBER : UiKit.MUTED));
                boolean open = entry.getKey().intValue() == this.expanded;
                this.tiles.get(entry.getKey()).setContentDescription(I18n.t(reading.sensor.name) + ": " + reading.value + " " + reading.sensor.unit + ". " + I18n.t(reading.quality) + ". " + I18n.t(open ? "Gráfico aberto abaixo" : "Toque para ver o gráfico"));
            }
            DashboardState.Reading reading = reading(dashboardState, this.expanded);
            this.value.setText(I18n.t(reading.value));
            String warning = alert(reading);
            this.quality.setText(warning == null ? I18n.t(reading.quality) : I18n.t(warning) + " · " + I18n.t(reading.quality));
            this.quality.setTextColor(uiKit.palette.color(warning != null || attention(reading) ? UiKit.AMBER : UiKit.MUTED));
            this.origin.setText(I18n.t(reading.origin));
            this.origin.setVisibility(reading.origin.equals("Dado simulado") ? View.GONE : View.VISIBLE);
            this.time.setText(I18n.t(reading.time));
            this.value.setContentDescription(I18n.t(reading.sensor.name) + ": " + reading.value + " " + reading.sensor.unit + ". " + I18n.t(reading.quality) + ". " + I18n.t(reading.origin));
            this.details.setContentDescription(I18n.t("Abrir detalhes de " + reading.sensor.name) + ". " + reading.value + " " + reading.sensor.unit + ". " + I18n.t(reading.quality) + ". " + I18n.t(reading.origin) + ". " + I18n.t(reading.time));
            this.coverage.setText(I18n.t(dashboardState.coverage));
            this.voltage.setText(voltage);
            this.voltageInfo.setText(I18n.t(text));
            this.recordingInfo.setText(I18n.t(dashboardState.recording + "\n" + text5));
            boolean flag = dashboardState.collecting && reading.selected && reading.status == TelemetrySample.Status.VALID;
            if (list.isEmpty()) {
                string = "SEM AMOSTRAS · aguardando leituras";
            } else {
                string = (flag ? "AMOSTRAS DA SESSÃO" : "HISTÓRICO DA SESSÃO · sem atualização atual") + " · " + text3;
            }
            this.chartContext.setText(I18n.t(string));
            this.chart.data(list, reading.sensor.unit, reading.sensor.decimals);
            this.chartRange.setText(I18n.t(this.chart.summary() + (historySessionId.isEmpty() ? "" : "\nSessão de aquisição: " + historySessionId)));
        }
    }

    static DashboardState.Reading reading(DashboardState dashboardState, int pid) {
        DashboardState.Reading reading = dashboardState.readings.get(Integer.valueOf(pid));
        return reading != null ? reading : dashboardState.rpm;
    }

    static String alert(DashboardState.Reading reading) {
        if (reading.status != TelemetrySample.Status.VALID || reading.sample == null || reading.sample.value == null) {
            return null;
        }
        double value = reading.sample.value;
        if (reading.sensor.pid == 5 && value >= 110.0d) {
            return "Temperatura alta: pare com segurança e confira o painel";
        }
        if (reading.sensor.pid == 66 && (value < 11.8d || value > 15.2d)) {
            return value < 11.8d ? "Tensão baixa: bateria ou alternador" : "Tensão alta: confira o alternador";
        }
        if (reading.sensor.pid == 12 && value > 6500.0d) {
            return "Rotação muito alta";
        }
        return null;
    }

    static boolean attention(DashboardState.Reading reading) {
        return reading.status != TelemetrySample.Status.VALID || reading.origin.equals("Dado simulado");
    }

    public static Views render(LinearLayout linearLayout, VehicleProfile vehicleProfile, DashboardState dashboardState, int expanded, String status, boolean busy, boolean enabled, String name, final Actions actions) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        final Views views = new Views();
        Objects.requireNonNull(actions);
        uiKit.title(linearLayout, "Ao vivo", 22).setPadding(0, 0, 0, uiKit.dp(2));
        String vehicle;
        if (vehicleProfile == null) {
            vehicle = "Adicione um perfil na Garagem para associar suas gravações.";
        } else {
            vehicle = vehicleProfile.name + (vehicleProfile.description().isEmpty() ? "" : " · " + vehicleProfile.description());
        }
        uiKit.label(linearLayout, vehicle, 13, UiKit.MUTED, false).setPadding(0, 0, 0, uiKit.dp(4));
        boolean live = dashboardState.mode == DashboardState.Mode.CONNECTED || dashboardState.mode == DashboardState.Mode.DEMONSTRATION;
        LinearLayout connection = uiKit.card(linearLayout, false);
        uiKit.label(connection, dashboardState.mode == DashboardState.Mode.DEMONSTRATION ? "CENÁRIO LOCAL" : "CONEXÃO", 12, dashboardState.mode == DashboardState.Mode.DEMONSTRATION ? UiKit.AMBER : UiKit.ACCENT, true);
        uiKit.label(connection, status, 16, UiKit.TEXT, true);
        String text2;
        if (busy) {
            text2 = dashboardState.mode == DashboardState.Mode.CONNECTING ? "Conectando…" : "Operação em andamento…";
        } else {
            text2 = live ? "Desconectar" : "Conectar adaptador";
        }
        uiKit.button(connection, text2, true, () -> actions.connection()).setEnabled(!busy);
        if (live) {
            linearLayout.removeView(connection);
            uiKit.label(linearLayout, status, 12, UiKit.MUTED, false).setPadding(0, 0, 0, uiKit.dp(2));
        }
        views.coverage = uiKit.label(linearLayout, "", 12, UiKit.MUTED, false);

        List<DashboardState.Reading> selected = new ArrayList<>(dashboardState.readings.values());
        if (selected.isEmpty()) {
            uiKit.label(linearLayout, "Nenhum sensor selecionado. Use Selecionar sensores para mudar o lote.", 14, UiKit.MUTED, false);
        }
        views.expanded = expanded;
        summary(linearLayout, selected, views, uiKit, actions);

        LinearLayout card = uiKit.card(linearLayout, true);
        card.setTag(EXPANDED);
        sensor(card, reading(dashboardState, expanded), views, uiKit);
        views.chartContext = uiKit.label(card, "", 12, UiKit.AMBER, true);
        views.chart = new ChartView(linearLayout.getContext());
        views.chart.setTag("sensor-chart");
        float fontScale = linearLayout.getResources().getConfiguration().fontScale;
        card.addView(views.chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, uiKit.dp(fontScale > 1.3f ? 210 : 160)));
        views.chartRange = uiKit.label(card, "", 12, UiKit.MUTED, false);
        views.chartSelection = uiKit.label(card, "Toque na curva ou use os controles para explorar as capturas.", 13, UiKit.TEXT, false);
        final DashboardState.Reading chartReading = reading(dashboardState, expanded);
        views.chart.setSelectionListener((point) -> DashboardScreen.showChartSelection(views, point, chartReading));
        LinearLayout steps = uiKit.column();
        boolean sideBySide = fontScale <= 1.3f;
        steps.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        card.addView(steps, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button previous = uiKit.button(steps, "Amostra anterior", false, () -> views.chart.moveSelection(-1));
        Button next = uiKit.button(steps, "Próxima amostra", false, () -> views.chart.moveSelection(1));
        if (sideBySide) {
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            first.setMarginEnd(uiKit.dp(8));
            previous.setLayoutParams(first);
            next.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        }

        LinearLayout card5 = uiKit.card(linearLayout, false);
        uiKit.label(card5, "TENSÃO NO ADAPTADOR", 12, UiKit.MUTED, true);
        views.voltage = uiKit.label(card5, "—", 28, UiKit.TEXT, true);
        views.voltageInfo = uiKit.label(card5, "", 13, UiKit.MUTED, false);
        uiKit.label(card5, "Alimentação do adaptador, não um teste de saúde da bateria. Não confundir com o PID de tensão do módulo.", 13, UiKit.MUTED, false);
        if (live) {
            linearLayout.addView(connection);
        }
        LinearLayout card6 = uiKit.card(linearLayout, false);
        uiKit.label(card6, "COLETA E GRAVAÇÃO", 12, UiKit.ACCENT, true);
        views.recordingInfo = uiKit.label(card6, "", 13, UiKit.MUTED, false);
        uiKit.label(card6, "A aquisição pausa fora deste painel ou do primeiro plano. Cada sinal conserva seu próprio horário; ausência nunca vira zero.", 14, UiKit.MUTED, false);
        uiKit.button(card6, "Selecionar sensores", false, () -> actions.sensors());
        boolean recordingOpen = dashboardState.recordingOpen;
        uiKit.button(card6, recordingOpen ? "Parar gravação CSV" : "Iniciar gravação CSV", true, () -> actions.recording()).setEnabled(recordingOpen || live);
        uiKit.label(card6, enabled ? "Último CSV: " + name + "\nO arquivo mantém seu contexto original e pode pertencer a outro perfil." : "Nenhum CSV anterior disponível.", 13, UiKit.MUTED, false);
        uiKit.button(card6, "Exportar último CSV", false, () -> actions.export()).setEnabled(enabled);
        return views;
    }

    private static void summary(LinearLayout parent, List<DashboardState.Reading> readings, Views views, UiKit ui, Actions actions) {
        if (readings.isEmpty()) {
            return;
        }
        android.content.res.Configuration configuration = parent.getResources().getConfiguration();
        int columns = configuration.fontScale > 1.3f || configuration.screenWidthDp < 300 ? 1 : configuration.screenWidthDp >= 600 ? 3 : 2;
        LinearLayout grid = ui.column();
        grid.setTag(SUMMARY);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = ui.dp(4);
        gridParams.bottomMargin = ui.dp(6);
        parent.addView(grid, gridParams);
        LinearLayout row = null;
        for (int i = 0; i < readings.size(); i++) {
            if (i % columns == 0) {
                row = ui.column();
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            tile(row, readings.get(i), views, ui, actions, i % columns != 0);
        }
        while (readings.size() % columns != 0 && row.getChildCount() < columns) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 1, 1.0f);
            params.setMarginStart(ui.dp(8));
            row.addView(new View(parent.getContext()), params);
        }
    }

    private static void tile(LinearLayout row, DashboardState.Reading reading, Views views, UiKit ui, Actions actions, boolean gap) {
        final int pid = reading.sensor.pid;
        boolean open = pid == views.expanded;
        LinearLayout tile = ui.column();
        tile.setTag(TILE + pid);
        tile.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(8));
        tile.setBackground(ui.surface(open));
        tile.setMinimumHeight(ui.dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.bottomMargin = ui.dp(8);
        if (gap) {
            params.setMarginStart(ui.dp(8));
        }
        row.addView(tile, params);
        TextView name = ui.label(tile, reading.sensor.name, 12, open ? UiKit.ACCENT : UiKit.MUTED, true);
        name.setPadding(0, 0, 0, ui.dp(2));
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout line = ui.column();
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.BOTTOM);
        tile.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView value = ui.label(line, "—", 22, UiKit.TEXT, true);
        value.setPadding(0, 0, ui.dp(4), 0);
        value.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView unit = ui.label(line, reading.sensor.unit, 12, UiKit.MUTED, false);
        unit.setPadding(0, 0, 0, ui.dp(3));
        unit.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView quality = ui.label(tile, "", 11, UiKit.MUTED, false);
        quality.setPadding(0, ui.dp(2), 0, 0);
        quality.setMaxLines(2);
        quality.setEllipsize(TextUtils.TruncateAt.END);
        views.values.put(Integer.valueOf(pid), value);
        views.qualities.put(Integer.valueOf(pid), quality);
        views.tiles.put(Integer.valueOf(pid), tile);
        value.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setSelected(open);
        tile.setOnClickListener((view) -> {
            if (!open) {
                actions.expand(pid);
            }
        });
        Motion.press(tile);
    }

    static void showChartSelection(Views views, ChartView.Point point, DashboardState.Reading reading) {
        String text;
        if (point == null) {
            text = "Nenhuma captura selecionada.";
        } else {
            text = Format.when(point.time) + " · " + views.chart.format(point.value) + " " + reading.sensor.unit + " · amostra histórica selecionada";
        }
        views.chartSelection.setText(I18n.t(text));
    }

    private static void sensor(final LinearLayout linearLayout, final DashboardState.Reading reading, final Views views, final UiKit uiKit) {
        final int i = reading.sensor.pid;
        TextView label = uiKit.label(linearLayout, reading.sensor.name, 17, UiKit.TEXT, true);
        label.setTag("sensor-details-" + i);
        label.setMinHeight(uiKit.dp(48));
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setFocusable(true);
        Drawable mutate = linearLayout.getContext().getDrawable(R.drawable.ic_info).mutate();
        mutate.setTint(uiKit.palette.accent);
        mutate.setBounds(0, 0, uiKit.dp(20), uiKit.dp(20));
        label.setCompoundDrawablesRelative(null, null, mutate, null);
        label.setCompoundDrawablePadding(uiKit.dp(8));
        views.details = label;
        LinearLayout line = uiKit.column();
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.BOTTOM);
        linearLayout.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        views.value = uiKit.label(line, "—", 34, UiKit.TEXT, true);
        views.value.setPadding(0, 0, uiKit.dp(6), 0);
        views.value.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView unit = uiKit.label(line, reading.sensor.unit, 14, UiKit.MUTED, false);
        unit.setPadding(0, 0, 0, uiKit.dp(6));
        unit.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        views.quality = uiKit.label(linearLayout, "", 13, UiKit.AMBER, true);
        views.origin = uiKit.label(linearLayout, "", 12, UiKit.MUTED, false);
        views.time = uiKit.label(linearLayout, "", 12, UiKit.MUTED, false);
        label.setOnClickListener((view) -> DashboardScreen.showSensorDetail(views, reading, i, uiKit, linearLayout, view));
    }

    static void showSensorDetail(Views views, DashboardState.Reading reading, int i, UiKit uiKit, LinearLayout linearLayout, View view) {
        if (views.lastState != null) {
            reading = reading(views.lastState, i);
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
        new LocalizedDialog(linearLayout.getContext()).setTitle("Detalhes da captura").setView(scrollView).setPositiveButton("Fechar", null).show();
    }
}
