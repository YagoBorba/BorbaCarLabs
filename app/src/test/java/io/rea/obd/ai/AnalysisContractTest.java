package io.rea.obd.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticCaseBuilder;
import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import io.rea.obd.vehicle.VehicleProfile;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class AnalysisContractTest {
    private static final long NOW = 1_793_000_000_000L;
    private static final String VEHICLE_ID = "vehicle-7f3a";

    private static AnalysisRequest request() throws JSONException {
        VehicleProfile profile = new VehicleProfile(VEHICLE_ID, "Carro do Yago", "Chevrolet", "Cobalt", 2019, "BR",
                "LTZ", "1.8", "Flex", "Automático", "placa ABC1D23, comprado em Curitiba");
        TelemetrySample rpm = new TelemetrySample(0x0C, 812.0, TelemetrySample.Status.VALID, DataOrigin.SIMULATED, NOW, 5, "");
        DiagnosticCase diagnosticCase = DiagnosticCaseBuilder.create(profile, "OBDLink MX+ 00:11:22:33:44:55",
                DtcDemoScenario.create(VEHICLE_ID, "session-1", NOW),
                Collections.singletonList(new DiagnosticCase.Sample(VEHICLE_ID, "session-1", rpm)),
                new DiagnosticCase.Symptoms("Falha em marcha lenta", "Motor frio", "Diária", "Após partida"), NOW);
        return AnalysisRequest.from(diagnosticCase);
    }

    @Test
    public void requestCarriesNoPersonalOrDeviceData() throws JSONException {
        String sent = request().body.toString();
        assertFalse(sent.contains("Carro do Yago"));
        assertFalse(sent.contains("ABC1D23"));
        assertFalse(sent.contains(VEHICLE_ID));
        assertFalse(sent.contains("OBDLink"));
        assertFalse(sent.contains("00:11:22"));
        assertFalse(sent.contains("session-1"));
        assertTrue(sent.contains("Cobalt"));
        assertTrue(sent.contains("Falha em marcha lenta"));
    }

    @Test
    public void symptomsAreSentWithoutContactDataOrPlates() throws JSONException {
        VehicleProfile profile = new VehicleProfile(VEHICLE_ID, "Carro", "Chevrolet", "Cobalt", 2019, "BR",
                "LTZ", "1.8", "Flex", "Automático", "");
        DiagnosticCase diagnosticCase = DiagnosticCaseBuilder.create(profile, "adapter",
                DtcDemoScenario.create(VEHICLE_ID, "session-1", NOW), Collections.emptyList(),
                new DiagnosticCase.Symptoms("Placa ABC1D23 falhando, me liga (41) 99876-5432", "Motor frio",
                        "yago@example.com", "P0300 acima de 2500 rpm"), NOW);
        AnalysisRequest request = AnalysisRequest.from(diagnosticCase);
        String sent = request.body.toString();
        assertFalse(sent.contains("ABC1D23"));
        assertFalse(sent.contains("99876"));
        assertFalse(sent.contains("yago@example.com"));
        assertTrue(sent.contains("P0300 acima de 2500 rpm"));
        assertEquals("1 telefone, 1 placa, 1 e-mail", request.removed);
    }

    @Test
    public void everyObservationHasASyntheticReference() throws JSONException {
        AnalysisRequest request = request();
        JSONArray observations = request.body.getJSONArray("observations");
        assertEquals(observations.length(), request.refs().size());
        for (int i = 0; i < observations.length(); i++) {
            assertTrue(request.refs().contains(observations.getJSONObject(i).getString("ref")));
        }
        assertTrue(request.refs().contains("T1"));
        assertEquals(AnalysisRequest.SCHEMA, request.body.getString("schema"));
    }

    private static JSONObject answer(String evidence, String confidence) throws JSONException {
        return new JSONObject()
                .put("model", "claude-opus-5-5")
                .put("analysis", new JSONObject()
                        .put("summary", "Resumo")
                        .put("hypotheses", new JSONArray().put(new JSONObject()
                                .put("title", "Falha de ignição")
                                .put("reasoning", "Código de falha de combustão observado.")
                                .put("evidence", new JSONArray().put(evidence))
                                .put("confidence", confidence)
                                .put("next_check", "Verificar velas e bobinas.")))
                        .put("missing_information", new JSONArray().put("Quilometragem"))
                        .put("safety_note", ""));
    }

    @Test
    public void acceptsAnswerCitingSentEvidence() throws JSONException {
        AnalysisRequest request = request();
        AnalysisResult result = AnalysisResult.parse(answer("T1", "media"), request.refs());
        assertEquals(1, result.hypotheses.size());
        assertTrue(result.display(request.labels).contains("Rotação do motor"));
        assertTrue(result.display(request.labels).contains("confiança média"));
    }

    @Test
    public void dropsInventedEvidenceButKeepsTheAnswer() throws JSONException {
        AnalysisRequest request = request();
        AnalysisResult result = AnalysisResult.parse(answer("X9", "alta"), request.refs());
        assertEquals("Resumo", result.summary);
        assertEquals(1, result.hypotheses.size());
        assertTrue(result.hypotheses.get(0).evidence.isEmpty());
        assertFalse(result.display(request.labels).contains("X9"));
        assertFalse(result.display(request.labels).contains("Evidências"));
    }

    @Test
    public void proseInsteadOfRefsAndOverlongListsAreTrimmed() throws JSONException {
        JSONObject reply = answer("T1", "media");
        JSONObject analysis = reply.getJSONObject("analysis");
        JSONArray hypotheses = analysis.getJSONArray("hypotheses");
        hypotheses.getJSONObject(0).getJSONArray("evidence").put("Código P0301 no cilindro 1");
        for (int i = 0; i < 6; i++) hypotheses.put(new JSONObject(hypotheses.getJSONObject(0).toString()));
        for (int i = 0; i < 10; i++) analysis.getJSONArray("missing_information").put("Item " + i);
        AnalysisResult result = AnalysisResult.parse(reply, request().refs());
        assertEquals(5, result.hypotheses.size());
        assertEquals(java.util.Collections.singletonList("T1"), result.hypotheses.get(0).evidence);
        assertEquals(8, result.missing.size());
        assertTrue(result.hasDetails());
        assertFalse(result.details(request().labels).startsWith("Resumo"));
    }

    @Test(expected = JSONException.class)
    public void rejectsConfidenceOutsideContract() throws JSONException {
        AnalysisResult.parse(answer("T1", "certeza"), request().refs());
    }
}
