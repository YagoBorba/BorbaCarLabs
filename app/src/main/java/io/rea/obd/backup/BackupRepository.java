package io.rea.obd.backup;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AtomicFile;
import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.history.DiagnosticCaseJson;
import io.rea.obd.history.DiagnosticCaseStore;
import io.rea.obd.history.DiagnosticHistoryStore;
import io.rea.obd.journal.JournalEntry;
import io.rea.obd.journal.JournalJson;
import io.rea.obd.journal.JournalStore;
import io.rea.obd.vehicle.VehicleRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONObject;

/** Portable logical backup. No SQL from the imported file is ever executed. */
public final class BackupRepository {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final String FORMAT = "borbacar-backup-1";
    private static final Object TRANSFER_LOCK = new Object();
    private final Context context;
    private final SharedPreferences preferences;

    public BackupRepository(Context context, SharedPreferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }

    private AtomicFile recoveryFile() { return new AtomicFile(new File(context.getFilesDir(), "backup-recovery.json")); }

    public static JSONObject read(InputStream input) throws Exception {
        if (input == null) throw new IllegalArgumentException("Arquivo indisponível.");
        try (InputStream stream = input; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = stream.read(chunk)) != -1) {
                if (out.size() + count > MAX_BYTES) throw new IllegalArgumentException("Backup excede 8 MB.");
                out.write(chunk, 0, count);
            }
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }

    public JSONObject snapshot() throws Exception {
        synchronized (TRANSFER_LOCK) { return snapshotInternal(); }
    }

    private JSONObject snapshotInternal() throws Exception {
        JSONObject payload = new JSONObject();
        // Initialize schemas through their owners; take both databases under one SQLite transaction.
        try (JournalStore journal = new JournalStore(context); DiagnosticCaseStore cases = new DiagnosticCaseStore(context)) {
            journal.getWritableDatabase();
            SQLiteDatabase db = cases.getWritableDatabase();
            db.execSQL("ATTACH DATABASE ? AS journal_backup", new Object[]{context.getDatabasePath("vehicle-journal.db").getAbsolutePath()});
            try {
                db.beginTransaction();
                try {
                    payload.put("garage", preferences.getString(VehicleRepository.KEY, ""));
                    payload.put("history", preferences.getString(DiagnosticHistoryStore.KEY, ""));
                    payload.put("cases", rows(db, "SELECT payload,checksum FROM case_revisions ORDER BY case_id,revision"));
                    payload.put("journal", rows(db, "SELECT payload,checksum FROM journal_backup.entries ORDER BY id,revision"));
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            } finally { db.execSQL("DETACH DATABASE journal_backup"); }
        }
        JSONObject result = new JSONObject().put("format", FORMAT).put("payload", payload).put("checksum", DiagnosticCaseJson.hash(payload));
        if (result.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IllegalArgumentException("Backup excede 8 MB.");
        return result;
    }

    private static JSONArray rows(SQLiteDatabase db, String sql) throws Exception {
        JSONArray result = new JSONArray();
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                JSONObject document = new JSONObject(cursor.getString(0));
                if (!DiagnosticCaseJson.hash(document).equals(cursor.getString(1))) throw new IllegalStateException("Registro local com integridade inválida; backup cancelado.");
                result.put(document);
            }
        }
        return result;
    }

    public String validate(JSONObject archive) throws Exception {
        if (!FORMAT.equals(archive.getString("format"))) throw new IllegalArgumentException("Formato de backup desconhecido.");
        JSONObject payload = archive.getJSONObject("payload");
        if (!archive.getString("checksum").equals(DiagnosticCaseJson.hash(payload))) throw new IllegalArgumentException("Backup alterado ou incompleto.");
        JSONArray cases = payload.getJSONArray("cases"), journal = payload.getJSONArray("journal");
        if (cases.length() + journal.length() > 10000) throw new IllegalArgumentException("Backup com registros demais.");
        String name = "backup-validation-" + java.util.UUID.randomUUID();
        SharedPreferences scratch = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        int profiles;
        try {
            if (!scratch.edit().putString(VehicleRepository.KEY, payload.getString("garage"))
                    .putString(DiagnosticHistoryStore.KEY, payload.getString("history")).commit()) throw new IllegalStateException("Falha na validação.");
            profiles = new VehicleRepository(scratch).records().size();
            new DiagnosticHistoryStore(scratch);
        } finally { context.deleteSharedPreferences(name); }
        // Existing domain validators also check revision continuity and immutable measurement facts.
        try (DiagnosticCaseStore stage = new DiagnosticCaseStore(context, (String) null)) {
            ArrayList<DiagnosticCase> sorted = new ArrayList<>();
            for (int i = 0; i < cases.length(); i++) sorted.add(DiagnosticCaseJson.validateDocument(cases.getJSONObject(i)));
            sorted.sort(Comparator.comparing((DiagnosticCase c) -> c.id).thenComparingInt(c -> c.revision));
            for (DiagnosticCase item : sorted) stage.save(item);
        }
        try (JournalStore stage = new JournalStore(context, (String) null)) {
            ArrayList<JournalEntry> sorted = new ArrayList<>();
            for (int i = 0; i < journal.length(); i++) sorted.add(JournalJson.decode(journal.getJSONObject(i)));
            sorted.sort(Comparator.comparing((JournalEntry e) -> e.id).thenComparingInt(e -> e.revision));
            for (JournalEntry item : sorted) stage.save(item);
        }
        return profiles + " perfis · " + cases.length() + " revisões de investigações · " + journal.length() + " revisões do diário";
    }

    public void restore(JSONObject archive) throws Exception {
        synchronized (TRANSFER_LOCK) { restoreInternal(archive); }
    }

    private void restoreInternal(JSONObject archive) throws Exception {
        validate(archive); // No existing data is changed until the whole file has been checked.
        JSONObject before = snapshot();
        validate(before);
        writeAtomic(recoveryFile(), before);
        try {
            apply(archive.getJSONObject("payload"));
            // Retain an undo copy before clearing crash recovery.
            writeAtomic(new AtomicFile(new File(context.getFilesDir(), "backup-previous.json")), before);
            recoveryFile().delete();
        } catch (Exception failure) {
            try { apply(before.getJSONObject("payload")); recoveryFile().delete(); }
            catch (Exception rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }

    public boolean recoverInterruptedRestore() throws Exception {
        synchronized (TRANSFER_LOCK) { return recoverInternal(); }
    }

    private boolean recoverInternal() throws Exception {
        AtomicFile recovery = recoveryFile();
        if (!recovery.getBaseFile().exists() && !new File(recovery.getBaseFile().getPath() + ".bak").exists()) return false;
        JSONObject previous = read(recovery.openRead());
        if (!FORMAT.equals(previous.getString("format")) || !previous.getString("checksum").equals(DiagnosticCaseJson.hash(previous.getJSONObject("payload")))) {
            throw new IllegalStateException("Não foi possível recuperar a restauração interrompida.");
        }
        apply(previous.getJSONObject("payload"));
        recovery.delete();
        return true;
    }

    public boolean hasPendingRestore() {
        File file = recoveryFile().getBaseFile();
        return file.exists() || new File(file.getPath() + ".bak").exists();
    }

    public JSONObject previous() throws Exception {
        return read(new AtomicFile(new File(context.getFilesDir(), "backup-previous.json")).openRead());
    }

    private static void writeAtomic(AtomicFile file, JSONObject value) throws Exception {
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(value.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(output);
        } catch (Exception e) { if (output != null) file.failWrite(output); throw e; }
    }

    private void apply(JSONObject payload) throws Exception {
        try (JournalStore journal = new JournalStore(context); DiagnosticCaseStore cases = new DiagnosticCaseStore(context)) {
            journal.getWritableDatabase();
            SQLiteDatabase db = cases.getWritableDatabase();
            db.execSQL("ATTACH DATABASE ? AS journal_backup", new Object[]{context.getDatabasePath("vehicle-journal.db").getAbsolutePath()});
            try {
                db.beginTransaction();
                try {
                    db.delete("case_revisions", null, null);
                    db.delete("journal_backup.entries", null, null);
                    JSONArray documents = payload.getJSONArray("cases");
                    for (int i = 0; i < documents.length(); i++) {
                        JSONObject document = documents.getJSONObject(i);
                        DiagnosticCase item = DiagnosticCaseJson.validateDocument(document);
                        ContentValues values = new ContentValues();
                        values.put("case_id", item.id); values.put("revision", item.revision); values.put("vehicle_id", item.vehicle.id);
                        values.put("updated_at", item.revisedAtEpochMillis); values.put("payload", document.toString()); values.put("checksum", DiagnosticCaseJson.hash(document));
                        db.insertOrThrow("case_revisions", null, values);
                    }
                    JSONArray entries = payload.getJSONArray("journal");
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject document = entries.getJSONObject(i);
                        JournalEntry item = JournalJson.decode(document);
                        ContentValues values = new ContentValues();
                        values.put("id", item.id); values.put("revision", item.revision); values.put("vehicle_id", item.vehicleId);
                        values.put("payload", document.toString()); values.put("checksum", DiagnosticCaseJson.hash(document));
                        db.insertOrThrow("journal_backup.entries", null, values);
                    }
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            } finally { db.execSQL("DETACH DATABASE journal_backup"); }
        }
        if (!preferences.edit().putString(VehicleRepository.KEY, payload.getString("garage"))
                .putString(DiagnosticHistoryStore.KEY, payload.getString("history")).commit()) {
            throw new IllegalStateException("Não foi possível restaurar os perfis.");
        }
    }
}
