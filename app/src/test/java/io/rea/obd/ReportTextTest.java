package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticCaseBuilder;
import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.history.DiagnosticCaseJson;
import io.rea.obd.vehicle.VehicleProfile;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;

public class ReportTextTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    public void instantsBecomeLocalTimeWithTheZoneStatedOnce() {
        List<String> out = ReportText.readable(Arrays.asList("Título", "Coleta DTC: 2026-10-27T01:01:28.207Z a 2026-10-27T01:02:00Z"), SAO_PAULO);
        assertEquals("Horários no fuso do aparelho (UTC-03:00).", out.get(1));
        assertEquals("Coleta DTC: 26/10/2026 22:01:28 a 26/10/2026 22:02:00", out.get(2));
    }

    @Test
    public void noTimesMeansNoZoneLine() {
        assertEquals(Arrays.asList("Título", "Sem horário"), ReportText.readable(Arrays.asList("Título", "Sem horário"), SAO_PAULO));
    }

    @Test
    public void pluralsFollowTheCount() {
        assertEquals("MIL: acesa · 1 falha indicada", ReportText.plurals("MIL: acesa · 1 falha(s) indicada(s)"));
        assertEquals("Armazenado: 2 códigos · resposta positiva", ReportText.plurals("Armazenado: 2 código(s) · resposta positiva"));
        assertEquals("Permanente: 0 códigos · resposta positiva", ReportText.plurals("Permanente: 0 código(s) · resposta positiva"));
        assertEquals("negativa · 1 código preservado", ReportText.plurals("negativa · 1 código(s) preservado(s)"));
        assertEquals("3 indicações de monitor", ReportText.plurals("3 indicação(ões) de monitor"));
        assertEquals("1 indicação de monitor", ReportText.plurals("1 indicação(ões) de monitor"));
    }

    @Test
    public void storedReportIsNotChangedBySavingAndReadingIt() throws Exception {
        long now = 1_793_000_000_000L;
        VehicleProfile profile = new VehicleProfile("v", "Carro", "Chevrolet", "Cobalt", 2019, "BR", "", "", "", "", "");
        DiagnosticCase diagnosticCase = DiagnosticCaseBuilder.create(profile, "simulador", DtcDemoScenario.create("v", "s", now),
                Collections.emptyList(), new DiagnosticCase.Symptoms("x", "", "", ""), now);
        JSONObject document = DiagnosticCaseJson.document(diagnosticCase);
        List<String> stored = DiagnosticCaseJson.report(document);
        ReportText.readable(stored, SAO_PAULO);
        assertEquals(stored, DiagnosticCaseJson.report(document));
        DiagnosticCaseJson.validateDocument(new JSONObject(document.toString()));
        assertTrue(String.join("\n", stored).contains("código(s)"));
    }

    @Test
    public void appendixDefinesOnlyCodesTheSavedAnalysisLacked() {
        long now = 1_793_000_000_000L;
        VehicleProfile profile = new VehicleProfile("v", "Carro", "Chevrolet", "Cobalt", 2019, "BR", "", "", "", "", "");
        DiagnosticCase diagnosticCase = DiagnosticCaseBuilder.create(profile, "simulador", DtcDemoScenario.create("v", "s", now),
                Collections.emptyList(), new DiagnosticCase.Symptoms("x", "", "", ""), now);
        assertTrue(ReportText.referenceAppendix(diagnosticCase).isEmpty());
    }
}
