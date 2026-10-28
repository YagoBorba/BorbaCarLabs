package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.rea.obd.diagnostics.DtcReadResult;
import java.util.Arrays;
import org.junit.Test;

public class ObdParserTest {
    @Test
    public void decodesCommonPids() {
        assertEquals(1726.0, ObdParser.pid("410C1AF8", 0x0C), 0.001);
        assertEquals(60.0, ObdParser.pid("410D3C", 0x0D), 0.001);
        assertEquals(83.0, ObdParser.pid("41057B", 0x05), 0.001);
    }

    @Test
    public void zeroIsAValidReading() {
        assertEquals(0.0, ObdParser.pid("410D00", 0x0D), 0.0);
    }

    @Test
    public void decodesAdapterVoltage() {
        assertEquals(12.6, ObdParser.voltage("12.6V"), 0.001);
    }

    @Test
    public void decodesCanDtcsWithCountByte() {
        ObdParser.DecodedDiagnostic<String> read = ObdParser.decodeDtcs("430203010420", 3, true);
        assertEquals(DtcReadResult.Outcome.POSITIVE, read.outcome);
        assertEquals(Arrays.asList("P0301", "P0420"), read.values);
    }

    @Test
    public void decodesLegacyDtcsAndSkipsPadding() {
        ObdParser.DecodedDiagnostic<String> read = ObdParser.decodeDtcs("43030104200000", 3, false);
        assertEquals(Arrays.asList("P0301", "P0420"), read.values);
    }

    @Test
    public void decodesSystemLetterFromFirstBits() {
        assertEquals(Arrays.asList("C0123", "B1000", "U0100"), ObdParser.decodeDtcs("430341239000C100", 3, true).values);
    }

    @Test
    public void noDataIsNotAPositiveAnswer() {
        ObdParser.DecodedDiagnostic<String> read = ObdParser.decodeDtcs("NO DATA", 3, true);
        assertEquals(DtcReadResult.Outcome.NO_DATA, read.outcome);
        assertTrue(read.values.isEmpty());
    }

    @Test
    public void negativeResponseKeepsServiceAndCode() {
        ObdParser.DecodedDiagnostic<String> read = ObdParser.decodeDtcs("7F0312", 3, true);
        assertEquals(DtcReadResult.Outcome.NEGATIVE, read.outcome);
        assertEquals(0x12, read.negativeResponses.get(0).code);
    }

    @Test
    public void milByteCarriesLampAndCount() {
        ObdParser.DecodedDiagnostic<Integer> mil = ObdParser.decodeMil("41018107E100");
        assertEquals(0x81, (int) mil.values.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void adapterBufferErrorsAreRejected() {
        ObdParser.messages("BUFFER FULL");
    }
}
