package io.rea.obd.vehicle;

import android.content.SharedPreferences;
import io.rea.obd.model.FactProvenance;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class VehicleRepository {
    public static final String KEY = "vehicle_store_v2";
    public static final String LEGACY_KEY = "vehicle_store_v1";
    private final SharedPreferences preferences;
    private List<VehicleRecord> records = new ArrayList();
    private String activeId = "";
    private String migrationSource = "fresh";

    public VehicleRepository(SharedPreferences sharedPreferences) throws Exception {
        this.preferences = sharedPreferences;
        if (sharedPreferences.contains(KEY)) {
            load(new JSONObject(sharedPreferences.getString(KEY, "")));
        } else {
            migrate();
        }
    }

    public List<VehicleRecord> records() {
        return Collections.unmodifiableList(new ArrayList(this.records));
    }

    public VehicleRecord record(String id) {
        for (VehicleRecord vehicleRecord : this.records) {
            if (vehicleRecord.id.equals(id)) {
                return vehicleRecord;
            }
        }
        return null;
    }

    public VehicleRecord activeRecord() {
        return record(this.activeId);
    }

    public List<VehicleProfile> all() {
        ArrayList items = new ArrayList();
        Iterator<VehicleRecord> it = this.records.iterator();
        while (it.hasNext()) {
            items.add(it.next().projection());
        }
        return Collections.unmodifiableList(items);
    }

    public VehicleProfile active() {
        VehicleRecord activeRecord = activeRecord();
        if (activeRecord == null) {
            return null;
        }
        return activeRecord.projection();
    }

    public void select(String id) throws Exception {
        if (record(id) == null) {
            throw new IllegalArgumentException("Veículo inexistente");
        }
        persist(this.records, id);
    }

    public void save(VehicleProfile vehicleProfile) throws Exception {
        VehicleRecord vehicleRecord = record(vehicleProfile.id);
        long currentTimeMillis = System.currentTimeMillis();
        saveRecord(vehicleRecord == null ? VehicleRecord.declared(vehicleProfile, currentTimeMillis) : vehicleRecord.editDeclaration(vehicleProfile, currentTimeMillis), vehicleRecord == null ? 0 : vehicleRecord.revision);
    }

    public void saveRecord(VehicleRecord vehicleRecord, int i) throws Exception {
        saveSelection(vehicleRecord, i, null);
    }

    public void saveSelection(VehicleRecord vehicleRecord, int i, VehicleCatalog vehicleCatalog) throws Exception {
        VehicleRecord vehicleRecord2 = record(vehicleRecord.id);
        if ((vehicleRecord2 == null ? 0 : vehicleRecord2.revision) != i) {
            throw new IllegalStateException("Perfil já alterado; reabra a revisão atual");
        }
        if ((vehicleRecord2 == null && vehicleRecord.revision != 1) || (vehicleRecord2 != null && vehicleRecord.revision != vehicleRecord2.revision && vehicleRecord.revision != vehicleRecord2.revision + 1)) {
            throw new IllegalArgumentException("Revisão inválida");
        }
        if (vehicleRecord2 != null && vehicleRecord.revision == vehicleRecord2.revision && !VehicleRecordJson.encode(vehicleRecord2).toString().equals(VehicleRecordJson.encode(vehicleRecord).toString())) {
            throw new IllegalArgumentException("Uma mudança precisa de nova revisão");
        }
        String text = null;
        for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
            if (vehicleAttribute.provenance != null && vehicleAttribute.provenance.origin == FactProvenance.Origin.CATALOG) {
                VehicleAttribute attribute = vehicleRecord2 == null ? null : vehicleRecord2.attribute(vehicleAttribute.key);
                if ((attribute == null || !Objects.equals(vehicleAttribute.value, attribute.value) || !sameProvenance(vehicleAttribute.provenance, attribute.provenance)) && !inCatalog(vehicleCatalog, vehicleRecord, vehicleAttribute)) {
                    throw new IllegalArgumentException("Atributo sem configuração disponível e coerente no catálogo revisado");
                }
                if (text != null && !text.equals(vehicleAttribute.provenance.scope)) {
                    throw new IllegalArgumentException("Atributos de configurações diferentes");
                }
                text = vehicleAttribute.provenance.scope;
            }
        }
        ArrayList<VehicleRecord> items = new ArrayList<>(this.records);
        int value = -1;
        for (int i2 = 0; i2 < items.size(); i2++) {
            if (items.get(i2).id.equals(vehicleRecord.id)) {
                value = i2;
            }
        }
        if (value < 0) {
            items.add(vehicleRecord);
        } else {
            items.set(value, vehicleRecord);
        }
        persist(items, this.activeId.isEmpty() ? vehicleRecord.id : this.activeId);
    }

    private static boolean inCatalog(VehicleCatalog vehicleCatalog, VehicleRecord vehicleRecord, VehicleAttribute vehicleAttribute) {
        if (vehicleCatalog == null) {
            return false;
        }
        for (VehicleCatalog.Configuration configuration : vehicleCatalog.configurations.values()) {
            if (vehicleAttribute.equals(configuration.attributes.get(vehicleAttribute.key)) && coherent(configuration, vehicleRecord)) {
                return true;
            }
        }
        return false;
    }

    private static boolean coherent(VehicleCatalog.Configuration configuration, VehicleRecord vehicleRecord) {
        for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
            VehicleAttribute expected = configuration.attributes.get(vehicleAttribute.key);
            if (vehicleAttribute.value != null && expected != null && !vehicleAttribute.value.equals(expected.value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameProvenance(FactProvenance factProvenance, FactProvenance provenance) {
        return factProvenance != null && provenance != null && factProvenance.origin == provenance.origin && factProvenance.verification == provenance.verification && factProvenance.references.equals(provenance.references) && factProvenance.scope.equals(provenance.scope) && factProvenance.method.equals(provenance.method) && Objects.equals(factProvenance.recordedAtMillis, provenance.recordedAtMillis);
    }

    public void remove(final String text) throws Exception {
        ArrayList<VehicleRecord> items = new ArrayList<>(this.records);
        items.removeIf((obj) -> ((VehicleRecord) obj).id.equals(text));
        persist(items, text.equals(this.activeId) ? "" : this.activeId);
    }

    private void migrate() throws Exception {
        ArrayList<VehicleRecord> items = new ArrayList<>();
        int i = 0;
        String activeId = "";
        if (this.preferences.contains(LEGACY_KEY)) {
            JSONObject json = new JSONObject(this.preferences.getString(LEGACY_KEY, ""));
            if (json.getInt("schema_version") != 1) {
                throw new JSONException("Versão antiga da garagem não reconhecida");
            }
            JSONArray profiles = json.getJSONArray("profiles");
            HashSet seen = new HashSet();
            while (i < profiles.length()) {
                JSONObject jsonObject = profiles.getJSONObject(i);
                JSONArray array = profiles;
                VehicleProfile vehicleProfile = new VehicleProfile(jsonObject.getString("id"), jsonObject.getString("name"), jsonObject.optString("make"), jsonObject.optString("model"), jsonObject.isNull("model_year") ? null : Integer.valueOf(jsonObject.getInt("model_year")), jsonObject.optString("market"), jsonObject.optString("trim"), jsonObject.optString("engine"), jsonObject.optString("fuel_type"), jsonObject.optString("transmission"), jsonObject.optString("notes"));
                if (!seen.add(vehicleProfile.id)) {
                    throw new JSONException("IDs antigos duplicados");
                }
                items.add(VehicleRecord.legacy(vehicleProfile));
                i++;
                profiles = array;
            }
            String string = json.getString("active_id");
            if (!string.isEmpty() && !seen.contains(string)) {
                throw new JSONException("Veículo ativo antigo não encontrado");
            }
            this.migrationSource = LEGACY_KEY;
            activeId = string;
        } else if (this.preferences.contains("vehicles")) {
            JSONArray array = new JSONArray(this.preferences.getString("vehicles", "[]"));
            for (int i2 = 0; i2 < array.length(); i2++) {
                JSONObject jsonObject = array.getJSONObject(i2);
                items.add(VehicleRecord.legacy(new VehicleProfile(UUID.nameUUIDFromBytes(("legacy:" + i2 + ":" + jsonObject.toString()).getBytes(StandardCharsets.UTF_8)).toString(), jsonObject.getString("name"), "", "", null, "", "", "", "", "", jsonObject.optString("details"))));
            }
            activeId = items.size() == 1 ? items.get(0).id : "";
            this.migrationSource = "vehicles";
        }
        persist(items, activeId);
    }

    private void load(JSONObject json) throws Exception {
        if (VehicleRecordJson.integer(json, "schema_version") == 2) {
            VehicleRecordJson.strict(json, "schema_version", "active_id", "profiles", "migration_source");
            ArrayList items = new ArrayList();
            HashSet seen = new HashSet();
            JSONArray profiles = json.getJSONArray("profiles");
            for (int i = 0; i < profiles.length(); i++) {
                VehicleRecord vehicleRecord = VehicleRecordJson.decode(profiles.getJSONObject(i));
                if (!seen.add(vehicleRecord.id)) {
                    throw new JSONException("IDs de veículo duplicados");
                }
                items.add(vehicleRecord);
            }
            String activeId = VehicleRecordJson.text(json, "active_id");
            if (!activeId.isEmpty() && !seen.contains(activeId)) {
                throw new JSONException("Veículo ativo não encontrado");
            }
            this.migrationSource = VehicleRecordJson.text(json, "migration_source");
            this.records = items;
            this.activeId = activeId;
            return;
        }
        throw new JSONException("Versão da garagem não reconhecida");
    }

    private void persist(List<VehicleRecord> list, String activeId) throws Exception {
        JSONArray profiles = new JSONArray();
        Iterator<VehicleRecord> it = list.iterator();
        while (it.hasNext()) {
            profiles.put(VehicleRecordJson.encode(it.next()));
        }
        if (!this.preferences.edit().putString(KEY, new JSONObject().put("schema_version", 2).put("active_id", activeId).put("profiles", profiles).put("migration_source", this.migrationSource).toString()).commit()) {
            throw new IllegalStateException("Não foi possível salvar a garagem; origem preservada");
        }
        this.records = new ArrayList(list);
        this.activeId = activeId;
    }
}