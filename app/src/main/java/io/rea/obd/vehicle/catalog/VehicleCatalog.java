package io.rea.obd.vehicle.catalog;

import io.rea.obd.vehicle.VehicleAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VehicleCatalog {
    public final Map<String, Configuration> configurations;
    public final String coverageNote;
    public final Set<String> markets;
    public final int sequence;
    public final Map<String, Source> sources;
    public final String version;

    public static final class Source {
        public final String accessedOn;
        public final String documentVersion;
        public final Set<VehicleAttribute.Key> fields;
        public final String id;
        public final Set<String> markets;
        public final String permission;
        public final String permissionReference;
        public final String publisher;
        public final String reviewedBy;
        public final String reviewedOn;
        public final String section;
        public final String title;
        public final String url;

        public Source(String id, String title, String publisher, String documentVersion, String url, String section, String accessedOn, String permission, String permissionReference, String reviewedBy, String reviewedOn, Set<String> set, Set<VehicleAttribute.Key> enumSetNoneOf) {
            this.id = id;
            this.title = title;
            this.publisher = publisher;
            this.documentVersion = documentVersion;
            this.url = url;
            this.section = section;
            this.accessedOn = accessedOn;
            this.permission = permission;
            this.permissionReference = permissionReference;
            this.reviewedBy = reviewedBy;
            this.reviewedOn = reviewedOn;
            this.markets = Collections.unmodifiableSet(new HashSet(set));
            this.fields = Collections.unmodifiableSet(new HashSet(enumSetNoneOf));
        }
    }

    public static final class Configuration {
        public final Map<VehicleAttribute.Key, VehicleAttribute> attributes;
        public final String id;
        public final String market;

        public Configuration(String id, String market, Map<VehicleAttribute.Key, VehicleAttribute> map) {
            this.id = id;
            this.market = market;
            this.attributes = Collections.unmodifiableMap(new EnumMap(map));
        }
    }

    VehicleCatalog(String version, int i, String coverageNote, Set<String> set, Map<String, Source> map, Map<String, Configuration> map2) {
        this.version = version;
        this.sequence = i;
        this.coverageNote = coverageNote;
        this.markets = Collections.unmodifiableSet(new HashSet(set));
        this.sources = Collections.unmodifiableMap(new LinkedHashMap(map));
        this.configurations = Collections.unmodifiableMap(new LinkedHashMap(map2));
    }

    public List<Configuration> matching(Map<VehicleAttribute.Key, String> map) {
        VehicleAttribute vehicleAttribute;
        ArrayList items = new ArrayList();
        for (Configuration configuration : this.configurations.values()) {
            Iterator<Map.Entry<VehicleAttribute.Key, String>> it = map.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    items.add(configuration);
                    break;
                }
                Map.Entry<VehicleAttribute.Key, String> next = it.next();
                if (next.getValue() != null && ((vehicleAttribute = configuration.attributes.get(next.getKey())) == null || !next.getValue().equals(vehicleAttribute.value))) {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(items);
    }
}