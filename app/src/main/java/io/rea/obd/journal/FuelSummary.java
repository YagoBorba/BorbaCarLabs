package io.rea.obd.journal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

public final class FuelSummary {
    public final BigDecimal distance;
    public final List<String> entryRevisions;
    public final String explanation;
    public final BigDecimal kmPerLiter;
    public final BigDecimal liters;

    private FuelSummary(BigDecimal bigDecimal, BigDecimal distance, BigDecimal liters, String explanation, List<String> list) {
        this.kmPerLiter = bigDecimal;
        this.distance = distance;
        this.liters = liters;
        this.explanation = explanation;
        this.entryRevisions = Collections.unmodifiableList(new ArrayList(list));
    }

    private static FuelSummary pending(String explanation) {
        return new FuelSummary(null, null, null, explanation, Collections.emptyList());
    }

    public static FuelSummary latest(List<JournalEntry> list, String text) {
        ArrayList items = new ArrayList();
        HashSet seen = new HashSet();
        for (JournalEntry journalEntry : list) {
            if (journalEntry.kind == JournalEntry.Kind.FUEL && journalEntry.vehicleId.equals(text)) {
                if (!seen.add(journalEntry.id)) {
                    return pending("Há revisões duplicadas. Use apenas a revisão atual de cada registro.");
                }
                if (!journalEntry.voided) {
                    items.add(journalEntry);
                }
            }
        }
        items.sort(Comparator.comparingLong((obj) -> ((JournalEntry) obj).occurredAt).thenComparing((obj) -> ((JournalEntry) obj).id));
        if (items.size() < 2) {
            return pending("Registre dois fechamentos de tanque cheio e os abastecimentos entre eles.");
        }
        int size = items.size();
        int i = size - 1;
        if (!((JournalEntry) items.get(i)).fullTank) {
            return pending("Período aberto: o último abastecimento é parcial.");
        }
        JournalEntry journalEntry2 = (JournalEntry) items.get(i);
        if (!journalEntry2.completeInterval) {
            return pending("Confirme no fechamento se todos os abastecimentos do intervalo foram registrados.");
        }
        int value = size - 2;
        while (value >= 0 && !((JournalEntry) items.get(value)).fullTank) {
            value--;
        }
        if (value < 0) {
            return pending("Falta um tanque cheio anterior como referência.");
        }
        BigDecimal bigDecimal = BigDecimal.ZERO;
        HashSet seen2 = new HashSet();
        ArrayList items2 = new ArrayList();
        JournalEntry journalEntry3 = (JournalEntry) items.get(value);
        items2.add(journalEntry3.id + ":" + journalEntry3.revision);
        int value2 = value + 1;
        BigDecimal liters = bigDecimal;
        while (value2 <= i) {
            JournalEntry journalEntry4 = (JournalEntry) items.get(value2);
            int value3 = i;
            if (journalEntry4.occurredAt <= journalEntry3.occurredAt) {
                return pending("Horários iguais: revise a ordem dos abastecimentos.");
            }
            if (journalEntry4.odometer.compareTo(journalEntry3.odometer) < 0) {
                return pending("Hodômetro regressivo: revise os registros antes de calcular.");
            }
            liters = liters.add(journalEntry4.liters);
            seen2.add(journalEntry4.fuel);
            items2.add(journalEntry4.id + ":" + journalEntry4.revision);
            value2++;
            journalEntry3 = journalEntry4;
            i = value3;
        }
        BigDecimal subtract = journalEntry2.odometer.subtract(((JournalEntry) items.get(value)).odometer);
        if (subtract.signum() <= 0 || liters.signum() <= 0) {
            return pending("Distância ou volume insuficiente para calcular.");
        }
        for (JournalEntry journalEntry5 : list) {
            if (journalEntry5.kind == JournalEntry.Kind.FUEL && journalEntry5.vehicleId.equals(text) && journalEntry5.voided && journalEntry5.occurredAt >= ((JournalEntry) items.get(value)).occurredAt && journalEntry5.occurredAt <= journalEntry2.occurredAt) {
                items2.add(journalEntry5.id + ":" + journalEntry5.revision + ":ANULADO");
            }
        }
        seen2.add(((JournalEntry) items.get(value)).fuel);
        return new FuelSummary(subtract.divide(liters, 2, RoundingMode.HALF_UP), subtract, liters, "Calculado por tanque cheio a tanque cheio, método v1. Hodômetro, litros e completude informados por você; não é medição OBD.".concat((seen2.size() > 1 || seen2.contains("Não sei") || seen2.contains("Outro / misto")) ? " Composição do combustível mista ou não confirmada." : ""), items2);
    }
}