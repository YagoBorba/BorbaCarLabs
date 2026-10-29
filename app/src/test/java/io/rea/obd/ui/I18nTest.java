package io.rea.obd.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class I18nTest {
    @Before
    public void english() throws IOException, JSONException {
        I18n.use(new String(Files.readAllBytes(Paths.get("src/main/res/raw/translations_en.json")), StandardCharsets.UTF_8));
    }

    @After
    public void portuguese() {
        I18n.reset();
    }

    @Test
    public void translatesWholeTextsAndKeepsUnknownOnes() {
        assertEquals("Start guided reading", I18n.t("Iniciar leitura guiada"));
        assertEquals("P0301", I18n.t("P0301"));
        assertEquals("My Cobalt", I18n.t("My Cobalt"));
        assertEquals("", I18n.t(""));
    }

    @Test
    public void translatesEachLineAndEachSegment() {
        assertEquals("Last reading · today at 21:47", I18n.t("Última leitura · hoje às 21:47"));
        assertEquals("Settings\nHistory · 2 saved readings · 1 investigation", I18n.t("Ajustes\nHistórico · 2 leituras salvas · 1 investigação"));
    }

    @Test
    public void translatesNumbersPluralsAndDates() {
        assertEquals("2 trouble codes found", I18n.t("2 códigos de falha encontrados"));
        assertEquals("1 trouble code found", I18n.t("1 código de falha encontrado"));
        assertEquals("due in 1 day (11/11)", I18n.t("vence em 1 dia (11/11)"));
        assertEquals("due in 4 days (14/11)", I18n.t("vence em 4 dias (14/11)"));
        assertEquals("350 km left", I18n.t("faltam 350 km"));
        assertEquals("1 capture · min 1574 · max 1574 rpm · 0 gaps", I18n.t("1 capturas · mín 1574 · máx 1574 rpm · 0 lacunas"));
    }

    @Test
    public void translatesInsideCapturedGroups() {
        assertEquals("Language: Automatic", I18n.t("Idioma: Automático"));
        assertEquals("Removed from the text before sending: 1 phone number, 1 license plate(s).",
                I18n.t("Removido do texto antes do envio: 1 telefone, 1 placa."));
        assertEquals("Engine speed. Expanded", I18n.t("Rotação do motor. Expandido"));
    }

    @Test
    public void portugueseIsUntouched() {
        I18n.reset();
        assertFalse(I18n.english());
        assertEquals("Iniciar leitura guiada", I18n.t("Iniciar leitura guiada"));
        assertEquals("12,4", Format.decimal(new java.math.BigDecimal("12.4")));
    }

    @Test
    public void numbersFollowTheLanguage() {
        assertTrue(I18n.english());
        assertEquals("12.4", Format.decimal(new java.math.BigDecimal("12.4")));
        assertEquals("48,000 km", Format.km(new java.math.BigDecimal("48000")));
    }
}
