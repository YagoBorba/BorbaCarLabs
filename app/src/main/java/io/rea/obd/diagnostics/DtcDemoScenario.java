package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class DtcDemoScenario {
    private DtcDemoScenario() {
    }

    public static DtcReadResult create(String vehicleId, String sessionId, long value) {
        String string = UUID.randomUUID().toString();
        DtcReadResult.MilResult milResult = new DtcReadResult.MilResult(positive(value), Collections.singletonList(new DtcReadResult.MilObservation(string + ":mil:0", 129, value)));
        ArrayList items = new ArrayList();
        DtcObservation.Category[] categorys = DtcObservation.Category.values();
        int length = categorys.length;
        for (int i = 0; i < length; i++) {
            DtcObservation.Category category = categorys[i];
            String code = category == DtcObservation.Category.STORED ? "P0133" : category == DtcObservation.Category.PENDING ? "P0301" : null;
            items.add(new DtcReadResult.ServiceResult(category, positive(value), code == null ? Collections.emptyList() : Collections.singletonList(new DtcObservation(string + ":" + category.name() + ":" + code, code, category, value))));
        }
        return new DtcReadResult(string, vehicleId, sessionId, DataOrigin.SIMULATED, value, value, milResult, items);
    }

    private static DtcReadResult.Status positive(long value) {
        return new DtcReadResult.Status(DtcReadResult.Outcome.POSITIVE, Long.valueOf(value), Long.valueOf(value), 1, Collections.emptyList(), "Cenário fictício");
    }
}