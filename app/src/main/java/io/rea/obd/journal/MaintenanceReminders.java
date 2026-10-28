package io.rea.obd.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MaintenanceReminders {
    public static final int DAYS_AHEAD = 7;
    public static final BigDecimal KM_AHEAD = new BigDecimal("500");

    public static final class Due {
        public final String key;
        public final String vehicleName;
        public final String title;
        public final String detail;
        public final boolean overdue;

        Due(String key, String vehicleName, String title, String detail, boolean overdue) {
            this.key = key;
            this.vehicleName = vehicleName;
            this.title = title;
            this.detail = detail;
            this.overdue = overdue;
        }
    }

    private MaintenanceReminders() {
    }

    public static List<Due> due(List<JournalEntry> entries, LocalDate today) {
        Map<String, JournalEntry> reminders = new HashMap<>();
        Map<String, BigDecimal> odometers = new HashMap<>();
        for (JournalEntry entry : entries) {
            if (entry.voided) {
                continue;
            }
            if (entry.odometer != null) {
                odometers.merge(entry.vehicleId, entry.odometer, BigDecimal::max);
            }
            boolean hasReminder = !entry.dueDate.isEmpty() || entry.dueOdometer != null;
            JournalEntry current = reminders.get(entry.vehicleId);
            if (entry.kind == JournalEntry.Kind.SERVICE && hasReminder && (current == null || entry.occurredAt > current.occurredAt)) {
                reminders.put(entry.vehicleId, entry);
            }
        }
        List<Due> due = new ArrayList<>();
        for (JournalEntry reminder : reminders.values()) {
            List<String> reasons = new ArrayList<>();
            boolean overdue = false;
            if (!reminder.dueDate.isEmpty()) {
                LocalDate date = LocalDate.parse(reminder.dueDate);
                long days = ChronoUnit.DAYS.between(today, date);
                String when = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                if (days < 0) {
                    overdue = true;
                    reasons.add("data passou (" + when + ")");
                } else if (days <= DAYS_AHEAD) {
                    reasons.add(days == 0 ? "vence hoje" : "vence em " + days + (days == 1 ? " dia" : " dias") + " (" + when + ")");
                }
            }
            BigDecimal odometer = odometers.get(reminder.vehicleId);
            if (reminder.dueOdometer != null && odometer != null) {
                BigDecimal left = reminder.dueOdometer.subtract(odometer);
                if (left.signum() <= 0) {
                    overdue = true;
                    reasons.add("quilometragem atingida");
                } else if (left.compareTo(KM_AHEAD) <= 0) {
                    reasons.add("faltam " + left.stripTrailingZeros().toPlainString() + " km");
                }
            }
            if (!reasons.isEmpty()) {
                String title = reminder.note.isEmpty() ? "Serviço programado" : reminder.note;
                due.add(new Due(reminder.id + ":" + reminder.revision, reminder.vehicleName, title, String.join(" · ", reasons), overdue));
            }
        }
        return due;
    }
}
