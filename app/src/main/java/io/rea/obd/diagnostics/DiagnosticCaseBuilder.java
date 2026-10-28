package io.rea.obd.diagnostics;

import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleRecord;
import java.util.List;
import java.util.UUID;

public final class DiagnosticCaseBuilder {
    private DiagnosticCaseBuilder() {
    }

    public static DiagnosticCase createWithProfile(VehicleRecord vehicleRecord, String currentDtcAdapter, DtcReadResult dtcReadResult, List<DiagnosticCase.Sample> list, DiagnosticCase.Symptoms symptoms, long epochMillis, SupplementalReadResult supplementalReadResult) {
        return new DiagnosticCase(4, UUID.randomUUID().toString(), 1, epochMillis, epochMillis, vehicleRecord.projection(), currentDtcAdapter, dtcReadResult, list, symptoms, supplementalReadResult, vehicleRecord);
    }

    public static DiagnosticCase create(VehicleProfile vehicleProfile, String currentDtcAdapter, DtcReadResult dtcReadResult, List<DiagnosticCase.Sample> list, DiagnosticCase.Symptoms symptoms, long now) {
        return create(vehicleProfile, currentDtcAdapter, dtcReadResult, list, symptoms, now, null);
    }

    public static DiagnosticCase create(VehicleProfile vehicleProfile, String currentDtcAdapter, DtcReadResult dtcReadResult, List<DiagnosticCase.Sample> list, DiagnosticCase.Symptoms symptoms, long epochMillis, SupplementalReadResult supplementalReadResult) {
        return new DiagnosticCase(3, UUID.randomUUID().toString(), 1, epochMillis, epochMillis, vehicleProfile, currentDtcAdapter, dtcReadResult, list, symptoms, supplementalReadResult);
    }
}