package io.rea.obd.ui;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.catalog.VehicleEfficiency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class HomeScreen {
    public interface Actions {
        void add();

        void connect();

        void history();

        void journal();
    }

    public static void render(LinearLayout linearLayout, VehicleProfile vehicleProfile, String status, String text, int i, int value, boolean demo, boolean flag, boolean connected, DtcReadResult lastRead, JournalController.Glance glance, VehicleEfficiency.Official official, final Actions actions) {
        String text2;
        String text3;
        UiKit uiKit = new UiKit(linearLayout.getContext());
        uiKit.eyebrow(linearLayout, "BORBACAR · FOCO NO ESSENCIAL");
        uiKit.label(linearLayout, "Hoje", 32, UiKit.TEXT, true);
        LinearLayout feature = uiKit.feature(linearLayout);
        uiKit.eyebrow(feature, vehicleProfile == null ? "SUA GARAGEM" : "SEU VEÍCULO");
        uiKit.label(feature, vehicleProfile == null ? "Tudo começa pelo seu carro" : vehicleProfile.name, 26, UiKit.TEXT, true);
        uiKit.artwork(feature, vehicleProfile == null ? 120 : 76);
        if (vehicleProfile == null) {
            uiKit.label(feature, "Escolha um modelo do catálogo ou crie um perfil simples.", 15, UiKit.MUTED, false);
            Objects.requireNonNull(actions);
            uiKit.button(feature, "Adicionar veículo", true, () -> actions.add());
        } else {
            if (!vehicleProfile.description().isEmpty()) {
                uiKit.label(feature, vehicleProfile.description(), 14, UiKit.MUTED, false);
            }
            if (demo) {
                text2 = "Demonstração em andamento";
            } else {
                text2 = connected ? "Scanner conectado" : "Pronto para a próxima leitura";
            }
            uiKit.label(feature, text2, 19, UiKit.TEXT, true);
            uiKit.label(feature, status, 14, UiKit.MUTED, false);
            if (!demo && !connected) {
                Objects.requireNonNull(actions);
                uiKit.button(feature, "Conectar adaptador", true, () -> actions.connect()).setEnabled(!flag);
            }
        }
        uiKit.section(linearLayout, "Última leitura", "");
        if (lastRead != null) {
            DiagnosisScreen.verdict(linearLayout, lastRead, true);
        } else {
            if (i == 0) {
                text3 = "Nenhuma leitura salva para este perfil.";
            } else {
                text3 = "Última leitura · " + text;
            }
            uiKit.label(linearLayout, text3, 14, UiKit.MUTED, false);
        }
        if (i > 0 || value > 0) {
            String saved = i > 0 ? Format.count(i, "leitura salva", "leituras salvas") : "";
            if (value > 0) {
                saved = (saved.isEmpty() ? "" : saved + " · ") + Format.count(value, "investigação", "investigações");
            }
            Objects.requireNonNull(actions);
            uiKit.row(linearLayout, "Histórico · " + saved, () -> actions.history());
        }
        if (vehicleProfile != null && glance != null) {
            journal(linearLayout, uiKit, glance, official, actions);
        }
    }

    private static void journal(LinearLayout parent, UiKit ui, JournalController.Glance glance, VehicleEfficiency.Official official, final Actions actions) {
        ui.section(parent, "Diário do carro", "");
        Runnable open = () -> actions.journal();
        if (glance.count == 0) {
            ui.label(parent, "Registre abastecimentos e serviços para acompanhar consumo e lembretes de manutenção.", 14, UiKit.MUTED, false);
            ui.row(parent, "Abrir o Diário", open);
            return;
        }
        LinearLayout row = ui.column();
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout fuel = tile(ui, row, true);
        ui.eyebrow(fuel, "CONSUMO");
        if (glance.fuel.kmPerLiter != null) {
            ui.label(fuel, Format.decimal(glance.fuel.kmPerLiter) + " km/L", 22, UiKit.TEXT, true);
            ui.label(fuel, "último tanque cheio a cheio", 12, UiKit.MUTED, false);
        } else {
            ui.label(fuel, "—", 22, UiKit.TEXT, true);
            ui.label(fuel, "aguardando um intervalo completo", 12, UiKit.MUTED, false);
        }
        official(ui, fuel, glance, official);

        LinearLayout service = tile(ui, row, false);
        ui.eyebrow(service, "PRÓXIMO SERVIÇO");
        JournalEntry reminder = glance.reminder;
        if (reminder == null) {
            ui.label(service, "—", 22, UiKit.TEXT, true);
            ui.label(service, "sem lembrete registrado", 12, UiKit.MUTED, false);
        } else {
            if (!reminder.dueDate.isEmpty()) {
                LocalDate due = LocalDate.parse(reminder.dueDate);
                long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
                ui.label(service, due.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 20, days < 0 ? UiKit.AMBER : UiKit.TEXT, true);
                ui.label(service, days < 0 ? "vencido há " + Format.count((int) -days, "dia", "dias") : days == 0 ? "vence hoje" : "em " + Format.count((int) days, "dia", "dias"), 12, days < 0 ? UiKit.AMBER : UiKit.MUTED, false);
            }
            if (reminder.dueOdometer != null) {
                ui.label(service, (reminder.dueDate.isEmpty() ? "" : "ou ") + Format.km(reminder.dueOdometer), reminder.dueDate.isEmpty() ? 20 : 13, UiKit.TEXT, reminder.dueDate.isEmpty());
            }
        }
        ui.row(parent, "Abrir o Diário · " + Format.count(glance.count, "registro", "registros"), open);
    }

    private static void official(UiKit ui, LinearLayout tile, JournalController.Glance glance, VehicleEfficiency.Official official) {
        if (official == null || glance.mixedFuel()) {
            return;
        }
        BigDecimal[] range = official.forFuel(glance.lastFuel);
        if (range == null) {
            return;
        }
        String fuel = glance.lastFuel.toLowerCase(java.util.Locale.ROOT);
        ui.label(tile, "Inmetro (" + fuel + "): " + Format.decimal(range[0]) + " cidade · " + Format.decimal(range[1]) + " estrada", 12, UiKit.MUTED, false)
                .setPadding(0, ui.dp(6), 0, ui.dp(2));
        if (glance.fuel.kmPerLiter != null) {
            String position = VehicleEfficiency.position(glance.fuel.kmPerLiter, range);
            String text = position.equals("dentro") ? "dentro da faixa oficial"
                    : position.equals("abaixo") ? "abaixo da faixa oficial" : "acima da faixa oficial";
            ui.label(tile, text, 12, position.equals("abaixo") ? UiKit.AMBER : UiKit.ACCENT, true);
        }
    }

    private static LinearLayout tile(UiKit ui, LinearLayout row, boolean first) {
        LinearLayout tile = ui.card(row, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        params.bottomMargin = ui.dp(8);
        if (first) {
            params.rightMargin = ui.dp(10);
        }
        tile.setLayoutParams(params);
        return tile;
    }
}