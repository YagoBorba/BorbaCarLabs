package io.rea.obd.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PersonalDataFilterTest {
    private static String clean(String text) {
        return new PersonalDataFilter().apply(text);
    }

    @Test
    public void removesPhoneNumbers() {
        assertEquals("Ligar [telefone removido] depois", clean("Ligar (41) 99876-5432 depois"));
        assertEquals("[telefone removido]", clean("+55 41 99876-5432"));
        assertEquals("fixo [telefone removido]", clean("fixo 3333-4444"));
        assertEquals("cel [telefone removido]", clean("cel 41998765432"));
    }

    @Test
    public void removesEmails() {
        assertEquals("falar com [e-mail removido] amanhã", clean("falar com joao.silva+carro@gmail.com amanhã"));
    }

    @Test
    public void removesCpfAndCnpj() {
        assertEquals("dono [CPF removido]", clean("dono 123.456.789-09"));
        assertEquals("dono [CPF removido]", clean("dono 12345678909"));
        assertEquals("oficina [CNPJ removido]", clean("oficina 12.345.678/0001-95"));
    }

    @Test
    public void removesOldAndMercosulPlates() {
        assertEquals("placa [placa removida]", clean("placa ABC-1234"));
        assertEquals("placa [placa removida]", clean("placa ABC1234"));
        assertEquals("placa [placa removida]", clean("placa abc1d23"));
    }

    @Test
    public void keepsDiagnosticText() {
        String text = "P0300 e P0171 com RPM 812, rpm1500 a 90 °C, 48000 km, troca em 15/11/2026, sensor O2 B1S1, 12,4 V";
        assertEquals(text, clean(text));
    }

    @Test
    public void summarizesWhatWasRemoved() {
        PersonalDataFilter filter = new PersonalDataFilter();
        filter.apply("ABC1D23 e DEF-5678, ligar 41 99876-5432");
        assertTrue(filter.changed());
        assertEquals("1 telefone, 2 placas", filter.summary());
        assertFalse(new PersonalDataFilter().changed());
    }
}
