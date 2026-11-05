package io.rea.obd.vehicle.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.rea.obd.vehicle.VehicleProfile;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.Test;

public class VehicleEfficiencyTest {
    private static VehicleEfficiency efficiency() throws Exception {
        try (InputStream input = new FileInputStream("src/main/res/raw/vehicle_efficiency.json")) {
            return VehicleEfficiency.read(input);
        }
    }

    private static VehicleCatalog catalog() throws Exception {
        try (InputStream input = new FileInputStream("src/main/res/raw/vehicle_catalog.json")) {
            return VehicleCatalogJson.read(input).catalog;
        }
    }

    @Test
    public void almostEveryCatalogVersionHasOfficialConsumption() throws Exception {
        VehicleEfficiency efficiency = efficiency();
        VehicleCatalog catalog = catalog();
        int found = 0;
        for (String id : catalog.configurations.keySet()) {
            if (efficiency.get(id) != null) {
                found++;
            }
        }
        assertTrue(found * 100 >= catalog.configurations.size() * 95);
        for (String id : catalog.configurations.keySet()) {
            if (id.contains("-my19-") && id.startsWith("br-chevrolet-") && id.length() < 40) {
                assertTrue(id, efficiency.get(id) != null);
            }
        }
        VehicleEfficiency.Official cobalt = efficiency.get("br-chevrolet-cobalt-my19-lt-14-m6");
        assertEquals(new BigDecimal("12.5"), cobalt.otherCity);
        assertEquals(new BigDecimal("15.1"), cobalt.otherHighway);
        assertEquals(6, cobalt.page);
    }

    @Test
    public void profileFromTheCatalogFindsItsVersionAndHandWrittenOnesDoNot() throws Exception {
        VehicleCatalog catalog = catalog();
        VehicleCatalog.Configuration lt = catalog.configurations.get("br-chevrolet-cobalt-my19-lt-14-m6");
        VehicleProfile fromCatalog = new VehicleProfile("p", "Meu carro", "Chevrolet", "Cobalt", 2019, "BR",
                lt.attributes.get(io.rea.obd.vehicle.VehicleAttribute.Key.TRIM).value,
                lt.attributes.get(io.rea.obd.vehicle.VehicleAttribute.Key.ENGINE).value,
                lt.attributes.get(io.rea.obd.vehicle.VehicleAttribute.Key.FUEL_TYPE).value,
                lt.attributes.get(io.rea.obd.vehicle.VehicleAttribute.Key.TRANSMISSION).value, "");
        assertEquals("br-chevrolet-cobalt-my19-lt-14-m6", efficiency().find(catalog, fromCatalog).configurationId);
        VehicleProfile handWritten = new VehicleProfile("p", "Meu carro", "Chevrolet", "Cobalt", 2019, "BR", "LT", "1.4", "Flex", "", "");
        assertNull(efficiency().find(catalog, handWritten));
    }

    @Test
    public void fuelSelectsTheRightColumnsAndPositionIsHonest() throws Exception {
        VehicleEfficiency.Official cobalt = efficiency().get("br-chevrolet-cobalt-my19-lt-14-m6");
        BigDecimal[] ethanol = cobalt.forFuel("Etanol");
        assertEquals(new BigDecimal("8.5"), ethanol[0]);
        assertNull(cobalt.forFuel("Não sei"));
        BigDecimal[] gasoline = cobalt.forFuel("Gasolina");
        assertEquals("abaixo", VehicleEfficiency.position(new BigDecimal("11.0"), gasoline));
        assertEquals("dentro", VehicleEfficiency.position(new BigDecimal("13.0"), gasoline));
        assertEquals("acima", VehicleEfficiency.position(new BigDecimal("16.0"), gasoline));
    }
}
