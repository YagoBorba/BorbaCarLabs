package io.rea.obd.telemetry;

import io.rea.obd.SensorCatalog;
import io.rea.obd.model.DataOrigin;
import io.rea.obd.ui.Format;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DashboardState {
    public static final long MAX_AGE_MS = 15000;
    public final boolean collecting;
    public final String coverage;
    public final Mode mode;
    public final Map<Integer, Reading> readings;
    public final String recording;
    public final boolean recordingOpen;
    public final Reading rpm;

    public enum Mode {
        OFFLINE,
        CONNECTING,
        CONNECTED,
        DEMONSTRATION
    }

    public static final class Reading {
        public final String origin;
        public final String quality;
        public final TelemetrySample sample;
        public final boolean selected;
        public final SensorCatalog.Sensor sensor;
        public final TelemetrySample.Status status;
        public final String time;
        public final String value;

        Reading(int i, TelemetrySample telemetrySample, boolean selected, long elapsedMillis) {
            String text;
            String label;
            String origin;
            String text2;
            SensorCatalog.Sensor sensor = SensorCatalog.get(i);
            this.sensor = sensor;
            if (sensor == null) {
                throw new IllegalArgumentException("Sensor desconhecido");
            }
            this.sample = telemetrySample;
            this.selected = selected;
            TelemetrySample.Status statusAt = telemetrySample == null ? TelemetrySample.Status.NOT_READ : telemetrySample.statusAt(elapsedMillis, DashboardState.MAX_AGE_MS);
            this.status = statusAt;
            if (selected && statusAt == TelemetrySample.Status.VALID) {
                text = String.format(Locale.getDefault(), "%." + sensor.decimals + "f", telemetrySample.value);
            } else {
                text = "—";
            }
            this.value = text;
            if (selected) {
                label = telemetrySample == null ? "Não consultado" : telemetrySample.label(elapsedMillis);
            } else {
                label = "Fora da seleção";
            }
            this.quality = label;
            if (telemetrySample == null) {
                origin = "Sem aquisição";
            } else {
                origin = telemetrySample.origin == DataOrigin.SIMULATED ? "Dado simulado" : "Leitura OBD · ECU não identificada";
            }
            this.origin = origin;
            if (telemetrySample == null) {
                this.time = "Sem horário de aquisição";
                return;
            }
            long value = elapsedMillis - telemetrySample.elapsedMillis;
            StringBuilder builder = new StringBuilder("Recebido ");
            builder.append(Format.clock(telemetrySample.epochMillis));
            if (value < 0) {
                text2 = " · idade indisponível";
            } else {
                text2 = " · há " + (value / 1000) + " s";
            }
            builder.append(text2);
            this.time = builder.toString();
        }

        public List<String> details() {
            ArrayList items = new ArrayList();
            items.add(this.sensor.name + " · PID " + String.format(Locale.ROOT, "01 %02X", Integer.valueOf(this.sensor.pid)));
            StringBuilder builder = new StringBuilder("Unidade: ");
            builder.append(this.sensor.unit);
            items.add(builder.toString());
            items.add("Estado: " + this.quality);
            items.add("Origem: " + this.origin);
            if (this.sample != null) {
                items.add("Aquisição: " + Format.when(this.sample.epochMillis));
                if (this.sample.value != null) {
                    StringBuilder builder2 = new StringBuilder("Valor na captura: ");
                    builder2.append(String.format(Locale.getDefault(), "%." + this.sensor.decimals + "f %s", this.sample.value, this.sensor.unit));
                    builder2.append((this.status == TelemetrySample.Status.VALID && this.selected) ? "" : " · histórico, não é valor atual");
                    items.add(builder2.toString());
                }
                if (!this.sample.detail.isEmpty()) {
                    items.add("Detalhe da resposta: " + this.sample.detail);
                }
            }
            items.add("A presença no catálogo não comprova suporte do veículo. Respostas sem identificação de ECU não confirmam módulo específico.");
            return Collections.unmodifiableList(items);
        }
    }

    public DashboardState(boolean demo, boolean connected, boolean busy, boolean resumed, int i, Set<Integer> set, Map<Integer, TelemetrySample> map, long elapsedMillis, boolean recordingOpen) {
        Mode mode;
        StringBuilder builder;
        String text;
        String string;
        String recording;
        if (demo) {
            mode = Mode.DEMONSTRATION;
        } else {
            mode = connected ? Mode.CONNECTED : busy ? Mode.CONNECTING : Mode.OFFLINE;
        }
        this.mode = mode;
        int value = 0;
        this.collecting = resumed && i == 0 && (demo || connected) && !busy;
        this.recordingOpen = recordingOpen;
        this.rpm = new Reading(12, map.get(12), set.contains(12), elapsedMillis);
        LinkedHashMap map2 = new LinkedHashMap();
        int value2 = 0;
        for (Integer num : set) {
            Reading reading = new Reading(num.intValue(), map.get(num), true, elapsedMillis);
            map2.put(num, reading);
            if (reading.status == TelemetrySample.Status.VALID) {
                value++;
            } else {
                value2++;
            }
        }
        this.readings = Collections.unmodifiableMap(map2);
        if (value == 0) {
            string = "Nenhuma leitura atual disponível";
        } else {
            if (value2 == 0) {
                builder = new StringBuilder();
                builder.append(value);
                text = " sinais com leitura atual";
            } else {
                builder = new StringBuilder("Leituras parciais · ");
                builder.append(value);
                builder.append(" sinais atuais e ");
                builder.append(value2);
                text = " sem leitura atual";
            }
            builder.append(text);
            string = builder.toString();
        }
        this.coverage = string;
        if (recordingOpen) {
            recording = this.collecting ? "GRAVAÇÃO ATIVA · Painel em primeiro plano" : "GRAVAÇÃO PAUSADA · volte ao Painel em primeiro plano";
        } else {
            recording = "GRAVAÇÃO DESLIGADA";
        }
        this.recording = recording;
    }
}