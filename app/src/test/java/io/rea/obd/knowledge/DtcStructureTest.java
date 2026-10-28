package io.rea.obd.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DtcStructureTest {
    @Test
    public void genericPowertrainCodesNameTheirGroup() {
        assertEquals("Motor e transmissão · código genérico · ignição ou falha de combustão", DtcStructure.describe("P0301"));
        assertEquals("Motor e transmissão · código genérico · controles auxiliares de emissões", DtcStructure.describe("P0420"));
        assertEquals("Motor e transmissão · código genérico · transmissão", DtcStructure.describe("P0700"));
    }

    @Test
    public void manufacturerAndOtherSystemsStopAtWhatTheFormatSays() {
        assertEquals("Motor e transmissão · código específico do fabricante", DtcStructure.describe("P1234"));
        assertEquals("Rede de comunicação entre módulos · código genérico", DtcStructure.describe("U0100"));
        assertEquals("Carroceria · código específico do fabricante", DtcStructure.describe("B1000"));
    }

    @Test
    public void rejectsNonCodes() {
        assertNull(DtcStructure.describe("X1234"));
        assertNull(DtcStructure.describe("P030"));
        assertNull(DtcStructure.describe(null));
    }
}
