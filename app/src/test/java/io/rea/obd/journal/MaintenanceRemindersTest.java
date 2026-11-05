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
    public void aNewerEntryOfTheSameServiceReplacesTheOldReminder() {
        List<JournalEntry> entries = Arrays.asList(
                service("old", T, "Troca de óleo", "2026-11-11", null),
                service("new", T + 1000, "troca de oleo", "2027-05-10", null));
        assertTrue(MaintenanceReminders.due(entries, TODAY).isEmpty());
    }

    @Test
    public void anotherServiceDoesNotHideAReminder() {
        List<JournalEntry> entries = Arrays.asList(
                service("oil", T, "Troca de óleo", "2026-11-11", null),
                service("tires", T + 1000, "Rodízio de pneus", "2027-05-10", null));
        List<MaintenanceReminders.Due> due = MaintenanceReminders.due(entries, TODAY);
        assertEquals(1, due.size());
        assertEquals("Troca de óleo", due.get(0).title);
        assertEquals("vence em 1 dia (11/11)", due.get(0).detail);
    }

    @Test
    public void everyDueServiceIsListedWithOverdueFirst() {
        List<JournalEntry> entries = Arrays.asList(
                service("oil", T, "Troca de óleo", "2026-11-12", null),
                service("belt", T + 1000, "Correia dentada", null, "48000"),
                service("tires", T + 2000, "Rodízio de pneus", "2026-11-15", null));
        List<MaintenanceReminders.Due> due = MaintenanceReminders.due(entries, TODAY);
        assertEquals(3, due.size());
        assertEquals("Correia dentada", due.get(0).title);
        assertTrue(due.get(0).overdue);
        assertEquals("Rodízio de pneus", due.get(1).title);
        assertEquals("Troca de óleo", due.get(2).title);
    }

    @Test
    public void doingTheServiceAgainWithoutANewDateCompletesTheReminder() {
        List<JournalEntry> entries = Arrays.asList(
                service("planned", T, "Troca de óleo", "2026-11-11", null),
                service("done", T + 1000, "Troca de óleo.", null, null));
        assertTrue(MaintenanceReminders.due(entries, TODAY).isEmpty());
    }

    @Test
    public void voidingTheNewerEntryBringsTheReminderBack() {
        JournalEntry done = service("done", T + 1000, "Troca de óleo", null, null).withVoided(true, T + 2000);
        List<JournalEntry> entries = Arrays.asList(service("planned", T, "Troca de óleo", "2026-11-11", null), done);
        assertEquals(1, MaintenanceReminders.due(entries, TODAY).size());
    }

    @Test
    public void nextServiceIsTheSoonestNotTheLatestRegistered() {
        List<JournalEntry> entries = Arrays.asList(
                service("oil", T, "Troca de óleo", "2026-11-11", null),
                service("belt", T + 500, "Correia dentada", null, "60000"),
                service("tires", T + 1000, "Rodízio de pneus", "2027-05-10", null));
        assertEquals("oil", MaintenanceReminders.next(entries, "car").id);
        assertEquals("belt", MaintenanceReminders.next(Arrays.asList(entries.get(1)), "car").id);
        assertEquals(null, MaintenanceReminders.next(entries, "car2"));
    }

    @Test
    public void remindersOfDifferentVehiclesAreIndependent() {
        JournalEntry other = new JournalEntry("other", "car2", "Onix", JournalEntry.Kind.SERVICE, 1, T + 1000, T + 1000, T + 1000, "Troca de óleo",
                new BigDecimal("10000"), null, null, null, false, false, null, null);
        List<JournalEntry> entries = Arrays.asList(service("oil", T, "Troca de óleo", "2026-11-11", null), other);
        assertEquals(1, MaintenanceReminders.due(entries, TODAY).size());
    }

    @Test public void overdueMileageTakesPriorityOverAnyFutureDate() {
        List<JournalEntry> entries = Arrays.asList(
            service("oil", T, "Óleo", "2027-05-10", null),
            service("brakes", T + 1, "Freios", null, "50000"),
            fuel("fill", T + 2, "51000"));
        assertEquals("brakes", MaintenanceReminders.next(entries, "car", TODAY).id);
        assertTrue(MaintenanceReminders.overdue(entries.get(1), TODAY, MaintenanceReminders.odometer(entries, "car")));
    }

    @Test public void soonMileageTakesPriorityOverADistantDate() {
        List<JournalEntry> entries = Arrays.asList(
            service("oil", T, "Óleo", "2027-05-10", null),
            service("brakes", T + 1, "Freios", null, "50000"),
            fuel("fill", T + 2, "49800"));
        assertEquals("brakes", MaintenanceReminders.next(entries, "car", TODAY).id);
    }
}
