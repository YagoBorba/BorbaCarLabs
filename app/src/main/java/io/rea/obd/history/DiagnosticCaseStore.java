package io.rea.obd.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import io.rea.obd.diagnostics.DiagnosticCase;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class DiagnosticCaseStore extends SQLiteOpenHelper {
    public DiagnosticCaseStore(Context context) {
        this(context, "diagnostic-cases.db");
    }

    public DiagnosticCaseStore(Context context, String text) {
        super(context.getApplicationContext(), text, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE case_revisions (case_id TEXT NOT NULL, revision INTEGER NOT NULL, vehicle_id TEXT NOT NULL, updated_at INTEGER NOT NULL, payload TEXT NOT NULL, checksum TEXT NOT NULL, PRIMARY KEY(case_id,revision))");
        sQLiteDatabase.execSQL("CREATE INDEX cases_vehicle ON case_revisions(vehicle_id,updated_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int value) {
        throw new SQLiteException("Versão de investigações não reconhecida; dados preservados");
    }

    public void save(DiagnosticCase diagnosticCase) throws Exception {
        JSONObject document = DiagnosticCaseJson.document(diagnosticCase);
        String string = document.toString();
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            JSONObject latest = latest(writableDatabase, diagnosticCase.id);
            if (latest == null) {
                if (diagnosticCase.revision != 1) {
                    throw new IllegalArgumentException("Primeira revisão deve ser 1");
                }
            } else {
                DiagnosticCase diagnosticCase2 = DiagnosticCaseJson.validateDocument(latest);
                if (diagnosticCase.revision != diagnosticCase2.revision + 1) {
                    throw new IllegalStateException("Este caso já recebeu outra revisão. Abra a revisão mais recente.");
                }
                if (diagnosticCase.revisedAtEpochMillis < diagnosticCase2.revisedAtEpochMillis || !DiagnosticCaseJson.measurementFingerprint(diagnosticCase2).equals(DiagnosticCaseJson.measurementFingerprint(diagnosticCase))) {
                    throw new IllegalArgumentException("Uma revisão de sintomas não pode alterar os fatos originais");
                }
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("case_id", diagnosticCase.id);
            contentValues.put("revision", Integer.valueOf(diagnosticCase.revision));
            contentValues.put("vehicle_id", diagnosticCase.vehicle.id);
            contentValues.put("updated_at", Long.valueOf(diagnosticCase.revisedAtEpochMillis));
            contentValues.put("payload", string);
            contentValues.put("checksum", DiagnosticCaseJson.hash(document));
            writableDatabase.insertOrThrow("case_revisions", null, contentValues);
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Throwable throwable) {
            writableDatabase.endTransaction();
            throw throwable;
        }
    }

    public JSONObject load(String id, int i) throws Exception {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM case_revisions WHERE case_id=? AND revision=?", new String[]{id, Integer.toString(i)});
        try {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("Caso não encontrado");
            }
            JSONObject checked = checked(cursor);
            if (cursor != null) {
                cursor.close();
            }
            return checked;
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

    private JSONObject latest(SQLiteDatabase sQLiteDatabase, String text) throws Exception {
        Cursor cursor = sQLiteDatabase.rawQuery("SELECT * FROM case_revisions WHERE case_id=? ORDER BY revision DESC LIMIT 1", new String[]{text});
        try {
            JSONObject checked = cursor.moveToFirst() ? checked(cursor) : null;
            if (cursor != null) {
                cursor.close();
            }
            return checked;
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

    public List<JSONObject> latestAll() throws Exception {
        ArrayList items = new ArrayList();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT r.* FROM case_revisions r JOIN (SELECT case_id,MAX(revision) rev FROM case_revisions GROUP BY case_id) last ON r.case_id=last.case_id AND r.revision=last.rev ORDER BY r.updated_at DESC,r.case_id", null);
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

    public List<JSONObject> revisions(String text) throws Exception {
        ArrayList items = new ArrayList();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM case_revisions WHERE case_id=? ORDER BY revision DESC", new String[]{text});
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

    public void delete(String id, String id2) throws Exception {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            JSONObject latest = latest(writableDatabase, id);
            if (latest == null || !DiagnosticCaseJson.validateDocument(latest).vehicle.id.equals(id2)) {
                throw new IllegalArgumentException("Caso pertence a outro veículo");
            }
            writableDatabase.delete("case_revisions", "case_id=? AND vehicle_id=?", new String[]{id, id2});
            writableDatabase.setTransactionSuccessful();
            writableDatabase.endTransaction();
        } catch (Throwable throwable) {
            writableDatabase.endTransaction();
            throw throwable;
        }
    }

    private JSONObject checked(Cursor cursor) throws Exception {
        JSONObject json = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow("payload")));
        if (!DiagnosticCaseJson.hash(json).equals(cursor.getString(cursor.getColumnIndexOrThrow("checksum")))) {
            throw new JSONException("Investigação corrompida; registro original preservado");
        }
        DiagnosticCase diagnosticCase = DiagnosticCaseJson.validateDocument(json);
        if (diagnosticCase.id.equals(cursor.getString(cursor.getColumnIndexOrThrow("case_id"))) && diagnosticCase.revision == cursor.getInt(cursor.getColumnIndexOrThrow("revision")) && diagnosticCase.vehicle.id.equals(cursor.getString(cursor.getColumnIndexOrThrow("vehicle_id"))) && diagnosticCase.revisedAtEpochMillis == cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))) {
            return json;
        }
        throw new JSONException("Índice de investigação inconsistente");
    }
}