package io.rea.obd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FakeAdapter implements ElmTransport {
    static final String DISCONNECT = "<disconnect>";

    private final Map<String, String> replies = new HashMap<>();
    private final Deque<Integer> pending = new ArrayDeque<>();
    final List<String> sent = new ArrayList<>();
    boolean closed;

    static FakeAdapter canCar() {
        FakeAdapter adapter = new FakeAdapter();
        adapter.reply("ATZ", "ELM327 v1.5");
        for (String setting : new String[]{"ATE0", "ATL0", "ATS0", "ATH0", "ATCAF1", "ATSP0"}) {
            adapter.reply(setting, "OK");
        }
        adapter.reply("ATI", "ELM327 v1.5");
        adapter.reply("0100", "4100BE3FA810");
        adapter.reply("ATDPN", "A6");
        adapter.reply("ATDP", "AUTO, ISO 15765-4 (CAN 11/500)");
        return adapter;
    }

    FakeAdapter reply(String command, String response) {
        replies.put(command, response);
        return this;
    }

    @Override
    public void connect() {
    }

    @Override
    public String description() {
        return "adaptador simulado";
    }

    @Override
    public InputStream input() {
        return new InputStream() {
            @Override
            public int available() {
                synchronized (pending) {
                    return pending.size();
                }
            }

            @Override
            public int read() {
                synchronized (pending) {
                    Integer next = pending.poll();
                    return next == null ? -1 : next;
                }
            }
        };
    }

    @Override
    public OutputStream output() {
        return new OutputStream() {
            private final StringBuilder line = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                if (closed) {
                    throw new IOException("closed");
                }
                if (b != '\r') {
                    line.append((char) b);
                    return;
                }
                String command = line.toString();
                line.setLength(0);
                sent.add(command);
                String reply = replies.containsKey(command) ? replies.get(command) : "?";
                synchronized (pending) {
                    if (DISCONNECT.equals(reply)) {
                        pending.add(-1);
                        return;
                    }
                    for (byte byte2 : (reply + "\r\r>").getBytes(StandardCharsets.US_ASCII)) {
                        pending.add(byte2 & 0xFF);
                    }
                }
            }
        };
    }

    @Override
    public void close() {
        closed = true;
    }
}
