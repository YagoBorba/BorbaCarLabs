package io.rea.obd.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MaintenanceRemindersTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final long T = 1_790_000_000_000L;

    private static JournalEntry service(String id, long at, String note, String dueDate, String dueKm) {
        return new JournalEntry(id, "car", "Cobalt", JournalEntry.Kind.SERVICE, 1, at, at, at, note,
                new BigDecimal("48000"), null, null, null, false, false, dueDate, dueKm == null ? null : new BigDecimal(dueKm));
    }

    private static JournalEntry fuel(String id, long at, String km) {
        return new JournalEntry(id, "car", "Cobalt", JournalEntry.Kind.FUEL, 1, at, at, at, "",
                new BigDecimal(km), new BigDecimal("40"), null, "Gasolina", true, false, null, null);
    }

    @Test
    public void dateWithinAWeekIsDue() {
        List<MaintenanceReminders.Due> due = MaintenanceReminders.due(Arrays.asList(service("s", T, "Troca de óleo", "2026-11-14", null)), TODAY);
        assertEquals(1, due.size());
        assertEquals("Troca de óleo", due.get(0).title);
        assertEquals("vence em 4 dias (14/11)", due.get(0).detail);
        assertFalse(due.get(0).overdue);
    }

    @Test
    public void farDateIsNotDueAndPastDateIsOverdue() {
        assertTrue(MaintenanceReminders.due(Arrays.asList(service("s", T, "x", "2027-01-30", null)), TODAY).isEmpty());
        assertTrue(MaintenanceReminders.due(Arrays.asList(service("s", T, "x", "2026-11-01", null)), TODAY).get(0).overdue);
    }

    @Test
    public void mileageUsesTheNewestRecordedOdometer() {
        List<JournalEntry> entries = Arrays.asList(service("s", T, "Correia", null, "50000"), fuel("f", T + 1000, "49650"));
        MaintenanceReminders.Due due = MaintenanceReminders.due(entries, TODAY).get(0);
        assertEquals("faltam 350 km", due.detail);
        assertTrue(MaintenanceReminders.due(Arrays.asList(service("s", T, "Correia", null, "50000"), fuel("f", T + 1, "50010")), TODAY).get(0).overdue);
    }

    @Test
    public void aNewerServiceSupersedesTheOldReminder() {
        List<JournalEntry> entries = Arrays.asList(
                service("old", T, "Troca antiga", "2026-11-11", null),
                service("new", T + 1000, "Troca nova", "2027-05-10", null));
        assertTrue(MaintenanceReminders.due(entries, TODAY).isEmpty());
    }
}
