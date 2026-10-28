package io.rea.obd.history;

import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class SupplementalReadJson {
    private SupplementalReadJson() {
    }

    public static JSONObject encode(SupplementalReadResult supplementalReadResult) throws JSONException {
        JSONObject readiness = DtcReadJson.status(supplementalReadResult.readiness.status).put("service", "01").put("pid", 1);
        JSONArray observations = new JSONArray();
        for (SupplementalReadResult.ReadinessObservation readinessObservation : supplementalReadResult.readiness.observations) {
            observations.put(new JSONObject().put("id", readinessObservation.id).put("a", readinessObservation.bits.a).put("b", readinessObservation.bits.b).put("c", readinessObservation.bits.c).put("d", readinessObservation.bits.d).put("observed_at_ms", readinessObservation.observedAtEpochMillis));
        }
        readiness.put("observations", observations);
        JSONArray pids = new JSONArray();
        for (SupplementalReadResult.FramePid framePid : supplementalReadResult.frame) {
            JSONArray observations2 = new JSONArray();
            for (SupplementalReadResult.FrameValue frameValue : framePid.observations) {
                observations2.put(new JSONObject().put("id", frameValue.id).put("value", frameValue.value).put("observed_at_ms", frameValue.observedAtEpochMillis));
                pids = pids;
            }
            pids = pids;
            pids.put(DtcReadJson.status(framePid.status).put("service", "02").put("pid", framePid.pid).put("observations", observations2));
        }
        return new JSONObject().put("schema_version", 1).put("capture_id", supplementalReadResult.captureId).put("vehicle_id", supplementalReadResult.vehicleId).put("session_id", supplementalReadResult.sessionId).put("origin", supplementalReadResult.origin.name()).put("source_scope", "HEADERLESS_OBD").put("ecu_id", JSONObject.NULL).put("started_at_ms", supplementalReadResult.startedAtEpochMillis).put("completed_at_ms", supplementalReadResult.completedAtEpochMillis).put("readiness", readiness).put("freeze_frame", new JSONObject().put("frame_number", 0).put("associated_dtc", JSONObject.NULL).put("association_status", "UNKNOWN_ECU_NOT_IDENTIFIED").put("frozen_at", JSONObject.NULL).put("pids", pids));
    }

    public static SupplementalReadResult decode(JSONObject json) throws JSONException {
        if (json.getInt("schema_version") != 1 || !"HEADERLESS_OBD".equals(json.getString("source_scope")) || !json.isNull("ecu_id")) {
            throw new JSONException("Complemento não reconhecido");
        }
        JSONObject readiness = json.getJSONObject("readiness");
        JSONObject freezeFrame = json.getJSONObject("freeze_frame");
        if (!"01".equals(readiness.getString("service")) || readiness.getInt("pid") != 1 || freezeFrame.getInt("frame_number") != 0 || !freezeFrame.isNull("associated_dtc") || !freezeFrame.isNull("frozen_at") || !"UNKNOWN_ECU_NOT_IDENTIFIED".equals(freezeFrame.getString("association_status"))) {
            throw new JSONException("Escopo complementar incompatível");
        }
        ArrayList items = new ArrayList();
        JSONArray observations = readiness.getJSONArray("observations");
        int i = 0;
        while (i < observations.length()) {
            JSONObject jsonObject = observations.getJSONObject(i);
            items.add(new SupplementalReadResult.ReadinessObservation(jsonObject.getString("id"), new SupplementalReadResult.ReadinessBits(jsonObject.getInt("a"), jsonObject.getInt("b"), jsonObject.getInt("c"), jsonObject.getInt("d")), jsonObject.getLong("observed_at_ms")));
            i++;
            observations = observations;
            readiness = readiness;
        }
        JSONObject json2 = readiness;
        ArrayList items2 = new ArrayList();
        JSONArray pids = freezeFrame.getJSONArray("pids");
        int value = 0;
        while (value < pids.length()) {
            JSONObject jsonObject = pids.getJSONObject(value);
            if (!"02".equals(jsonObject.getString("service"))) {
                throw new JSONException("Serviço de quadro incompatível");
            }
            ArrayList items3 = new ArrayList();
            JSONArray observations2 = jsonObject.getJSONArray("observations");
            int value2 = 0;
            while (value2 < observations2.length()) {
                JSONObject jsonObject2 = observations2.getJSONObject(value2);
                items3.add(new SupplementalReadResult.FrameValue(jsonObject2.getString("id"), jsonObject2.getDouble("value"), jsonObject2.getLong("observed_at_ms")));
                value2++;
                pids = pids;
            }
            items2.add(new SupplementalReadResult.FramePid(jsonObject.getInt("pid"), DtcReadJson.readStatus(jsonObject), items3));
            value++;
            pids = pids;
        }
        return new SupplementalReadResult(json.getString("capture_id"), json.getString("vehicle_id"), json.getString("session_id"), DataOrigin.valueOf(json.getString("origin")), json.getLong("started_at_ms"), json.getLong("completed_at_ms"), new SupplementalReadResult.Readiness(DtcReadJson.readStatus(json2), items), items2);
    }
}