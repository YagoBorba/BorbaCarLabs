package io.rea.obd.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DtcReferenceTest {
    @Test
    public void savedInvestigationsKeepTheFrozenCatalog() {
        assertEquals("generic-dtc-1", DtcCatalog.VERSION);
        assertEquals(5, DtcCatalog.all().size());
        assertEquals("generic-dtc-1:P0301", DtcCatalog.find("P0301").id);
    }

    @Test
    public void referenceExtendsTheFrozenCatalogUnchanged() {
        for (DtcDefinition frozen : DtcCatalog.all()) {
            assertSame(frozen, DtcReference.find(frozen.code));
        }
        assertTrue(DtcReference.all().size() >= 90);
        assertEquals("generic-dtc-3:P0302", DtcReference.find("P0302").id);
    }

    @Test
    public void onlyGenericCodesWithResolvableSources() {
        String previous = "";
        for (DtcDefinition definition : DtcReference.all()) {
            assertTrue(definition.code, definition.code.startsWith("P0") || definition.code.startsWith("U0"));
            assertNotNull(definition.code, DtcStructure.describe(definition.code));
            assertFalse(DtcReference.sources(definition).isEmpty());
            assertTrue("sorted", definition.code.compareTo(previous) > 0);
            previous = definition.code;
        }
    }

    @Test
    public void sourceDefectsAreLeftOut() {
        for (String code : new String[]{"P0141", "P0161", "P0180", "P0553"}) {
            assertEquals(null, DtcReference.find(code));
        }
        assertNotNull(DtcReference.find("P0420"));
        assertNotNull(DtcReference.find("P0176"));
    }

    @Test
    public void commonCodesFromTheFord2014ListAreDescribed() {
        for (String code : new String[]{"P0016", "P0101", "P0128", "P0335", "P0506", "P0507", "P0562", "U0100", "U0121"}) {
            DtcDefinition definition = DtcReference.find(code);
            assertNotNull(code, definition);
            assertEquals(code, "ford-boss302s-2014-obd-codes", DtcReference.sources(definition).get(0).id);
            assertTrue(code, definition.sourceSection.startsWith("Ford 2014 OBD-II, página "));
        }
    }
}
