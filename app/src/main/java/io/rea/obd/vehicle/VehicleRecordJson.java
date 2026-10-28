package io.rea.obd.vehicle;

import io.rea.obd.model.FactProvenance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class VehicleRecordJson {
    private VehicleRecordJson() {
    }

    public static JSONObject encode(VehicleRecord vehicleRecord) throws JSONException {
        JSONObject attributes = new JSONObject();
        for (VehicleAttribute vehicleAttribute : vehicleRecord.attributes().values()) {
            JSONObject json = new JSONObject().put("value", vehicleAttribute.value == null ? JSONObject.NULL : vehicleAttribute.value);
            if (vehicleAttribute.provenance == null) {
                json.put("provenance", JSONObject.NULL);
            } else {
                FactProvenance factProvenance = vehicleAttribute.provenance;
                json.put("provenance", new JSONObject().put("origin", factProvenance.origin.name()).put("verification", factProvenance.verification.name()).put("references", new JSONArray((Collection) factProvenance.references)).put("scope", factProvenance.scope).put("method", factProvenance.method).put("recorded_at_ms", factProvenance.recordedAtMillis == null ? JSONObject.NULL : factProvenance.recordedAtMillis));
            }
            attributes.put(vehicleAttribute.key.name(), json);
        }
        return new JSONObject().put("id", vehicleRecord.id).put("name", vehicleRecord.name).put("revision", vehicleRecord.revision).put("attributes", attributes);
    }

    public static VehicleRecord decode(JSONObject json) throws JSONException {
        FactProvenance factProvenance;
        strict(json, "id", "name", "revision", "attributes");
        JSONObject attributes = json.getJSONObject("attributes");
        EnumMap enumMap = new EnumMap(VehicleAttribute.Key.class);
        if (attributes.length() != VehicleAttribute.Key.values().length) {
            throw new JSONException("Conjunto de atributos incompleto ou desconhecido");
        }
        Iterator<String> itKeys = attributes.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            VehicleAttribute.Key key = VehicleAttribute.Key.valueOf(next);
            JSONObject jsonObject = attributes.getJSONObject(next);
            strict(jsonObject, "value", "provenance");
            if (jsonObject.isNull("provenance")) {
                factProvenance = null;
            } else {
                JSONObject provenance = jsonObject.getJSONObject("provenance");
                strict(provenance, "origin", "verification", "references", "scope", "method", "recorded_at_ms");
                ArrayList items = new ArrayList();
                JSONArray references = provenance.getJSONArray("references");
                for (int i = 0; i < references.length(); i++) {
                    Object reference = references.get(i);
                    if (!(reference instanceof String)) {
                        throw new JSONException("Referência precisa ser texto");
                    }
                    items.add((String) reference);
                }
                factProvenance = new FactProvenance(FactProvenance.Origin.valueOf(text(provenance, "origin")), FactProvenance.Verification.valueOf(text(provenance, "verification")), items, text(provenance, "scope"), text(provenance, "method"), provenance.isNull("recorded_at_ms") ? null : Long.valueOf(integer(provenance, "recorded_at_ms")));
            }
            enumMap.put(key, new VehicleAttribute(key, jsonObject.isNull("value") ? null : text(jsonObject, "value"), factProvenance));
        }
        long value = integer(json, "revision");
        if (value < 1 || value > 2147483647L) {
            throw new JSONException("Revisão fora do limite");
        }
        return new VehicleRecord(text(json, "id"), text(json, "name"), (int) value, enumMap);
    }

    public static String text(JSONObject json, String text) throws JSONException {
        Object value = json.get(text);
        if (value instanceof String) {
            return (String) value;
        }
        throw new JSONException("Campo precisa ser texto: " + text);
    }

    public static long integer(JSONObject json, String text) throws JSONException {
        Object value = json.get(text);
        if ((value instanceof Integer) || (value instanceof Long)) {
            return ((Number) value).longValue();
        }
        throw new JSONException("Campo precisa ser inteiro: " + text);
    }

    static void strict(JSONObject json, String... text) throws JSONException {
        HashSet seen = new HashSet();
        Iterator<String> itKeys = json.keys();
        while (itKeys.hasNext()) {
            seen.add(itKeys.next());
        }
        if (!seen.equals(new HashSet(Arrays.asList(text)))) {
            throw new JSONException("Campos de perfil ausentes ou desconhecidos");
        }
    }
}