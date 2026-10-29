package io.rea.obd.ai;

import io.rea.obd.SensorCatalog;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DtcFormatter;
import io.rea.obd.diagnostics.DtcObservation;
import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.DtcReference;
import io.rea.obd.knowledge.DtcStructure;
import io.rea.obd.vehicle.VehicleProfile;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AnalysisRequest {
    public static final String SCHEMA = "borbacar-ai-request-1";
    static final int MAX_TEXT = 500;

    public final JSONObject body;
    public final Map<String, String> labels;
    public final String removed;

    private AnalysisRequest(JSONObject body, Map<String, String> labels, String removed) {
        this.body = body;
        this.labels = Collections.unmodifiableMap(labels);
        this.removed = removed;
    }

    public Set<String> refs() {
        return this.labels.keySet();
    }

    public static AnalysisRequest from(DiagnosticCase diagnosticCase) throws JSONException {
        Map<String, String> labels = new LinkedHashMap<>();
        PersonalDataFilter filter = new PersonalDataFilter();
        JSONArray observations = new JSONArray();

        Set<String> seen = new LinkedHashSet<>();
        int value = 0;
        for (DtcObservation observation : diagnosticCase.dtc.observations()) {
            String category = DtcFormatter.category(observation.category);
            if (!seen.add(observation.code + "/" + category)) {
                continue;
            }
            String ref = "D" + (++value);
            JSONObject item = new JSONObject()
                    .put("ref", ref)
                    .put("type", "dtc")
                    .put("code", observation.code)
                    .put("category", category);
            String system = DtcStructure.describe(observation.code);
            if (system != null) {
                item.put("system", system);
            }
            DtcDefinition definition = DtcReference.find(observation.code);
            if (definition != null) {
                item.put("catalog_meaning", definition.meaning);
            }
            observations.put(item);
            labels.put(ref, observation.code + " (" + category + ")");
        }

        int m = 0;
        for (DtcReadResult.MilObservation mil : diagnosticCase.dtc.mil.observations) {
            String ref = "M" + (++m);
            observations.put(new JSONObject()
                    .put("ref", ref)
                    .put("type", "mil")
                    .put("lamp_on", mil.illuminated)
                    .put("reported_dtc_count", mil.reportedDtcCount));
            labels.put(ref, "Luz de injeção " + (mil.illuminated ? "acesa" : "apagada"));
        }

        int t = 0;
        for (DiagnosticCase.Sample sample : diagnosticCase.telemetry) {
            SensorCatalog.Sensor sensor = SensorCatalog.get(sample.measurement.pid);
            if (sensor == null) {
                continue;
            }
            String ref = "T" + (++t);
            JSONObject item = new JSONObject()
                    .put("ref", ref)
                    .put("type", "reading")
                    .put("sensor", sensor.name)
                    .put("unit", sensor.unit)
                    .put("status", sample.measurement.status.name());
            if (sample.measurement.value != null) {
                item.put("value", sample.measurement.value);
            }
            observations.put(item);
            labels.put(ref, sensor.name);
        }

        JSONObject body = new JSONObject()
                .put("schema", SCHEMA)
                .put("vehicle", vehicle(diagnosticCase.vehicle, filter))
                .put("read_complete", !diagnosticCase.dtc.incomplete())
                .put("demonstration_data", diagnosticCase.dtc.origin == io.rea.obd.model.DataOrigin.SIMULATED)
                .put("observations", observations);
        if (diagnosticCase.symptoms != null) {
            body.put("symptoms", new JSONObject()
                    .put("description", clip(filter.apply(diagnosticCase.symptoms.description)))
                    .put("when", clip(filter.apply(diagnosticCase.symptoms.when)))
                    .put("frequency", clip(filter.apply(diagnosticCase.symptoms.frequency)))
                    .put("conditions", clip(filter.apply(diagnosticCase.symptoms.conditions))));
        }
        return new AnalysisRequest(body, labels, filter.summary());
    }

    private static JSONObject vehicle(VehicleProfile profile, PersonalDataFilter filter) throws JSONException {
        JSONObject vehicle = new JSONObject();
        if (profile == null) {
            return vehicle;
        }
        putText(vehicle, "make", filter.apply(profile.make));
        putText(vehicle, "model", filter.apply(profile.model));
        putText(vehicle, "trim", filter.apply(profile.trim));
        putText(vehicle, "engine", filter.apply(profile.engine));
        putText(vehicle, "fuel", filter.apply(profile.fuel));
        putText(vehicle, "transmission", filter.apply(profile.transmission));
        putText(vehicle, "market", filter.apply(profile.market));
        if (profile.year != null) {
            vehicle.put("year", profile.year);
        }
        return vehicle;
    }

    private static void putText(JSONObject target, String key, String market) throws JSONException {
        if (market != null && !market.trim().isEmpty()) {
            target.put(key, clip(market));
        }
    }

    static String clip(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.trim();
        return trimmed.length() <= MAX_TEXT ? trimmed : trimmed.substring(0, MAX_TEXT);
    }
}
