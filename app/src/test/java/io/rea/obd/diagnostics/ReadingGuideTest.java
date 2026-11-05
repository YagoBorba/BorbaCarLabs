package io.rea.obd.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.rea.obd.model.DataOrigin;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

public class ReadingGuideTest {
    private static final long NOW = 1_793_000_000_000L;

    private static DtcReadResult.Status positive() {
        return new DtcReadResult.Status(DtcReadResult.Outcome.POSITIVE, NOW, NOW, 1, Collections.emptyList(), "");
    }

    private static DtcReadResult read(DtcReadResult.ServiceResult... services) {
        DtcReadResult.MilResult mil = new DtcReadResult.MilResult(positive(), Collections.singletonList(new DtcReadResult.MilObservation("mil", 0x02, NOW)));
        return new DtcReadResult("capture", "car", "session", DataOrigin.VEHICLE, NOW, NOW, mil, Arrays.asList(services));
    }

    private static DtcReadResult.ServiceResult service(DtcObservation.Category category, String... codes) {
        DtcObservation[] observations = new DtcObservation[codes.length];
        for (int i = 0; i < codes.length; i++) {
            observations[i] = new DtcObservation(category + ":" + codes[i], codes[i], category, NOW);
        }
        return new DtcReadResult.ServiceResult(category, positive(), Arrays.asList(observations));
    }

    @Test
    public void codesSeenByTwoServicesAreExplainedOnce() {
        DtcReadResult read = read(service(DtcObservation.Category.STORED, "P0301"), service(DtcObservation.Category.PENDING, "P0301", "P1234"), service(DtcObservation.Category.PERMANENT));
        List<ReadingGuide.Code> codes = ReadingGuide.codes(read);

        assertEquals(2, codes.size());
        ReadingGuide.Code misfire = codes.get(0);
        assertEquals("P0301", misfire.code);
        assertEquals(EnumSet.of(DtcObservation.Category.STORED, DtcObservation.Category.PENDING), misfire.categories);
        assertEquals("Armazenado e Pendente", misfire.statuses());
        assertEquals("Falha de combustão indicada no cilindro 1.", misfire.meaning);
        assertEquals(2, misfire.evidence().size());
        assertTrue(misfire.evidence().get(0).startsWith("Armazenado (serviço 03)"));

        ReadingGuide.Code manufacturer = codes.get(1);
        assertNull(manufacturer.meaning);
        assertNull(manufacturer.question);
        assertEquals("Motor e transmissão · código específico do fabricante", manufacturer.structure);
    }

    @Test
    public void contextStatesTheMilAndTheLimitsOfTheReading() {
        List<String> context = ReadingGuide.context(read(service(DtcObservation.Category.STORED, "P0420"), service(DtcObservation.Category.PENDING), service(DtcObservation.Category.PERMANENT)));
        assertEquals("Luz de injeção (MIL): apagada · 2 falhas indicadas", context.get(0));
        assertTrue(context.get(context.size() - 1).contains("ABS e airbag"));

        List<String> demo = ReadingGuide.context(DtcDemoScenario.create("car", "session", NOW));
        assertEquals("Leitura de demonstração: os códigos são fictícios.", demo.get(0));
        assertEquals("Luz de injeção (MIL): acesa · 1 falha indicada", demo.get(1));
    }

    @Test
    public void workshopSummaryListsWhatAMechanicNeeds() {
        DtcReadResult read = DtcDemoScenario.create("car", "session", NOW);
        SupplementalReadResult supplement = SupplementalDemoScenario.create("car", "session", NOW);
        List<String> lines = ReadingGuide.workshopSummary(read, "Meu Cobalt · Chevrolet Cobalt 2019", "OBDII", supplement);

        assertEquals("Resumo para oficina · BorbaCar Labs", lines.get(0));
        assertEquals("SIMULAÇÃO · dados fictícios", lines.get(1));
        assertTrue(lines.contains("Veículo: Meu Cobalt · Chevrolet Cobalt 2019"));
        assertTrue(lines.contains("Adaptador: OBDII"));
        assertTrue(lines.contains("Luz de injeção (MIL): acesa · 1 falha indicada"));
        int codes = lines.indexOf("Códigos encontrados");
        assertTrue(codes > 0);
        assertEquals("P0133 · Armazenado", lines.get(codes + 1));
        assertEquals("  Resposta lenta no circuito do sensor de oxigênio.", lines.get(codes + 2));
        assertTrue(lines.contains("P0301 · Pendente"));
        assertTrue(lines.contains("Prontidão e quadro congelado"));
        assertTrue(lines.contains(ReadingGuide.SCOPE));
        assertEquals(1, Collections.frequency(lines, "SIMULAÇÃO · dados fictícios"));
    }

    @Test
    public void workshopSummaryWithoutVehicleOrCodesStaysHonest() {
        DtcReadResult read = read(service(DtcObservation.Category.STORED), service(DtcObservation.Category.PENDING), service(DtcObservation.Category.PERMANENT));
        List<String> lines = ReadingGuide.workshopSummary(read, "", "", null);

        assertTrue(lines.contains("Veículo: não associado"));
        assertTrue(lines.contains("Nenhum código informado pela ECU."));
        assertFalse(lines.contains("Prontidão e quadro congelado"));
        assertFalse(lines.stream().anyMatch((line) -> line.startsWith("Adaptador")));
    }
}
