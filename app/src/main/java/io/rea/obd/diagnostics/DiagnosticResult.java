package io.rea.obd.diagnostics;

import io.rea.obd.knowledge.DtcCatalog;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.SourceReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticResult {
    public final String caseId;
    public final List<Check> checks;
    public final CaseReview coverage;
    public final List<DefinitionMatch> definitions;
    public final List<String> limitations;
    public final List<String> missing;
    public final int revision;
    final DiagnosticCase snapshot;
    public final List<SourceReference> sources;
    public final String rulesVersion = RuleDiagnosticEngine.VERSION;
    public final String catalogVersion = DtcCatalog.VERSION;
    public final String status = "INSUFFICIENT_EVIDENCE";

    public static final class DefinitionMatch {
        public final DtcDefinition definition;
        public final List<String> evidenceRefs;

        public DefinitionMatch(DtcDefinition dtcDefinition, List<String> list) {
            if (dtcDefinition == null || list == null || list.isEmpty()) {
                throw new IllegalArgumentException("Definição sem evidência");
            }
            this.definition = dtcDefinition;
            this.evidenceRefs = DiagnosticResult.freeze(list);
        }
    }

    public static final class Check {
        public final List<String> evidenceRefs;
        public final String id;
        public final Kind kind;
        public final List<String> sourceRefs;
        public final String text;

        public enum Kind {
            ASK_USER,
            REVIEW_CAPTURE,
            CONSULT_APPLICABLE_DOCUMENTATION
        }

        public Check(String id, String question, Kind kind, List<String> list, List<String> emptyList) {
            if (id == null || id.isEmpty() || question == null || question.isEmpty() || kind == null) {
                throw new IllegalArgumentException("Verificação inválida");
            }
            this.id = id;
            this.text = question;
            this.kind = kind;
            this.evidenceRefs = DiagnosticResult.freeze(list);
            this.sourceRefs = DiagnosticResult.freeze(emptyList);
        }
    }

    DiagnosticResult(DiagnosticCase diagnosticCase, List<DefinitionMatch> list, List<Check> items4, List<String> arrayList, List<String> items2, List<SourceReference> items) {
        this.snapshot = diagnosticCase;
        this.caseId = diagnosticCase.id;
        this.revision = diagnosticCase.revision;
        this.definitions = freeze(list);
        this.checks = freeze(items4);
        this.missing = freeze(arrayList);
        this.limitations = freeze(items2);
        this.sources = freeze(items);
        this.coverage = new CaseReview(diagnosticCase, false);
    }

    public static <T> List<T> freeze(List<T> list) {
        if (list == null || list.contains(null)) {
            throw new IllegalArgumentException("Lista inválida");
        }
        return Collections.unmodifiableList(new ArrayList(list));
    }
}