package io.rea.obd.journal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import io.rea.obd.history.DiagnosticCaseJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import org.json.JSONException;
import org.json.JSONObject;

public final class JournalStore extends SQLiteOpenHelper {
    public JournalStore(Context context) {
        this(context, "vehicle-journal.db");
    }

    public JournalStore(Context context, String text) {
        super(context.getApplicationContext(), text, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE entries (id TEXT NOT NULL, revision INTEGER NOT NULL, vehicle_id TEXT NOT NULL, payload TEXT NOT NULL, checksum TEXT NOT NULL, PRIMARY KEY(id,revision))");
        sQLiteDatabase.execSQL("CREATE INDEX entries_vehicle ON entries(vehicle_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int value) {
        throw new SQLiteException("Diário de versão desconhecida; dados preservados.");
    }

    public void save(JournalEntry journalEntry) throws Exception {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            Cursor cursor = writableDatabase.rawQuery("SELECT * FROM entries WHERE id=? ORDER BY revision DESC LIMIT 1", new String[]{journalEntry.id});
            try {
                JournalEntry checked = cursor.moveToFirst() ? checked(cursor) : null;
                if (cursor != null) {
                    cursor.close();
                }
                if (checked == null) {
                    if (journalEntry.revision != 1) {
                        throw new IllegalArgumentException("Registro inicial precisa de revisão 1.");
                    }
                } else if (journalEntry.revision != checked.revision + 1 || !journalEntry.vehicleId.equals(checked.vehicleId) || !journalEntry.vehicleName.equals(checked.vehicleName) || journalEntry.kind != checked.kind || journalEntry.createdAt != checked.createdAt || journalEntry.revisedAt < checked.revisedAt) {
                    throw new IllegalStateException("Registro já salvo ou alterado. Reabra a versão atual antes de corrigir.");
                }
                JSONObject json = JournalJson.encode(journalEntry);
                ContentValues contentValues = new ContentValues();
                contentValues.put("id", journalEntry.id);
                contentValues.put("revision", Integer.valueOf(journalEntry.revision));
                contentValues.put("vehicle_id", journalEntry.vehicleId);
                contentValues.put("payload", json.toString());
                contentValues.put("checksum", DiagnosticCaseJson.hash(json));
                writableDatabase.insertOrThrow("entries", null, contentValues);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
            } catch (Throwable throwable) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        } catch (Throwable throwable) {
            writableDatabase.endTransaction();
            throw throwable;
        }
    }

    public List<JournalEntry> latest() throws Exception {
        ArrayList items = new ArrayList();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT e.* FROM entries e JOIN (SELECT id,MAX(revision) rev FROM entries GROUP BY id) last ON e.id=last.id AND e.revision=last.rev", null);
        while (cursor.moveToNext()) {
            try {
                items.add(checked(cursor));
            } catch (Throwable throwable) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        items.sort(Comparator.comparingLong((obj) -> ((JournalEntry) obj).occurredAt).reversed().thenComparing((obj) -> ((JournalEntry) obj).id));
        return Collections.unmodifiableList(items);
    }

    public List<JournalEntry> revisions(String text) throws Exception {
        ArrayList items = new ArrayList();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM entries WHERE id=? ORDER BY revision DESC", new String[]{text});
        while (cursor.moveToNext()) {
            try {
                items.add(checked(cursor));
            } catch (Throwable throwable) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return items;
    }

    private JournalEntry checked(Cursor cursor) throws Exception {
        JSONObject json = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow("payload")));
        if (!DiagnosticCaseJson.hash(json).equals(cursor.getString(cursor.getColumnIndexOrThrow("checksum")))) {
            throw new JSONException("Registro do diário corrompido; original preservado.");
        }
        JournalEntry journalEntry = JournalJson.decode(json);
        if (journalEntry.id.equals(cursor.getString(cursor.getColumnIndexOrThrow("id"))) && journalEntry.revision == cursor.getInt(cursor.getColumnIndexOrThrow("revision")) && journalEntry.vehicleId.equals(cursor.getString(cursor.getColumnIndexOrThrow("vehicle_id")))) {
            return journalEntry;
        }
        throw new JSONException("Índice do diário inconsistente.");
    }
}