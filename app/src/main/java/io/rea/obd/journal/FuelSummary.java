package io.rea.obd.journal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FuelSummary {
    public final BigDecimal cost;
    public final BigDecimal costPerKm;
    public final BigDecimal distance;
    public final String endId;
    public final long endedAt;
    public final List<String> entryRevisions;
    public final String explanation;
    public final String fuel;
    public final BigDecimal kmPerLiter;
    public final BigDecimal liters;

    private FuelSummary(BigDecimal kmPerLiter, BigDecimal distance, BigDecimal liters, BigDecimal cost, String fuel, String endId, long endedAt, String explanation, List<String> list) {
        this.kmPerLiter = kmPerLiter;
        this.distance = distance;
        this.liters = liters;
        this.cost = cost;
        this.costPerKm = cost == null || distance == null ? null : cost.divide(distance, 2, RoundingMode.HALF_UP);
        this.fuel = fuel;
        this.endId = endId;
        this.endedAt = endedAt;
        this.explanation = explanation;
        this.entryRevisions = Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static FuelSummary pending(String explanation) {
        return new FuelSummary(null, null, null, null, "", "", 0, explanation, Collections.emptyList());
    }

    private static List<JournalEntry> fills(List<JournalEntry> list, String vehicleId) {
        List<JournalEntry> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JournalEntry entry : list) {
            if (entry.kind == JournalEntry.Kind.FUEL && entry.vehicleId.equals(vehicleId)) {
                if (!seen.add(entry.id)) {
                    return null;
                }
                if (!entry.voided) {
                    items.add(entry);
                }
            }
        }
        items.sort(Comparator.comparingLong((JournalEntry entry) -> entry.occurredAt).thenComparing(entry -> entry.id));
        return items;
    }

    public static FuelSummary latest(List<JournalEntry> list, String vehicleId) {
        List<JournalEntry> items = fills(list, vehicleId);
        if (items == null) {
            return pending("Há revisões duplicadas. Use apenas a revisão atual de cada registro.");
        }
        if (items.size() < 2) {
            return pending("Registre dois fechamentos de tanque cheio e os abastecimentos entre eles.");
        }
        return interval(list, items, items.size() - 1);
    }

    public static List<FuelSummary> history(List<JournalEntry> list, String vehicleId) {
        List<JournalEntry> items = fills(list, vehicleId);
        List<FuelSummary> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).fullTank && items.get(i).completeInterval) {
                FuelSummary summary = interval(list, items, i);
                if (summary.kmPerLiter != null) {
                    result.add(summary);
                }
            }
        }
        return result;
    }

    public static Map<String, BigDecimal> averageCostPerKm(List<FuelSummary> history) {
        Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
        for (FuelSummary summary : history) {
            if (summary.cost != null && (summary.fuel.equals("Gasolina") || summary.fuel.equals("Etanol") || summary.fuel.equals("Diesel"))) {
                BigDecimal[] sum = sums.computeIfAbsent(summary.fuel, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                sum[0] = sum[0].add(summary.cost);
                sum[1] = sum[1].add(summary.distance);
            }
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal[]> entry : sums.entrySet()) {
            result.put(entry.getKey(), entry.getValue()[0].divide(entry.getValue()[1], 2, RoundingMode.HALF_UP));
        }
        return result;
    }

    public static BigDecimal lastPrice(List<JournalEntry> list, String vehicleId, String fuel) {
        JournalEntry last = null;
        for (JournalEntry entry : list) {
            if (entry.kind == JournalEntry.Kind.FUEL && entry.vehicleId.equals(vehicleId) && !entry.voided && fuel.equals(entry.fuel) && entry.cost != null && entry.liters != null && entry.liters.signum() > 0 && (last == null || entry.occurredAt > last.occurredAt)) {
                last = entry;
            }
        }
        return last == null ? null : last.cost.divide(last.liters, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal spentSince(List<JournalEntry> list, String vehicleId, long since) {
        BigDecimal total = BigDecimal.ZERO;
        for (JournalEntry entry : list) {
            if (entry.kind == JournalEntry.Kind.FUEL && entry.vehicleId.equals(vehicleId) && !entry.voided && entry.cost != null && entry.occurredAt >= since) {
                total = total.add(entry.cost);
            }
        }
        return total;
    }

    private static FuelSummary interval(List<JournalEntry> list, List<JournalEntry> items, int end) {
        JournalEntry closing = items.get(end);
        if (!closing.fullTank) {
            return pending("Período aberto: o último abastecimento é parcial.");
        }
        if (!closing.completeInterval) {
            return pending("Confirme no fechamento se todos os abastecimentos do intervalo foram registrados.");
        }
        int start = end - 1;
        while (start >= 0 && !items.get(start).fullTank) {
            start--;
        }
        if (start < 0) {
            return pending("Falta um tanque cheio anterior como referência.");
        }
        BigDecimal liters = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        boolean priced = true;
        Set<String> fuels = new HashSet<>();
        List<String> revisions = new ArrayList<>();
        JournalEntry previous = items.get(start);
        revisions.add(previous.id + ":" + previous.revision);
        for (int i = start + 1; i <= end; i++) {
            JournalEntry entry = items.get(i);
            if (entry.occurredAt <= previous.occurredAt) {
                return pending("Horários iguais: revise a ordem dos abastecimentos.");
            }
            if (entry.odometer.compareTo(previous.odometer) < 0) {
                return pending("Hodômetro regressivo: revise os registros antes de calcular.");
            }
            liters = liters.add(entry.liters);
            if (entry.cost == null) {
                priced = false;
            } else {
                cost = cost.add(entry.cost);
            }
            fuels.add(entry.fuel);
            revisions.add(entry.id + ":" + entry.revision);
            previous = entry;
        }
        BigDecimal distance = closing.odometer.subtract(items.get(start).odometer);
        if (distance.signum() <= 0 || liters.signum() <= 0) {
            return pending("Distância ou volume insuficiente para calcular.");
        }
        for (JournalEntry entry : list) {
            if (entry.kind == JournalEntry.Kind.FUEL && entry.vehicleId.equals(closing.vehicleId) && entry.voided && entry.occurredAt >= items.get(start).occurredAt && entry.occurredAt <= closing.occurredAt) {
                revisions.add(entry.id + ":" + entry.revision + ":ANULADO");
            }
        }
        Set<String> tankFuels = new HashSet<>(fuels);
        fuels.add(items.get(start).fuel);
        boolean mixed = fuels.size() > 1 || fuels.contains("Não sei") || fuels.contains("Outro / misto");
        String fuel = tankFuels.size() == 1 ? tankFuels.iterator().next() : "Outro / misto";
        String explanation = "Calculado por tanque cheio a tanque cheio, método v1. Hodômetro, litros e completude informados por você; não é medição OBD." + (mixed ? " Composição do combustível mista ou não confirmada." : "");
        return new FuelSummary(distance.divide(liters, 2, RoundingMode.HALF_UP), distance, liters, priced ? cost : null, fuel, closing.id, closing.occurredAt, explanation, revisions);
    }
}
