package io.rea.obd.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DtcGuidanceTest {
    @Test
    public void misfiresAndOverheatingAskTheDriverToStopOrAvoidDriving() {
        assertEquals(DtcGuidance.Level.AVOID, DtcGuidance.of("P0301").level);
        assertEquals(DtcGuidance.Level.AVOID, DtcGuidance.of("P0300").level);
        assertEquals(DtcGuidance.Level.STOP, DtcGuidance.of("P0217").level);
        assertTrue(DtcGuidance.of("P0301").urgent());
        assertTrue(DtcGuidance.of("P0301").causes.contains("Velas, cabos ou bobinas de ignição"));
    }

    @Test
    public void emissionsCodesCanWaitForTheWorkshop() {
        for (String code : new String[]{"P0420", "P0455", "P0171", "P0133", "P0128"}) {
            assertEquals(code, DtcGuidance.Level.SCHEDULE, DtcGuidance.of(code).level);
            assertFalse(code, DtcGuidance.of(code).urgent());
            assertFalse(code, DtcGuidance.of(code).causes.isEmpty());
        }
    }

    @Test
    public void manufacturerAndNetworkCodesGetNoGeneralAdvice() {
        for (String code : new String[]{"P1351", "U0100", "B1000", "C0035", "P00A0", "", null}) {
            assertEquals(String.valueOf(code), DtcGuidance.Level.UNKNOWN, DtcGuidance.of(code).level);
        }
    }
}
