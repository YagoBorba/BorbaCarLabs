package io.rea.obd;

import io.rea.obd.model.DataOrigin;
import io.rea.obd.telemetry.TelemetrySample;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TelemetryRecorder implements Closeable {
    public static final long MAX_BYTES = 10485760;
    private final String adapter;
    private long bytes;
    private boolean closed;
    private final File file;
    private long samples;
    private final String sessionId;
    private final boolean simulated;
    private final Map<Integer, double[]> stats;
    private final String vehicle;
    private final String vehicleId;
    private final Writer writer;

    public TelemetryRecorder(File file, boolean simulated, String vehicle) throws IOException {
        this(file, simulated, "", vehicle, "");
    }

    public TelemetryRecorder(File file, boolean simulated, String vehicleId, String vehicle, String adapter) throws IOException {
        this.stats = new LinkedHashMap();
        this.file = file;
        this.simulated = simulated;
        this.vehicleId = vehicleId;
        this.vehicle = vehicle;
        this.adapter = adapter;
        this.sessionId = UUID.randomUUID().toString();
        this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        write("timestamp_utc,epoch_ms,simulated,vehicle_id,vehicle,adapter,session_id,pid,sensor,status,value,unit\r\n");
    }

    private void write(String text) throws IOException {
        long length = this.bytes + ((long) text.getBytes(StandardCharsets.UTF_8).length);
        this.bytes = length;
        if (length > MAX_BYTES) {
            throw new IOException("A gravação atingiu o limite de 10 MiB por sessão");
        }
        this.writer.write(text);
    }

    public synchronized void add(long epochMillis, int i, String text, double value, String text2) throws IOException {
        add(new TelemetrySample(i, Double.valueOf(value), TelemetrySample.Status.VALID, this.simulated ? DataOrigin.SIMULATED : DataOrigin.VEHICLE, epochMillis, 0L, ""), text, text2);
    }

    public synchronized void add(TelemetrySample telemetrySample, String adapter, String adapter2) throws IOException {
        if (this.closed) {
            throw new IOException("Gravação encerrada");
        }
        if ((telemetrySample.origin == DataOrigin.SIMULATED) != this.simulated) {
            throw new IOException("Origem da amostra incompatível com a sessão");
        }
        String string = Instant.ofEpochMilli(telemetrySample.epochMillis).toString();
        StringBuilder builder = new StringBuilder();
        builder.append(csv(string));
        builder.append(",");
        builder.append(telemetrySample.epochMillis);
        builder.append(",");
        builder.append(this.simulated);
        builder.append(",");
        builder.append(csv(this.vehicleId));
        builder.append(",");
        builder.append(csv(this.vehicle));
        builder.append(",");
        builder.append(csv(this.adapter));
        builder.append(",");
        builder.append(csv(this.sessionId));
        builder.append(",");
        builder.append(String.format(Locale.ROOT, "%02X", Integer.valueOf(telemetrySample.pid)));
        builder.append(",");
        builder.append(csv(adapter));
        builder.append(",");
        builder.append(telemetrySample.status);
        builder.append(",");
        builder.append(telemetrySample.value == null ? "" : Double.toString(telemetrySample.value.doubleValue()));
        builder.append(",");
        builder.append(csv(adapter2));
        builder.append("\r\n");
        write(builder.toString());
        this.samples++;
        if (telemetrySample.status == TelemetrySample.Status.VALID) {
            double doubleValue = telemetrySample.value.doubleValue();
            double[] stats = this.stats.get(Integer.valueOf(telemetrySample.pid));
            if (stats == null) {
                stats = new double[]{doubleValue, doubleValue, 0.0d, 0.0d};
                this.stats.put(Integer.valueOf(telemetrySample.pid), stats);
            }
            stats[0] = Math.min(stats[0], doubleValue);
            stats[1] = Math.max(stats[1], doubleValue);
            stats[2] = stats[2] + doubleValue;
            stats[3] = stats[3] + 1.0d;
        }
        if (this.samples % 20 == 0) {
            this.writer.flush();
        }
    }

    public static String csv(String adapter) {
        return "\"" + adapter.replace("\"", "\"\"") + "\"";
    }

    public synchronized long samples() {
        return this.samples;
    }

    public String recordingId() {
        return this.sessionId;
    }

    public File file() {
        return this.file;
    }

    public synchronized Map<Integer, double[]> statistics() {
        LinkedHashMap map;
        map = new LinkedHashMap();
        for (Map.Entry<Integer, double[]> entry : this.stats.entrySet()) {
            map.put(entry.getKey(), Arrays.copyOf(entry.getValue(), 4));
        }
        return map;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            this.writer.close();
        }
    }
}