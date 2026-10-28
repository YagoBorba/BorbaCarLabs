package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class SupplementalDemoScenario {
    private SupplementalDemoScenario() {
    }

    public static SupplementalReadResult create(String vehicleId, String sessionId, long observedAtEpochMillis) {
        String string = UUID.randomUUID().toString();
        DtcReadResult.Status status = new DtcReadResult.Status(DtcReadResult.Outcome.POSITIVE, Long.valueOf(observedAtEpochMillis), Long.valueOf(observedAtEpochMillis), 1, Collections.emptyList(), "Cenário fictício");
        SupplementalReadResult.Readiness readiness = new SupplementalReadResult.Readiness(status, Collections.singletonList(new SupplementalReadResult.ReadinessObservation(string + ":ready:0", new SupplementalReadResult.ReadinessBits(129, 7, 5, 4), observedAtEpochMillis)));
        double[] values = {1450.0d, 0.0d, 88.0d, 32.0d, 18.0d};
        ArrayList items = new ArrayList();
        for (int i = 0; i < 5; i++) {
            int intValue = SupplementalReadResult.FRAME_PIDS.get(i).intValue();
            items.add(new SupplementalReadResult.FramePid(intValue, status, Collections.singletonList(new SupplementalReadResult.FrameValue(string + ":frame00:" + intValue + ":0", values[i], observedAtEpochMillis))));
        }
        return new SupplementalReadResult(string, vehicleId, sessionId, DataOrigin.SIMULATED, observedAtEpochMillis, observedAtEpochMillis, readiness, items);
    }
}