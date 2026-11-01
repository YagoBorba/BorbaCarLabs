package io.rea.obd.vehicle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import android.content.Context;
import io.rea.obd.model.FactProvenance;
import io.rea.obd.vehicle.catalog.VehicleCatalog;
import io.rea.obd.vehicle.catalog.VehicleCatalogJson;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class VehicleRepositoryTest {
    private static final String EMBEDDED = "src/main/res/raw/vehicle_catalog.json";
    private static final String COBALT_LT = "br-chevrolet-cobalt-my19-lt-14-m6";
    private static final String COBALT_LTZ = "br-chevrolet-cobalt-my19-ltz-14-m6";

    private VehicleCatalog catalog;
    private VehicleRepository repository;

    @Before
    public void setUp() throws Exception {
        try (InputStream input = new FileInputStream(EMBEDDED)) {
            catalog = VehicleCatalogJson.read(input).catalog;
        }
        repository = new VehicleRepository(RuntimeEnvironment.getApplication().getSharedPreferences("garagem-teste", Context.MODE_PRIVATE));
    }

    private void save(VehicleRecord record, int revision, VehicleCatalog source) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "salvar-perfil");
            thread.setDaemon(true);
            return thread;
        });
        try {
            executor.submit(() -> {
                repository.saveSelection(record, revision, source);
                return null;
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Salvar o perfil travou por mais de 5 segundos");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    private VehicleDraft configured(String id) {
        VehicleDraft draft = new VehicleDraft(null);
        draft.name = "Meu Cobalt";
        draft.chooseConfiguration(catalog, id);
        return draft;
    }

    private VehicleRecord replacing(VehicleDraft draft, VehicleAttribute attribute) {
        Map<VehicleAttribute.Key, VehicleAttribute> attributes = new EnumMap<>(VehicleAttribute.Key.class);
        attributes.putAll(draft.attributes());
        attributes.put(attribute.key, attribute);
        return new VehicleRecord(draft.id, draft.name, 1, attributes);
    }

    @Test
    public void savesAProfileChosenFromACatalogVersion() throws Exception {
        save(configured(COBALT_LT).result(), 0, catalog);
        VehicleRecord saved = repository.activeRecord();
        VehicleAttribute engine = catalog.configurations.get(COBALT_LT).attributes.get(VehicleAttribute.Key.ENGINE);
        assertEquals("Meu Cobalt", saved.name);
        assertEquals(1, saved.revision);
        assertEquals(engine, saved.attribute(VehicleAttribute.Key.ENGINE));
        assertEquals(FactProvenance.Origin.CATALOG, saved.attribute(VehicleAttribute.Key.MAKE).provenance.origin);
    }

    @Test
    public void savesMakeModelAndYearChosenStepByStep() throws Exception {
        VehicleDraft draft = new VehicleDraft(null);
        draft.name = "Cobalt";
        draft.choose(catalog, VehicleAttribute.Key.MAKE, "Chevrolet");
        draft.choose(catalog, VehicleAttribute.Key.MODEL, "Cobalt");
        draft.choose(catalog, VehicleAttribute.Key.MODEL_YEAR, "2019");
        save(draft.result(), 0, catalog);
        VehicleRecord saved = repository.activeRecord();
        assertEquals("Chevrolet", saved.attribute(VehicleAttribute.Key.MAKE).value);
        assertEquals("Cobalt", saved.attribute(VehicleAttribute.Key.MODEL).value);
        assertEquals("2019", saved.attribute(VehicleAttribute.Key.MODEL_YEAR).value);
    }

    @Test
    public void editsASavedProfileAndSwitchesItsVersion() throws Exception {
        save(configured(COBALT_LT).result(), 0, catalog);
        VehicleDraft renamed = new VehicleDraft(repository.activeRecord());
        renamed.name = "Cobalt da família";
        save(renamed.result(), renamed.expectedRevision(), catalog);
        assertEquals(2, repository.activeRecord().revision);
        assertEquals("Cobalt da família", repository.activeRecord().name);
        VehicleDraft switched = new VehicleDraft(repository.activeRecord());
        switched.chooseConfiguration(catalog, COBALT_LTZ);
        save(switched.result(), switched.expectedRevision(), catalog);
        assertEquals(3, repository.activeRecord().revision);
        assertEquals("LTZ", repository.activeRecord().attribute(VehicleAttribute.Key.TRIM).value);
        assertEquals(1, repository.records().size());
    }

    @Test
    public void refusesCatalogDataThatIsNotInTheCatalog() throws Exception {
        VehicleDraft draft = configured(COBALT_LT);
        VehicleAttribute engine = draft.attribute(VehicleAttribute.Key.ENGINE);
        VehicleRecord forged = replacing(draft, new VehicleAttribute(VehicleAttribute.Key.ENGINE, "9.9 L · 32V", engine.provenance));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> save(forged, 0, catalog));
        assertEquals("Atributo sem configuração disponível e coerente no catálogo revisado", error.getMessage());
        assertEquals(null, repository.activeRecord());
    }

    @Test
    public void refusesCatalogDataThatContradictsTheOtherChoices() throws Exception {
        VehicleRecord mixed = replacing(configured(COBALT_LT), new VehicleAttribute(VehicleAttribute.Key.MODEL, "Onix", FactProvenance.declared(1L)));
        assertThrows(IllegalArgumentException.class, () -> save(mixed, 0, catalog));
        assertEquals(0, repository.records().size());
    }

    @Test
    public void refusesCatalogDataWhenTheCatalogIsUnavailable() throws Exception {
        VehicleRecord record = configured(COBALT_LT).result();
        assertThrows(IllegalArgumentException.class, () -> save(record, 0, null));
        assertEquals(0, repository.records().size());
    }
}
