package io.rea.obd.diagnostics;

import io.rea.obd.SensorCatalog;
import io.rea.obd.knowledge.KnowledgeRepository;
import io.rea.obd.knowledge.SourceReference;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public final class CaseReview {
    public static final String VERSION = "coverage-2";
    public final List<String> checks;
    public final List<String> evidenceIds;
    public final List<String> limitations;
    public final List<String> missing;
    public final List<String> observed;
    public final String version;

    public CaseReview(DiagnosticCase diagnosticCase) {
        this(diagnosticCase, true);
    }

    public CaseReview(DiagnosticCase diagnosticCase, boolean flag) {
        String string;
        this.version = diagnosticCase.schemaVersion == 1 ? "coverage-1" : VERSION;
        ArrayList items = new ArrayList();
        ArrayList items2 = new ArrayList();
        ArrayList items3 = new ArrayList();
        ArrayList items4 = new ArrayList();
        ArrayList items5 = new ArrayList();
        items.add(DtcFormatter.summary(diagnosticCase.dtc));
        for (DtcObservation dtcObservation : diagnosticCase.dtc.observations()) {
            items5.add(dtcObservation.id);
            items.add(dtcObservation.code + " · " + DtcFormatter.category(dtcObservation.category) + " · observado em " + Instant.ofEpochMilli(dtcObservation.observedAtEpochMillis) + " · evidência " + dtcObservation.id);
        }
        Iterator<DtcReadResult.MilObservation> it = diagnosticCase.dtc.mil.observations.iterator();
        while (it.hasNext()) {
            items5.add(it.next().id);
        }
        if (diagnosticCase.dtc.incomplete()) {
            items2.add("Uma ou mais consultas OBD não tiveram resposta positiva completa.");
            items3.add("Repetir a leitura quando houver comunicação estável; preservar esta captura parcial.");
        }
        if (diagnosticCase.dtc.observations().isEmpty()) {
            items4.add("Nenhum código observado não comprova a saúde de todos os sistemas.");
        } else {
            if (flag) {
                items2.add("Definições técnicas revisadas e fontes aplicáveis para os códigos observados.");
            }
            items3.add("Consultar documentação aplicável antes de atribuir causas ou indicar peças.");
        }
        if (diagnosticCase.symptoms.isEmpty()) {
            items2.add("Sintomas e condições ainda não informados.");
            items3.add("Informar o que foi percebido, quando ocorre e com que frequência.");
        }
        if (diagnosticCase.telemetry.isEmpty()) {
            items2.add("Telemetria da mesma sessão não foi anexada.");
        }
        ArrayList items6 = new ArrayList(diagnosticCase.telemetry);
        items6.sort(Comparator.comparingInt((obj) -> ((DiagnosticCase.Sample) obj).measurement.pid));
        Iterator iterator = items6.iterator();
        while (iterator.hasNext()) {
            DiagnosticCase.Sample sample = (DiagnosticCase.Sample) iterator.next();
            TelemetrySample telemetrySample = sample.measurement;
            SensorCatalog.Sensor sensor = SensorCatalog.get(telemetrySample.pid);
            String name = sensor == null ? String.format(Locale.ROOT, "PID %02X", Integer.valueOf(telemetrySample.pid)) : sensor.name;
            String name2 = telemetrySample.status == TelemetrySample.Status.VALID ? diagnosticCase.historical(sample) ? "antiga em relação à leitura de DTCs" : "válida na captura" : telemetrySample.status.name();
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append(": ");
            if (telemetrySample.value == null) {
                string = "sem valor";
            } else {
                StringBuilder builder2 = new StringBuilder();
                builder2.append(Double.toString(telemetrySample.value.doubleValue()));
                builder2.append(sensor == null ? "" : " " + sensor.unit);
                string = builder2.toString();
            }
            builder.append(string);
            builder.append(" · ");
            builder.append(name2);
            builder.append(" · ");
            ArrayList items7 = items3;
            ArrayList items8 = items4;
            builder.append(Instant.ofEpochMilli(telemetrySample.epochMillis));
            items.add(builder.toString());
            items5.add("telemetry:" + Integer.toString(telemetrySample.pid));
            if (telemetrySample.status != TelemetrySample.Status.VALID || diagnosticCase.historical(sample)) {
                items2.add(name + ": dado ausente ou inadequado para representar o instante da leitura de DTCs.");
            }
            iterator = iterator;
            items3 = items7;
            items4 = items8;
        }
        ArrayList items9 = items3;
        ArrayList arrayList10 = items4;
        if (diagnosticCase.schemaVersion == 1) {
            items2.add("Prontidão dos monitores e quadro congelado não estão anexados em formato estruturado.");
            items9.add("Se necessário, consultar prontidão e quadro congelado nas ferramentas de leitura; os resultados ainda são separados deste caso.");
        } else if (diagnosticCase.supplement == null) {
            items2.add("Prontidão e quadro congelado não foram coletados para este caso.");
            items9.add("Para uma nova investigação, usar Ler dados complementares antes de salvar o caso, na mesma sessão e perfil.");
        } else {
            items.addAll(SupplementalFormatter.lines(diagnosticCase.supplement));
            if (diagnosticCase.supplement.incomplete()) {
                items2.add("Complemento incompleto: conferir os estados de prontidão e de cada PID do quadro.");
            }
            boolean flag2 = false;
            int i = 0;
            for (SupplementalReadResult.ReadinessObservation readinessObservation : diagnosticCase.supplement.readiness.observations) {
                items5.add(readinessObservation.id);
                Iterator<SupplementalReadResult.Monitor> iterator2 = readinessObservation.bits.monitors.iterator();
                while (iterator2.hasNext()) {
                    if (iterator2.next().state == SupplementalReadResult.MonitorState.INCOMPLETE) {
                        i++;
                    }
                }
            }
            for (SupplementalReadResult.ReadinessObservation observation : diagnosticCase.supplement.readiness.observations) {
                Iterator<DtcReadResult.MilObservation> iterator3 = diagnosticCase.dtc.mil.observations.iterator();
                while (iterator3.hasNext()) {
                    if (observation.bits.milIlluminated() != iterator3.next().illuminated) {
                        flag2 = true;
                    }
                }
            }
            if (flag2) {
                items2.add("Indicações MIL divergentes entre consultas. Horários e ECUs podem diferir; não se infere uma causa mecânica dessa divergência.");
            }
            if (i > 0) {
                items2.add(i + " indicação(ões) de monitor não concluído nas respostas de prontidão.");
                items9.add("Consultar as condições de conclusão dos monitores na documentação aplicável; este resultado não é aprovação de inspeção nem confirmação de peça defeituosa.");
            }
            Iterator<SupplementalReadResult.FramePid> iterator4 = diagnosticCase.supplement.frame.iterator();
            while (iterator4.hasNext()) {
                Iterator<SupplementalReadResult.FrameValue> iterator5 = iterator4.next().observations.iterator();
                while (iterator5.hasNext()) {
                    items5.add(iterator5.next().id);
                }
            }
            arrayList10.add("Consultas complementares e DTCs têm janelas próprias; não se presume simultaneidade nem correspondência entre ECUs das respostas.");
        }
        if (diagnosticCase.vehicle.engine.isEmpty() || diagnosticCase.vehicle.transmission.isEmpty() || diagnosticCase.vehicle.market.isEmpty()) {
            items2.add("Motor, câmbio ou mercado do veículo ainda não foram informados.");
        }
        arrayList10.add("Análise por regras de cobertura " + this.version + ". Sem modelo de IA e sem hipóteses mecânicas nesta versão.");
        arrayList10.add("Registro histórico. Abrir ou revisar sintomas não atualiza as medições.");
        arrayList10.add("Respostas OBD sem identificação de ECU; não representam cobertura de todos os módulos.");
        if (diagnosticCase.dtc.origin == DataOrigin.SIMULATED) {
            arrayList10.add("Dados simulados; nenhum veículo real foi consultado para esta captura.");
        }
        this.observed = immutable(items);
        this.missing = immutable(items2);
        this.checks = immutable(items9);
        this.limitations = immutable(arrayList10);
        this.evidenceIds = immutable(items5);
    }

    private static List<String> immutable(List<String> list) {
        return Collections.unmodifiableList(new ArrayList(list));
    }

    public List<String> report(DiagnosticCase diagnosticCase) {
        ArrayList items = new ArrayList(header(diagnosticCase));
        section(items, "Dados observados", this.observed);
        section(items, "Informações ausentes", this.missing);
        section(items, "Hipóteses", Collections.singletonList("Nenhuma hipótese técnica gerada: faltam fontes revisadas."));
        section(items, "Verificações", this.checks);
        section(items, "Fontes técnicas utilizadas", Collections.singletonList("Nenhuma fonte técnica de causas foi utilizada. Catálogo técnico ainda pendente."));
        if (diagnosticCase.schemaVersion == 2 && diagnosticCase.supplement != null) {
            ArrayList items2 = new ArrayList();
            Iterator<SourceReference> it = KnowledgeRepository.all().iterator();
            while (it.hasNext()) {
                items2.add(it.next().display());
            }
            section(items, "Referências de protocolo e codificação · protocol-links-1", items2);
        }
        section(items, "Limitações", this.limitations);
        return Collections.unmodifiableList(items);
    }

    public static List<String> header(DiagnosticCase diagnosticCase) {
        ArrayList items = new ArrayList();
        items.add("BorbaCar Labs · investigação offline");
        items.add("Caso: " + diagnosticCase.id + " · revisão " + diagnosticCase.revision);
        items.add("Veículo: " + diagnosticCase.vehicle.name + " · " + diagnosticCase.vehicle.description() + " · ID " + diagnosticCase.vehicle.id);
        items.add("Motor: " + known(diagnosticCase.vehicle.engine) + " · Câmbio: " + known(diagnosticCase.vehicle.transmission) + " · Mercado: " + known(diagnosticCase.vehicle.market));
        items.add("Origem: " + diagnosticCase.dtc.origin + " · Sessão: " + diagnosticCase.dtc.sessionId + " · Adaptador: " + known(diagnosticCase.adapter));
        StringBuilder builder = new StringBuilder("Coleta DTC: ");
        builder.append(Instant.ofEpochMilli(diagnosticCase.dtc.startedAtEpochMillis));
        builder.append(" a ");
        builder.append(Instant.ofEpochMilli(diagnosticCase.dtc.completedAtEpochMillis));
        items.add(builder.toString());
        items.add("Caso criado: " + Instant.ofEpochMilli(diagnosticCase.createdAtEpochMillis) + " · Revisão: " + Instant.ofEpochMilli(diagnosticCase.revisedAtEpochMillis));
        items.add("Sintomas declarados pelo usuário (não são medições)");
        StringBuilder builder2 = new StringBuilder("Descrição: ");
        builder2.append(known(diagnosticCase.symptoms.description));
        items.add(builder2.toString());
        items.add("Quando: " + known(diagnosticCase.symptoms.when));
        items.add("Frequência: " + known(diagnosticCase.symptoms.frequency));
        items.add("Condições: " + known(diagnosticCase.symptoms.conditions));
        return Collections.unmodifiableList(items);
    }

    private static String known(String market) {
        return market.isEmpty() ? "não informado" : market;
    }

    private static void section(List<String> list, String id, List<String> items) {
        list.add("");
        list.add(id);
        list.addAll(items);
    }
}