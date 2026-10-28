package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DtcFormatter {
    private DtcFormatter() {
    }

    public static String category(DtcObservation.Category category) {
        switch (category) {
            case STORED:
                return "Armazenado";
            case PENDING:
                return "Pendente";
            default:
                return "Permanente";
        }
    }

    public static String outcome(DtcReadResult.Outcome outcome) {
        switch (outcome) {
            case POSITIVE:
                return "resposta positiva";
            case NO_DATA:
                return "sem dados; ausência de falhas não confirmada";
            case NEGATIVE:
                return "resposta negativa da ECU";
            case INVALID:
                return "resposta inválida";
            case TIMEOUT:
                return "tempo esgotado; conexão encerrada";
            case DISCONNECTED:
                return "conexão encerrada";
            case NOT_READ:
                return "não consultado";
            default:
                return "resposta parcial";
        }
    }

    public static String summary(DtcReadResult dtcReadResult) {
        StringBuilder builder;
        String text;
        ArrayList items = new ArrayList();
        if (dtcReadResult.origin == DataOrigin.SIMULATED) {
            items.add("SIMULAÇÃO · dados fictícios");
        }
        if (dtcReadResult.incomplete()) {
            items.add("LEITURA INCOMPLETA · confira cada consulta abaixo");
        }
        if (dtcReadResult.mil.observations.isEmpty()) {
            items.add("MIL: " + outcome(dtcReadResult.mil.status.outcome) + negatives(dtcReadResult.mil.status));
        } else {
            for (int i = 0; i < dtcReadResult.mil.observations.size(); i++) {
                DtcReadResult.MilObservation milObservation = dtcReadResult.mil.observations.get(i);
                StringBuilder builder2 = new StringBuilder("MIL");
                builder2.append(dtcReadResult.mil.observations.size() > 1 ? " · resposta " + (i + 1) : "");
                builder2.append(": ");
                builder2.append(milObservation.illuminated ? "acesa" : "apagada");
                builder2.append(" · ");
                builder2.append(milObservation.reportedDtcCount);
                builder2.append(" falha(s) indicada(s)");
                items.add(builder2.toString());
            }
            if (dtcReadResult.mil.status.outcome != DtcReadResult.Outcome.POSITIVE) {
                items.add("MIL: " + outcome(dtcReadResult.mil.status.outcome) + negatives(dtcReadResult.mil.status));
            }
        }
        for (DtcReadResult.ServiceResult serviceResult : dtcReadResult.services) {
            if (serviceResult.status.outcome == DtcReadResult.Outcome.POSITIVE) {
                builder = new StringBuilder();
                builder.append(serviceResult.observations.size());
                text = " código(s) · resposta positiva";
            } else {
                builder = new StringBuilder();
                builder.append(outcome(serviceResult.status.outcome));
                text = serviceResult.observations.isEmpty() ? "" : " · " + serviceResult.observations.size() + " código(s) preservado(s)";
            }
            builder.append(text);
            items.add(category(serviceResult.category) + ": " + builder.toString() + negatives(serviceResult.status));
        }
        return String.join("\n", items);
    }

    private static String negatives(DtcReadResult.Status status) {
        StringBuilder builder = new StringBuilder();
        for (DtcReadResult.NegativeResponse negativeResponse : status.negativeResponses) {
            builder.append(String.format(Locale.ROOT, " · 7F %02X %02X", Integer.valueOf(negativeResponse.service), Integer.valueOf(negativeResponse.code)));
        }
        return builder.toString();
    }

    public static List<String> legacyCodeLines(DtcReadResult dtcReadResult) {
        ArrayList items = new ArrayList();
        for (DtcObservation dtcObservation : dtcReadResult.observations()) {
            StringBuilder builder = new StringBuilder();
            builder.append(dtcObservation.code);
            builder.append(" · ");
            builder.append(category(dtcObservation.category));
            builder.append(dtcReadResult.origin == DataOrigin.SIMULATED ? " · exemplo simulado" : "");
            items.add(builder.toString());
        }
        return items;
    }
}