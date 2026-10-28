package io.rea.obd.history;

import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.model.DataOrigin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DtcReadJson {
    private DtcReadJson() {
    }

    public static DtcReadResult decode(JSONObject json) throws JSONException {
        if (json.getInt("schema_version") != 1 || !"HEADERLESS_OBD".equals(json.getString("source_scope")) || !json.isNull("ecu_id")) {
            throw new JSONException("Formato de leitura não reconhecido");
        }
        JSONObject mil = json.getJSONObject("mil");
        ArrayList items = new ArrayList();
        String text = "observations";
        JSONArray observations = mil.getJSONArray("observations");
        for (int i = 0; i < observations.length(); i++) {
            JSONObject jsonObject = observations.getJSONObject(i);
            int reportedDtcCount = jsonObject.getInt("reported_dtc_count");
            if (reportedDtcCount < 0 || reportedDtcCount > 127) {
                throw new JSONException("Contagem MIL inválida");
            }
            items.add(new DtcReadResult.MilObservation(jsonObject.getString("id"), reportedDtcCount | (jsonObject.getBoolean("illuminated") ? 128 : 0), time(jsonObject, "observed_at")));
        }
        ArrayList items2 = new ArrayList();
        JSONArray serviceResults = json.getJSONArray("service_results");
        int value = 0;
        while (value < serviceResults.length()) {
            JSONObject jsonObject = serviceResults.getJSONObject(value);
            DtcObservation.Category category = DtcObservation.Category.valueOf(jsonObject.getString("category"));
            if (Integer.parseInt(jsonObject.getString("service"), 16) != category.service) {
                throw new JSONException("Serviço incompatível");
            }
            ArrayList items3 = new ArrayList();
            JSONArray jsonArray = jsonObject.getJSONArray(text);
            int value2 = 0;
            while (value2 < jsonArray.length()) {
                JSONObject jsonObject2 = jsonArray.getJSONObject(value2);
                items3.add(new DtcObservation(jsonObject2.getString("id"), jsonObject2.getString("code"), DtcObservation.Category.valueOf(jsonObject2.getString("category")), time(jsonObject2, "observed_at")));
                value2++;
                text = text;
                serviceResults = serviceResults;
            }
            items2.add(new DtcReadResult.ServiceResult(category, readStatus(jsonObject), items3));
            value++;
            text = text;
            serviceResults = serviceResults;
        }
        if (!"01".equals(mil.getString("service")) || !"01".equals(mil.getString("pid"))) {
            throw new JSONException("Consulta MIL incompatível");
        }
        return new DtcReadResult(json.getString("capture_id"), json.isNull("vehicle_id") ? null : json.getString("vehicle_id"), json.getString("session_id"), DataOrigin.valueOf(json.getString("origin")), time(json, "started_at"), time(json, "completed_at"), new DtcReadResult.MilResult(readStatus(mil), items), items2);
    }

    private static long time(JSONObject json, String text) throws JSONException {
        return Instant.parse(json.getString(text)).toEpochMilli();
    }

    static DtcReadResult.Status readStatus(JSONObject json) throws JSONException {
        ArrayList items = new ArrayList();
        JSONArray negativeResponses = json.getJSONArray("negative_responses");
        for (int i = 0; i < negativeResponses.length(); i++) {
            JSONObject jsonObject = negativeResponses.getJSONObject(i);
            items.add(new DtcReadResult.NegativeResponse(Integer.parseInt(jsonObject.getString("service"), 16), Integer.parseInt(jsonObject.getString("code"), 16)));
        }
        return new DtcReadResult.Status(DtcReadResult.Outcome.valueOf(json.getString("outcome")), json.isNull("requested_at") ? null : Long.valueOf(time(json, "requested_at")), json.isNull("completed_at") ? null : Long.valueOf(time(json, "completed_at")), json.getInt("positive_response_count"), items, json.getString("detail"));
    }

    public static JSONObject encode(DtcReadResult dtcReadResult) throws JSONException {
        JSONObject json = new JSONObject().put("schema_version", 1).put("capture_id", dtcReadResult.captureId).put("vehicle_id", dtcReadResult.vehicleId == null ? JSONObject.NULL : dtcReadResult.vehicleId).put("session_id", dtcReadResult.sessionId).put("origin", dtcReadResult.origin.name()).put("source_scope", "HEADERLESS_OBD").put("ecu_id", JSONObject.NULL).put("started_at", Instant.ofEpochMilli(dtcReadResult.startedAtEpochMillis).toString()).put("completed_at", Instant.ofEpochMilli(dtcReadResult.completedAtEpochMillis).toString()).put("incomplete", dtcReadResult.incomplete());
        JSONObject status = status(dtcReadResult.mil.status);
        JSONArray observations = new JSONArray();
        for (DtcReadResult.MilObservation milObservation : dtcReadResult.mil.observations) {
            observations.put(new JSONObject().put("id", milObservation.id).put("illuminated", milObservation.illuminated).put("reported_dtc_count", milObservation.reportedDtcCount).put("observed_at", Instant.ofEpochMilli(milObservation.observedAtEpochMillis).toString()).put("ecu_id", JSONObject.NULL));
        }
        status.put("service", "01").put("pid", "01").put("observations", observations);
        json.put("mil", status);
        JSONArray serviceResults = new JSONArray();
        for (DtcReadResult.ServiceResult serviceResult : dtcReadResult.services) {
            JSONObject json2 = status(serviceResult.status).put("service", String.format(Locale.ROOT, "%02X", Integer.valueOf(serviceResult.category.service))).put("category", serviceResult.category.name());
            JSONArray observations2 = new JSONArray();
            for (DtcObservation dtcObservation : serviceResult.observations) {
                observations2.put(new JSONObject().put("id", dtcObservation.id).put("code", dtcObservation.code).put("category", dtcObservation.category.name()).put("observed_at", Instant.ofEpochMilli(dtcObservation.observedAtEpochMillis).toString()).put("ecu_id", JSONObject.NULL));
            }
            serviceResults.put(json2.put("observations", observations2));
        }
        return json.put("service_results", serviceResults);
    }

    static JSONObject status(DtcReadResult.Status status) throws JSONException {
        JSONArray negativeResponses = new JSONArray();
        for (DtcReadResult.NegativeResponse negativeResponse : status.negativeResponses) {
            negativeResponses.put(new JSONObject().put("service", String.format(Locale.ROOT, "%02X", Integer.valueOf(negativeResponse.service))).put("code", String.format(Locale.ROOT, "%02X", Integer.valueOf(negativeResponse.code))));
        }
        return new JSONObject().put("outcome", status.outcome.name()).put("requested_at", status.requestedAtEpochMillis == null ? JSONObject.NULL : Instant.ofEpochMilli(status.requestedAtEpochMillis.longValue()).toString()).put("completed_at", status.completedAtEpochMillis == null ? JSONObject.NULL : Instant.ofEpochMilli(status.completedAtEpochMillis.longValue()).toString()).put("positive_response_count", status.positiveResponseCount).put("negative_responses", negativeResponses).put("detail", status.detail);
    }
}