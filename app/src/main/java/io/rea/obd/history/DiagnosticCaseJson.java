package io.rea.obd.history;

import io.rea.obd.diagnostics.CaseReview;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.knowledge.KnowledgeRepository;
import io.rea.obd.knowledge.SourceReference;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleProfileReportV1;
import io.rea.obd.vehicle.VehicleRecordJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DiagnosticCaseJson {
    private DiagnosticCaseJson() {
    }

    public static JSONObject encode(DiagnosticCase diagnosticCase) throws JSONException {
        VehicleProfile vehicleProfile = diagnosticCase.vehicle;
        JSONObject vehicle = new JSONObject().put("id", vehicleProfile.id).put("name", vehicleProfile.name).put("make", vehicleProfile.make).put("model", vehicleProfile.model).put("year", vehicleProfile.year == null ? JSONObject.NULL : vehicleProfile.year).put("market", vehicleProfile.market).put("trim", vehicleProfile.trim).put("engine", vehicleProfile.engine).put("fuel", vehicleProfile.fuel).put("transmission", vehicleProfile.transmission).put("notes", vehicleProfile.notes);
        JSONObject symptoms = new JSONObject().put("provenance", "USER_DECLARED").put("description", diagnosticCase.symptoms.description).put("when", diagnosticCase.symptoms.when).put("frequency", diagnosticCase.symptoms.frequency).put("conditions", diagnosticCase.symptoms.conditions);
        ArrayList<DiagnosticCase.Sample> items = new ArrayList(diagnosticCase.telemetry);
        items.sort(Comparator.comparingInt((obj) -> ((DiagnosticCase.Sample) obj).measurement.pid));
        JSONArray telemetry = new JSONArray();
        for (DiagnosticCase.Sample sample : items) {
            TelemetrySample telemetrySample = sample.measurement;
            telemetry.put(new JSONObject().put("vehicle_id", sample.vehicleId).put("session_id", sample.sessionId).put("pid", telemetrySample.pid).put("value", telemetrySample.value == null ? JSONObject.NULL : telemetrySample.value).put("status", telemetrySample.status.name()).put("origin", telemetrySample.origin.name()).put("epoch_ms", telemetrySample.epochMillis).put("elapsed_ms", telemetrySample.elapsedMillis).put("detail", telemetrySample.detail));
        }
        JSONObject json = new JSONObject().put("schema_version", diagnosticCase.schemaVersion).put("case_id", diagnosticCase.id).put("revision", diagnosticCase.revision).put("created_at_ms", diagnosticCase.createdAtEpochMillis).put("revised_at_ms", diagnosticCase.revisedAtEpochMillis).put("vehicle", vehicle).put("adapter", diagnosticCase.adapter).put("dtc_read", DtcReadJson.encode(diagnosticCase.dtc)).put("telemetry", telemetry).put("symptoms", symptoms);
        if (diagnosticCase.schemaVersion == 4) {
            json.put("vehicle_record", VehicleRecordJson.encode(diagnosticCase.vehicleRecord));
        }
        if (diagnosticCase.schemaVersion == 1) {
            return json.put("readiness", new JSONObject().put("status", "NOT_COLLECTED")).put("freeze_frame", new JSONObject().put("status", "NOT_COLLECTED").put("associated_dtc", JSONObject.NULL));
        }
        return json.put("supplement", diagnosticCase.supplement == null ? JSONObject.NULL : SupplementalReadJson.encode(diagnosticCase.supplement));
    }

    public static DiagnosticCase decode(JSONObject json) throws JSONException {
        int i = json.getInt("schema_version");
        if (i < 1 || i > 4) {
            throw new JSONException("Versão de caso não reconhecida");
        }
        if (i == 4 && VehicleRecordJson.integer(json, "schema_version") != 4) {
            throw new JSONException("Versão de caso inválida");
        }
        if (i == 1 && (!"NOT_COLLECTED".equals(json.getJSONObject("readiness").getString("status")) || !"NOT_COLLECTED".equals(json.getJSONObject("freeze_frame").getString("status")) || !json.getJSONObject("freeze_frame").isNull("associated_dtc"))) {
            throw new JSONException("Caso legado incompatível");
        }
        JSONObject vehicle = json.getJSONObject("vehicle");
        JSONObject symptoms = json.getJSONObject("symptoms");
        if (!"USER_DECLARED".equals(symptoms.getString("provenance"))) {
            throw new JSONException("Origem do sintoma inválida");
        }
        VehicleProfile vehicleProfile = new VehicleProfile(vehicle.getString("id"), vehicle.getString("name"), vehicle.getString("make"), vehicle.getString("model"), vehicle.isNull("year") ? null : Integer.valueOf(vehicle.getInt("year")), vehicle.getString("market"), vehicle.getString("trim"), vehicle.getString("engine"), vehicle.getString("fuel"), vehicle.getString("transmission"), vehicle.getString("notes"));
        ArrayList items = new ArrayList();
        JSONArray telemetry = json.getJSONArray("telemetry");
        for (int i2 = 0; i2 < telemetry.length(); i2++) {
            JSONObject jsonObject = telemetry.getJSONObject(i2);
            items.add(new DiagnosticCase.Sample(jsonObject.getString("vehicle_id"), jsonObject.getString("session_id"), new TelemetrySample(jsonObject.getInt("pid"), jsonObject.isNull("value") ? null : Double.valueOf(jsonObject.getDouble("value")), TelemetrySample.Status.valueOf(jsonObject.getString("status")), DataOrigin.valueOf(jsonObject.getString("origin")), jsonObject.getLong("epoch_ms"), jsonObject.getLong("elapsed_ms"), jsonObject.getString("detail"))));
        }
        return new DiagnosticCase(i, json.getString("case_id"), json.getInt("revision"), json.getLong("created_at_ms"), json.getLong("revised_at_ms"), vehicleProfile, json.getString("adapter"), DtcReadJson.decode(json.getJSONObject("dtc_read")), items, new DiagnosticCase.Symptoms(symptoms.getString("description"), symptoms.getString("when"), symptoms.getString("frequency"), symptoms.getString("conditions")), (i < 2 || json.isNull("supplement")) ? null : SupplementalReadJson.decode(json.getJSONObject("supplement")), i == 4 ? VehicleRecordJson.decode(json.getJSONObject("vehicle_record")) : null);
    }

    public static JSONObject document(DiagnosticCase diagnosticCase) throws JSONException {
        if (diagnosticCase.schemaVersion == 4) {
            DiagnosticCase diagnosticCase2 = new DiagnosticCase(3, diagnosticCase.id, diagnosticCase.revision, diagnosticCase.createdAtEpochMillis, diagnosticCase.revisedAtEpochMillis, diagnosticCase.vehicle, diagnosticCase.adapter, diagnosticCase.dtc, diagnosticCase.telemetry, diagnosticCase.symptoms, diagnosticCase.supplement);
            JSONObject input = encode(diagnosticCase);
            JSONObject analysis = DiagnosticResultJson.encode(diagnosticCase2);
            JSONArray reportLines = analysis.getJSONArray("report_lines");
            reportLines.put("").put("Identificação do perfil capturado · vehicle-profile-1");
            Iterator<String> it = VehicleProfileReportV1.report(diagnosticCase.vehicleRecord).iterator();
            while (it.hasNext()) {
                reportLines.put(it.next());
            }
            analysis.put("vehicle_report_version", "vehicle-profile-1");
            return new JSONObject().put("schema_version", 4).put("fingerprint_algorithm", "canonical-json-sha256-v1").put("input_fingerprint", hash(input)).put("input", input).put("analysis", analysis);
        }
        if (diagnosticCase.schemaVersion == 3) {
            JSONObject input = encode(diagnosticCase);
            return new JSONObject().put("schema_version", 3).put("fingerprint_algorithm", "canonical-json-sha256-v1").put("input_fingerprint", hash(input)).put("input", input).put("analysis", DiagnosticResultJson.encode(diagnosticCase));
        }
        JSONObject input2 = encode(diagnosticCase);
        CaseReview caseReview = new CaseReview(diagnosticCase);
        JSONObject analysis2 = new JSONObject().put("analyzer_type", "RULE_ENGINE").put("rules_version", caseReview.version).put("catalog_version", "none").put("status", "INSUFFICIENT_EVIDENCE").put("observed", new JSONArray((Collection) caseReview.observed)).put("evidence_refs", new JSONArray((Collection) caseReview.evidenceIds)).put("missing_information", new JSONArray((Collection) caseReview.missing)).put("hypotheses", new JSONArray()).put("checks", new JSONArray((Collection) caseReview.checks)).put("limitations", new JSONArray((Collection) caseReview.limitations)).put("sources_used", new JSONArray()).put("report_lines", new JSONArray((Collection) caseReview.report(diagnosticCase)));
        if (diagnosticCase.schemaVersion == 2) {
            JSONArray protocolSources = new JSONArray();
            if (diagnosticCase.supplement != null) {
                for (SourceReference sourceReference : KnowledgeRepository.all()) {
                    protocolSources.put(new JSONObject().put("id", sourceReference.id).put("title", sourceReference.title).put("publisher", sourceReference.publisher).put("version", sourceReference.version).put("location", sourceReference.location).put("section", sourceReference.section).put("accessed_on", sourceReference.accessedOn).put("scope", sourceReference.scope).put("review", sourceReference.review).put("distribution", sourceReference.distribution.name()));
                }
            }
            analysis2.put("protocol_sources_version", KnowledgeRepository.VERSION).put("protocol_sources", protocolSources);
        }
        return new JSONObject().put("schema_version", diagnosticCase.schemaVersion).put("fingerprint_algorithm", "canonical-json-sha256-v1").put("input_fingerprint", hash(input2)).put("input", input2).put("analysis", analysis2);
    }

    public static DiagnosticCase validateDocument(JSONObject json) throws JSONException {
        if (json.getInt("schema_version") < 1 || json.getInt("schema_version") > 4 || !"canonical-json-sha256-v1".equals(json.getString("fingerprint_algorithm"))) {
            throw new JSONException("Documento não reconhecido");
        }
        JSONObject input = json.getJSONObject("input");
        if (!hash(input).equals(json.getString("input_fingerprint"))) {
            throw new JSONException("Integridade do caso inválida");
        }
        DiagnosticCase diagnosticCase = decode(input);
        if (canonical(json).equals(canonical(document(diagnosticCase)))) {
            return diagnosticCase;
        }
        throw new JSONException("Caso ou revisão offline incompatível; dados preservados");
    }

    public static String measurementFingerprint(DiagnosticCase diagnosticCase) throws JSONException {
        JSONObject json = encode(diagnosticCase);
        json.remove("symptoms");
        json.remove("revision");
        json.remove("revised_at_ms");
        return hash(json);
    }

    public static List<String> report(JSONObject json) throws JSONException {
        validateDocument(json);
        JSONArray reportLines = json.getJSONObject("analysis").getJSONArray("report_lines");
        ArrayList items = new ArrayList();
        for (int i = 0; i < reportLines.length(); i++) {
            items.add(reportLines.getString(i));
        }
        items.add("");
        items.add("Impressão digital da entrada: " + json.getString("input_fingerprint"));
        return items;
    }

    public static String hash(Object document) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("SHA-256").digest(canonical(document).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bArrDigest) {
                builder.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível calcular integridade", e);
        }
    }

    public static String canonical(Object document) throws JSONException {
        if (document == null || document == JSONObject.NULL) {
            return "null";
        }
        if (!(document instanceof JSONObject)) {
            if (!(document instanceof JSONArray)) {
                if (document instanceof Number) {
                    return JSONObject.numberToString((Number) document);
                }
                if (document instanceof Boolean) {
                    return document.toString();
                }
                if (document instanceof String) {
                    return JSONObject.quote((String) document);
                }
                throw new JSONException("Tipo JSON não permitido");
            }
            JSONArray array = (JSONArray) document;
            ArrayList items = new ArrayList();
            for (int i = 0; i < array.length(); i++) {
                items.add(canonical(array.get(i)));
            }
            return "[" + String.join(",", items) + "]";
        }
        JSONObject json = (JSONObject) document;
        ArrayList<String> items2 = new ArrayList();
        Iterator<String> itKeys = json.keys();
        while (itKeys.hasNext()) {
            items2.add(itKeys.next());
        }
        Collections.sort(items2);
        ArrayList items3 = new ArrayList();
        for (String text : items2) {
            items3.add(JSONObject.quote(text) + ":" + canonical(json.get(text)));
        }
        return "{" + String.join(",", items3) + "}";
    }
}