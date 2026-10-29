package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TelemetryRecorderTest {
    private static final long T = 1_793_000_000_000L;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static TelemetrySample rpm(long at, Double value, TelemetrySample.Status status) {
        return new TelemetrySample(0x0C, value, status, DataOrigin.VEHICLE, at, 0, "");
    }

    @Test
    public void connectionLossIsRecordedAndThePartialFileIsComplete() throws IOException {
        File file = folder.newFile("gravacao.csv");
        TelemetryRecorder recorder = new TelemetryRecorder(file, false, "car-1", "Cobalt", "OBDLink");
        recorder.add(rpm(T, 812.0, TelemetrySample.Status.VALID), "RPM", "rpm");
        recorder.add(rpm(T + 1000, 845.0, TelemetrySample.Status.VALID), "RPM", "rpm");
        recorder.add(rpm(T + 2000, null, TelemetrySample.Status.DISCONNECTED), "RPM", "rpm");
        recorder.close();

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertEquals(4, lines.size());
        assertTrue(lines.get(3).endsWith(",0C,\"RPM\",DISCONNECTED,,\"rpm\""));
        assertEquals(3, recorder.samples());
        double[] stats = recorder.statistics().get(0x0C);
        assertEquals(812.0, stats[0], 0.001);
        assertEquals(845.0, stats[1], 0.001);
        assertEquals(2.0, stats[3], 0.001);
    }

    @Test
    public void closedRecordingRejectsNewSamples() throws IOException {
        TelemetryRecorder recorder = new TelemetryRecorder(folder.newFile("a.csv"), false, "Cobalt");
        recorder.close();
        recorder.close();
        try {
            recorder.add(rpm(T, 800.0, TelemetrySample.Status.VALID), "RPM", "rpm");
            fail();
        } catch (IOException expected) {
            assertEquals("Gravação encerrada", expected.getMessage());
        }
    }

    @Test
    public void simulatedSamplesNeverEnterARealRecording() throws IOException {
        File file = folder.newFile("b.csv");
        TelemetryRecorder recorder = new TelemetryRecorder(file, false, "Cobalt");
        try {
            recorder.add(new TelemetrySample(0x0C, 800.0, TelemetrySample.Status.VALID, DataOrigin.SIMULATED, T, 0, ""), "RPM", "rpm");
            fail();
        } catch (IOException expected) {
            assertEquals("Origem da amostra incompatível com a sessão", expected.getMessage());
        }
        recorder.close();
        assertEquals(1, Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).size());
        assertFalse(recorder.statistics().containsKey(0x0C));
    }
}
