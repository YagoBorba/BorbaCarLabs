package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class CaptureComparison {
    private CaptureComparison() {
    }

    public static List<String> lines(DtcReadResult dtcReadResult, DtcReadResult dtcReadResult2) {
        if (dtcReadResult.captureId.equals(dtcReadResult2.captureId)) {
            throw new IllegalArgumentException("Selecione duas capturas diferentes.");
        }
        if (dtcReadResult.vehicleId == null || !dtcReadResult.vehicleId.equals(dtcReadResult2.vehicleId)) {
            throw new IllegalArgumentException("As capturas precisam pertencer ao mesmo veículo identificado.");
        }
        if (dtcReadResult.origin != dtcReadResult2.origin) {
            throw new IllegalArgumentException("Não é possível comparar demonstração com leitura real.");
        }
        if (dtcReadResult.completedAtEpochMillis > dtcReadResult2.completedAtEpochMillis) {
            dtcReadResult2 = dtcReadResult;
            dtcReadResult = dtcReadResult2;
        }
        ArrayList items = new ArrayList();
        items.add("Origem: ".concat(dtcReadResult.origin == DataOrigin.SIMULATED ? "SIMULAÇÃO" : "conforme capturas do veículo"));
        items.add("Antes: " + Instant.ofEpochMilli(dtcReadResult.completedAtEpochMillis));
        items.add("Depois: " + Instant.ofEpochMilli(dtcReadResult2.completedAtEpochMillis));
        for (DtcReadResult.ServiceResult serviceResult : dtcReadResult.services) {
            DtcReadResult.ServiceResult serviceResult2 = null;
            for (DtcReadResult.ServiceResult service : dtcReadResult2.services) {
                if (service.category == serviceResult.category) {
                    serviceResult2 = service;
                }
            }
            items.add(DtcFormatter.category(serviceResult.category));
            if (serviceResult2 == null || serviceResult.status.outcome != DtcReadResult.Outcome.POSITIVE || serviceResult2.status.outcome != DtcReadResult.Outcome.POSITIVE) {
                StringBuilder builder = new StringBuilder("Não comparável: ");
                builder.append(DtcFormatter.outcome(serviceResult.status.outcome));
                builder.append(" → ");
                builder.append(serviceResult2 == null ? "não consultado" : DtcFormatter.outcome(serviceResult2.status.outcome));
                items.add(builder.toString());
            } else {
                Set<String> codes = codes(serviceResult);
                Set<String> codes2 = codes(serviceResult2);
                TreeSet treeSet = new TreeSet(codes2);
                TreeSet seen = new TreeSet(codes);
                TreeSet seen2 = new TreeSet(codes2);
                treeSet.removeAll(codes);
                seen.removeAll(codes2);
                seen2.retainAll(codes);
                items.add("Novos nesta captura: " + display(treeSet));
                items.add("Presentes nas duas: " + display(seen2));
                items.add("Não retornaram na segunda: " + display(seen));
            }
        }
        items.add("Um código que não retornou não comprova reparo. Condições de uso e identificação da ECU não foram equiparadas. Esta comparação não confirma uma causa.");
        return Collections.unmodifiableList(items);
    }

    private static Set<String> codes(DtcReadResult.ServiceResult serviceResult) {
        TreeSet treeSet = new TreeSet();
        Iterator<DtcObservation> it = serviceResult.observations.iterator();
        while (it.hasNext()) {
            treeSet.add(it.next().code);
        }
        return treeSet;
    }

    private static String display(Set<String> set) {
        return set.isEmpty() ? "nenhum" : String.join(", ", set);
    }
}