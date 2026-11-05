package io.rea.obd.vehicle.catalog;

import io.rea.obd.vehicle.VehicleAttribute;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class VehicleSearch {
    public static final class Result {
        public final List<VehicleCatalog.Configuration> matches;
        public final int total;

        Result(List<VehicleCatalog.Configuration> matches, int total) {
            this.matches = Collections.unmodifiableList(matches);
            this.total = total;
        }
    }

    private static final class Entry {
        final VehicleCatalog.Configuration configuration;
        final String text;
        final String make;
        final String model;
        final String year;
        final String trim;

        Entry(VehicleCatalog.Configuration configuration) {
            this.configuration = configuration;
            this.make = value(configuration, VehicleAttribute.Key.MAKE);
            this.model = value(configuration, VehicleAttribute.Key.MODEL);
            this.year = value(configuration, VehicleAttribute.Key.MODEL_YEAR);
            this.trim = value(configuration, VehicleAttribute.Key.TRIM);
            String engine = value(configuration, VehicleAttribute.Key.ENGINE);
            StringBuilder text = new StringBuilder(" ");
            for (String part : new String[]{this.make, this.model, this.year, this.trim, engine, engine.replace(".", ""), value(configuration, VehicleAttribute.Key.TRANSMISSION), value(configuration, VehicleAttribute.Key.FUEL_TYPE)}) {
                text.append(normalize(part)).append(' ');
            }
            text.append(normalize(this.model).replace(" ", "")).append(' ');
            this.text = text.toString();
        }
    }

    private static final Map<VehicleCatalog, List<Entry>> INDEX = new WeakHashMap<>();

    private VehicleSearch() {
    }

    public static String normalize(String text) {
        String plain = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.+]+", " ").trim();
    }

    private static String value(VehicleCatalog.Configuration configuration, VehicleAttribute.Key key) {
        VehicleAttribute attribute = configuration.attributes.get(key);
        return attribute == null || attribute.value == null ? "" : attribute.value;
    }

    private static synchronized List<Entry> index(VehicleCatalog catalog) {
        List<Entry> entries = INDEX.get(catalog);
        if (entries == null) {
            entries = new ArrayList<>(catalog.configurations.size());
            for (VehicleCatalog.Configuration configuration : catalog.configurations.values()) {
                entries.add(new Entry(configuration));
            }
            INDEX.put(catalog, entries);
        }
        return entries;
    }

    public static Result search(VehicleCatalog catalog, String query, int limit) {
        String normalized = normalize(query);
        if (catalog == null || normalized.isEmpty()) {
            return new Result(Collections.emptyList(), 0);
        }
        String[] tokens = normalized.split(" ");
        List<Entry> found = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (Entry entry : index(catalog)) {
            int score = 0;
            boolean all = true;
            for (String token : tokens) {
                if (entry.text.contains(" " + token + " ")) {
                    score += 3;
                } else if (entry.text.contains(" " + token)) {
                    score += 1;
                } else {
                    all = false;
                    break;
                }
            }
            if (all) {
                if (normalize(entry.model).equals(normalized) || normalize(entry.make + " " + entry.model).equals(normalized)) {
                    score += 5;
                }
                found.add(entry);
                scores.add(score);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            order.add(i);
        }
        Comparator<Integer> comparator = Comparator.comparing((Integer i) -> -scores.get(i));
        comparator = comparator.thenComparing(i -> found.get(i).year, Comparator.reverseOrder());
        comparator = comparator.thenComparing(i -> normalize(found.get(i).make));
        comparator = comparator.thenComparing(i -> normalize(found.get(i).model));
        comparator = comparator.thenComparing(i -> normalize(found.get(i).trim));
        order.sort(comparator);
        List<VehicleCatalog.Configuration> matches = new ArrayList<>();
        for (int i = 0; i < order.size() && matches.size() < limit; i++) {
            matches.add(found.get(order.get(i)).configuration);
        }
        return new Result(matches, found.size());
    }

    public static String headline(VehicleCatalog.Configuration configuration) {
        return (value(configuration, VehicleAttribute.Key.MAKE) + " " + value(configuration, VehicleAttribute.Key.MODEL) + " " + value(configuration, VehicleAttribute.Key.MODEL_YEAR)).trim();
    }

    public static String details(VehicleCatalog.Configuration configuration, boolean withYear) {
        List<String> parts = new ArrayList<>();
        VehicleAttribute.Key[] keys = {VehicleAttribute.Key.MODEL_YEAR, VehicleAttribute.Key.TRIM, VehicleAttribute.Key.ENGINE, VehicleAttribute.Key.TRANSMISSION, VehicleAttribute.Key.FUEL_TYPE};
        for (VehicleAttribute.Key key : keys) {
            String value = value(configuration, key);
            if (!value.isEmpty() && (withYear || key != VehicleAttribute.Key.MODEL_YEAR)) {
                parts.add(value);
            }
        }
        if (value(configuration, VehicleAttribute.Key.TRIM).isEmpty()) {
            parts.add(withYear ? 1 : 0, "Versão única");
        }
        return String.join(" · ", parts);
    }
}
