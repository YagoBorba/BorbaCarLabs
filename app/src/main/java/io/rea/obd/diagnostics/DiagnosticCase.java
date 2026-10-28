package io.rea.obd.diagnostics;

import io.rea.obd.telemetry.DashboardState;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class DiagnosticCase {
    public final String adapter;
    public final long createdAtEpochMillis;
    public final DtcReadResult dtc;
    public final String id;
    public final long revisedAtEpochMillis;
    public final int revision;
    public final int schemaVersion;
    public final SupplementalReadResult supplement;
    public final Symptoms symptoms;
    public final List<Sample> telemetry;
    public final VehicleProfile vehicle;
    public final VehicleRecord vehicleRecord;

    public static final class Symptoms {
        public final String conditions;
        public final String description;
        public final String frequency;
        public final String when;

        public Symptoms(String description, String when, String frequency, String conditions) {
            this.description = clean(description);
            this.when = clean(when);
            this.frequency = clean(frequency);
            this.conditions = clean(conditions);
        }

        private static String clean(String text) {
            String text2 = text == null ? "" : text.trim();
            if (text2.length() <= 2000) {
                return text2;
            }
            throw new IllegalArgumentException("Cada campo de sintomas aceita até 2000 caracteres");
        }

        public boolean isEmpty() {
            return this.description.isEmpty() && this.when.isEmpty() && this.frequency.isEmpty() && this.conditions.isEmpty();
        }
    }

    public static final class Sample {
        public final TelemetrySample measurement;
        public final String sessionId;
        public final String vehicleId;

        public Sample(String vehicleId, String sessionId, TelemetrySample telemetrySample) {
            if (vehicleId == null || vehicleId.isEmpty() || sessionId == null || sessionId.isEmpty() || telemetrySample == null || telemetrySample.pid < 0 || telemetrySample.pid > 255 || telemetrySample.epochMillis < 0) {
                throw new IllegalArgumentException("Amostra sem contexto válido");
            }
            this.vehicleId = vehicleId;
            this.sessionId = sessionId;
            this.measurement = telemetrySample;
        }
    }

    public DiagnosticCase(String id, int i, long createdAtEpochMillis, long revisedAtEpochMillis, VehicleProfile vehicleProfile, String adapter, DtcReadResult dtcReadResult, List<Sample> list, Symptoms symptoms) {
        this(1, id, i, createdAtEpochMillis, revisedAtEpochMillis, vehicleProfile, adapter, dtcReadResult, list, symptoms, null);
    }

    public DiagnosticCase(int i, String id, int revision, long createdAtEpochMillis, long revisedAtEpochMillis, VehicleProfile vehicleProfile, String adapter, DtcReadResult dtcReadResult, List<Sample> list, Symptoms symptoms, SupplementalReadResult supplementalReadResult) {
        this(i, id, revision, createdAtEpochMillis, revisedAtEpochMillis, vehicleProfile, adapter, dtcReadResult, list, symptoms, supplementalReadResult, null);
    }

    public DiagnosticCase(int i, String id, int revision, long createdAtEpochMillis, long revisedAtEpochMillis, VehicleProfile vehicleProfile, String adapter, DtcReadResult dtcReadResult, List<Sample> list, Symptoms symptoms, SupplementalReadResult supplementalReadResult, VehicleRecord vehicleRecord) {
        if (id != null && !id.isEmpty()) {
            if (revision >= 1 && createdAtEpochMillis >= 0 && revisedAtEpochMillis >= createdAtEpochMillis && vehicleProfile != null && dtcReadResult != null && symptoms != null && list != null) {
                if (i >= 1 && i <= 4 && (i != 1 || supplementalReadResult == null)) {
                    if ((i == 4) == (vehicleRecord != null)) {
                        if (vehicleRecord != null) {
                            VehicleProfile projection = vehicleRecord.projection();
                            if (!projection.id.equals(vehicleProfile.id) || !projection.name.equals(vehicleProfile.name) || !Objects.equals(projection.year, vehicleProfile.year) || !Arrays.asList(projection.make, projection.model, projection.market, projection.trim, projection.engine, projection.fuel, projection.transmission, projection.notes).equals(Arrays.asList(vehicleProfile.make, vehicleProfile.model, vehicleProfile.market, vehicleProfile.trim, vehicleProfile.engine, vehicleProfile.fuel, vehicleProfile.transmission, vehicleProfile.notes))) {
                                throw new IllegalArgumentException("Snapshot de perfil divergente da projeção");
                            }
                        }
                        if (!vehicleProfile.id.equals(dtcReadResult.vehicleId)) {
                            throw new IllegalArgumentException("Leitura pertence a outro veículo");
                        }
                        if (supplementalReadResult != null && (!vehicleProfile.id.equals(supplementalReadResult.vehicleId) || !dtcReadResult.sessionId.equals(supplementalReadResult.sessionId) || dtcReadResult.origin != supplementalReadResult.origin)) {
                            throw new IllegalArgumentException("Complemento de outro veículo, sessão ou origem");
                        }
                        HashSet seen = new HashSet();
                        for (Sample sample : list) {
                            if (sample == null || !vehicleProfile.id.equals(sample.vehicleId) || !dtcReadResult.sessionId.equals(sample.sessionId) || dtcReadResult.origin != sample.measurement.origin) {
                                throw new IllegalArgumentException("Mistura de veículos, sessões ou origens");
                            }
                            if (!seen.add(Integer.valueOf(sample.measurement.pid))) {
                                throw new IllegalArgumentException("PID duplicado no snapshot");
                            }
                        }
                        this.id = id;
                        this.revision = revision;
                        this.createdAtEpochMillis = createdAtEpochMillis;
                        this.revisedAtEpochMillis = revisedAtEpochMillis;
                        this.vehicle = vehicleProfile;
                        this.adapter = adapter == null ? "" : adapter;
                        this.dtc = dtcReadResult;
                        this.telemetry = Collections.unmodifiableList(new ArrayList(list));
                        this.symptoms = symptoms;
                        this.schemaVersion = i;
                        this.supplement = supplementalReadResult;
                        this.vehicleRecord = vehicleRecord;
                        return;
                    }
                }
                throw new IllegalArgumentException("Versão de caso incompatível");
            }
        }
        throw new IllegalArgumentException("Caso incompleto");
    }

    public DiagnosticCase revise(Symptoms symptoms, long epochMillis) {
        return new DiagnosticCase(this.schemaVersion, this.id, this.revision + 1, this.createdAtEpochMillis, Math.max(epochMillis, this.revisedAtEpochMillis), this.vehicle, this.adapter, this.dtc, this.telemetry, symptoms, this.supplement, this.vehicleRecord);
    }

    public boolean historical(Sample sample) {
        long value = this.dtc.completedAtEpochMillis - sample.measurement.epochMillis;
        return value < 0 || value > DashboardState.MAX_AGE_MS;
    }
}