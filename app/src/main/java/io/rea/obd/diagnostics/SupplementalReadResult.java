package io.rea.obd.diagnostics;

import io.rea.obd.model.DataOrigin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class SupplementalReadResult {
    public static final List<Integer> FRAME_PIDS = Collections.unmodifiableList(Arrays.asList(12, 13, 5, 4, 17));
    public final String captureId;
    public final long completedAtEpochMillis;
    public final List<FramePid> frame;
    public final DataOrigin origin;
    public final Readiness readiness;
    public final String sessionId;
    public final long startedAtEpochMillis;
    public final String vehicleId;

    public enum MonitorState {
        UNSUPPORTED,
        COMPLETE,
        INCOMPLETE
    }

    public static final class Monitor {
        public final String id;
        public final String name;
        public final MonitorState state;

        private Monitor(String id, String name, boolean flag, boolean flag2) {
            this.id = id;
            this.name = name;
            this.state = !flag ? MonitorState.UNSUPPORTED : flag2 ? MonitorState.INCOMPLETE : MonitorState.COMPLETE;
        }
    }

    public static final class ReadinessBits {
        public final int a;
        public final int b;
        public final int c;
        public final int d;
        public final List<Monitor> monitors;

        public ReadinessBits(int i, int b, int c, int value) {
            int[] values = {i, b, c, value};
            for (int i2 = 0; i2 < 4; i2++) {
                int value2 = values[i2];
                if (value2 < 0 || value2 > 255) {
                    throw new IllegalArgumentException("Byte de prontidão inválido");
                }
            }
            this.a = i;
            this.b = b;
            this.c = c;
            this.d = value;
            ArrayList items = new ArrayList();
            String[] parts = {"Falhas de ignição", "Sistema de combustível", "Componentes abrangentes"};
            for (int i3 = 0; i3 < 3; i3++) {
                items.add(new Monitor("common:" + i3, parts[i3], ((1 << i3) & b) != 0, ((1 << (i3 + 4)) & b) != 0));
            }
            int value3 = b & 8;
            String[] parts2 = new String[8];
            if (value3 == 0) {
                parts2[0] = "Catalisador";
                parts2[1] = "Catalisador aquecido";
                parts2[2] = "Sistema evaporativo";
                parts2[3] = "Ar secundário";
                parts2[4] = "Refrigerante do ar-condicionado";
                parts2[5] = "Sondas de oxigênio";
                parts2[6] = "Aquecimento das sondas";
                parts2[7] = "EGR/VVT";
            } else {
                parts2[0] = "Catalisador NMHC";
                parts2[1] = "NOx/SCR";
                parts2[2] = null;
                parts2[3] = "Pressão de sobrealimentação";
                parts2[4] = null;
                parts2[5] = "Sensor de gases de escape";
                parts2[6] = "Filtro de partículas";
                parts2[7] = "EGR/VVT";
            }
            for (int i4 = 0; i4 < 8; i4++) {
                if (parts2[i4] != null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(value3 == 0 ? "spark:" : "compression:");
                    builder.append(i4);
                    String string = builder.toString();
                    int value4 = 1 << i4;
                    items.add(new Monitor(string, parts2[i4], (c & value4) != 0, (value4 & value) != 0));
                }
            }
            this.monitors = Collections.unmodifiableList(items);
        }

        public boolean milIlluminated() {
            return (this.a & 128) != 0;
        }

        public int reportedDtcCount() {
            return this.a & 127;
        }
    }

    public static final class ReadinessObservation {
        public final ReadinessBits bits;
        public final String id;
        public final long observedAtEpochMillis;

        public ReadinessObservation(String id, ReadinessBits readinessBits, long observedAtEpochMillis) {
            SupplementalReadResult.requireId(id);
            if (readinessBits == null) {
                throw new IllegalArgumentException("Prontidão ausente");
            }
            this.id = id;
            this.bits = readinessBits;
            this.observedAtEpochMillis = observedAtEpochMillis;
        }
    }

    public static final class Readiness {
        public final List<ReadinessObservation> observations;
        public final DtcReadResult.Status status;

        public Readiness(DtcReadResult.Status status, List<ReadinessObservation> list) {
            List<ReadinessObservation> observations = SupplementalReadResult.copy(list);
            this.observations = observations;
            SupplementalReadResult.checkStatus(status, 1, observations.size());
            this.status = status;
        }
    }

    public static final class FrameValue {
        public final String id;
        public final long observedAtEpochMillis;
        public final double value;

        public FrameValue(String id, double doubleValue, long observedAtEpochMillis) {
            SupplementalReadResult.requireId(id);
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                throw new IllegalArgumentException("Valor de quadro inválido");
            }
            this.id = id;
            this.value = doubleValue;
            this.observedAtEpochMillis = observedAtEpochMillis;
        }
    }

    public static final class FramePid {
        public final List<FrameValue> observations;
        public final int pid;
        public final DtcReadResult.Status status;

        public FramePid(int i, DtcReadResult.Status status, List<FrameValue> list) {
            if (!SupplementalReadResult.FRAME_PIDS.contains(Integer.valueOf(i))) {
                throw new IllegalArgumentException("PID fora do escopo de quadro 00");
            }
            this.pid = i;
            List<FrameValue> observations = SupplementalReadResult.copy(list);
            this.observations = observations;
            SupplementalReadResult.checkStatus(status, 2, observations.size());
            this.status = status;
        }
    }

    public SupplementalReadResult(String captureId, String vehicleId, String sessionId, DataOrigin dataOrigin, long startedAtEpochMillis, long completedAtEpochMillis, Readiness readiness, List<FramePid> list) {
        requireId(captureId);
        requireId(vehicleId);
        requireId(sessionId);
        if (dataOrigin == null || readiness == null) {
            throw new IllegalArgumentException("Contexto complementar incompleto");
        }
        this.captureId = captureId;
        this.vehicleId = vehicleId;
        this.sessionId = sessionId;
        this.origin = dataOrigin;
        this.startedAtEpochMillis = startedAtEpochMillis;
        this.completedAtEpochMillis = completedAtEpochMillis;
        this.readiness = readiness;
        this.frame = copy(list);
        HashSet seen = new HashSet();
        HashSet seen2 = new HashSet();
        Iterator<ReadinessObservation> it = readiness.observations.iterator();
        while (it.hasNext()) {
            if (!seen2.add(it.next().id)) {
                throw new IllegalArgumentException("ID repetido");
            }
        }
        for (FramePid framePid : this.frame) {
            if (!seen.add(Integer.valueOf(framePid.pid))) {
                throw new IllegalArgumentException("PID de quadro repetido");
            }
            Iterator<FrameValue> iterator = framePid.observations.iterator();
            while (iterator.hasNext()) {
                if (!seen2.add(iterator.next().id)) {
                    throw new IllegalArgumentException("ID repetido");
                }
            }
        }
        if (!seen.equals(new HashSet(FRAME_PIDS))) {
            throw new IllegalArgumentException("Cada PID do quadro precisa de estado, inclusive quando não consultado");
        }
    }

    public boolean transportFailed() {
        if (this.readiness.status.transportFailed()) {
            return true;
        }
        Iterator<FramePid> it = this.frame.iterator();
        while (it.hasNext()) {
            if (it.next().status.transportFailed()) {
                return true;
            }
        }
        return false;
    }

    public boolean incomplete() {
        if (this.readiness.status.outcome != DtcReadResult.Outcome.POSITIVE) {
            return true;
        }
        Iterator<FramePid> it = this.frame.iterator();
        while (it.hasNext()) {
            if (it.next().status.outcome != DtcReadResult.Outcome.POSITIVE) {
                return true;
            }
        }
        return false;
    }

    public static void checkStatus(DtcReadResult.Status status, int i, int value) {
        if (status == null || status.positiveResponseCount != value) {
            throw new IllegalArgumentException("Estado e contagem de observações incompatíveis");
        }
        Iterator<DtcReadResult.NegativeResponse> it = status.negativeResponses.iterator();
        while (it.hasNext()) {
            if (it.next().service != i) {
                throw new IllegalArgumentException("Resposta negativa de outro serviço");
            }
        }
    }

    public static void requireId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Identidade obrigatória");
        }
    }

    public static <T> List<T> copy(List<T> list) {
        if (list == null || list.contains(null)) {
            throw new IllegalArgumentException("Lista inválida");
        }
        return Collections.unmodifiableList(new ArrayList(list));
    }
}