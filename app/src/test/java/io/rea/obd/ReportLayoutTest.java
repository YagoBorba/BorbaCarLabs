package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ReportLayoutTest {
    private static final ReportLayout.Measure MEASURE = (text, style) -> text.length() * style.size * 0.5f;

    private static List<ReportLayout.Line> all(ReportLayout layout) {
        List<ReportLayout.Line> lines = new ArrayList<>();
        layout.pages.forEach(lines::addAll);
        return lines;
    }

    @Test
    public void titleHeadingsAndDetailsFollowTheReportShape() {
        ReportLayout layout = ReportLayout.of(Arrays.asList(
                "BorbaCar Labs · investigação offline", "Caso: x", "", "Dados observados", "P0301 · Pendente",
                "  Resposta 1: 1450 rpm"), MEASURE);
        List<ReportLayout.Line> lines = all(layout);
        assertEquals(ReportLayout.Style.TITLE, lines.get(0).style);
        assertEquals(ReportLayout.Style.BODY, lines.get(1).style);
        assertEquals(ReportLayout.Style.HEADING, lines.get(2).style);
        assertEquals("Dados observados", lines.get(2).text);
        assertEquals(ReportLayout.Style.DETAIL, lines.get(4).style);
        assertTrue(lines.get(4).x > ReportLayout.LEFT);
        assertFalse(layout.simulated);
    }

    @Test
    public void longReportsPaginateAndStayInsideTheMargins() {
        List<String> source = new ArrayList<>();
        source.add("Título");
        for (int i = 0; i < 40; i++) {
            source.add("");
            source.add("Seção " + i);
            source.add("Uma linha de texto comprida o bastante para precisar quebrar dentro da largura útil da página A4 do relatório. " + i);
        }
        ReportLayout layout = ReportLayout.of(source, MEASURE);
        assertTrue(layout.pages.size() > 2);
        for (List<ReportLayout.Line> page : layout.pages) {
            for (ReportLayout.Line line : page) {
                assertTrue(line.y <= ReportLayout.BOTTOM);
                assertTrue(line.x + MEASURE.width(line.text, line.style) <= ReportLayout.RIGHT + 0.01f);
            }
            assertFalse(page.get(page.size() - 1).style == ReportLayout.Style.HEADING);
        }
    }

    @Test
    public void simulatedDataIsFlaggedForTheBanner() {
        assertTrue(ReportLayout.of(Arrays.asList("Título", "SIMULAÇÃO · dados fictícios"), MEASURE).simulated);
        assertTrue(ReportLayout.of(Arrays.asList("Relatórios", "26/10/2026 14:00 · SIMULADO"), MEASURE).simulated);
    }

    @Test
    public void multiLineEntriesAndEmptyInputDoNotBreak() {
        assertEquals(3, all(ReportLayout.of(Arrays.asList("Título", "linha a\nlinha b"), MEASURE)).size());
        assertEquals(1, ReportLayout.of(new ArrayList<>(), MEASURE).pages.size());
    }
}
