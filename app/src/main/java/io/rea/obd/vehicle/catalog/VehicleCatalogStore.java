package io.rea.obd.vehicle.catalog;

import android.content.Context;
import android.util.AtomicFile;
import io.rea.obd.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

public final class VehicleCatalogStore {
    private final Context context;
    private final AtomicFile current;

    public static final class Loaded {
        public final VehicleCatalog catalog;
        public final String warning;

        Loaded(VehicleCatalog vehicleCatalog, String warning) {
            this.catalog = vehicleCatalog;
            this.warning = warning;
        }
    }

    public VehicleCatalogStore(Context context) {
        this(context, new File(context.getFilesDir(), "vehicle-catalog.json"));
    }

    public VehicleCatalogStore(Context context, File file) {
        this.context = context.getApplicationContext();
        this.current = new AtomicFile(file);
    }

    public synchronized Loaded load() throws Exception {
        InputStream inputStream = this.context.getResources().openRawResource(R.raw.vehicle_catalog);
        try {
            VehicleCatalog vehicleCatalog = VehicleCatalogJson.read(inputStream).catalog;
            if (inputStream != null) {
                inputStream.close();
            }
            if (!this.current.getBaseFile().exists()) {
                if (!new File(this.current.getBaseFile() + ".bak").exists()) {
                    return new Loaded(vehicleCatalog, "");
                }
            }
            try {
                FileInputStream fileInputStream = this.current.openRead();
                try {
                    VehicleCatalog catalog = VehicleCatalogJson.read(fileInputStream).catalog;
                    if (catalog.sequence < vehicleCatalog.sequence) {
                        Loaded loaded = new Loaded(vehicleCatalog, "");
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        return loaded;
                    }
                    Loaded loaded2 = new Loaded(catalog, "");
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    return loaded2;
                } catch (Throwable throwable) {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
            } catch (Exception e) {
                return new Loaded(vehicleCatalog, "Catálogo local inválido preservado; usando a base embarcada: " + e.getMessage());
            }
        } catch (Throwable throwable) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable throwable3) {
                    throwable.addSuppressed(throwable3);
                }
            }
            throw throwable;
        }
    }

    public synchronized void install(InputStream inputStream, String text) throws Exception {
        VehicleCatalogJson.Parsed parsed = VehicleCatalogJson.read(inputStream);
        String text2 = hex(MessageDigest.getInstance("SHA-256").digest(parsed.bytes()));
        if (text == null || !text.matches("[0-9a-f]{64}") || !text2.equals(text)) {
            throw new IOException("Integridade do pacote de catálogo inválida");
        }
        Loaded loaded = load();
        if (!loaded.warning.isEmpty()) {
            throw new IOException("Catálogo instalado precisa de revisão; original preservado");
        }
        if (parsed.catalog.sequence <= loaded.catalog.sequence || parsed.catalog.version.equals(loaded.catalog.version)) {
            throw new IOException("Atualização não avança versão e sequência");
        }
        File parentFile = this.current.getBaseFile().getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Diretório do catálogo indisponível");
        }
        if (this.current.getBaseFile().exists()) {
            byte[] bArrBytes;
            try (FileInputStream fileInputStream2 = this.current.openRead()) {
                bArrBytes = VehicleCatalogJson.read(fileInputStream2).bytes();
            }
            String text3 = hex(MessageDigest.getInstance("SHA-256").digest(bArrBytes));
            File file = new File(parentFile, "vehicle-catalog-" + loaded.catalog.sequence + "-" + text3 + ".json");
            if (file.exists()) {
                byte[] bArrBytes2;
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    bArrBytes2 = VehicleCatalogJson.read(fileInputStream).bytes();
                }
                if (!hex(MessageDigest.getInstance("SHA-256").digest(bArrBytes2)).equals(text3)) {
                    throw new IOException("Arquivo histórico de catálogo divergente; atualização bloqueada");
                }
            } else {
                AtomicFile atomicFile = new AtomicFile(file);
                FileOutputStream startWrite = null;
                try {
                    startWrite = atomicFile.startWrite();
                    startWrite.write(bArrBytes);
                    atomicFile.finishWrite(startWrite);
                } catch (Exception e) {
                    if (startWrite != null) {
                        atomicFile.failWrite(startWrite);
                    }
                    throw e;
                }
            }
        }
        FileOutputStream startWrite2 = null;
        try {
            startWrite2 = this.current.startWrite();
            startWrite2.write(parsed.bytes());
            this.current.finishWrite(startWrite2);
        } catch (Exception e) {
            if (startWrite2 != null) {
                this.current.failWrite(startWrite2);
            }
            throw e;
        }
        try (FileInputStream fileInputStream3 = this.current.openRead()) {
            if (!hex(MessageDigest.getInstance("SHA-256").digest(VehicleCatalogJson.read(fileInputStream3).bytes())).equals(text2)) {
                throw new IOException("Catálogo não confirmado após gravação; versão anterior preservada no arquivo histórico");
            }
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
        }
        return builder.toString();
    }
}