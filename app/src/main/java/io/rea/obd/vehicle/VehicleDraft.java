package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import io.rea.obd.vehicle.catalog.VehicleSearch;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

public final class VehicleDraft {
    public static final List<VehicleAttribute.Key> GUIDED = Collections.unmodifiableList(Arrays.asList(VehicleAttribute.Key.MAKE, VehicleAttribute.Key.MODEL, VehicleAttribute.Key.MODEL_YEAR, VehicleAttribute.Key.TRIM, VehicleAttribute.Key.ENGINE, VehicleAttribute.Key.TRANSMISSION, VehicleAttribute.Key.FUEL_TYPE));
    private final EnumMap<VehicleAttribute.Key, VehicleAttribute> attrs;
    public final String id;
    public String name;
    public final VehicleRecord original;

    public VehicleDraft(VehicleRecord vehicleRecord) {
        this(vehicleRecord, vehicleRecord == null ? UUID.randomUUID().toString() : vehicleRecord.id, vehicleRecord == null ? "" : vehicleRecord.name, vehicleRecord == null ? Collections.emptyMap() : vehicleRecord.attributes());
    }

    public VehicleDraft(VehicleRecord vehicleRecord, String id, String name, Map<VehicleAttribute.Key, VehicleAttribute> map) {
        this.attrs = new EnumMap<>(VehicleAttribute.Key.class);
        this.original = vehicleRecord;
        this.id = id;
        this.name = name;
        for (VehicleAttribute.Key key : VehicleAttribute.Key.values()) {
            this.attrs.put(key, map.containsKey(key) ? map.get(key) : VehicleAttribute.unknown(key));
        }
        if (vehicleRecord != null && !id.equals(vehicleRecord.id)) {
            throw new IllegalArgumentException("Rascunho de outro perfil");
        }
    }

    public VehicleAttribute attribute(VehicleAttribute.Key key) {
        return this.attrs.get(key);
    }

    public Map<VehicleAttribute.Key, VehicleAttribute> attributes() {
        return Collections.unmodifiableMap(this.attrs);
    }

    private Map<VehicleAttribute.Key, String> parents(VehicleAttribute.Key key) {
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        if (attribute(VehicleAttribute.Key.MARKET).value != null) {
            enumMap.put(VehicleAttribute.Key.MARKET, attribute(VehicleAttribute.Key.MARKET).value);
        }
        int indexOf = GUIDED.indexOf(key);
        for (int i = 0; i < indexOf; i++) {
            VehicleAttribute attribute = attribute(GUIDED.get(i));
            if (attribute.value != null) {
                enumMap.put(attribute.key, attribute.value);
            }
        }
        return enumMap;
    }

