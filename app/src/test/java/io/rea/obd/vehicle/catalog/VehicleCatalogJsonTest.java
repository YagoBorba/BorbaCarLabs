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
        assertEquals("br-pbev-2019-2", catalog.version);
        assertEquals(3, catalog.sequence);
        assertTrue(catalog.markets.contains("BR"));
        assertTrue(catalog.configurations.size() >= 500);
        assertEquals(catalog.configurations.size(), catalog.matching(Collections.emptyMap()).size());
        Set<String> makes = new HashSet<>();
        for (VehicleCatalog.Configuration configuration : catalog.configurations.values()) {
            makes.add(configuration.attributes.get(VehicleAttribute.Key.MAKE).value);
        }
        assertTrue(makes.size() >= 30);
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
}
