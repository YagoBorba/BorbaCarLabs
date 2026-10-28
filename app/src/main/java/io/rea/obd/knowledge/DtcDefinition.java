package io.rea.obd.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DtcDefinition {
    public static final String CONTENT_POLICY = "ORIGINAL_BRIEF_FACT_SUMMARY";
    public static final String SCOPE = "GENERIC_CODE_MEANING_ONLY";
    public final String code;
    public final String id;
    public final String meaning;
    public final String question;
    public final List<String> sourceIds;
    public final String sourceSection;

    public DtcDefinition(String code, String meaning, String question, String sourceSection, List<String> list) {
        this("generic-dtc-1", code, meaning, question, sourceSection, list);
    }

    public DtcDefinition(String catalog, String code, String meaning, String question, String sourceSection, List<String> list) {
        if (code == null || !code.matches("P0[0-9A-F]{3}") || meaning == null || meaning.isEmpty() || question == null || question.isEmpty() || sourceSection == null || sourceSection.isEmpty() || list == null || list.isEmpty() || list.contains(null)) {
            throw new IllegalArgumentException("Definição incompleta");
        }
        this.id = catalog + ":" + code;
        this.code = code;
        this.meaning = meaning;
        this.question = question;
        this.sourceSection = sourceSection;
        this.sourceIds = Collections.unmodifiableList(new ArrayList(list));
    }

    public String display() {
        return this.code + " · " + this.meaning + "\nSignificado genérico do código. Não identifica uma peça defeituosa nem confirma o procedimento aplicável ao veículo.\nPergunta de contexto: " + this.question;
    }
}