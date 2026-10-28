package io.rea.obd;

import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ObdReader {
    private final Clock clock;
    private final ElmSession session;

    public interface Clock {
        long elapsedMillis();

        long epochMillis();
    }

    public static final class Batch {
        public final Double adapterVoltage;
        public final IOException failure;
        public final List<TelemetrySample> samples;
        public final long voltageElapsedMillis;

        Batch(List<TelemetrySample> list, Double adapterVoltage, long voltageElapsedMillis, IOException failure) {
            this.samples = Collections.unmodifiableList(list);
            this.adapterVoltage = adapterVoltage;
            this.voltageElapsedMillis = voltageElapsedMillis;
            this.failure = failure;
        }
    }

    public ObdReader(ElmSession elmSession, Clock clock) {
        this.session = elmSession;
        this.clock = clock;
    }

    public Batch poll(Collection<Integer> collection) {
        ArrayList<TelemetrySample> items = new ArrayList<>();
        IOException failure = null;
        for (Integer num : collection) {
            int intValue = num.intValue();
            String message = "";
            Double value = null;
            TelemetrySample.Status status;
            if (failure != null) {
                status = TelemetrySample.Status.NOT_READ;
            } else if (!this.session.supported().contains(num)) {
                status = TelemetrySample.Status.UNSUPPORTED;
            } else {
                try {
                    value = ObdParser.pid(this.session.command(String.format(Locale.ROOT, "01%02X", num), 5000L), intValue);
                    status = value == null ? TelemetrySample.Status.NO_DATA : TelemetrySample.Status.VALID;
                } catch (IOException e) {
                    failure = e;
                    message = e.getMessage();
                    status = e instanceof ElmSession.ResponseTimeoutException ? TelemetrySample.Status.TIMEOUT : TelemetrySample.Status.DISCONNECTED;
                } catch (IllegalArgumentException e) {
                    message = e.getMessage();
                    status = TelemetrySample.Status.INVALID;
                }
            }
            items.add(new TelemetrySample(intValue, value, status, DataOrigin.VEHICLE, this.clock.epochMillis(), this.clock.elapsedMillis(), message));
        }
        Double voltage = null;
        long elapsedMillis = 0;
        IOException batchFailure = failure;
        if (failure == null) {
            try {
                voltage = ObdParser.voltage(this.session.command("ATRV", 3000L));
                elapsedMillis = this.clock.elapsedMillis();
            } catch (IOException e) {
                batchFailure = e;
            }
        }
        return new Batch(items, voltage, elapsedMillis, batchFailure);
    }

    public DtcReadResult readCodes(String id, String sessionId) {
        DtcReadResult.ServiceResult category;
        if (sessionId == null || sessionId.isEmpty() || (id != null && id.isEmpty())) {
            throw new IllegalArgumentException("Contexto da leitura inválido");
        }
        String string = UUID.randomUUID().toString();
        long epochMillis = this.clock.epochMillis();
        DtcReadResult.MilResult mil = readMil(string);
        ArrayList items = new ArrayList();
        boolean transportFailed = mil.status.transportFailed();
        for (DtcObservation.Category category2 : DtcObservation.Category.values()) {
            if (transportFailed) {
                category = new DtcReadResult.ServiceResult(category2, new DtcReadResult.Status(DtcReadResult.Outcome.NOT_READ, null, null, 0, Collections.emptyList(), "Não consultado após falha no transporte"), Collections.emptyList());
            } else {
                category = readCategory(string, category2);
            }
            items.add(category);
            transportFailed |= category.status.transportFailed();
        }
        return new DtcReadResult(string, id, sessionId, DataOrigin.VEHICLE, epochMillis, this.clock.epochMillis(), mil, items);
    }

    private DtcReadResult.MilResult readMil(String text) {
        long epochMillis = this.clock.epochMillis();
        try {
            ObdParser.DecodedDiagnostic<Integer> mil = ObdParser.decodeMil(this.session.command("0101", 5000L));
            long epochMillis2 = this.clock.epochMillis();
            ArrayList items = new ArrayList();
            for (int i = 0; i < mil.values.size(); i++) {
                items.add(new DtcReadResult.MilObservation(text + ":mil:" + i, mil.values.get(i).intValue(), epochMillis2));
            }
            return new DtcReadResult.MilResult(decodedStatus(mil, epochMillis, epochMillis2), items);
        } catch (IOException | IllegalArgumentException e) {
            return new DtcReadResult.MilResult(failureStatus(e, epochMillis), Collections.emptyList());
        }
    }

    private DtcReadResult.ServiceResult readCategory(String text, DtcObservation.Category category) {
        String text2 = ":";
        long epochMillis = this.clock.epochMillis();
        try {
            ObdParser.DecodedDiagnostic<String> dtcs = ObdParser.decodeDtcs(this.session.command(String.format(Locale.ROOT, "%02X", Integer.valueOf(category.service)), 10000L), category.service, this.session.isCan());
            long epochMillis2 = this.clock.epochMillis();
            ArrayList items = new ArrayList();
            for (String value : dtcs.values) {
                items.add(new DtcObservation(text + text2 + category.name() + text2 + value, value, category, epochMillis2));
                text2 = text2;
            }
            return new DtcReadResult.ServiceResult(category, decodedStatus(dtcs, epochMillis, epochMillis2), items);
        } catch (IOException | IllegalArgumentException e) {
            return new DtcReadResult.ServiceResult(category, failureStatus(e, epochMillis), Collections.emptyList());
        }
    }

    private DtcReadResult.Status decodedStatus(ObdParser.DecodedDiagnostic<?> decodedDiagnostic, long epochMillis, long epochMillis2) {
        return new DtcReadResult.Status(decodedDiagnostic.outcome, Long.valueOf(epochMillis), Long.valueOf(epochMillis2), decodedDiagnostic.positiveResponseCount, decodedDiagnostic.negativeResponses, decodedDiagnostic.detail);
    }

    private DtcReadResult.Status failureStatus(Exception exc, long epochMillis) {
        DtcReadResult.Outcome outcome;
        if (exc instanceof ElmSession.ResponseTimeoutException) {
            outcome = DtcReadResult.Outcome.TIMEOUT;
        } else {
            outcome = exc instanceof IOException ? DtcReadResult.Outcome.DISCONNECTED : DtcReadResult.Outcome.INVALID;
        }
        return new DtcReadResult.Status(outcome, Long.valueOf(epochMillis), Long.valueOf(this.clock.epochMillis()), 0, Collections.emptyList(), exc.getMessage());
    }

    public SupplementalReadResult readSupplement(String vehicleId, String sessionId) {
        if (vehicleId == null || vehicleId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("Contexto complementar inválido");
        }
        String string = UUID.randomUUID().toString();
        long epochMillis = this.clock.epochMillis();
        long requestedAt = this.clock.epochMillis();
        SupplementalReadResult.Readiness readiness;
        try {
            ObdParser.DecodedDiagnostic<SupplementalReadResult.ReadinessBits> decoded = ObdParser.decodeReadiness(this.session.command("0101", 7000L));
            long respondedAt = this.clock.epochMillis();
            ArrayList<SupplementalReadResult.ReadinessObservation> observations = new ArrayList<>();
            for (int i = 0; i < decoded.values.size(); i++) {
                observations.add(new SupplementalReadResult.ReadinessObservation(string + ":ready:" + i, decoded.values.get(i), respondedAt));
            }
            readiness = new SupplementalReadResult.Readiness(decodedStatus(decoded, requestedAt, respondedAt), observations);
        } catch (IOException | IllegalArgumentException e) {
            readiness = new SupplementalReadResult.Readiness(failureStatus(e, requestedAt), Collections.emptyList());
        }
        boolean transportFailed = readiness.status.transportFailed();
        ArrayList<SupplementalReadResult.FramePid> frames = new ArrayList<>();
        for (Integer num : SupplementalReadResult.FRAME_PIDS) {
            int intValue = num.intValue();
            ArrayList<SupplementalReadResult.FrameValue> values = new ArrayList<>();
            DtcReadResult.Status status;
            if (transportFailed) {
                status = new DtcReadResult.Status(DtcReadResult.Outcome.NOT_READ, null, null, 0, Collections.emptyList(), "Não consultado após falha no transporte");
            } else {
                long frameRequestedAt = this.clock.epochMillis();
                try {
                    ObdParser.DecodedDiagnostic<Double> decoded = ObdParser.decodeFreezePid(this.session.command(String.format(Locale.ROOT, "02%02X00", num), 7000L), intValue);
                    long frameRespondedAt = this.clock.epochMillis();
                    status = decodedStatus(decoded, frameRequestedAt, frameRespondedAt);
                    for (int i = 0; i < decoded.values.size(); i++) {
                        values.add(new SupplementalReadResult.FrameValue(string + ":frame00:" + intValue + ":" + i, decoded.values.get(i).doubleValue(), frameRespondedAt));
                    }
                } catch (IOException | IllegalArgumentException e) {
                    status = failureStatus(e, frameRequestedAt);
                }
            }
            frames.add(new SupplementalReadResult.FramePid(intValue, status, values));
            transportFailed |= status.transportFailed();
        }
        return new SupplementalReadResult(string, vehicleId, sessionId, DataOrigin.VEHICLE, epochMillis, this.clock.epochMillis(), readiness, frames);
    }

    public String readiness() throws IOException {
        List<String> list = ObdParser.readiness(this.session.command("0101", 7000L));
        return list.isEmpty() ? "Sem resposta de prontidão" : String.join("\n", list);
    }

    public String vehicleInfo() throws IOException {
        String vehicleInfo = ObdParser.vehicleInfo(this.session.command("0902", 12000L), 2);
        String vehicleInfo2 = ObdParser.vehicleInfo(this.session.command("0904", 12000L), 4);
        StringBuilder builder = new StringBuilder("VIN: ");
        if (vehicleInfo == null) {
            vehicleInfo = "não informado";
        }
        builder.append(vehicleInfo);
        builder.append("\nID de calibração: ");
        if (vehicleInfo2 == null) {
            vehicleInfo2 = "não informado";
        }
        builder.append(vehicleInfo2);
        return builder.toString();
    }

    public String freezeFrame() throws IOException {
        StringBuilder builder = new StringBuilder();
        int[] values = {12, 13, 5, 4, 17};
        for (int i = 0; i < 5; i++) {
            int value = values[i];
            Double freezePid = ObdParser.freezePid(this.session.command(String.format(Locale.ROOT, "02%02X00", Integer.valueOf(value)), 7000L), value);
            if (freezePid != null) {
                SensorCatalog.Sensor sensor = SensorCatalog.get(value);
                builder.append(sensor.name);
                builder.append(": ");
                builder.append(String.format(Locale.getDefault(), "%.1f", freezePid));
                builder.append(' ');
                builder.append(sensor.unit);
                builder.append('\n');
            }
        }
        return builder.length() == 0 ? "Nenhum quadro congelado 00 disponível" : builder.toString();
    }
}