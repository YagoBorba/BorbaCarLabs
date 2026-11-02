package io.rea.obd.vehicle.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.rea.obd.vehicle.VehicleAttribute;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class VehicleCatalogJsonTest {
    private static final String EMBEDDED = "src/main/res/raw/vehicle_catalog.json";

    private static VehicleCatalog embedded() throws Exception {
        try (InputStream input = new FileInputStream(EMBEDDED)) {
            return VehicleCatalogJson.read(input).catalog;
        }
    }

    @Test
    public void readsTheEmbeddedPbevCatalogThroughTheAppValidator() throws Exception {
        VehicleCatalog catalog = embedded();
        assertEquals("br-pbev-2012-2026-1", catalog.version);
        assertEquals(4, catalog.sequence);
        assertTrue(catalog.markets.contains("BR"));
        assertTrue(catalog.configurations.size() >= 9000);
        assertEquals(catalog.configurations.size(), catalog.matching(Collections.emptyMap()).size());
        Set<String> makes = new HashSet<>();
        for (VehicleCatalog.Configuration configuration : catalog.configurations.values()) {
            makes.add(configuration.attributes.get(VehicleAttribute.Key.MAKE).value);
        }
        assertTrue(makes.size() >= 40);
    }

    @Test
    public void curatedChevroletVersionsKeepTheirIds() throws Exception {
        VehicleCatalog catalog = embedded();
        for (String id : new String[]{"br-chevrolet-cobalt-my19-lt-14-m6", "br-chevrolet-cobalt-my19-ltz-14-m6"}) {
            assertNotNull(id, catalog.configurations.get(id));
        }
    }

    @Test
    public void filtersByAttributesAcrossBrands() throws Exception {
        Map<VehicleAttribute.Key, String> fiat = new EnumMap<>(VehicleAttribute.Key.class);
        fiat.put(VehicleAttribute.Key.MAKE, "Fiat");
        fiat.put(VehicleAttribute.Key.MODEL, "Mobi");
        assertTrue(embedded().matching(fiat).size() >= 2);
    }

    @Test
    public void coversEveryYearFrom2012To2026() throws Exception {
        Set<String> years = new HashSet<>();
        for (VehicleCatalog.Configuration configuration : embedded().configurations.values()) {
            years.add(configuration.attributes.get(VehicleAttribute.Key.MODEL_YEAR).value);
        }
        for (int year = 2012; year <= 2026; year++) {
            assertTrue(String.valueOf(year), years.contains(String.valueOf(year)));
        }
    }

    @Test
    public void popularModelsAreFoundAndNamesAreClean() throws Exception {
        VehicleCatalog catalog = embedded();
        Set<String> models = new HashSet<>();
        for (VehicleCatalog.Configuration configuration : catalog.configurations.values()) {
            models.add(configuration.attributes.get(VehicleAttribute.Key.MAKE).value + " " + configuration.attributes.get(VehicleAttribute.Key.MODEL).value);
        }
        for (String model : new String[]{"Renault Kwid", "Chevrolet Tracker", "Volkswagen Jetta", "Volkswagen T-Cross", "Chevrolet Montana", "Chevrolet Equinox", "Peugeot 308", "Peugeot 3008", "Hyundai Tucson", "Chevrolet Onix", "Fiat Strada"}) {
            assertTrue(model, models.contains(model));
        }
        for (String broken : new String[]{"Hyundai Turbo", "JAC CVT", "JAC Jetflex", "Chrysler Town &", "Jaguar Epace", "Jaguar Fpace", "Suzuki Scross", "Kia RIO", "Kia New Soul", "Fiat Novo Uno", "Toyota RAV4 4X2", "Volkswagen Saveiro CD", "Chevrolet Novo Cruze", "Volvo XC90 (2019)"}) {
            assertTrue(broken, !models.contains(broken));
        }
    }

    @Test
    public void searchFindsACarFromAFewWords() throws Exception {
        VehicleCatalog catalog = embedded();
        VehicleSearch.Result result = VehicleSearch.search(catalog, "onix 2019 ltz", 8);
        assertTrue(result.total > 0);
        for (VehicleCatalog.Configuration configuration : result.matches) {
            assertEquals("Onix", configuration.attributes.get(VehicleAttribute.Key.MODEL).value);
            assertEquals("2019", configuration.attributes.get(VehicleAttribute.Key.MODEL_YEAR).value);
        }
        assertEquals("Kwid", VehicleSearch.search(catalog, "kwid", 1).matches.get(0).attributes.get(VehicleAttribute.Key.MODEL).value);
        assertEquals("T-Cross", VehicleSearch.search(catalog, "tcross", 1).matches.get(0).attributes.get(VehicleAttribute.Key.MODEL).value);
        assertEquals("2026", VehicleSearch.search(catalog, "tracker", 1).matches.get(0).attributes.get(VehicleAttribute.Key.MODEL_YEAR).value);
        assertEquals(0, VehicleSearch.search(catalog, "carro inexistente xyz", 8).total);
    }
}
