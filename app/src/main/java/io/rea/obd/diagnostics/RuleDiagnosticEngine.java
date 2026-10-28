package io.rea.obd.diagnostics;

import io.rea.obd.knowledge.DtcCatalog;
import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.KnowledgeRepository;
import io.rea.obd.knowledge.SourceReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class RuleDiagnosticEngine {
    public static final String VERSION = "investigation-1";

    public DiagnosticResult analyze(DiagnosticCase diagnosticCase) {
        if (diagnosticCase == null || diagnosticCase.schemaVersion != 3) {
            throw new IllegalArgumentException("O motor de investigação requer um caso esquema 3");
        }
        CaseReview caseReview = new CaseReview(diagnosticCase, false);
        ArrayList items = new ArrayList(caseReview.missing);
        ArrayList items2 = new ArrayList(caseReview.limitations);
        TreeMap<String, List> map = new TreeMap<>();
        for (DtcObservation dtcObservation : diagnosticCase.dtc.observations()) {
            ((List) map.computeIfAbsent(dtcObservation.code, (obj) -> RuleDiagnosticEngine.newGroup((String) obj))).add(dtcObservation.id);
        }
        ArrayList items3 = new ArrayList();
        ArrayList items4 = new ArrayList();
        TreeMap map2 = new TreeMap();
        for (Map.Entry entry : map.entrySet()) {
            DtcDefinition dtcDefinition = DtcCatalog.find((String) entry.getKey());
            List list = (List) entry.getValue();
            if (dtcDefinition == null) {
                items.add(((String) entry.getKey()) + ": definição ausente no catálogo generic-dtc-1; nenhuma causa presumida.");
                StringBuilder builder = new StringBuilder("unknown:");
                builder.append((String) entry.getKey());
                items4.add(new DiagnosticResult.Check(builder.toString(), "Obter definição e documentação aplicáveis para " + ((String) entry.getKey()) + " antes de propor uma causa.", DiagnosticResult.Check.Kind.CONSULT_APPLICABLE_DOCUMENTATION, list, Collections.emptyList()));
            } else {
                items3.add(new DiagnosticResult.DefinitionMatch(dtcDefinition, list));
                for (SourceReference sourceReference : DtcCatalog.sources(dtcDefinition)) {
                    map2.put(sourceReference.id, sourceReference);
                }
                items4.add(new DiagnosticResult.Check("context:" + dtcDefinition.code, dtcDefinition.question, DiagnosticResult.Check.Kind.ASK_USER, list, dtcDefinition.sourceIds));
                items4.add(new DiagnosticResult.Check("verify:" + dtcDefinition.code, "Conferir categorias, datas e demais códigos relacionados a " + dtcDefinition.code + ". Uma definição genérica não comprova uma peça defeituosa.", DiagnosticResult.Check.Kind.REVIEW_CAPTURE, list, dtcDefinition.sourceIds));
            }
        }
        if (!items3.isEmpty()) {
            items.add("Procedimento técnico aplicável ao motor, módulo e configuração do veículo; as referências do catálogo sustentam somente significados genéricos.");
            items2.add("Catálogo generic-dtc-1: anotações factuais breves e originais. Documentos de outras configurações não autorizam seus limites, causas ou reparos neste veículo.");
            items4.add(new DiagnosticResult.Check("applicability", "Confirmar motor, mercado, transmissão e identidade do módulo antes de escolher procedimentos técnicos.", DiagnosticResult.Check.Kind.CONSULT_APPLICABLE_DOCUMENTATION, new ArrayList(caseReview.evidenceIds), Collections.emptyList()));
        }
        if (map.isEmpty()) {
            items4.add(new DiagnosticResult.Check("no-code", "Registrar os sintomas e a cobertura das consultas; ausência de códigos observados não certifica todos os sistemas.", DiagnosticResult.Check.Kind.ASK_USER, Collections.emptyList(), Collections.emptyList()));
        }
        return new DiagnosticResult(diagnosticCase, items3, items4, items, items2, new ArrayList(map2.values()));
    }

    static List newGroup(String text) {
        return new ArrayList();
    }

    public List<String> report(DiagnosticCase diagnosticCase, DiagnosticResult diagnosticResult) {
        if (diagnosticCase != diagnosticResult.snapshot || !diagnosticCase.id.equals(diagnosticResult.caseId) || diagnosticCase.revision != diagnosticResult.revision) {
            throw new IllegalArgumentException("Resultado de outro snapshot, caso ou revisão");
        }
        ArrayList items = new ArrayList(CaseReview.header(diagnosticCase));
        items.add("Motor de regras: investigation-1 · catálogo generic-dtc-1 · evidência insuficiente para confirmar causa");
        section(items, "Dados observados", diagnosticResult.coverage.observed);
        ArrayList items2 = new ArrayList();
        for (DiagnosticResult.DefinitionMatch definitionMatch : diagnosticResult.definitions) {
            items2.add(definitionMatch.definition.display());
            items2.add("Evidências: " + String.join(", ", definitionMatch.evidenceRefs));
            items2.add("Fontes: " + String.join(", ", definitionMatch.definition.sourceIds) + " · " + definitionMatch.definition.sourceSection);
        }
        if (items2.isEmpty()) {
            items2.add("Nenhuma definição disponível para os códigos observados nesta captura.");
        }
        section(items, "Definições genéricas", items2);
        section(items, "Informações ausentes", diagnosticResult.missing);
        section(items, "Hipóteses causais", Collections.singletonList("Nenhuma causa mecânica confirmada ou hipótese de peça gerada por estas regras."));
        ArrayList items3 = new ArrayList(diagnosticResult.coverage.checks);
        for (DiagnosticResult.Check check : diagnosticResult.checks) {
            StringBuilder builder = new StringBuilder();
            builder.append(check.text);
            builder.append(" · regra ");
            builder.append(check.id);
            builder.append(check.evidenceRefs.isEmpty() ? "" : " · evidências " + String.join(", ", check.evidenceRefs));
            items3.add(builder.toString());
        }
        section(items, "Roteiro de investigação", items3);
        ArrayList items4 = new ArrayList();
        Iterator<SourceReference> it = diagnosticResult.sources.iterator();
        while (it.hasNext()) {
            items4.add(it.next().display());
        }
        if (items4.isEmpty()) {
            items4.add("Nenhuma fonte de definição foi utilizada.");
        }
        section(items, "Fontes dos significados · uso restrito", items4);
        if (diagnosticCase.supplement != null) {
            ArrayList items5 = new ArrayList();
            Iterator<SourceReference> iterator = KnowledgeRepository.all().iterator();
            while (iterator.hasNext()) {
                items5.add(iterator.next().display());
            }
            section(items, "Referências de protocolo · protocol-links-1", items5);
        }
        section(items, "Limitações", diagnosticResult.limitations);
        return Collections.unmodifiableList(items);
    }

    private static void section(List<String> list, String id, List<String> items) {
        list.add("");
        list.add(id);
        list.addAll(items);
    }
}