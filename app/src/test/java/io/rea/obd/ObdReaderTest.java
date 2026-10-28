package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.telemetry.TelemetrySample;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;

public class ObdReaderTest {
    private static final class StepClock implements ObdReader.Clock {
        private long now = 1_800_000_000_000L;

        @Override
        public long elapsedMillis() {
            return now - 1_800_000_000_000L;
        }

        @Override
        public long epochMillis() {
            return now += 10;
        }
    }

    private static ObdReader reader(FakeAdapter adapter) throws IOException {
        ElmSession session = new ElmSession(adapter);
        session.connect();
        return new ObdReader(session, new StepClock());
    }

    @Test
    public void pollReadsEachPidOnceAndThenVoltage() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar()
                .reply("010C", "410C1AF8")
                .reply("010D", "410D3C")
                .reply("0105", "NO DATA")
                .reply("ATRV", "12.6V");
        ObdReader.Batch batch = reader(adapter).poll(Arrays.asList(0x0C, 0x0D, 0x05, 0x46));

        assertNull(batch.failure);
        assertEquals(4, batch.samples.size());
        assertEquals(TelemetrySample.Status.VALID, batch.samples.get(0).status);
        assertEquals(1726.0, batch.samples.get(0).value, 0.001);
        assertEquals(60.0, batch.samples.get(1).value, 0.001);
        assertEquals(TelemetrySample.Status.NO_DATA, batch.samples.get(2).status);
        assertEquals(TelemetrySample.Status.UNSUPPORTED, batch.samples.get(3).status);
        assertEquals(12.6, batch.adapterVoltage, 0.001);
        assertTrue(!adapter.sent.contains("0146"));
    }

    @Test
    public void transportLossStopsTheBatchAndSkipsVoltage() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar()
                .reply("010C", FakeAdapter.DISCONNECT)
                .reply("ATRV", "12.6V");
        ObdReader.Batch batch = reader(adapter).poll(Arrays.asList(0x0C, 0x0D));

        assertNotNull(batch.failure);
        assertEquals(TelemetrySample.Status.DISCONNECTED, batch.samples.get(0).status);
        assertEquals(TelemetrySample.Status.NOT_READ, batch.samples.get(1).status);
        assertNull(batch.adapterVoltage);
        assertTrue(!adapter.sent.contains("ATRV"));
    }

    @Test
    public void supplementCollectsReadinessAndFrameZero() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar()
                .reply("0101", "41018107E100")
                .reply("020C00", "420C001AF8")
                .reply("020D00", "NO DATA")
                .reply("020500", "4205007B")
                .reply("020400", "NO DATA")
                .reply("021100", "NO DATA");
        SupplementalReadResult result = reader(adapter).readSupplement("vehicle-1", "session-1");

        assertEquals(DtcReadResult.Outcome.POSITIVE, result.readiness.status.outcome);
        assertEquals(5, result.frame.size());
        assertEquals(DtcReadResult.Outcome.POSITIVE, result.frame.get(0).status.outcome);
        assertEquals(1726.0, result.frame.get(0).observations.get(0).value, 0.001);
        assertEquals(DtcReadResult.Outcome.NO_DATA, result.frame.get(1).status.outcome);
        assertEquals(83.0, result.frame.get(2).observations.get(0).value, 0.001);
    }

    @Test
    public void supplementStopsQueryingAfterTransportLoss() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar().reply("0101", FakeAdapter.DISCONNECT);
        SupplementalReadResult result = reader(adapter).readSupplement("vehicle-1", "session-1");

        assertTrue(result.readiness.status.transportFailed());
        for (SupplementalReadResult.FramePid frame : result.frame) {
            assertEquals(DtcReadResult.Outcome.NOT_READ, frame.status.outcome);
        }
        assertTrue(!adapter.sent.contains("020C00"));
    }
}
