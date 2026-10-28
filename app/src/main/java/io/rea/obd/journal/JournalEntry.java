package io.rea.obd.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class JournalEntry {
    public static final List<String> FUELS = Collections.unmodifiableList(Arrays.asList("Não sei", "Gasolina", "Etanol", "Diesel", "Outro / misto"));
    public final boolean completeInterval;
    public final BigDecimal cost;
    public final long createdAt;
    public final String dueDate;
    public final BigDecimal dueOdometer;
    public final String fuel;
    public final boolean fullTank;
    public final String id;
    public final Kind kind;
    public final BigDecimal liters;
    public final String note;
    public final long occurredAt;
    public final BigDecimal odometer;
    public final long revisedAt;
    public final int revision;
    public final String vehicleId;
    public final String vehicleName;
    public final boolean voided;

    public enum Kind {
        SYMPTOM,
        SERVICE,
        FUEL
    }

    public JournalEntry(String id, String vehicleId, String vehicleName, Kind kind, int i, long occurredAt, long createdAt, long revisedAt, String note, BigDecimal bigDecimal, BigDecimal bigDecimal2, BigDecimal bigDecimal3, String fuel, boolean full, boolean complete, String dueDate, BigDecimal bigDecimal4) {
        this(id, vehicleId, vehicleName, kind, i, occurredAt, createdAt, revisedAt, note, bigDecimal, bigDecimal2, bigDecimal3, fuel, full, complete, dueDate, bigDecimal4, false);
    }

    public JournalEntry(String id, String vehicleId, String vehicleName, Kind kind, int i, long occurredAt, long createdAt, long revisedAt, String note, BigDecimal bigDecimal, BigDecimal bigDecimal2, BigDecimal bigDecimal3, String fuel, boolean fullTank, boolean completeInterval, String dueDate, BigDecimal bigDecimal4, boolean voided) {
        this.voided = voided;
        this.id = required(id, 100);
        this.vehicleId = required(vehicleId, 100);
        this.vehicleName = required(vehicleName, 160);
        if (kind == null || i < 1 || occurredAt <= 0 || createdAt <= 0 || revisedAt < createdAt) {
            throw new IllegalArgumentException("Identidade, revisão ou horário inválido.");
        }
        this.kind = kind;
        this.revision = i;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.revisedAt = revisedAt;
        String note2 = note == null ? "" : note.trim();
        this.note = note2;
        if (note2.length() > 2000 || (kind != Kind.FUEL && note2.isEmpty())) {
            throw new IllegalArgumentException("Descreva o acontecimento em até 2.000 caracteres.");
        }
        this.odometer = number(bigDecimal, 3, false);
        this.cost = number(bigDecimal3, 2, false);
        this.liters = number(bigDecimal2, 3, true);
        this.dueOdometer = number(bigDecimal4, 3, false);
        String fuel2 = fuel == null ? "" : fuel;
        this.fuel = fuel2;
        this.fullTank = fullTank;
        this.completeInterval = completeInterval;
        String dueDate2 = dueDate != null ? dueDate : "";
        this.dueDate = dueDate2;
        if (kind == Kind.FUEL) {
            if (bigDecimal == null || bigDecimal2 == null || !FUELS.contains(fuel2)) {
                throw new IllegalArgumentException("Informe hodômetro, litros e tipo de combustível (ou Não sei).");
            }
            if (completeInterval && !fullTank) {
                throw new IllegalArgumentException("Confirme um intervalo somente ao completar o tanque.");
            }
        } else if (bigDecimal2 != null || !fuel2.isEmpty() || fullTank || completeInterval) {
            throw new IllegalArgumentException("Campos de abastecimento em outro tipo de registro.");
        }
        if (kind != Kind.SERVICE && (!dueDate2.isEmpty() || bigDecimal4 != null)) {
            throw new IllegalArgumentException("Prazo de serviço em outro tipo de registro.");
        }
        if (!dueDate2.isEmpty()) {
            LocalDate.parse(dueDate2);
        }
        if (bigDecimal4 != null && bigDecimal != null && bigDecimal4.compareTo(bigDecimal) < 0) {
            throw new IllegalArgumentException("O próximo hodômetro não pode ser anterior ao informado no serviço.");
        }
    }

    private static String required(String text, int i) {
        if (text == null || text.trim().isEmpty() || text.length() > i) {
            throw new IllegalArgumentException("Identidade obrigatória ou muito longa.");
        }
        return text;
    }

    private static BigDecimal number(BigDecimal bigDecimal, int i, boolean flag) {
        if (bigDecimal == null) {
            return null;
        }
        if (bigDecimal.signum() < 0 || ((flag && bigDecimal.signum() == 0) || bigDecimal.scale() > i || bigDecimal.compareTo(new BigDecimal("1000000000")) > 0)) {
            throw new IllegalArgumentException("Valor numérico fora do formato permitido.");
        }
        return bigDecimal;
    }

    public static BigDecimal decimal(String text, int i) {
        String text2 = text == null ? "" : text.trim();
        if (text2.isEmpty()) {
            return null;
        }
        if (text2.matches("[0-9]+([.,][0-9]{1," + i + "})?")) {
            return new BigDecimal(text2.replace(',', '.'));
        }
        throw new IllegalArgumentException("Use números sem separador de milhares e até " + i + " casas decimais.");
    }

    public String title() {
        if (this.kind == Kind.SYMPTOM) {
            return "Sintoma";
        }
        return this.kind == Kind.SERVICE ? "Serviço" : "Abastecimento";
    }

    public JournalEntry withVoided(boolean voided, long now) {
        return new JournalEntry(this.id, this.vehicleId, this.vehicleName, this.kind, this.revision + 1, this.occurredAt, this.createdAt, Math.max(now, this.revisedAt), this.note, this.odometer, this.liters, this.cost, this.fuel, this.fullTank, this.completeInterval, this.dueDate, this.dueOdometer, voided);
    }
}