    public List<String> options(VehicleCatalog vehicleCatalog, VehicleAttribute.Key key) {
        if (!GUIDED.contains(key)) {
            return Collections.emptyList();
        }
        Collator collator = Collator.getInstance(new Locale("pt", "BR"));
        collator.setStrength(Collator.SECONDARY);
        Comparator<String> order = key == VehicleAttribute.Key.MODEL_YEAR ? Comparator.<String>reverseOrder() : (a, b) -> {
            int compared = collator.compare(a, b);
            return compared != 0 ? compared : a.compareTo(b);
        };
        TreeSet<String> treeSet = new TreeSet<>(order);
        for (VehicleCatalog.Configuration configuration : vehicleCatalog.matching(parents(key))) {
            VehicleAttribute vehicleAttribute = configuration.attributes.get(key);
            if (vehicleAttribute != null) {
                treeSet.add(vehicleAttribute.value);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(treeSet));
    }

    public String option(VehicleCatalog vehicleCatalog, VehicleAttribute.Key key, String text) {
        if (vehicleCatalog == null || text == null) {
            return null;
        }
        String wanted = VehicleSearch.normalize(text);
        for (String option : options(vehicleCatalog, key)) {
            if (VehicleSearch.normalize(option).equals(wanted)) {
                return option;
            }
        }
        return null;
    }

    public void selectConfiguration(VehicleCatalog vehicleCatalog, String id) {
        for (VehicleAttribute.Key key : GUIDED) {
            this.attrs.put(key, VehicleAttribute.unknown(key));
        }
        chooseConfiguration(vehicleCatalog, id);
    }

    private void clearChildren(VehicleAttribute.Key key) {
        int indexOf = GUIDED.indexOf(key);
        if (indexOf >= 0) {
            while (true) {
                indexOf++;
                List<VehicleAttribute.Key> list = GUIDED;
                if (indexOf >= list.size()) {
                    break;
                } else {
                    this.attrs.put(list.get(indexOf), VehicleAttribute.unknown(list.get(indexOf)));
                }
            }
        }
        if (key == VehicleAttribute.Key.MARKET) {
            for (VehicleAttribute.Key key2 : GUIDED) {
                if (attribute(key2).provenance != null && attribute(key2).provenance.origin == FactProvenance.Origin.CATALOG) {
                    this.attrs.put(key2, VehicleAttribute.unknown(key2));
                }
            }
        }
    }

    public void unknown(VehicleAttribute.Key key) {
        if (attribute(key).value != null) {
            clearChildren(key);
            this.attrs.put(key, VehicleAttribute.unknown(key));
        }
    }

    public void declare(VehicleAttribute.Key key, String text, long now) {
        String required4 = (text == null || text.trim().isEmpty()) ? null : text.trim();
        if (Objects.equals(required4, attribute(key).value)) {
            return;
        }
        VehicleAttribute unknown = required4 == null ? VehicleAttribute.unknown(key) : new VehicleAttribute(key, required4, FactProvenance.declared(Long.valueOf(now)));
        clearChildren(key);
        this.attrs.put(key, unknown);
    }

    public void confirmDeclaration(VehicleAttribute.Key key, String text, long now) {
        declare(key, text, now);
        VehicleAttribute attribute = attribute(key);
        if (attribute.value == null || attribute.provenance.origin == FactProvenance.Origin.USER_INPUT) {
            return;
        }
        this.attrs.put(key, new VehicleAttribute(key, attribute.value, FactProvenance.declared(Long.valueOf(now))));
    }

    public void choose(VehicleCatalog vehicleCatalog, VehicleAttribute.Key key, String text) {
        if (!options(vehicleCatalog, key).contains(text)) {
            throw new IllegalArgumentException("Opção fora das configurações disponíveis");
        }
        clearChildren(key);
        Map<VehicleAttribute.Key, String> parents = parents(key);
        parents.put(key, text);
        VehicleCatalog.Configuration configuration = vehicleCatalog.matching(parents).get(0);
        this.attrs.put(key, configuration.attributes.get(key));
        ArrayList<VehicleAttribute.Key> items = new ArrayList(GUIDED);
        items.add(VehicleAttribute.Key.MARKET);
        for (VehicleAttribute.Key item : items) {
            VehicleAttribute attribute = attribute(item);
            if (attribute.provenance != null && attribute.provenance.origin == FactProvenance.Origin.CATALOG) {
                VehicleAttribute vehicleAttribute = configuration.attributes.get(item);
                if (vehicleAttribute == null || !Objects.equals(vehicleAttribute.value, attribute.value)) {
                    throw new IllegalArgumentException("Configuração incoerente");
                }
                this.attrs.put(item, vehicleAttribute);
            }
        }
    }

    public void chooseConfiguration(VehicleCatalog vehicleCatalog, String id) {
        VehicleCatalog.Configuration configuration = vehicleCatalog.configurations.get(id);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuração indisponível");
        }
        VehicleAttribute.Key[] keys = {VehicleAttribute.Key.MAKE, VehicleAttribute.Key.MODEL, VehicleAttribute.Key.MODEL_YEAR};
        for (int i = 0; i < 3; i++) {
            VehicleAttribute.Key key = keys[i];
            String text = attribute(key).value;
            VehicleAttribute vehicleAttribute = configuration.attributes.get(key);
            if (text != null && (vehicleAttribute == null || !text.equals(vehicleAttribute.value))) {
                throw new IllegalArgumentException("Configuração não corresponde ao veículo escolhido");
            }
        }
        for (VehicleAttribute.Key key2 : GUIDED) {
            this.attrs.put(key2, configuration.attributes.containsKey(key2) ? configuration.attributes.get(key2) : VehicleAttribute.unknown(key2));
        }
        this.attrs.put(VehicleAttribute.Key.MARKET, configuration.attributes.get(VehicleAttribute.Key.MARKET));
    }

    public int expectedRevision() {
        VehicleRecord vehicleRecord = this.original;
        if (vehicleRecord == null) {
            return 0;
        }
        return vehicleRecord.revision;
    }

    public VehicleRecord result() {
        boolean flag = this.original == null || !Objects.equals(this.name.trim(), this.original.name);
        if (this.original != null) {
            for (VehicleAttribute.Key key : VehicleAttribute.Key.values()) {
                if (!this.attrs.get(key).equals(this.original.attribute(key))) {
                    flag = true;
                }
            }
        }
        return flag ? new VehicleRecord(this.id, this.name, expectedRevision() + 1, this.attrs) : this.original;
    }
}