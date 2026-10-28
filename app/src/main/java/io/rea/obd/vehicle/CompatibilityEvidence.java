package io.rea.obd.vehicle;

public final class CompatibilityEvidence {
    public final String adapter;
    public final String androidVersion;
    public final String appVersion;
    public final String configuration;
    public final String function;
    public final Kind kind;
    public final Long observedAtMillis;
    public final String protocol;
    public final String reference;
    public final String result;
    public final String vehicleId;

    public enum Kind {
        IMPLEMENTED,
        DOCUMENTED,
        ANNOUNCED,
        OBSERVED,
        PHYSICALLY_VALIDATED
    }

    public CompatibilityEvidence(Kind kind, String function, String vehicleId, String configuration, String protocol, String reference, String adapter, String androidVersion, String appVersion, String text, Long observedAtMillis) {
        if (kind == null || empty(function) || empty(reference) || empty(text)) {
            throw new IllegalArgumentException("Afirmação de compatibilidade incompleta");
        }
        if (observedAtMillis != null && observedAtMillis.longValue() < 0) {
            throw new IllegalArgumentException("Instante inválido");
        }
        if ((kind == Kind.ANNOUNCED || kind == Kind.OBSERVED || kind == Kind.PHYSICALLY_VALIDATED) && (empty(vehicleId) || empty(protocol) || empty(adapter) || empty(appVersion) || observedAtMillis == null)) {
            throw new IllegalArgumentException("Evidência de sessão sem contexto");
        }
        if (kind == Kind.PHYSICALLY_VALIDATED && (empty(configuration) || empty(androidVersion))) {
            throw new IllegalArgumentException("Validação física exige configuração e Android identificados");
        }
        this.kind = kind;
        this.function = function;
        this.vehicleId = vehicleId;
        this.configuration = configuration;
        this.protocol = protocol;
        this.reference = reference;
        this.adapter = adapter;
        this.androidVersion = androidVersion;
        this.appVersion = appVersion;
        this.result = text;
        this.observedAtMillis = observedAtMillis;
    }

    private static boolean empty(String reference) {
        return reference == null || reference.trim().isEmpty();
    }
}