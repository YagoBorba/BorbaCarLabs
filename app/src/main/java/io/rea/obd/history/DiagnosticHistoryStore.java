package io.rea.obd.history;

import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DiagnosticHistoryStore {
    public static final String KEY = "diagnostic_history_v1";
    private final SharedPreferences preferences;
    private JSONArray reports;

    public DiagnosticHistoryStore(SharedPreferences sharedPreferences) throws Exception {
        this.preferences = sharedPreferences;
        int i = 0;
        if (sharedPreferences.contains(KEY)) {
            JSONObject json = new JSONObject(sharedPreferences.getString(KEY, ""));
            if (json.getInt("schema_version") != 1) {
                throw new JSONException("Versão do histórico não reconhecida");
            }
            this.reports = json.getJSONArray("reports");
            while (i < this.reports.length()) {
                JSONObject jsonObject = this.reports.getJSONObject(i);
                jsonObject.getString("id");
                if (!jsonObject.has("vehicle_id")) {
                    throw new JSONException("Relatório sem escopo");
                }
                i++;
            }
            return;
        }
        JSONArray array = new JSONArray(sharedPreferences.getString("history", "[]"));
        JSONArray array2 = new JSONArray();
        while (i < array.length()) {
            JSONObject json = new JSONObject(array.getJSONObject(i).toString());
            json.put("id", UUID.nameUUIDFromBytes(("legacy:" + i + ":" + json.toString()).getBytes(StandardCharsets.UTF_8)).toString());
            json.put("vehicle_id", JSONObject.NULL).put("legacy_unassigned", true);
            array2.put(json);
            i++;
        }
        persist(array2);
    }

    public JSONArray forVehicle(String id) throws JSONException {
        JSONArray array = new JSONArray();
        if (id == null) {
            return array;
        }
        for (int i = 0; i < this.reports.length(); i++) {
            JSONObject jsonObject = this.reports.getJSONObject(i);
            if (!jsonObject.isNull("vehicle_id") && id.equals(jsonObject.getString("vehicle_id"))) {
                array.put(new JSONObject(jsonObject.toString()));
            }
        }
        return array;
    }

    public JSONArray unassigned() throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < this.reports.length(); i++) {
            JSONObject jsonObject = this.reports.getJSONObject(i);
            if (jsonObject.isNull("vehicle_id")) {
                array.put(new JSONObject(jsonObject.toString()));
            }
        }
        return array;
    }

    public JSONArray detached(Set<String> set) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < this.reports.length(); i++) {
            JSONObject jsonObject = this.reports.getJSONObject(i);
            if (jsonObject.isNull("vehicle_id") || !set.contains(jsonObject.getString("vehicle_id"))) {
                array.put(new JSONObject(jsonObject.toString()));
            }
        }
        return array;
    }

    public void add(JSONObject id) throws Exception {
        JSONObject json = new JSONObject(id.toString());
        if (!json.has("vehicle_id")) {
            throw new JSONException("Relatório sem veículo ou estado desconhecido explícito");
        }
        json.put("id", UUID.randomUUID().toString());
        JSONArray array = new JSONArray().put(json);
        String string = json.isNull("vehicle_id") ? null : json.getString("vehicle_id");
        int i = 1;
        for (int i2 = 0; i2 < this.reports.length(); i2++) {
            JSONObject jsonObject = this.reports.getJSONObject(i2);
            if (!Objects.equals(string, jsonObject.isNull("vehicle_id") ? null : jsonObject.getString("vehicle_id")) || (i = i + 1) <= 50) {
                array.put(jsonObject);
            }
        }
        persist(array);
    }

    public void deleteForVehicle(String id) throws Exception {
        JSONArray array = new JSONArray();
        for (int i = 0; i < this.reports.length(); i++) {
            JSONObject jsonObject = this.reports.getJSONObject(i);
            if (jsonObject.isNull("vehicle_id") || !id.equals(jsonObject.getString("vehicle_id"))) {
                array.put(jsonObject);
            }
        }
        persist(array);
    }

    private void persist(JSONArray reports) throws Exception {
        if (!this.preferences.edit().putString(KEY, new JSONObject().put("schema_version", 1).put("reports", reports).toString()).commit()) {
            throw new IllegalStateException("Não foi possível salvar o histórico");
        }
        this.reports = reports;
    }
}