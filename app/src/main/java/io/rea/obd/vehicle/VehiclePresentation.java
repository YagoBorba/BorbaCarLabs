package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import java.util.List;

public final class VehiclePresentation {
    private VehiclePresentation() {
    }

    public static String title(VehicleAttribute.Key key) {
        return VehicleProfileReportV1.title(key);
    }

    public static String origin(VehicleAttribute vehicleAttribute) {
        return VehicleProfileReportV1.origin(vehicleAttribute);
    }

    public static String compactOrigin(VehicleAttribute vehicleAttribute) {
        switch (vehicleAttribute.provenance.origin) {
            case CATALOG:
                return "Catálogo · veículo não confirmado";
            case USER_INPUT:
                return "Informado por você";
            case UNKNOWN_LEGACY:
                return "Origem a revisar";
            default:
                return origin(vehicleAttribute);
        }
    }

    public static List<String> report(VehicleRecord vehicleRecord) {
        return VehicleProfileReportV1.report(vehicleRecord);
    }
}