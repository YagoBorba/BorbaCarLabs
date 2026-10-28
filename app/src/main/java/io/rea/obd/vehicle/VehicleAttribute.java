package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import java.util.Objects;

public final class VehicleAttribute {
    public final Key key;
    public final FactProvenance provenance;
    public final String value;

    public enum Key {
        MAKE,
        MODEL,
        MANUFACTURE_YEAR,
        MODEL_YEAR,
        MARKET,
        TRIM,
        ENGINE,
        FUEL_TYPE,
        TRANSMISSION,
        NOTES
    }

    public VehicleAttribute(Key key, String required4, FactProvenance factProvenance) {
        if (key == null) {
            throw new IllegalArgumentException("Atributo sem chave");
        }
        String text = (required4 == null || required4.trim().isEmpty()) ? null : required4.trim();
        if ((text == null) != (factProvenance == null)) {
            throw new IllegalArgumentException("Desconhecido não recebe fato ou proveniência fictícia");
        }
        if (text != null && ((key == Key.MODEL_YEAR || key == Key.MANUFACTURE_YEAR) && (!text.matches("[0-9]{4}") || Integer.parseInt(text) < 1886 || Integer.parseInt(text) > 2100))) {
            throw new IllegalArgumentException("Ano inválido");
        }
        if (factProvenance != null && factProvenance.origin != FactProvenance.Origin.USER_INPUT && factProvenance.origin != FactProvenance.Origin.UNKNOWN_LEGACY && factProvenance.origin != FactProvenance.Origin.CATALOG) {
            throw new IllegalArgumentException("Identidade do perfil exige declaração, legado ou catálogo; observação não confirma configuração automaticamente");
        }
        this.key = key;
        this.value = text;
        this.provenance = factProvenance;
    }

    public static VehicleAttribute unknown(Key key) {
        return new VehicleAttribute(key, null, null);
    }

    public boolean equals(Object value) {
        if (!(value instanceof VehicleAttribute)) {
            return false;
        }
        VehicleAttribute vehicleAttribute = (VehicleAttribute) value;
        return this.key == vehicleAttribute.key && Objects.equals(this.value, vehicleAttribute.value) && Objects.equals(this.provenance, vehicleAttribute.provenance);
    }

    public int hashCode() {
        return Objects.hash(this.key, this.value, this.provenance);
    }
}