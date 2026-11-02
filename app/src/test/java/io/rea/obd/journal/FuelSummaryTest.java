package io.rea.obd.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class FuelSummaryTest {
    private static JournalEntry fuel(String id, long at, String km, String liters, boolean full, boolean complete) {
        return new JournalEntry(id, "car", "Cobalt", JournalEntry.Kind.FUEL, 1, at, at, at, "",
                new BigDecimal(km), new BigDecimal(liters), null, "Gasolina", full, complete, null, null);
    }

    @Test
    public void fullToFullIncludesPartialFillsInBetween() {
        List<JournalEntry> entries = Arrays.asList(
                fuel("a", 1000, "10000", "40", true, false),
                fuel("b", 2000, "10200", "10", false, false),
                fuel("c", 3000, "10500", "30", true, true));
        FuelSummary summary = FuelSummary.latest(entries, "car");
        assertEquals(0, new BigDecimal("12.50").compareTo(summary.kmPerLiter));
        assertEquals(0, new BigDecimal("500").compareTo(summary.distance));
    }

    @Test
    public void openIntervalIsNotCalculated() {
        List<JournalEntry> entries = Arrays.asList(
                fuel("a", 1000, "10000", "40", true, false),
                fuel("b", 2000, "10200", "10", false, false));
        FuelSummary summary = FuelSummary.latest(entries, "car");
        assertNull(summary.kmPerLiter);
        assertTrue(summary.explanation.contains("parcial"));
    }

    @Test
    public void otherVehiclesAreIgnored() {
        List<JournalEntry> entries = Arrays.asList(
                fuel("a", 1000, "10000", "40", true, false),
                fuel("c", 3000, "10500", "30", true, true));
        assertNull(FuelSummary.latest(entries, "another-car").kmPerLiter);
    }

    private static JournalEntry priced(String id, long at, String km, String liters, String cost, String fuel) {
        return new JournalEntry(id, "car", "Cobalt", JournalEntry.Kind.FUEL, 1, at, at, at, "",
                new BigDecimal(km), new BigDecimal(liters), new BigDecimal(cost), fuel, true, true, null, null);
    }

    @Test
    public void historyKeepsEveryClosedTankWithItsCostPerKm() {
        List<JournalEntry> entries = Arrays.asList(
                priced("a", 1000, "10000", "40", "232.00", "Gasolina"),
                priced("b", 2000, "10500", "40", "240.00", "Gasolina"),
                priced("c", 3000, "10850", "50", "200.00", "Etanol"),
                fuel("d", 4000, "11000", "10", false, false));
        List<FuelSummary> history = FuelSummary.history(entries, "car");
        assertEquals(2, history.size());
        assertEquals("b", history.get(0).endId);
        assertEquals(0, new BigDecimal("12.50").compareTo(history.get(0).kmPerLiter));
        assertEquals(0, new BigDecimal("0.48").compareTo(history.get(0).costPerKm));
        assertEquals("Etanol", history.get(1).fuel);
        assertEquals(0, new BigDecimal("7.00").compareTo(history.get(1).kmPerLiter));
        assertEquals(0, new BigDecimal("0.57").compareTo(FuelSummary.averageCostPerKm(history).get("Etanol")));
        assertEquals(0, new BigDecimal("4.00").compareTo(FuelSummary.lastPrice(entries, "car", "Etanol")));
        assertEquals(0, new BigDecimal("672.00").compareTo(FuelSummary.spentSince(entries, "car", 0)));
        assertNull(FuelSummary.latest(entries, "car").kmPerLiter);
    }
}
