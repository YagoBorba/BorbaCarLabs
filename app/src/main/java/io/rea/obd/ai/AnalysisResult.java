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

    public static AnalysisResult parse(JSONObject response, Set<String> refs) throws JSONException {
        String model = text(response, "model", 80, false);
        JSONObject analysis = response.getJSONObject("analysis");
        String summary = text(analysis, "summary", 1200, false);

        JSONArray hypotheses2 = analysis.getJSONArray("hypotheses");
        if (hypotheses2.length() > 5) {
            throw new ContractException("Resposta da IA com hipóteses demais");
        }
        List<Hypothesis> hypotheses = new ArrayList<>();
        for (int i = 0; i < hypotheses2.length(); i++) {
            JSONObject item = hypotheses2.getJSONObject(i);
            JSONArray cited = item.getJSONArray("evidence");
            if (cited.length() == 0) {
                throw new ContractException("Hipótese da IA sem evidência citada");
            }
            List<String> evidence = new ArrayList<>();
            for (int i2 = 0; i2 < cited.length(); i2++) {
                String ref = cited.getString(i2);
                if (!refs.contains(ref)) {
                    throw new ContractException("A IA citou uma evidência que não foi enviada: " + ref);
                }
                evidence.add(ref);
            }
            String confidence = item.getString("confidence");
            if (!CONFIDENCE.contains(confidence)) {
                throw new ContractException("Confiança fora do contrato: " + confidence);
            }
            hypotheses.add(new Hypothesis(
                    text(item, "title", 160, false),
                    text(item, "reasoning", 1200, false),
                    evidence,
                    confidence,
                    text(item, "next_check", 600, true)));
        }

        JSONArray gaps = analysis.getJSONArray("missing_information");
        if (gaps.length() > 8) {
            throw new ContractException("Resposta da IA com lacunas demais");
        }
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < gaps.length(); i++) {
            missing.add(bounded(gaps.getString(i), 300));
        }
        return new AnalysisResult(model, summary, hypotheses, missing, text(analysis, "safety_note", 600, true));
    }

    public String display(Map<String, String> labels) {
        StringBuilder out = new StringBuilder(this.summary);
        for (int i = 0; i < this.hypotheses.size(); i++) {
            Hypothesis hypothesis = this.hypotheses.get(i);
            out.append("\n\n").append(i + 1).append(". ").append(hypothesis.title)
                    .append(" · confiança ").append(hypothesis.confidence.equals("media") ? "média" : hypothesis.confidence)
                    .append("\n").append(hypothesis.reasoning)
                    .append("\nEvidências: ");
            List<String> cited = new ArrayList<>();
            for (String ref : hypothesis.evidence) {
                cited.add(labels.containsKey(ref) ? labels.get(ref) : ref);
            }
            out.append(String.join("; ", cited));
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
        return bounded(text, max);
    }

    private static String bounded(String text, int max) throws JSONException {
        if (text.length() > max) {
            throw new ContractException("Texto longo demais na resposta da IA");
        }
        return text;
    }
}
