package io.rea.obd.diagnostics;

import io.rea.obd.SensorCatalog;
import io.rea.obd.model.DataOrigin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SupplementalFormatter {
    private SupplementalFormatter() {
    }

    public static String monitor(SupplementalReadResult.MonitorState monitorState) {
        switch (monitorState) {
            case COMPLETE:
                return "pronto";
            case INCOMPLETE:
                return "não concluído";
            default:
                return "não suportado";
        }
    }

    public static List<String> readinessLines(List<SupplementalReadResult.ReadinessBits> list) {
        ArrayList items = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            for (SupplementalReadResult.Monitor monitor : list.get(i).monitors) {
                StringBuilder builder = new StringBuilder();
                builder.append(list.size() > 1 ? "Resposta " + (i + 1) + " · " : "");
                builder.append(monitor.name);
                builder.append(": ");
                builder.append(monitor(monitor.state));
                items.add(builder.toString());
            }
        }
        return items;
    }

    public static List<String> lines(SupplementalReadResult supplementalReadResult) {
        ArrayList items = new ArrayList();
        items.add(supplementalReadResult.origin == DataOrigin.SIMULATED ? "COMPLEMENTO SIMULADO" : "COMPLEMENTO DO VEÍCULO");
        items.add("Consulta: " + Instant.ofEpochMilli(supplementalReadResult.startedAtEpochMillis) + " a " + Instant.ofEpochMilli(supplementalReadResult.completedAtEpochMillis));
        StringBuilder builder = new StringBuilder("Prontidão desde apagar DTCs: ");
        builder.append(status(supplementalReadResult.readiness.status));
        items.add(builder.toString());
        int i = 0;
        while (i < supplementalReadResult.readiness.observations.size()) {
            SupplementalReadResult.ReadinessObservation readinessObservation = supplementalReadResult.readiness.observations.get(i);
            StringBuilder builder2 = new StringBuilder("Resposta ");
            i++;
            builder2.append(i);
            builder2.append(" · MIL ");
            builder2.append(readinessObservation.bits.milIlluminated() ? "acesa" : "apagada");
            builder2.append(" · contagem indicada ");
            builder2.append(readinessObservation.bits.reportedDtcCount());
            builder2.append(" · coletada em ");
            builder2.append(Instant.ofEpochMilli(readinessObservation.observedAtEpochMillis));
            items.add(builder2.toString());
            items.addAll(readinessLines(Collections.singletonList(readinessObservation.bits)));
        }
        items.add("Quadro congelado 00 · código associado desconhecido; sem identificação de ECU");
        for (SupplementalReadResult.FramePid framePid : supplementalReadResult.frame) {
            SensorCatalog.Sensor sensor = SensorCatalog.get(framePid.pid);
            items.add(sensor.name + ": " + status(framePid.status));
            int value = 0;
            while (value < framePid.observations.size()) {
                SupplementalReadResult.FrameValue frameValue = framePid.observations.get(value);
                StringBuilder builder2 = new StringBuilder("  Resposta ");
                value++;
                builder2.append(value);
                builder2.append(": ");
                builder2.append(Double.toString(frameValue.value));
                builder2.append(" ");
                builder2.append(sensor.unit);
                builder2.append(" · coletada em ");
                builder2.append(Instant.ofEpochMilli(frameValue.observedAtEpochMillis));
                items.add(builder2.toString());
            }
        }
        items.add("Horários indicam a coleta pelo app. Instante original do congelamento desconhecido; valores não são leituras ao vivo.");
        return items;
    }

    private static String status(DtcReadResult.Status status) {
        StringBuilder builder = new StringBuilder(DtcFormatter.outcome(status.outcome));
        for (DtcReadResult.NegativeResponse negativeResponse : status.negativeResponses) {
            builder.append(String.format(Locale.ROOT, " · 7F %02X %02X", Integer.valueOf(negativeResponse.service), Integer.valueOf(negativeResponse.code)));
        }
        return builder.toString();
    }
}