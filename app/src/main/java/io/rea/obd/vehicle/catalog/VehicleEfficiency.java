package io.rea.obd.vehicle.catalog;

import io.rea.obd.vehicle.VehicleAttribute;
import io.rea.obd.vehicle.VehicleProfile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public final class VehicleEfficiency {
    public static final class Official {
        public final String configurationId;
        public final BigDecimal ethanolCity;
        public final BigDecimal ethanolHighway;
        public final BigDecimal otherCity;
        public final BigDecimal otherHighway;
        public final String fuel;
        public final int page;

        Official(String configurationId, JSONObject entry) throws JSONException {
            this.configurationId = configurationId;
            this.ethanolCity = number(entry, "ethanolCity");
            this.ethanolHighway = number(entry, "ethanolHighway");
            this.otherCity = number(entry, "otherCity");
            this.otherHighway = number(entry, "otherHighway");
            this.fuel = entry.getString("fuel");
            this.page = entry.getInt("page");
        }

        public BigDecimal[] forFuel(String declared) {
            if ("Etanol".equals(declared)) {
                return this.ethanolCity == null ? null : new BigDecimal[]{this.ethanolCity, this.ethanolHighway};
            }
            boolean other = "Gasolina".equals(declared) || "Diesel".equals(declared);
            return other && this.otherCity != null ? new BigDecimal[]{this.otherCity, this.otherHighway} : null;
        }
    }

    public final String title;
    public final String publisher;
    public final String note;
    private final Map<String, Official> entries;

    private VehicleEfficiency(String title, String publisher, String note, Map<String, Official> entries) {
        this.title = title;
        this.publisher = publisher;
        this.note = note;
        this.entries = Collections.unmodifiableMap(entries);
    }

    public static VehicleEfficiency read(InputStream input) throws IOException, JSONException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        JSONObject root = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        if (root.getInt("schema_version") != 1) {
            throw new JSONException("Tabela de consumo não reconhecida");
        }
        Map<String, Official> entries = new HashMap<>();
        JSONObject list = root.getJSONObject("entries");
        for (Iterator<String> ids = list.keys(); ids.hasNext(); ) {
            String id = ids.next();
            entries.put(id, new Official(id, list.getJSONObject(id)));
        }
        return new VehicleEfficiency(root.getString("title"), root.getString("publisher"), root.getString("note"), entries);
    }

    public Official get(String configurationId) {
        return this.entries.get(configurationId);
    }

    public int size() {
        return this.entries.size();
    }

    public Official find(VehicleCatalog catalog, VehicleProfile profile) {
        if (catalog == null || profile == null || profile.make.isEmpty() || profile.model.isEmpty()) {
            return null;
        }
        Map<VehicleAttribute.Key, String> query = new EnumMap<>(VehicleAttribute.Key.class);
        query.put(VehicleAttribute.Key.MAKE, profile.make);
        query.put(VehicleAttribute.Key.MODEL, profile.model);
        if (profile.year != null) {
            query.put(VehicleAttribute.Key.MODEL_YEAR, profile.year.toString());
        }
        putIfKnown(query, VehicleAttribute.Key.TRIM, profile.trim);
        putIfKnown(query, VehicleAttribute.Key.ENGINE, profile.engine);
        putIfKnown(query, VehicleAttribute.Key.TRANSMISSION, profile.transmission);
        putIfKnown(query, VehicleAttribute.Key.FUEL_TYPE, profile.fuel);
        List<VehicleCatalog.Configuration> matches = catalog.matching(query);
        return matches.size() == 1 ? this.entries.get(matches.get(0).id) : null;
    }

    public static String position(BigDecimal measured, BigDecimal[] official) {
        BigDecimal low = official[0].min(official[1]);
        BigDecimal high = official[0].max(official[1]);
        if (measured.compareTo(low) < 0) {
            return "abaixo";
        }
        return measured.compareTo(high) > 0 ? "acima" : "dentro";
    }

    private static void putIfKnown(Map<VehicleAttribute.Key, String> query, VehicleAttribute.Key key, String transmission) {
        if (transmission != null && !transmission.isEmpty()) {
            query.put(key, transmission);
        }
    }

    private static BigDecimal number(JSONObject entry, String key) throws JSONException {
        return entry.isNull(key) ? null : new BigDecimal(entry.get(key).toString());
    }
}
