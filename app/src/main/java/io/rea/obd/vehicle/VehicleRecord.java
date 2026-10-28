package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class VehicleRecord {
    private final Map<VehicleAttribute.Key, VehicleAttribute> attributes;
    public final String id;
    public final String name;
    public final int revision;

    public VehicleRecord(String id, String name, int i, Map<VehicleAttribute.Key, VehicleAttribute> map) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty() || i < 1 || map == null) {
            throw new IllegalArgumentException("Perfil incompleto");
        }
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        for (VehicleAttribute.Key key : VehicleAttribute.Key.values()) {
            VehicleAttribute unknown = map.get(key);
            unknown = unknown == null ? VehicleAttribute.unknown(key) : unknown;
            if (unknown.key != key) {
                throw new IllegalArgumentException("Chave de atributo divergente");
            }
            enumMap.put(key, unknown);
        }
        this.id = id;
        this.name = name.trim();
        this.revision = i;
        this.attributes = Collections.unmodifiableMap(enumMap);
    }

    public Map<VehicleAttribute.Key, VehicleAttribute> attributes() {
        return this.attributes;
    }

    public VehicleAttribute attribute(VehicleAttribute.Key key) {
        return this.attributes.get(key);
    }

    public String value(VehicleAttribute.Key key) {
        return attribute(key).value;
    }

    private String display(VehicleAttribute.Key key) {
        String text = value(key);
        return text == null ? "" : text;
    }

    public VehicleProfile projection() {
        String text = value(VehicleAttribute.Key.MODEL_YEAR);
        return new VehicleProfile(this.id, this.name, display(VehicleAttribute.Key.MAKE), display(VehicleAttribute.Key.MODEL), text == null ? null : Integer.valueOf(text), display(VehicleAttribute.Key.MARKET), display(VehicleAttribute.Key.TRIM), display(VehicleAttribute.Key.ENGINE), display(VehicleAttribute.Key.FUEL_TYPE), display(VehicleAttribute.Key.TRANSMISSION), display(VehicleAttribute.Key.NOTES));
    }

    public static VehicleRecord legacy(VehicleProfile vehicleProfile) {
        return from(vehicleProfile, FactProvenance.legacy());
    }

    public static VehicleRecord declared(VehicleProfile vehicleProfile, long currentTimeMillis) {
        return from(vehicleProfile, FactProvenance.declared(Long.valueOf(currentTimeMillis)));
    }

    private static VehicleRecord from(VehicleProfile vehicleProfile, FactProvenance factProvenance) {
        Map<VehicleAttribute.Key, String> values = values(vehicleProfile);
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        for (Map.Entry<VehicleAttribute.Key, String> entry : values.entrySet()) {
            String required4 = entry.getValue();
            enumMap.put(entry.getKey(), (required4 == null || required4.isEmpty()) ? VehicleAttribute.unknown(entry.getKey()) : new VehicleAttribute(entry.getKey(), required4, factProvenance));
        }
        return new VehicleRecord(vehicleProfile.id, vehicleProfile.name, 1, enumMap);
    }

    public VehicleRecord editDeclaration(VehicleProfile vehicleProfile, long currentTimeMillis) {
        if (!this.id.equals(vehicleProfile.id)) {
            throw new IllegalArgumentException("Edição não pode trocar identificador");
        }
        EnumMap enumMap = new EnumMap(this.attributes);
        boolean flag = !this.name.equals(vehicleProfile.name);
        for (Map.Entry<VehicleAttribute.Key, String> entry : values(vehicleProfile).entrySet()) {
            String required4 = entry.getValue();
            if (required4 != null && required4.isEmpty()) {
                required4 = null;
            }
            if (!Objects.equals(required4, value(entry.getKey()))) {
                enumMap.put(entry.getKey(), required4 == null ? VehicleAttribute.unknown(entry.getKey()) : new VehicleAttribute(entry.getKey(), required4, FactProvenance.declared(Long.valueOf(currentTimeMillis))));
                flag = true;
            }
        }
        return flag ? new VehicleRecord(this.id, vehicleProfile.name, this.revision + 1, enumMap) : this;
    }

    private static Map<VehicleAttribute.Key, String> values(VehicleProfile vehicleProfile) {
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        enumMap.put(VehicleAttribute.Key.MAKE, vehicleProfile.make);
        enumMap.put(VehicleAttribute.Key.MODEL, vehicleProfile.model);
        enumMap.put(VehicleAttribute.Key.MODEL_YEAR, vehicleProfile.year == null ? null : vehicleProfile.year.toString());
        enumMap.put(VehicleAttribute.Key.MARKET, vehicleProfile.market);
        enumMap.put(VehicleAttribute.Key.TRIM, vehicleProfile.trim);
        enumMap.put(VehicleAttribute.Key.ENGINE, vehicleProfile.engine);
        enumMap.put(VehicleAttribute.Key.FUEL_TYPE, vehicleProfile.fuel);
        enumMap.put(VehicleAttribute.Key.TRANSMISSION, vehicleProfile.transmission);
        enumMap.put(VehicleAttribute.Key.NOTES, vehicleProfile.notes);
        return enumMap;
    }
}