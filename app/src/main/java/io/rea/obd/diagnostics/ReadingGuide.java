package io.rea.obd.diagnostics;

import io.rea.obd.knowledge.DtcDefinition;
import io.rea.obd.knowledge.DtcReference;
import io.rea.obd.knowledge.DtcStructure;
import io.rea.obd.model.DataOrigin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReadingGuide {
    public static final String NO_MEANING = "Sem descrição no catálogo local. O código continua registrado; nenhuma causa foi presumida.";
    public static final String SCOPE = "Significados genéricos do catálogo do app. Não identificam a peça defeituosa nem substituem o teste na oficina.";

    private ReadingGuide() {
    }

    public static final class Code {
        public final String code;
        public final Set<DtcObservation.Category> categories;
        public final String structure;
        public final String meaning;
        public final String question;

        Code(String code, Set<DtcObservation.Category> categories) {
            DtcDefinition definition = DtcReference.find(code);
            this.code = code;
            this.categories = Collections.unmodifiableSet(categories);
            this.structure = DtcStructure.describe(code);
            this.meaning = definition == null ? null : definition.meaning;
            this.question = definition == null ? null : definition.question;
        }

        public String statuses() {
            List<String> names = new ArrayList<>();
            for (DtcObservation.Category category : this.categories) {
                names.add(DtcFormatter.category(category));
            }
            String last = names.remove(names.size() - 1);
            return names.isEmpty() ? last : String.join(", ", names) + " e " + last;
        }

        public List<String> evidence() {
            List<String> lines = new ArrayList<>();
            for (DtcObservation.Category category : this.categories) {
                lines.add(ReadingGuide.evidence(category));
            }
            return lines;
        }
    }

    public static String evidence(DtcObservation.Category category) {
        switch (category) {
            case STORED:
                return "Armazenado (serviço 03): a ECU confirmou a falha e gravou o código.";
            case PENDING:
                return "Pendente (serviço 07): a falha apareceu, mas ainda não foi confirmada em ciclos seguidos.";
            default:
                return "Permanente (serviço 0A): só sai depois que o próprio carro confirmar o reparo; apagar não remove.";
        }
    }

    public static List<Code> codes(DtcReadResult read) {
        Map<String, Set<DtcObservation.Category>> found = new LinkedHashMap<>();
        for (DtcObservation observation : read.observations()) {
            found.computeIfAbsent(observation.code, (code) -> EnumSet.noneOf(DtcObservation.Category.class)).add(observation.category);
        }
        List<Code> codes = new ArrayList<>();
        for (Map.Entry<String, Set<DtcObservation.Category>> entry : found.entrySet()) {
            codes.add(new Code(entry.getKey(), entry.getValue()));
        }
        return codes;
    }

    public static List<String> context(DtcReadResult read) {
        List<String> lines = new ArrayList<>();
        if (read.origin == DataOrigin.SIMULATED) {
            lines.add("Leitura de demonstração: os códigos são fictícios.");
        }
        lines.add(mil(read));
        if (read.incomplete()) {
            lines.add("Algumas consultas não terminaram; pode haver outros códigos. Leia novamente antes de concluir.");
        }
        lines.add("Cobre o diagnóstico de emissões (OBD2). Módulos como ABS e airbag não são lidos.");
        return lines;
    }

    public static List<String> nextSteps() {
        List<String> lines = new ArrayList<>();
        lines.add("Guarde ou compartilhe o resumo antes de apagar qualquer código.");
        lines.add("Se a luz de injeção piscar ou o motor falhar com força, evite rodar e procure uma oficina.");
        lines.add("Anote quando o sintoma aparece (frio, quente, acelerando, em marcha lenta).");
        return lines;
    }

    static String mil(DtcReadResult read) {
        if (read.mil.observations.isEmpty()) {
            return "Luz de injeção (MIL): " + DtcFormatter.outcome(read.mil.status.outcome);
        }
        boolean on = false;
        int reported = 0;
        for (DtcReadResult.MilObservation observation : read.mil.observations) {
            on |= observation.illuminated;
            reported = Math.max(reported, observation.reportedDtcCount);
        }
        return "Luz de injeção (MIL): " + (on ? "acesa" : "apagada") + " · " + reported + (reported == 1 ? " falha indicada" : " falhas indicadas");
    }

    public static List<String> workshopSummary(DtcReadResult read, String vehicle, String adapter, SupplementalReadResult supplement) {
        List<String> lines = new ArrayList<>();
        lines.add("Resumo para oficina · BorbaCar Labs");
        if (read.origin == DataOrigin.SIMULATED) {
            lines.add("SIMULAÇÃO · dados fictícios");
        }
        lines.add("Leitura: " + Instant.ofEpochMilli(read.completedAtEpochMillis));
        lines.add("Veículo: " + (vehicle == null || vehicle.isEmpty() ? "não associado" : vehicle));
        if (adapter != null && !adapter.isEmpty()) {
            lines.add("Adaptador: " + adapter);
        }
        lines.add(mil(read));
        if (read.incomplete()) {
            lines.add("LEITURA INCOMPLETA · pode haver outros códigos");
        }
        List<Code> codes = codes(read);
        lines.add("");
        lines.add("Códigos encontrados");
        if (codes.isEmpty()) {
            lines.add("Nenhum código informado pela ECU.");
        }
        for (Code code : codes) {
            lines.add(code.code + " · " + code.statuses());
            lines.add("  " + (code.meaning == null ? NO_MEANING : code.meaning));
            if (code.structure != null) {
                lines.add("  " + code.structure);
            }
            io.rea.obd.knowledge.DtcGuidance guidance = io.rea.obd.knowledge.DtcGuidance.of(code.code);
            lines.add("  Orientação geral: " + guidance.headline());
            if (!guidance.causes.isEmpty()) {
                lines.add("  Causas comuns, não confirmadas: " + String.join(" · ", guidance.causes));
            }
            if (code.question != null) {
                lines.add("  Pergunta de contexto: " + code.question);
            }
        }
        lines.add("");
        lines.add("Consultas da leitura");
        for (String line : DtcFormatter.summary(read).split("\n")) {
            if (!line.startsWith("SIMULAÇÃO") && !line.startsWith("MIL")) {
                lines.add(line);
            }
        }
        if (supplement != null) {
            lines.add("");
            lines.add("Prontidão e quadro congelado");
            lines.addAll(SupplementalFormatter.lines(supplement));
        }
        lines.add("");
        lines.add("Observações");
        lines.add(SCOPE);
        lines.add("Resumo gerado no aparelho, sem internet.");
        return lines;
    }
}
