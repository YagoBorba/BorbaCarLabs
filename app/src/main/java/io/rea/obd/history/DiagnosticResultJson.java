package io.rea.obd.history;

import io.rea.obd.diagnostics.DiagnosticCase;
import io.rea.obd.diagnostics.DiagnosticResult;
import io.rea.obd.diagnostics.RuleDiagnosticEngine;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.KnowledgeRepository;
import io.rea.obd.knowledge.SourceReference;
import java.util.Collection;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DiagnosticResultJson {
    private DiagnosticResultJson() {
    }

    public static JSONObject encode(DiagnosticCase diagnosticCase) throws JSONException {
        RuleDiagnosticEngine ruleDiagnosticEngine = new RuleDiagnosticEngine();
        DiagnosticResult diagnosticResult = ruleDiagnosticEngine.analyze(diagnosticCase);
        JSONArray definitions = new JSONArray();
        JSONArray checks = new JSONArray();
        JSONArray definitionSources = new JSONArray();
        JSONArray protocolSources = new JSONArray();
        for (DiagnosticResult.DefinitionMatch definitionMatch : diagnosticResult.definitions) {
            DtcDefinition dtcDefinition = definitionMatch.definition;
            definitions.put(new JSONObject().put("id", dtcDefinition.id).put("code", dtcDefinition.code).put("meaning", dtcDefinition.meaning).put("scope", DtcDefinition.SCOPE).put("content_policy", DtcDefinition.CONTENT_POLICY).put("review", "MEANING_CHECKED_NOT_INDEPENDENT_MECHANICAL_REVIEW").put("repair_applicability", "NOT_ESTABLISHED").put("source_section", dtcDefinition.sourceSection).put("source_refs", new JSONArray((Collection) dtcDefinition.sourceIds)).put("evidence_refs", new JSONArray((Collection) definitionMatch.evidenceRefs)));
        }
        for (DiagnosticResult.Check check : diagnosticResult.checks) {
            checks.put(new JSONObject().put("id", check.id).put("kind", check.kind.name()).put("text", check.text).put("evidence_refs", new JSONArray((Collection) check.evidenceRefs)).put("source_refs", new JSONArray((Collection) check.sourceRefs)));
        }
        Iterator<SourceReference> it = diagnosticResult.sources.iterator();
        while (it.hasNext()) {
            definitionSources.put(source(it.next()));
        }
        if (diagnosticCase.supplement != null) {
            Iterator<SourceReference> iterator = KnowledgeRepository.all().iterator();
            while (iterator.hasNext()) {
                protocolSources.put(source(iterator.next()));
            }
        }
        return new JSONObject().put("analyzer_type", "RULE_ENGINE").put("rules_version", diagnosticResult.rulesVersion).put("catalog_version", diagnosticResult.catalogVersion).put("status", diagnosticResult.status).put("case_id", diagnosticCase.id).put("revision", diagnosticCase.revision).put("observed", new JSONArray((Collection) diagnosticResult.coverage.observed)).put("evidence_refs", new JSONArray((Collection) diagnosticResult.coverage.evidenceIds)).put("definitions", definitions).put("missing_information", new JSONArray((Collection) diagnosticResult.missing)).put("hypotheses", new JSONArray()).put("checks", checks).put("coverage_checks", new JSONArray((Collection) diagnosticResult.coverage.checks)).put("limitations", new JSONArray((Collection) diagnosticResult.limitations)).put("definition_sources", definitionSources).put("sources_used", new JSONArray()).put("protocol_sources_version", KnowledgeRepository.VERSION).put("protocol_sources", protocolSources).put("report_lines", new JSONArray((Collection) ruleDiagnosticEngine.report(diagnosticCase, diagnosticResult)));
    }

    private static JSONObject source(SourceReference sourceReference) throws JSONException {
        return new JSONObject().put("id", sourceReference.id).put("title", sourceReference.title).put("publisher", sourceReference.publisher).put("version", sourceReference.version).put("location", sourceReference.location).put("section", sourceReference.section).put("accessed_on", sourceReference.accessedOn).put("scope", sourceReference.scope).put("review", sourceReference.review).put("distribution", sourceReference.distribution.name());
    }
}