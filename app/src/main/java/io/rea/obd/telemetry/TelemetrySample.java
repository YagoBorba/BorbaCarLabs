package io.rea.obd.telemetry;

import io.rea.obd.model.DataOrigin;

public final class TelemetrySample {
    public final String detail;
    public final long elapsedMillis;
    public final long epochMillis;
    public final DataOrigin origin;
    public final int pid;
    public final Status status;
    public final Double value;

    public enum Status {
        VALID,
        UNSUPPORTED,
        NO_DATA,
        INVALID,
        TIMEOUT,
        DISCONNECTED,
        NOT_READ,
        STALE
    }

    public TelemetrySample(int i, Double value, Status status, DataOrigin dataOrigin, long epochMillis, long elapsedMillis, String detail) {
        if (status == null || dataOrigin == null) {
            throw new IllegalArgumentException("Origem e estado obrigatórios");
        }
        if (status == Status.VALID && (value == null || Double.isNaN(value.doubleValue()) || Double.isInfinite(value.doubleValue()))) {
            throw new IllegalArgumentException("Uma amostra válida precisa de valor numérico finito");
        }
        if (status != Status.VALID && value != null) {
            throw new IllegalArgumentException("Estado sem leitura não pode ter valor");
        }
        this.pid = i;
        this.value = value;
        this.status = status;
        this.origin = dataOrigin;
        this.epochMillis = epochMillis;
        this.elapsedMillis = elapsedMillis;
        this.detail = detail == null ? "" : detail;
    }

    public Status statusAt(long elapsedMillis, long value) {
        if (this.status != Status.VALID) {
            return this.status;
        }
        long value2 = elapsedMillis - this.elapsedMillis;
        return (value2 < 0 || value2 > value) ? Status.STALE : Status.VALID;
    }

    public String label(long elapsedMillis) {
        int ordinal = statusAt(elapsedMillis, DashboardState.MAX_AGE_MS).ordinal();
        if (ordinal == 0) {
            return this.origin == DataOrigin.SIMULATED ? "Simulado" : "Leitura do veículo";
        }
        if (ordinal == 1) {
            return "Não anunciado pela ECU";
        }
        if (ordinal == 2) {
            return "Sem resposta de dados";
        }
        if (ordinal == 3) {
            return "Resposta inválida";
        }
        if (ordinal == 4) {
            return "Tempo esgotado";
        }
        if (ordinal == 5) {
            return "Conexão encerrada";
        }
        if (ordinal == 7) {
            return "Leitura antiga";
        }
        return "Não consultado";
    }
}