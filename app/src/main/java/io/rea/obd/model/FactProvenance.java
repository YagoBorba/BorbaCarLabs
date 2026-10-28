package io.rea.obd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FactProvenance {
    public final String method;
    public final Origin origin;
    public final Long recordedAtMillis;
    public final List<String> references;
    public final String scope;
    public final Verification verification;

    public enum Origin {
        LIVE_OBD,
        CATALOG,
        USER_INPUT,
        DERIVED,
        SIMULATION,
        UNKNOWN_LEGACY
    }

    public enum Verification {
        UNKNOWN,
        DECLARED,
        PENDING,
        DOCUMENTED,
        OBSERVED,
        EXAMPLE
    }

    public FactProvenance(Origin origin, Verification verification, List<String> list, String scope, String method, Long recordedAtMillis) {
        if (origin == null || verification == null || list == null || scope == null || method == null || (recordedAtMillis != null && recordedAtMillis.longValue() < 0)) {
            throw new IllegalArgumentException("Proveniência incompleta");
        }
        ArrayList items = new ArrayList();
        for (String id : list) {
            if (id == null || id.trim().isEmpty() || items.contains(id)) {
                throw new IllegalArgumentException("Referência inválida ou duplicada");
            }
            items.add(id);
        }
        int ordinal = origin.ordinal();
        if (ordinal != 0) {
            if (ordinal != 1) {
                if (ordinal != 2) {
                    if (ordinal != 3) {
                        if (ordinal != 4) {
                            if (ordinal == 5 && (verification != Verification.PENDING || !items.isEmpty() || recordedAtMillis != null || !scope.isEmpty() || !method.isEmpty())) {
                                throw new IllegalArgumentException("Legado não recebe fonte ou data presumida");
                            }
                        } else if (verification != Verification.EXAMPLE || items.isEmpty() || scope.isEmpty()) {
                            throw new IllegalArgumentException("Simulação exige cenário identificado");
                        }
                    } else if (verification != Verification.PENDING || items.isEmpty() || method.isEmpty() || scope.isEmpty()) {
                        throw new IllegalArgumentException("Cálculo exige entradas, método e contexto");
                    }
                } else if (verification != Verification.DECLARED || !items.isEmpty() || !scope.isEmpty() || !method.isEmpty()) {
                    throw new IllegalArgumentException("Declaração não é confirmação documental");
                }
            } else if (verification != Verification.DOCUMENTED || items.isEmpty() || scope.isEmpty() || !method.isEmpty()) {
                throw new IllegalArgumentException("Catálogo exige fonte e escopo");
            }
        } else if (verification != Verification.OBSERVED || items.isEmpty() || scope.isEmpty()) {
            throw new IllegalArgumentException("Observação exige referência e escopo");
        }
        this.origin = origin;
        this.verification = verification;
        this.references = Collections.unmodifiableList(items);
        this.scope = scope;
        this.method = method;
        this.recordedAtMillis = recordedAtMillis;
    }

    public static FactProvenance declared(Long recordedAtMillis) {
        return new FactProvenance(Origin.USER_INPUT, Verification.DECLARED, Collections.emptyList(), "", "", recordedAtMillis);
    }

    public static FactProvenance legacy() {
        return new FactProvenance(Origin.UNKNOWN_LEGACY, Verification.PENDING, Collections.emptyList(), "", "", null);
    }

    public boolean equals(Object value) {
        if (!(value instanceof FactProvenance)) {
            return false;
        }
        FactProvenance factProvenance = (FactProvenance) value;
        return this.origin == factProvenance.origin && this.verification == factProvenance.verification && this.references.equals(factProvenance.references) && this.scope.equals(factProvenance.scope) && this.method.equals(factProvenance.method) && Objects.equals(this.recordedAtMillis, factProvenance.recordedAtMillis);
    }

    public int hashCode() {
        return Objects.hash(this.origin, this.verification, this.references, this.scope, this.method, this.recordedAtMillis);
    }
}