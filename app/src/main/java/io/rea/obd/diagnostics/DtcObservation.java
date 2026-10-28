package io.rea.obd.diagnostics;

public final class DtcObservation {
    public final Category category;
    public final String code;
    public final String id;
    public final long observedAtEpochMillis;

    public enum Category {
        STORED(3),
        PENDING(7),
        PERMANENT(10);

        public final int service;

        Category(int i) {
            this.service = i;
        }
    }

    public DtcObservation(String id, String code, Category category, long observedAtEpochMillis) {
        if (id == null || id.isEmpty() || code == null || !code.matches("[PCBU][0-3][0-9A-F]{3}") || category == null) {
            throw new IllegalArgumentException("Observação DTC inválida");
        }
        this.id = id;
        this.code = code;
        this.category = category;
        this.observedAtEpochMillis = observedAtEpochMillis;
    }
}