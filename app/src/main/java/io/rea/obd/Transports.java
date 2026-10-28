package io.rea.obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Build;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class Transports {
    private Transports() {
    }

    public static final class Classic implements ElmTransport {
        private final BluetoothAdapter adapter;
        private volatile boolean closed;
        private final BluetoothDevice device;
        private volatile BluetoothSocket socket;

        public Classic(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter) {
            this.device = bluetoothDevice;
            this.adapter = bluetoothAdapter;
        }

        @Override
        public void connect() throws IOException {
            if (this.closed) {
                throw new IOException("Conexão cancelada");
            }
            BluetoothSocket rfcommSocketToServiceRecord = this.device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            this.socket = rfcommSocketToServiceRecord;
            if (this.closed) {
                rfcommSocketToServiceRecord.close();
                throw new IOException("Conexão cancelada");
            }
            try {
                this.adapter.cancelDiscovery();
            } catch (SecurityException ignored) {
            }
            rfcommSocketToServiceRecord.connect();
        }

        @Override
        public InputStream input() throws IOException {
            return this.socket.getInputStream();
        }

        @Override
        public OutputStream output() throws IOException {
            return this.socket.getOutputStream();
        }

        @Override
        public String description() {
            return "Bluetooth clássico SPP";
        }

        @Override
        public void close() {
            this.closed = true;
            BluetoothSocket bluetoothSocket = this.socket;
            this.socket = null;
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static final class Wifi implements ElmTransport {
        private volatile boolean closed;
        private final String host;
        private final int port;
        private volatile Socket socket;

        public Wifi(String host, int i) {
            this.host = host;
            this.port = i;
        }

        @Override
        public void connect() throws IOException {
            if (this.closed) {
                throw new IOException("Conexão cancelada");
            }
            Socket socket = new Socket();
            this.socket = socket;
            if (this.closed) {
                socket.close();
                throw new IOException("Conexão cancelada");
            }
            socket.connect(new InetSocketAddress(this.host, this.port), 10000);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(12000);
        }

        @Override
        public InputStream input() throws IOException {
            return this.socket.getInputStream();
        }

        @Override
        public OutputStream output() throws IOException {
            return this.socket.getOutputStream();
        }

        @Override
        public String description() {
            return "Wi-Fi TCP " + this.host + ":" + this.port;
        }

        @Override
        public void close() {
            this.closed = true;
            Socket socket = this.socket;
            this.socket = null;
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static final class Ble implements ElmTransport {
        private static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        private volatile boolean closed;
        private final Context context;
        private final BluetoothDevice device;
        private volatile IOException failure;
        private volatile BluetoothGatt gatt;
        private BluetoothGattCharacteristic mode;
        private int offset;
        private BluetoothGattCharacteristic rx;
        private BluetoothGattCharacteristic tx;
        private volatile CountDownLatch writeDone;
        private int writeType;
        private final CountDownLatch ready = new CountDownLatch(1);
        private String profile = "";
        private final LinkedBlockingQueue<byte[]> incoming = new LinkedBlockingQueue<>(256);
        private byte[] current = new byte[0];
        private final BluetoothGattCallback callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int i, int value) {
                Ble ble;
                String text;
                if (Ble.this.closed) {
                    return;
                }
                if (i != 0) {
                    Ble.this.fail("Erro BLE GATT " + i);
                    return;
                }
                if (value == 2) {
                    if (bluetoothGatt.discoverServices()) {
                        return;
                    }
                    ble = Ble.this;
                    text = "Não foi possível descobrir os serviços BLE";
                } else {
                    if (value != 0) {
                        return;
                    }
                    ble = Ble.this;
                    text = "Adaptador BLE desconectado";
                }
                ble.fail(text);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int i) {
                if (Ble.this.closed) {
                    return;
                }
                if (i != 0) {
                    Ble.this.fail("Falha ao descobrir serviços BLE");
                    return;
                }
                if (Ble.this.profile(bluetoothGatt, UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"), UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"), "Nordic UART") || Ble.this.profile(bluetoothGatt, Ble.shortId("ffe0"), Ble.shortId("ffe1"), Ble.shortId("ffe1"), "FFE0/FFE1") || Ble.this.profile(bluetoothGatt, Ble.shortId("fff0"), Ble.shortId("fff2"), Ble.shortId("fff1"), "FFF0/FFF1/FFF2") || Ble.this.profile(bluetoothGatt, UUID.fromString("e47c8027-cca1-4e3b-981f-bdc47abeb5b5"), UUID.fromString("1cce1ea8-bd34-4813-a00a-c76e028fadcb"), UUID.fromString("cacc07ff-ffff-4c48-8fae-a9ef71b75e26"), "Kiwi 3")) {
                    if (Ble.this.profile.equals("Kiwi 3")) {
                        Ble.this.mode = bluetoothGatt.getService(UUID.fromString("e47c8027-cca1-4e3b-981f-bdc47abeb5b5")).getCharacteristic(UUID.fromString("20b9794f-da1a-4d14-8014-a0fb9cefb2f7"));
                        if (Ble.this.mode == null) {
                            Ble.this.fail("Característica de modo Kiwi 3 ausente");
                            return;
                        }
                    }
                    boolean flag = true;
                    if (!bluetoothGatt.setCharacteristicNotification(Ble.this.rx, true)) {
                        Ble.this.fail("O adaptador recusou notificações BLE");
                        return;
                    }
                    BluetoothGattDescriptor descriptor = Ble.this.rx.getDescriptor(Ble.CCC);
                    if (descriptor == null) {
                        Ble.this.fail("Descritor de notificações BLE ausente");
                        return;
                    }
                    byte[] bytes = (Ble.this.rx.getProperties() & 16) != 0 ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                    if (Build.VERSION.SDK_INT < 33) {
                        descriptor.setValue(bytes);
                        flag = bluetoothGatt.writeDescriptor(descriptor);
                    } else if (bluetoothGatt.writeDescriptor(descriptor, bytes) != BluetoothStatusCodes.SUCCESS) {
                        flag = false;
                    }
                    if (flag) {
                        return;
                    }
                    Ble.this.fail("Não foi possível habilitar as notificações BLE");
                    return;
                }
                Ble.this.fail("Nenhum perfil UART BLE compatível foi encontrado. Este modelo pode exigir integração específica.");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor bluetoothGattDescriptor, int i) {
                if (Ble.CCC.equals(bluetoothGattDescriptor.getUuid())) {
                    if (i != 0) {
                        Ble.this.fail("Notificações BLE recusadas: " + i);
                        return;
                    }
                    if (Ble.this.mode == null) {
                        Ble.this.ready.countDown();
                        return;
                    }
                    boolean flag2 = false;
                    if (Build.VERSION.SDK_INT < 33) {
                        Ble.this.mode.setWriteType(2);
                        Ble.this.mode.setValue(new byte[]{1});
                        flag2 = bluetoothGatt.writeCharacteristic(Ble.this.mode);
                    } else if (bluetoothGatt.writeCharacteristic(Ble.this.mode, new byte[]{1}, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS) {
                        flag2 = true;
                    }
                    if (flag2) {
                        return;
                    }
                    Ble.this.fail("Não foi possível iniciar o modo UART Kiwi 3");
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                if (Build.VERSION.SDK_INT < 33) {
                    Ble.this.receive(bluetoothGattCharacteristic, bluetoothGattCharacteristic.getValue());
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] bytes) {
                Ble.this.receive(bluetoothGattCharacteristic, bytes);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
                if (i != 0) {
                    Ble.this.fail("Falha na escrita UART BLE: " + i);
                }
                if (bluetoothGattCharacteristic == Ble.this.mode) {
                    Ble.this.ready.countDown();
                    return;
                }
                CountDownLatch countDownLatch = Ble.this.writeDone;
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }
        };

        public static UUID shortId(String text) {
            return UUID.fromString("0000" + text + "-0000-1000-8000-00805f9b34fb");
        }

        public Ble(Context context, BluetoothDevice bluetoothDevice) {
            this.context = context.getApplicationContext();
            this.device = bluetoothDevice;
        }

        @Override
        public String description() {
            return "Bluetooth LE UART " + this.profile;
        }

        @Override
        public void connect() throws IOException {
            if (this.closed) {
                throw new IOException("Conexão cancelada");
            }
            this.gatt = this.device.connectGatt(this.context, false, this.callback, 2);
            if (this.closed) {
                close();
                throw new IOException("Conexão cancelada");
            }
            try {
                if (!this.ready.await(20L, TimeUnit.SECONDS)) {
                    throw new IOException("O dispositivo BLE não concluiu a descoberta UART");
                }
                check();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Conexão interrompida", e);
            }
        }

        public void check() throws IOException {
            if (this.failure != null) {
                throw this.failure;
            }
            if (this.closed) {
                throw new IOException("Conexão BLE encerrada");
            }
        }

        public void fail(String text) {
            this.failure = new IOException(text);
            this.ready.countDown();
            CountDownLatch countDownLatch = this.writeDone;
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        public boolean profile(BluetoothGatt bluetoothGatt, UUID uuid, UUID shortId, UUID shortId2, String profile) {
            BluetoothGattService service = bluetoothGatt.getService(uuid);
            boolean flag = false;
            if (service == null) {
                return false;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(shortId);
            BluetoothGattCharacteristic characteristic2 = service.getCharacteristic(shortId2);
            if (characteristic != null && characteristic2 != null) {
                int properties = characteristic.getProperties() & 12;
                int properties2 = characteristic2.getProperties() & 48;
                if (properties != 0 && properties2 != 0) {
                    this.tx = characteristic;
                    this.rx = characteristic2;
                    this.profile = profile;
                    flag = true;
                    this.writeType = (characteristic.getProperties() & 8) != 0 ? 2 : 1;
                }
            }
            return flag;
        }

        public void receive(BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] bytes) {
            BluetoothGattCharacteristic bluetoothGattCharacteristic2;
            if (this.closed || (bluetoothGattCharacteristic2 = this.rx) == null || !bluetoothGattCharacteristic2.getUuid().equals(bluetoothGattCharacteristic.getUuid()) || bytes == null || bytes.length == 0 || this.incoming.offer(Arrays.copyOf(bytes, bytes.length))) {
                return;
            }
            fail("Buffer BLE excedido; leitura não é completa");
        }

        @Override
        public InputStream input() {
            return new InputStream() {
                @Override
                public int available() throws IOException {
                    Ble.this.check();
                    if (Ble.this.offset < Ble.this.current.length) {
                        return Ble.this.current.length - Ble.this.offset;
                    }
                    if (Ble.this.incoming.isEmpty()) {
                        return 0;
                    }
                    return ((byte[]) Ble.this.incoming.peek()).length;
                }

                @Override
                public int read() throws IOException {
                    Ble.this.check();
                    if (Ble.this.offset >= Ble.this.current.length) {
                        Ble ble = Ble.this;
                        ble.current = (byte[]) ble.incoming.poll();
                        Ble.this.offset = 0;
                        if (Ble.this.current == null) {
                            Ble.this.current = new byte[0];
                            return -1;
                        }
                    }
                    return Ble.this.current[Ble.this.offset++] & 255;
                }
            };
        }

        @Override
        public OutputStream output() {
            return new OutputStream() {
                @Override
                public void write(int i) throws IOException {
                    write(new byte[]{(byte) i}, 0, 1);
                }

                @Override
                public void write(byte[] chunk, int i, int value2) throws IOException {
                    Ble.this.check();
                    int value = 0;
                    while (value < value2) {
                        int value3 = i + value;
                        value += 20;
                        byte[] bArrCopyOfRange = Arrays.copyOfRange(chunk, value3, Math.min(value2, value) + i);
                        boolean flag = true;
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        Ble.this.writeDone = countDownLatch;
                        if (Build.VERSION.SDK_INT >= 33) {
                            if (Ble.this.gatt.writeCharacteristic(Ble.this.tx, bArrCopyOfRange, Ble.this.writeType) != BluetoothStatusCodes.SUCCESS) {
                                flag = false;
                            }
                        } else {
                            Ble.this.tx.setWriteType(Ble.this.writeType);
                            Ble.this.tx.setValue(bArrCopyOfRange);
                            flag = Ble.this.gatt.writeCharacteristic(Ble.this.tx);
                        }
                        if (!flag) {
                            throw new IOException("UART BLE ocupada ou indisponível");
                        }
                        try {
                            if (!countDownLatch.await(4L, TimeUnit.SECONDS)) {
                                throw new IOException("Escrita BLE sem confirmação");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Escrita interrompida", e);
                        } finally {
                            Ble.this.writeDone = null;
                        }
                        Ble.this.check();
                    }
                }
            };
        }

        @Override
        public void close() {
            this.closed = true;
            this.ready.countDown();
            CountDownLatch countDownLatch = this.writeDone;
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
            BluetoothGatt bluetoothGatt = this.gatt;
            this.gatt = null;
            if (bluetoothGatt != null) {
                try {
                    bluetoothGatt.disconnect();
                } catch (SecurityException ignored) {
                }
                bluetoothGatt.close();
            }
            this.incoming.clear();
        }
    }
}