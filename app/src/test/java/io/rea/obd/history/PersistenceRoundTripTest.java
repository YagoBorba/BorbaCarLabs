package io.rea.obd.history;

import static org.junit.Assert.assertEquals;

import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.SupplementalDemoScenario;
import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalJson;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

public class PersistenceRoundTripTest {
    private static final long NOW = 1_793_000_000_000L;

    private static List<String> codes(DtcReadResult read) {
        List<String> codes = new ArrayList<>();
        for (DtcObservation observation : read.observations()) {
            codes.add(observation.category + ":" + observation.code);
        }
        return codes;
    }

    @Test
    public void dtcReadSurvivesEncodeDecode() throws Exception {
        DtcReadResult read = DtcDemoScenario.create("vehicle-1", "session-1", NOW);
        JSONObject encoded = DtcReadJson.encode(read);
        DtcReadResult decoded = DtcReadJson.decode(new JSONObject(encoded.toString()));

        assertEquals(codes(read), codes(decoded));
        assertEquals(read.captureId, decoded.captureId);
        assertEquals(read.completedAtEpochMillis, decoded.completedAtEpochMillis);
        assertEquals(read.incomplete(), decoded.incomplete());
        assertEquals(encoded.toString(), DtcReadJson.encode(decoded).toString());
    }

    @Test
    public void supplementSurvivesEncodeDecode() throws Exception {
        SupplementalReadResult supplement = SupplementalDemoScenario.create("vehicle-1", "session-1", NOW);
        JSONObject encoded = SupplementalReadJson.encode(supplement);
        SupplementalReadResult decoded = SupplementalReadJson.decode(new JSONObject(encoded.toString()));
        assertEquals(encoded.toString(), SupplementalReadJson.encode(decoded).toString());
        assertEquals(supplement.frame.size(), decoded.frame.size());
    }

    @Test
    public void serviceReminderSurvivesEncodeDecode() throws Exception {
        JournalEntry service = new JournalEntry("s1", "car", "Cobalt", JournalEntry.Kind.SERVICE, 2, NOW, NOW - 10, NOW,
                "Troca de óleo", new BigDecimal("48210"), null, new BigDecimal("289.90"), null, false, false,
                "2027-04-26", new BigDecimal("58210"));
        JournalEntry decoded = JournalJson.decode(new JSONObject(JournalJson.encode(service).toString()));
        assertEquals("2027-04-26", decoded.dueDate);
        assertEquals(0, new BigDecimal("58210").compareTo(decoded.dueOdometer));
        assertEquals(0, new BigDecimal("289.90").compareTo(decoded.cost));
        assertEquals(2, decoded.revision);
    }
}
