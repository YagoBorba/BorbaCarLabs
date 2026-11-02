package io.rea.obd.vehicle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VinDecoderTest {
    @Test
    public void readsMakeAndModelYearFromABrazilianVin() {
        VinDecoder.Decoded decoded = VinDecoder.decode("9BGKS48U0KG123456", 2026);
        assertEquals("Chevrolet", decoded.make);
        assertEquals(Integer.valueOf(2019), decoded.modelYear);
        assertEquals(Integer.valueOf(2026), VinDecoder.decode("9BWAG45U1TT000001", 2026).modelYear);
        assertEquals(Integer.valueOf(2012), VinDecoder.decode("9BD15822AC1234567", 2026).modelYear);
    }

    @Test
    public void rejectsTextThatIsNotAVin() {
        assertNull(VinDecoder.decode("não informado", 2026));
        assertNull(VinDecoder.decode("9BGKS48U0KG12345", 2026));
        assertNull(VinDecoder.decode("9BGKS48U0KG12345I", 2026));
        assertEquals("", VinDecoder.describe(null, 2026, "Chevrolet", 2019));
    }

    @Test
    public void comparesTheVinWithTheActiveProfile() {
        assertEquals("Pelo VIN: Chevrolet · ano-modelo 2019\nCombina com o perfil ativo.", VinDecoder.describe("9BGKS48U0KG123456", 2026, "Chevrolet", 2019));
        String other = VinDecoder.describe("9BGKS48U0KG123456", 2026, "Fiat", 2018);
        assertTrue(other, other.endsWith("O perfil ativo diz Fiat 2018. Confira se é o mesmo carro."));
        assertEquals("Pelo VIN: Chevrolet · ano-modelo 2019", VinDecoder.describe("9BGKS48U0KG123456", 2026, null, null));
    }
}
