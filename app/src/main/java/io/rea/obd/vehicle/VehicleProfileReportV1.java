package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class VehicleProfileReportV1 {
    private VehicleProfileReportV1() {
    }

    public static String title(VehicleAttribute.Key key) {
        switch (key) {
            case MAKE:
                return "Fabricante";
            case MODEL:
                return "Modelo";
            case MANUFACTURE_YEAR:
                return "Ano de fabricação";
            case MODEL_YEAR:
                return "Ano-modelo";
            case MARKET:
                return "Mercado / país";
            case TRIM:
                return "Versão";
            case ENGINE:
                return "Motor";
            case FUEL_TYPE:
                return "Combustível";
            case TRANSMISSION:
                return "Câmbio";
            default:
                return "Observações";
        }
    }

    public static String origin(VehicleAttribute vehicleAttribute) {
        if (vehicleAttribute.value == null) {
            return "Não sei · pendente";
        }
        switch (vehicleAttribute.provenance.origin) {
            case USER_INPUT:
                return "Declarado por você";
            case CATALOG:
                return "Documentado no catálogo · veículo não confirmado";
            default:
                return "Legado · origem pendente";
        }
    }

    public static List<String> report(VehicleRecord vehicleRecord) {
        ArrayList items = new ArrayList();
        items.add("Perfil: " + vehicleRecord.name + " · ID " + vehicleRecord.id + " · revisão " + vehicleRecord.revision);
        for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
            StringBuilder builder = new StringBuilder();
            builder.append(title(vehicleAttribute.key));
            builder.append(": ");
            builder.append(vehicleAttribute.value == null ? "Não informado" : vehicleAttribute.value);
            builder.append(" · ");
            builder.append(origin(vehicleAttribute));
            items.add(builder.toString());
            if (vehicleAttribute.provenance != null) {
                FactProvenance factProvenance = vehicleAttribute.provenance;
                Iterator<String> it = factProvenance.references.iterator();
                while (it.hasNext()) {
                    items.add("Referência: " + it.next());
                }
                if (!factProvenance.scope.isEmpty()) {
                    items.add("Escopo: " + factProvenance.scope);
                }
                if (factProvenance.recordedAtMillis != null) {
                    items.add("Registro da declaração: " + Instant.ofEpochMilli(factProvenance.recordedAtMillis.longValue()));
                }
            }
        }
        items.add("Identificação do perfil não comprova compatibilidade OBD.");
        return Collections.unmodifiableList(items);
    }
}