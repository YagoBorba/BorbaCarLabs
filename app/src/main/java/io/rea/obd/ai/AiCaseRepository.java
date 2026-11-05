package io.rea.obd.ai;

import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.history.DiagnosticCaseJson;
import io.rea.obd.history.DiagnosticCaseStore;
import org.json.JSONObject;

/** Keeps the one-tap entry point tied to a durable, immutable capture. */
public final class AiCaseRepository {
    private AiCaseRepository() {}

    public static DiagnosticCase findOrSave(DiagnosticCaseStore store, DiagnosticCase candidate) throws Exception {
        String fingerprint = captureKey(candidate);
        for (JSONObject document : store.latestAll()) {
            DiagnosticCase saved = DiagnosticCaseJson.validateDocument(document);
            if (fingerprint.equals(captureKey(saved))) return saved;
        }
        store.save(candidate);
        return candidate;
    }

    private static String captureKey(DiagnosticCase kase) throws Exception {
        JSONObject input = DiagnosticCaseJson.encode(kase);
        for (String key : new String[]{"case_id", "revision", "created_at_ms", "revised_at_ms", "symptoms"}) input.remove(key);
        return DiagnosticCaseJson.hash(input);
    }
}
