package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class DtcReadResult {
    public final String captureId;
    public final long completedAtEpochMillis;
    public final MilResult mil;
    public final DataOrigin origin;
    public final List<ServiceResult> services;
    public final String sessionId;
    public final long startedAtEpochMillis;
    public final String vehicleId;

    public enum Outcome {
        POSITIVE,
        NO_DATA,
        NEGATIVE,
        INVALID,
        TIMEOUT,
        DISCONNECTED,
        NOT_READ,
        PARTIAL
    }

    public static final class NegativeResponse {
        public final int code;
        public final int service;

        public NegativeResponse(int i, int code) {
            if (i < 0 || i > 255 || code < 0 || code > 255) {
                throw new IllegalArgumentException("Resposta negativa inválida");
            }
            this.service = i;
            this.code = code;
        }
    }

    public static final class Status {
        public final Long completedAtEpochMillis;
        public final String detail;
        public final List<NegativeResponse> negativeResponses;
        public final Outcome outcome;
        public final int positiveResponseCount;
        public final Long requestedAtEpochMillis;

        public Status(Outcome outcome, Long requestedAtEpochMillis, Long completedAtEpochMillis, int i, List<NegativeResponse> list, String detail) {
            if (outcome == null || i < 0) {
                throw new IllegalArgumentException("Estado de consulta inválido");
            }
            if (outcome == Outcome.NOT_READ) {
                if (requestedAtEpochMillis != null || completedAtEpochMillis != null || i != 0) {
                    throw new IllegalArgumentException("Consulta não executada não possui horário de leitura");
                }
            } else if (requestedAtEpochMillis == null || completedAtEpochMillis == null) {
                throw new IllegalArgumentException("Consulta executada precisa de horários");
            }
            List<NegativeResponse> negativeResponses = DtcReadResult.copy(list);
            this.negativeResponses = negativeResponses;
            if ((outcome == Outcome.POSITIVE || outcome == Outcome.PARTIAL) != (i > 0)) {
                throw new IllegalArgumentException("Contagem positiva incompatível com estado");
            }
            if (outcome == Outcome.POSITIVE && !negativeResponses.isEmpty()) {
                throw new IllegalArgumentException("Resposta negativa impede confirmação completa");
            }
            if (outcome == Outcome.NEGATIVE && negativeResponses.isEmpty()) {
                throw new IllegalArgumentException("Resposta negativa ausente");
            }
            if (outcome != Outcome.NEGATIVE && outcome != Outcome.PARTIAL && !negativeResponses.isEmpty()) {
                throw new IllegalArgumentException("Estado incompatível com resposta negativa");
            }
            this.outcome = outcome;
            this.requestedAtEpochMillis = requestedAtEpochMillis;
            this.completedAtEpochMillis = completedAtEpochMillis;
            this.positiveResponseCount = i;
            this.detail = detail == null ? "" : detail;
        }

        public boolean transportFailed() {
            return this.outcome == Outcome.TIMEOUT || this.outcome == Outcome.DISCONNECTED;
        }
    }

    public static final class ServiceResult {
        public final DtcObservation.Category category;
        public final List<DtcObservation> observations;
        public final Status status;

        public ServiceResult(DtcObservation.Category category, Status status, List<DtcObservation> list) {
            if (category == null || status == null) {
                throw new IllegalArgumentException("Categoria e estado obrigatórios");
            }
            this.category = category;
            this.status = status;
            List<DtcObservation> observations = DtcReadResult.copy(list);
            this.observations = observations;
            if (status.positiveResponseCount == 0 && !observations.isEmpty()) {
                throw new IllegalArgumentException("Código sem resposta positiva");
            }
            HashSet seen = new HashSet();
            HashSet seen2 = new HashSet();
            for (DtcObservation dtcObservation : observations) {
                if (dtcObservation.category != category || !seen.add(dtcObservation.id) || !seen2.add(dtcObservation.code)) {
                    throw new IllegalArgumentException("Observação duplicada ou de outra categoria");
                }
            }
            Iterator<NegativeResponse> it = status.negativeResponses.iterator();
            while (it.hasNext()) {
                if (it.next().service != category.service) {
                    throw new IllegalArgumentException("Resposta negativa de outra categoria");
                }
            }
        }
    }

    public static final class MilObservation {
        public final String id;
        public final boolean illuminated;
        public final long observedAtEpochMillis;
        public final int reportedDtcCount;

        public MilObservation(String id, int i, long observedAtEpochMillis) {
            if (id == null || id.isEmpty() || i < 0 || i > 255) {
                throw new IllegalArgumentException("Observação MIL inválida");
            }
            this.id = id;
            this.illuminated = (i & 128) != 0;
            this.reportedDtcCount = i & 127;
            this.observedAtEpochMillis = observedAtEpochMillis;
        }
    }

    public static final class MilResult {
        public final List<MilObservation> observations;
        public final Status status;

        public MilResult(Status status, List<MilObservation> list) {
            if (status != null) {
                this.status = status;
                List<MilObservation> observations = DtcReadResult.copy(list);
                this.observations = observations;
                if (status.positiveResponseCount != observations.size()) {
                    throw new IllegalArgumentException("Contagem de respostas MIL inconsistente");
                }
                Iterator<NegativeResponse> it = status.negativeResponses.iterator();
                while (it.hasNext()) {
                    if (it.next().service != 1) {
                        throw new IllegalArgumentException("Resposta negativa de outro serviço MIL");
                    }
                }
                return;
            }
            throw new IllegalArgumentException("Estado MIL obrigatório");
        }
    }

    public DtcReadResult(String captureId, String vehicleId, String sessionId, DataOrigin dataOrigin, long startedAtEpochMillis, long completedAtEpochMillis, MilResult milResult, List<ServiceResult> list) {
        if (captureId == null || captureId.isEmpty() || sessionId == null || sessionId.isEmpty() || dataOrigin == null || milResult == null) {
            throw new IllegalArgumentException("Contexto de captura incompleto");
        }
        if (vehicleId != null && vehicleId.isEmpty()) {
            throw new IllegalArgumentException("ID de veículo vazio");
        }
        this.captureId = captureId;
        this.vehicleId = vehicleId;
        this.sessionId = sessionId;
        this.origin = dataOrigin;
        this.startedAtEpochMillis = startedAtEpochMillis;
        this.completedAtEpochMillis = completedAtEpochMillis;
        this.mil = milResult;
        this.services = copy(list);
        EnumSet noneOf = EnumSet.noneOf(DtcObservation.Category.class);
        HashSet seen = new HashSet();
        Iterator<MilObservation> it = milResult.observations.iterator();
        while (it.hasNext()) {
            if (!seen.add(it.next().id)) {
                throw new IllegalArgumentException("ID de observação repetido");
            }
        }
        for (ServiceResult serviceResult : this.services) {
            if (!noneOf.add(serviceResult.category)) {
                throw new IllegalArgumentException("Categoria repetida");
            }
            Iterator<DtcObservation> iterator = serviceResult.observations.iterator();
            while (iterator.hasNext()) {
                if (!seen.add(iterator.next().id)) {
                    throw new IllegalArgumentException("ID de observação repetido");
                }
            }
        }
        if (noneOf.size() != DtcObservation.Category.values().length) {
            throw new IllegalArgumentException("Toda categoria deve ter resultado, inclusive quando não consultada");
        }
    }

    public List<DtcObservation> observations() {
        ArrayList items = new ArrayList();
        Iterator<ServiceResult> it = this.services.iterator();
        while (it.hasNext()) {
            items.addAll(it.next().observations);
        }
        return Collections.unmodifiableList(items);
    }

    public boolean incomplete() {
        if (this.mil.status.outcome != Outcome.POSITIVE) {
            return true;
        }
        Iterator<ServiceResult> it = this.services.iterator();
        while (it.hasNext()) {
            if (it.next().status.outcome != Outcome.POSITIVE) {
                return true;
            }
        }
        return false;
    }

    public boolean transportFailed() {
        if (this.mil.status.transportFailed()) {
            return true;
        }
        Iterator<ServiceResult> it = this.services.iterator();
        while (it.hasNext()) {
            if (it.next().status.transportFailed()) {
                return true;
            }
        }
        return false;
    }

    public static <T> List<T> copy(List<T> list) {
        if (list == null || list.contains(null)) {
            throw new IllegalArgumentException("Lista inválida");
        }
        return Collections.unmodifiableList(new ArrayList(list));
    }
}