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
}
