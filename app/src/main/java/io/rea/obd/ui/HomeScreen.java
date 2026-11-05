package io.rea.obd.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.catalog.VehicleEfficiency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class HomeScreen {
    public static final String SUMMARY_READING = "today-summary-reading";
    public static final String SUMMARY_SERVICE = "today-summary-service";
    public static final String SUMMARY_FUEL = "today-summary-fuel";

    public interface Actions {
        void add();

        void connect();

        void history();

        void journal();

        default void fuel() {
        }
    }

    public static void render(LinearLayout linearLayout, VehicleProfile vehicleProfile, String status, String text, int i, int value, boolean demo, boolean flag, boolean connected, DtcReadResult lastRead, JournalController.Glance glance, VehicleEfficiency.Official official, final Actions actions) {
        UiKit uiKit = new UiKit(linearLayout.getContext());
        uiKit.title(linearLayout, "Hoje", 22).setPadding(0, 0, 0, uiKit.dp(4));
        if (vehicleProfile == null) {
            LinearLayout feature = uiKit.feature(linearLayout);
            uiKit.eyebrow(feature, "SUA GARAGEM");
            uiKit.label(feature, "Tudo começa pelo seu carro", 26, UiKit.TEXT, true);
            uiKit.artwork(feature, 120);
            uiKit.label(feature, "Escolha um modelo do catálogo ou crie um perfil simples.", 15, UiKit.MUTED, false);
            Objects.requireNonNull(actions);
            uiKit.button(feature, "Adicionar veículo", true, () -> actions.add());
        } else {
            vehicle(linearLayout, uiKit, vehicleProfile, status, demo, flag, connected, actions);
        }
        boolean journal = vehicleProfile != null && glance != null;
        LinearLayout summary = uiKit.card(linearLayout, false);
        summary.setPadding(uiKit.dp(16), uiKit.dp(10), uiKit.dp(16), uiKit.dp(8));
        reading(uiKit, summary, text, i, lastRead);
        if (journal) {
            service(uiKit, summary, glance.reminder, glance.odometer);
            fuel(uiKit, summary, glance, official);
        }
        if (journal && !demo) {
            Objects.requireNonNull(actions);
            uiKit.button(linearLayout, "Registrar abastecimento", false, () -> actions.fuel()).setTag("home-fuel");
        }
        if (i > 0 || value > 0) {
            String saved = i > 0 ? Format.count(i, "leitura salva", "leituras salvas") : "";
            if (value > 0) {
                saved = (saved.isEmpty() ? "" : saved + " · ") + Format.count(value, "investigação", "investigações");
            }
            Objects.requireNonNull(actions);
            uiKit.row(linearLayout, "Histórico · " + saved, () -> actions.history());
        }
        if (journal) {
            Runnable open = () -> actions.journal();
            if (glance.count == 0) {
                uiKit.label(linearLayout, "Registre abastecimentos e serviços para acompanhar consumo e lembretes de manutenção.", 14, UiKit.MUTED, false);
                uiKit.row(linearLayout, "Abrir o Diário", open);
            } else {
                uiKit.row(linearLayout, "Abrir o Diário · " + Format.count(glance.count, "registro", "registros") + (glance.odometer == null ? "" : " · " + Format.km(glance.odometer)), open).setTag("home-journal");
            }
        }
    }

    private static void vehicle(LinearLayout parent, UiKit ui, VehicleProfile vehicleProfile, String status, boolean demo, boolean flag, boolean connected, final Actions actions) {
        LinearLayout feature = ui.feature(parent);
        feature.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(10));
        ((LinearLayout.LayoutParams) feature.getLayoutParams()).bottomMargin = ui.dp(10);
        LinearLayout header = ui.column();
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        feature.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout heading = ui.column();
        header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tight(ui, ui.label(heading, vehicleProfile.name, 20, UiKit.TEXT, true));
        if (!vehicleProfile.description().isEmpty()) {
            tight(ui, ui.label(heading, vehicleProfile.description(), 13, UiKit.MUTED, false));
        }
        LinearLayout.LayoutParams art = new LinearLayout.LayoutParams(ui.dp(96), ui.dp(44));
        art.setMarginStart(ui.dp(8));
        ui.artwork(header, art);
        if (demo || connected) {
            ui.label(feature, demo ? "Demonstração em andamento" : "Scanner conectado", 15, UiKit.TEXT, true).setPadding(0, ui.dp(6), 0, 0);
            ui.label(feature, status, 13, UiKit.MUTED, false).setPadding(0, 0, 0, ui.dp(2));
            return;
        }
        ui.label(feature, status, 13, UiKit.MUTED, false).setPadding(0, ui.dp(6), 0, 0);
        Objects.requireNonNull(actions);
        Button connect = ui.button(feature, "Conectar adaptador", true, () -> actions.connect());
        connect.setEnabled(!flag);
        connect.setMinHeight(ui.dp(48));
        ((LinearLayout.LayoutParams) connect.getLayoutParams()).topMargin = ui.dp(6);
    }

    private static void reading(UiKit ui, LinearLayout card, String text, int i, DtcReadResult lastRead) {
        LinearLayout item = item(ui, card, SUMMARY_READING, "ÚLTIMA LEITURA");
        if (lastRead == null) {
            tight(ui, ui.label(item, i == 0 ? "Nenhuma leitura salva para este perfil." : text, 15, UiKit.TEXT, i > 0));
            return;
        }
        DiagnosisScreen.Headline headline = DiagnosisScreen.headline(lastRead);
        tight(ui, ui.label(item, headline.title, 17, headline.clean ? UiKit.TEXT : UiKit.AMBER, true));
        String when = Format.when(lastRead.completedAtEpochMillis);
        if (lastRead.origin == DataOrigin.SIMULATED) {
            when = "Demonstração · " + when;
        }
        tight(ui, ui.label(item, headline.hasCodes ? headline.detail + " · " + when : when, 12, UiKit.MUTED, false));
        if (headline.milOn) {
            tight(ui, ui.label(item, "Luz de injeção (MIL) acesa no painel", 12, UiKit.AMBER, true));
        }
    }

    private static void service(UiKit ui, LinearLayout card, JournalEntry reminder, BigDecimal odometer) {
        LinearLayout item = item(ui, card, SUMMARY_SERVICE, "PRÓXIMO SERVIÇO");
        if (reminder == null) {
            tight(ui, ui.label(item, "—", 17, UiKit.TEXT, true));
            tight(ui, ui.label(item, "sem lembrete registrado", 12, UiKit.MUTED, false));
            return;
        }
        Due due = due(reminder);
        if (io.rea.obd.journal.MaintenanceReminders.overdue(reminder, WallClock.today(), odometer) && !due.overdue) {
            due = new Due(due.value, "Quilometragem atingida" + (due.detail.isEmpty() ? "" : " · " + due.detail), true);
        }
        tight(ui, ui.label(item, due.value, 17, due.overdue ? UiKit.AMBER : UiKit.TEXT, true));
        if (!due.detail.isEmpty()) {
            tight(ui, ui.label(item, due.detail, 12, due.overdue ? UiKit.AMBER : UiKit.MUTED, false));
        }
    }

    static final class Due {
        final String value;
        final String detail;
        final boolean overdue;

        Due(String value, String detail, boolean overdue) {
            this.value = value;
            this.detail = detail;
            this.overdue = overdue;
        }
    }

    static Due due(JournalEntry reminder) {
        if (reminder.dueDate.isEmpty()) {
            return new Due(reminder.dueOdometer == null ? "—" : Format.km(reminder.dueOdometer), "", false);
        }
        LocalDate due = LocalDate.parse(reminder.dueDate);
        long days = ChronoUnit.DAYS.between(WallClock.today(), due);
        String detail = days < 0 ? "vencido há " + Format.count((int) -days, "dia", "dias") : days == 0 ? "vence hoje" : "em " + Format.count((int) days, "dia", "dias");
        if (reminder.dueOdometer != null) {
            detail = detail + " · ou " + Format.km(reminder.dueOdometer);
        }
        return new Due(due.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), detail, days < 0);
    }

    private static void fuel(UiKit ui, LinearLayout card, JournalController.Glance glance, VehicleEfficiency.Official official) {
        LinearLayout item = item(ui, card, SUMMARY_FUEL, "CONSUMO");
        BigDecimal[] range = official == null || glance.mixedFuel() ? null : official.forFuel(glance.lastFuel);
        LinearLayout row = ui.column();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView value = ui.label(row, glance.fuel.kmPerLiter != null ? Format.decimal(glance.fuel.kmPerLiter) + " km/L" : "—", 17, UiKit.TEXT, true);
        tight(ui, value);
        value.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (range != null && glance.fuel.kmPerLiter != null) {
            String position = VehicleEfficiency.position(glance.fuel.kmPerLiter, range);
            String text = position.equals("dentro") ? "dentro da faixa oficial"
                    : position.equals("abaixo") ? "abaixo da faixa oficial" : "acima da faixa oficial";
            TextView badge = ui.label(row, text, 12, position.equals("abaixo") ? UiKit.AMBER : UiKit.ACCENT, true);
            badge.setPadding(ui.dp(10), 0, 0, ui.dp(2));
            badge.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        }
        tight(ui, ui.label(item, glance.fuel.kmPerLiter != null ? "último tanque cheio a cheio" : "aguardando um intervalo completo", 12, UiKit.MUTED, false));
        if (range != null) {
            String fuel = glance.lastFuel.toLowerCase(java.util.Locale.ROOT);
            tight(ui, ui.label(item, "Inmetro (" + fuel + "): " + Format.decimal(range[0]) + " cidade · " + Format.decimal(range[1]) + " estrada", 12, UiKit.MUTED, false));
        }
    }

    private static LinearLayout item(UiKit ui, LinearLayout card, String tag, String title) {
        if (card.getChildCount() > 0) {
            View divider = new View(ui.context);
            divider.setBackgroundColor(ui.palette.line);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(1));
            params.topMargin = ui.dp(6);
            params.bottomMargin = ui.dp(6);
            card.addView(divider, params);
        }
        LinearLayout item = ui.column();
        item.setTag(tag);
        card.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ui.eyebrow(item, title);
        ((TextView) item.getChildAt(0)).setPadding(0, 0, 0, ui.dp(2));
        return item;
    }

    private static void tight(UiKit ui, TextView view) {
        view.setPadding(0, 0, 0, ui.dp(2));
    }
}
