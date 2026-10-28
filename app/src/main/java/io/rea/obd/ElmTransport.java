package io.rea.obd;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ElmTransport extends Closeable {
    void connect() throws IOException;

    String description();

    InputStream input() throws IOException;

    OutputStream output() throws IOException;
}