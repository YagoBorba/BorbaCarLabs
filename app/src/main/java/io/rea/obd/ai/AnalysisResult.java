package io.rea.obd.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AnalysisResult {
    private static final List<String> CONFIDENCE = Arrays.asList("baixa", "media", "alta");

    public static final class ContractException extends JSONException {
        ContractException(String message) {
            super(message);
        }
    }

    public static final class Hypothesis {
        public final String title;
        public final String reasoning;
        public final List<String> evidence;
        public final String confidence;
        public final String nextCheck;

        Hypothesis(String title, String reasoning, List<String> evidence, String confidence, String nextCheck) {
            this.title = title;
            this.reasoning = reasoning;
            this.evidence = Collections.unmodifiableList(evidence);
            this.confidence = confidence;
            this.nextCheck = nextCheck;
        }
    }

    public final String model;
    public final String summary;
    public final List<Hypothesis> hypotheses;
    public final List<String> missing;
    public final String safetyNote;

    private AnalysisResult(String model, String summary, List<Hypothesis> hypotheses, List<String> missing, String safetyNote) {
        this.model = model;
        this.summary = summary;
        this.hypotheses = Collections.unmodifiableList(hypotheses);
        this.missing = Collections.unmodifiableList(missing);
        this.safetyNote = safetyNote;
    }

    /**
     * Reads a stored or fresh reply. The answer itself must be present; extras the model got slightly
     * wrong are trimmed instead of throwing the whole reply away. Evidence that was never sent is
     * dropped, so the app never shows a reference it did not collect.
     */
    public static AnalysisResult parse(JSONObject response, Set<String> refs) throws JSONException {
        String model = text(response, "model", 80, false);
        JSONObject analysis = response.getJSONObject("analysis");
        String summary = text(analysis, "summary", 2000, false);

        JSONArray hypotheses2 = analysis.optJSONArray("hypotheses");
        List<Hypothesis> hypotheses = new ArrayList<>();
        for (int i = 0; hypotheses2 != null && i < hypotheses2.length() && hypotheses.size() < 5; i++) {
            JSONObject item = hypotheses2.optJSONObject(i);
            if (item == null || item.optString("title").trim().isEmpty()) {
                continue;
            }
            JSONArray cited = item.optJSONArray("evidence");
            List<String> evidence = new ArrayList<>();
            for (int i2 = 0; cited != null && i2 < cited.length(); i2++) {
                String ref = cited.optString(i2).trim();
                if (refs.contains(ref) && !evidence.contains(ref)) {
                    evidence.add(ref);
                }
            }
            String confidence = item.optString("confidence");
            if (!CONFIDENCE.contains(confidence)) {
                throw new ContractException("Confiança fora do contrato: " + confidence);
            }
            hypotheses.add(new Hypothesis(
                    clip(item.getString("title"), 160),
                    clip(item.optString("reasoning"), 1200),
                    evidence,
                    confidence,
                    clip(item.optString("next_check"), 600)));
        }

        JSONArray gaps = analysis.optJSONArray("missing_information");
        List<String> missing = new ArrayList<>();
        for (int i = 0; gaps != null && i < gaps.length() && missing.size() < 8; i++) {
            String gap = clip(gaps.optString(i), 300);
            if (!gap.isEmpty()) {
                missing.add(gap);
            }
        }
        return new AnalysisResult(model, summary, hypotheses, missing, clip(analysis.optString("safety_note"), 600));
    }

    /** True when there is more than the answer itself to show. */
    public boolean hasDetails() {
        return !this.hypotheses.isEmpty() || !this.missing.isEmpty();
    }

    /** Possible causes and missing information, without the answer or the safety note (shown separately in the chat). */
    public String details(Map<String, String> labels) {
        String full = new AnalysisResult(this.model, "", this.hypotheses, this.missing, "").display(labels);
        return full.trim();
    }

    private static String clip(String text, int max) {
        String clean = text == null ? "" : text.trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1).trim() + "…";
    }

    public String display(Map<String, String> labels) {
        StringBuilder out = new StringBuilder(this.summary);
        for (int i = 0; i < this.hypotheses.size(); i++) {
            Hypothesis hypothesis = this.hypotheses.get(i);
            out.append("\n\n").append(i + 1).append(". ").append(hypothesis.title)
                    .append(" · confiança ").append(hypothesis.confidence.equals("media") ? "média" : hypothesis.confidence)
                    .append("\n").append(hypothesis.reasoning);
            List<String> cited = new ArrayList<>();
            for (String ref : hypothesis.evidence) {
                cited.add(labels.containsKey(ref) ? labels.get(ref) : ref);
            }
            if (!cited.isEmpty()) {
                out.append("\nEvidências: ").append(String.join("; ", cited));
            }
            if (!hypothesis.nextCheck.isEmpty()) {
                out.append("\nPróxima verificação: ").append(hypothesis.nextCheck);
            }
        }
        if (!this.missing.isEmpty()) {
            out.append("\n\nInformações que faltam:");
            for (String gap : this.missing) {
                out.append("\n• ").append(gap);
            }
        }
        if (!this.safetyNote.isEmpty()) {
            out.append("\n\n").append(this.safetyNote);
        }
        return out.toString();
    }

    private static String text(JSONObject object, String key, int max, boolean allowEmpty) throws JSONException {
        String text = object.getString(key).trim();
        if (text.isEmpty() && !allowEmpty) {
            throw new ContractException("Campo vazio na resposta da IA: " + key);
        }
        return clip(text, max);
    }
}
