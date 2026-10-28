package io.rea.obd;

import io.rea.obd.telemetry.DashboardState;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ElmSession implements Closeable {
    private boolean can;
    private volatile boolean closed;
    private volatile boolean connected;
    private InputStream input;
    private OutputStream output;
    private final ElmTransport transport;
    private String protocol = "";
    private String identity = "";
    private final Set<Integer> supported = new LinkedHashSet();

    public static final class ResponseTimeoutException extends IOException {
        ResponseTimeoutException(String text) {
            super(text);
        }
    }

    public ElmSession(ElmTransport elmTransport) {
        this.transport = elmTransport;
    }

    public boolean isCan() {
        return this.can;
    }

    public boolean isConnected() {
        return this.connected && !this.closed;
    }

    public String protocol() {
        return this.protocol;
    }

    public String identity() {
        return this.identity;
    }

    public Set<Integer> supported() {
        return Collections.unmodifiableSet(this.supported);
    }

    public void connect() throws IOException {
        try {
            if (this.closed) {
                throw new IOException("Conexão cancelada");
            }
            this.transport.connect();
            if (this.closed) {
                throw new IOException("Conexão cancelada");
            }
            this.input = this.transport.input();
            this.output = this.transport.output();
            long nanoTime = System.nanoTime() + 200000000;
            while (System.nanoTime() < nanoTime) {
                while (this.input.available() > 0) {
                    this.input.read();
                }
                sleep(10L);
            }
            if (command("ATZ", 6000L).trim().isEmpty()) {
                throw new IOException("O adaptador não respondeu à inicialização");
            }
            String[] parts = {"ATE0", "ATL0", "ATS0", "ATH0", "ATCAF1", "ATSP0"};
            for (int i = 0; i < 6; i++) {
                requireOk(command(parts[i], 3000L));
            }
            this.identity = command("ATI", 3000L).trim();
            String command = command("0100", DashboardState.MAX_AGE_MS);
            boolean flag = false;
            for (byte[] message : ObdParser.messages(command)) {
                if (message.length >= 6 && message[0] == 65 && message[1] == 0) {
                    flag = true;
                }
            }
            if (!flag) {
                throw new IOException("O adaptador respondeu, mas a ECU não respondeu ao mapa de suporte OBD2. Verifique a ignição e a compatibilidade.");
            }
            String upperCase = command("ATDPN", 3000L).replaceAll("[\\r\\n> ]", "").toUpperCase(Locale.ROOT);
            if (upperCase.startsWith("A")) {
                upperCase = upperCase.substring(1);
            }
            if (!upperCase.matches("[1-9]")) {
                throw new IOException("Protocolo OBD não admitido nesta versão: " + upperCase);
            }
            int value = Integer.parseInt(upperCase, 16);
            this.can = value >= 6 && value <= 9;
            this.protocol = command("ATDP", 3000L).trim();
            this.supported.addAll(ObdParser.supportedPids(command, 0));
            for (int i2 = 32; i2 <= 96 && this.supported.contains(Integer.valueOf(i2)); i2 += 32) {
                this.supported.addAll(ObdParser.supportedPids(command(String.format(Locale.ROOT, "01%02X", Integer.valueOf(i2)), 6000L), i2));
            }
            this.connected = true;
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    private void requireOk(String command) throws IOException {
        for (String text : command.split("[\\r\\n]+")) {
            if (text.trim().equalsIgnoreCase("OK")) {
                return;
            }
        }
        throw new IOException("O adaptador não confirmou a configuração: " + command.trim());
    }

    public String command(String text, long value) throws IOException {
        try {
            return transact(text, value);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private String transact(String text, long value) throws IOException {
        if (this.closed || this.input == null || this.output == null) {
            throw new IOException("Conexão encerrada");
        }
        if (text.indexOf(13) >= 0 || text.indexOf(10) >= 0 || text.length() > 32) {
            throw new IOException("Comando inválido");
        }
        this.output.write((text + "\r").getBytes(StandardCharsets.US_ASCII));
        this.output.flush();
        long nanoTime = System.nanoTime() + (value * 1000000);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (System.nanoTime() < nanoTime) {
            if (this.closed) {
                throw new IOException("Conexão encerrada");
            }
            if (this.input.available() > 0) {
                int i = this.input.read();
                if (i < 0) {
                    throw new EOFException("Adaptador desconectado");
                }
                if (i == 62) {
                    String text2 = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.US_ASCII);
                    String upperCase = text2.toUpperCase(Locale.ROOT);
                    if (!upperCase.contains("UNABLE TO CONNECT") && !upperCase.contains("CAN ERROR") && !upperCase.contains("BUS ERROR") && !upperCase.contains("STOPPED") && !upperCase.contains("BUFFER FULL") && !upperCase.contains("?")) {
                        return text2;
                    }
                    throw new IOException("Erro do adaptador: " + text2.trim());
                }
                if (byteArrayOutputStream.size() >= 32768) {
                    close();
                    throw new IOException("Resposta excedeu o limite de 32 KiB");
                }
                byteArrayOutputStream.write(i);
            } else {
                sleep(10L);
            }
        }
        close();
        throw new ResponseTimeoutException("Tempo de resposta esgotado; a sessão foi encerrada para evitar misturar respostas.");
    }

    private void sleep(long value) throws IOException {
        try {
            Thread.sleep(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            throw new IOException("Operação interrompida", e);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.connected = false;
        try {
            this.transport.close();
        } catch (IOException ignored) {
        }
    }
}