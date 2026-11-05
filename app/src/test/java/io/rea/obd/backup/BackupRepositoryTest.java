package io.rea.obd.backup;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticCaseBuilder;
import io.rea.obd.diagnostics.DtcDemoScenario;
import io.rea.obd.history.DiagnosticCaseStore;
import io.rea.obd.history.DiagnosticHistoryStore;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalStore;
import io.rea.obd.vehicle.VehicleProfile;
import io.rea.obd.vehicle.VehicleRepository;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupRepositoryTest {
    private Context context;
    private SharedPreferences prefs;
    private BackupRepository backups;
    private static final long NOW = 1793000000000L;

    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        prefs = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
        new VehicleRepository(prefs); new DiagnosticHistoryStore(prefs);
        backups = new BackupRepository(context, prefs);
    }

    private void populate() throws Exception {
        VehicleProfile car = new VehicleProfile("car", "Cobalt", "Chevrolet", "Cobalt", 2019, "BR", "", "", "Flex", "", "");
        new VehicleRepository(prefs).save(car);
        try (JournalStore store = new JournalStore(context)) {
            store.save(new JournalEntry("oil", "car", "Cobalt", JournalEntry.Kind.SERVICE, 1, NOW, NOW, NOW,
                    "Troca de óleo", null, null, null, null, false, false, "2026-12-01", null));
        }
        DiagnosticCase kase = DiagnosticCaseBuilder.create(car, "Simulador", DtcDemoScenario.create("car", "session", NOW),
                Collections.emptyList(), new DiagnosticCase.Symptoms("Falha", "Frio", "", ""), NOW);
        try (DiagnosticCaseStore store = new DiagnosticCaseStore(context)) { store.save(kase); }
    }

    @Test public void restoresProfilesCasesAndJournalAndCanUndo() throws Exception {
        JSONObject empty = backups.snapshot();
        populate();
        JSONObject complete = backups.snapshot();
        assertTrue(backups.validate(complete).contains("1 perfis"));
        backups.restore(empty);
        assertEquals(0, new VehicleRepository(prefs).records().size());
        backups.restore(backups.previous());
        assertEquals(1, new VehicleRepository(prefs).records().size());
        try (JournalStore journal = new JournalStore(context); DiagnosticCaseStore cases = new DiagnosticCaseStore(context)) {
            assertEquals(1, journal.latest().size()); assertEquals(1, cases.latestAll().size());
        }
        assertFalse(complete.toString().contains("server_url"));
    }

    @Test public void corruptedOrUnsupportedBackupsLeaveExistingDataUntouched() throws Exception {
        populate();
        JSONObject before = backups.snapshot();
        JSONObject broken = new JSONObject(before.toString()); broken.getJSONObject("payload").put("garage", "{}");
        try { backups.restore(broken); fail(); } catch (Exception expected) { }
        assertEquals(before.toString(), backups.snapshot().toString());
        JSONObject unknown = new JSONObject(before.toString()).put("format", "future");
        try { backups.restore(unknown); fail(); } catch (Exception expected) { }
        assertEquals(1, new VehicleRepository(prefs).records().size());
    }

    @Test public void validatesRevisionHistoryEvenWhenChecksumIsRecomputed() throws Exception {
        populate();
        JSONObject broken = backups.snapshot();
        broken.getJSONObject("payload").getJSONArray("journal").getJSONObject(0).put("revision", 9);
        broken.put("checksum", io.rea.obd.history.DiagnosticCaseJson.hash(broken.getJSONObject("payload")));
        try { backups.restore(broken); fail(); } catch (Exception expected) { }
        try (JournalStore journal = new JournalStore(context)) { assertEquals(1, journal.latest().get(0).revision); }
    }

    @Test public void interruptedRestoreRollsBackBeforeTheAppLoadsData() throws Exception {
        JSONObject original = backups.snapshot();
        populate();
        File marker = new File(context.getFilesDir(), "backup-recovery.json");
        Files.write(marker.toPath(), original.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(backups.recoverInterruptedRestore());
        assertEquals(0, new VehicleRepository(prefs).records().size());
        assertFalse(marker.exists());
        assertFalse(backups.recoverInterruptedRestore());
    }

    @Test public void oversizedFilesAreRejectedBeforeParsing() throws Exception {
        try { BackupRepository.read(new ByteArrayInputStream(new byte[BackupRepository.MAX_BYTES + 1])); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("8 MB")); }
    }
}
