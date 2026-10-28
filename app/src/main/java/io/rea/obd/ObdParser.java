package io.rea.obd;

import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.SupplementalFormatter;
import io.rea.obd.diagnostics.SupplementalReadResult;
import io.rea.obd.ui.NavigationState;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ObdParser {
    private static final Pattern NUMBERED = Pattern.compile("^([0-9A-F]):\\s*(.*)$");

    private ObdParser() {
    }

    public static List<byte[]> messages(String command) {
        return messages(command, false);
    }

    private static List<byte[]> messages(String command, boolean flag) {
        if (command == null) {
            throw new IllegalArgumentException("Resposta ausente");
        }
        ArrayList items = new ArrayList();
        int i = -1;
        ByteArrayOutputStream byteArrayOutputStream = null;
        int value = 0;
        for (String text : command.replace('>', '\n').split("[\\r\\n]+")) {
            String upperCase = text.trim().toUpperCase(Locale.ROOT);
            if (!upperCase.isEmpty()) {
                if (upperCase.contains("BUFFER FULL") || upperCase.contains("DATA ERROR") || upperCase.contains("CAN ERROR") || upperCase.contains("BUS ERROR") || upperCase.equals("STOPPED")) {
                    throw new IllegalArgumentException("Falha de comunicação do adaptador: " + upperCase);
                }
                if (upperCase.matches("[0-9A-F]{3}")) {
                    if (byteArrayOutputStream != null) {
                        finish(items, byteArrayOutputStream, i, flag);
                        byteArrayOutputStream = null;
                    } else if (i >= 0) {
                        throw new IllegalArgumentException("Resposta sem dados");
                    }
                    i = Integer.parseInt(upperCase, 16);
                    if (i == 0) {
                        throw new IllegalArgumentException("Comprimento inválido");
                    }
                    value = 0;
                } else {
                    Matcher matcher = NUMBERED.matcher(upperCase);
                    if (matcher.matches()) {
                        int group = Integer.parseInt(matcher.group(1), 16);
                        if (i < 0 || group != value) {
                            throw new IllegalArgumentException("Sequência de resposta incompleta");
                        }
                        if (byteArrayOutputStream == null) {
                            byteArrayOutputStream = new ByteArrayOutputStream();
                        }
                        byte[] bArrHex = hex(matcher.group(2));
                        if (bArrHex.length == 0) {
                            throw new IllegalArgumentException("Quadro vazio");
                        }
                        byteArrayOutputStream.write(bArrHex, 0, bArrHex.length);
                        value = (value + 1) & 15;
                    } else if (upperCase.matches("[0-9A-F\\s]+")) {
                        String text2 = upperCase.replaceAll("\\s", "");
                        if (text2.length() % 2 != 0) {
                            if (text2.startsWith("4") || text2.startsWith("7F")) {
                                throw new IllegalArgumentException("Resposta hexadecimal incompleta");
                            }
                        } else {
                            byte[] bArrHex2 = hex(upperCase);
                            if (bArrHex2.length > 0) {
                                byte b = bArrHex2[0];
                                if ((b & 255) >= 64 && (b & 255) <= 127) {
                                    if (byteArrayOutputStream != null) {
                                        finish(items, byteArrayOutputStream, i, flag);
                                        i = -1;
                                        byteArrayOutputStream = null;
                                    } else if (i >= 0) {
                                        throw new IllegalArgumentException("Quadros numerados ausentes");
                                    }
                                    append(items, bArrHex2, flag);
                                }
                            } else {
                                continue;
                            }
                        }
                    } else if (upperCase.matches("^(?:4[0-9A-F]|7F)[0-9A-F\\s]*[^0-9A-F\\s].*")) {
                        throw new IllegalArgumentException("Resposta hexadecimal inválida");
                    }
                }
            }
        }
        if (byteArrayOutputStream != null) {
            finish(items, byteArrayOutputStream, i, flag);
        } else if (i >= 0) {
            throw new IllegalArgumentException("Resposta truncada");
        }
        return items;
    }

    private static void append(List<byte[]> list, byte[] id, boolean flag) {
        if (id.length != 0) {
            byte b = id[0];
            if ((b & 255) >= 64 && (b & 255) <= 127) {
                if ((b & 255) == 127 && !flag) {
                    throw new IllegalArgumentException("Resposta negativa da ECU");
                }
                list.add(id);
                return;
            }
        }
        throw new IllegalArgumentException("Serviço de resposta inválido");
    }

    private static void finish(List<byte[]> list, ByteArrayOutputStream byteArrayOutputStream, int i, boolean flag) {
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        if (i < 0 || byteArray.length < i) {
            throw new IllegalArgumentException("Resposta truncada");
        }
        append(list, Arrays.copyOf(byteArray, i), flag);
    }

    private static byte[] hex(String text) {
        String text2 = text.replaceAll("\\s", "");
        if (!text2.matches("(?:[0-9A-F]{2})*")) {
            throw new IllegalArgumentException("Resposta hexadecimal inválida");
        }
        int length = text2.length() / 2;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int value = i * 2;
            bytes[i] = (byte) Integer.parseInt(text2.substring(value, value + 2), 16);
        }
        return bytes;
    }

    private static void require(byte[] bytes, int i) {
        if (bytes.length < i) {
            throw new IllegalArgumentException("Resposta/PID incompleto");
        }
    }

    private static boolean matchesPid(byte[] bytes, int i, int pid) {
        if (bytes.length == 0 || (bytes[0] & 255) != i) {
            return false;
        }
        require(bytes, 2);
        return (bytes[1] & 255) == pid;
    }

    public static Double pid(String command, int i) {
        return signal(command, 65, i, false);
    }

    public static Double freezePid(String command, int i) {
        return signal(command, 66, i, true);
    }

    private static Double signal(String command, int i, int value, boolean flag) {
        return signal(messages(command), i, value, flag);
    }

    private static Double signal(List<byte[]> list, int i, int pid, boolean flag) {
        if (SensorCatalog.get(pid) == null) {
            return null;
        }
        for (byte[] bytes : list) {
            if (matchesPid(bytes, i, pid)) {
                int value = flag ? 3 : 2;
                if (flag) {
                    require(bytes, 3);
                    if (bytes[2] != 0) {
                        continue;
                    }
                }
                boolean flag2 = pid == 12 || pid == 16 || pid == 31 || pid == 33 || pid == 49 || pid == 66 || pid == 94;
                require(bytes, (flag2 ? 2 : 1) + value);
                int a = bytes[value] & 255;
                int ab = flag2 ? (a * 256) + (bytes[value + 1] & 255) : a;
                return formula(pid, a, ab);
            }
        }
        return null;
    }

    private static Double formula(int pid, int a, int ab) {
        switch (pid) {
            case 0x04:
            case 0x11:
            case 0x2F:
                return Double.valueOf((((double) a) * 100.0d) / 255.0d);
            case 0x05:
            case 0x0F:
            case 0x46:
            case 0x5C:
                return Double.valueOf(((double) a) - 40.0d);
            case 0x06:
            case 0x07:
            case 0x08:
            case 0x09:
                return Double.valueOf((((double) (a - 128)) * 100.0d) / 128.0d);
            case 0x0A:
                return Double.valueOf(((double) a) * 3.0d);
            case 0x0B:
            case 0x0D:
            case 0x33:
                return Double.valueOf(a);
            case 0x0C:
                return Double.valueOf(((double) ab) / 4.0d);
            case 0x0E:
                return Double.valueOf((((double) a) / 2.0d) - 64.0d);
            case 0x10:
                return Double.valueOf(((double) ab) / 100.0d);
            case 0x1F:
            case 0x21:
            case 0x31:
                return Double.valueOf(ab);
            case 0x42:
                return Double.valueOf(((double) ab) / 1000.0d);
            case 0x5E:
                return Double.valueOf(((double) ab) / 20.0d);
            default:
                return null;
        }
    }

    public static Set<Integer> supportedPids(String command, int i) {
        if (i < 0 || i > 224 || i % 32 != 0) {
            throw new IllegalArgumentException("Página PID inválida");
        }
        LinkedHashSet seen = new LinkedHashSet();
        for (byte[] message : messages(command)) {
            if (matchesPid(message, 65, i)) {
                require(message, 6);
                for (int i2 = 0; i2 < 32; i2++) {
                    if ((message[(i2 / 8) + 2] & 255 & (128 >> (i2 % 8))) != 0) {
                        seen.add(Integer.valueOf(i + i2 + 1));
                    }
                }
            }
        }
        return seen;
    }

    public static Double voltage(String command) {
        if (command == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?im)^\\s*([0-9]{1,2}(?:\\.[0-9]+)?)V\\s*$").matcher(command.replace(">", ""));
        if (matcher.find()) {
            return Double.valueOf(matcher.group(1));
        }
        return null;
    }

    public static Integer milAndCount(String command) {
        for (byte[] message : messages(command)) {
            if (matchesPid(message, 65, 1)) {
                require(message, 6);
                return Integer.valueOf(message[2] & 255);
            }
        }
        return null;
    }

    public static List<String> readiness(String command) {
        ArrayList items = new ArrayList();
        for (byte[] message : messages(command)) {
            if (matchesPid(message, 65, 1)) {
                require(message, 6);
                items.add(readinessBits(message));
            }
        }
        return SupplementalFormatter.readinessLines(items);
    }

    private static SupplementalReadResult.ReadinessBits readinessBits(byte[] bytes) {
        return new SupplementalReadResult.ReadinessBits(bytes[2] & 255, bytes[3] & 255, bytes[4] & 255, bytes[5] & 255);
    }

    public static String vehicleInfo(String command, int i) {
        if (i != 2 && i != 4) {
            return null;
        }
        LinkedHashSet seen = new LinkedHashSet();
        ByteArrayOutputStream byteArrayOutputStream = null;
        int value = 1;
        for (byte[] message : messages(command)) {
            if (matchesPid(message, 73, i)) {
                require(message, 4);
                int value2 = message[2] & 255;
                int value3 = i == 2 ? 17 : 16;
                if (message.length <= 7) {
                    if (message.length != 7) {
                        throw new IllegalArgumentException("Fragmento de informação truncado");
                    }
                    if (value2 == 1) {
                        if (byteArrayOutputStream != null) {
                            finishLegacy(seen, byteArrayOutputStream, i);
                        }
                        byteArrayOutputStream = new ByteArrayOutputStream();
                        value = 1;
                    }
                    if (byteArrayOutputStream != null) {
                        int value4 = value + 1;
                        if (value2 == value) {
                            byteArrayOutputStream.write(message, 3, 4);
                            value = value4;
                        }
                    }
                    throw new IllegalArgumentException("Sequência de informação incompleta");
                }
                if (byteArrayOutputStream != null) {
                    throw new IllegalArgumentException("Informação do veículo incompleta");
                }
                if (value2 == 0 || (i == 2 && value2 != 1)) {
                    throw new IllegalArgumentException("Contagem de registros inválida");
                }
                require(message, (value2 * value3) + 3);
                int value5 = 0;
                while (value5 < value2) {
                    int value6 = (value5 * value3) + 3;
                    value5++;
                    addRecord(seen, Arrays.copyOfRange(message, value6, (value5 * value3) + 3), i);
                }
            }
        }
        if (byteArrayOutputStream != null) {
            finishLegacy(seen, byteArrayOutputStream, i);
        }
        if (seen.isEmpty()) {
            return null;
        }
        return String.join("\n", seen);
    }

    private static void finishLegacy(Set<String> set, ByteArrayOutputStream byteArrayOutputStream, int i) {
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        int value = 0;
        if (i == 2) {
            if (byteArray.length != 20 || byteArray[0] != 0 || byteArray[1] != 0 || byteArray[2] != 0) {
                throw new IllegalArgumentException("VIN legado incompleto");
            }
            addRecord(set, Arrays.copyOfRange(byteArray, 3, 20), i);
            return;
        }
        if (byteArray.length == 0 || byteArray.length % 16 != 0) {
            throw new IllegalArgumentException("Calibração incompleta");
        }
        while (value < byteArray.length) {
            int value2 = value + 16;
            addRecord(set, Arrays.copyOfRange(byteArray, value, value2), i);
            value = value2;
        }
    }

    private static void addRecord(Set<String> set, byte[] bytes, int i) {
        StringBuilder builder = new StringBuilder();
        boolean flag = false;
        for (byte b : bytes) {
            int value = b & 255;
            if (value == 0 && i == 4) {
                flag = true;
            } else {
                if (flag || value < 32 || value > 126) {
                    throw new IllegalArgumentException("Texto do veículo inválido");
                }
                builder.append((char) value);
            }
        }
        String id = builder.toString().trim();
        if (i == 2 && !id.matches("[A-HJ-NPR-Z0-9]{17}")) {
            throw new IllegalArgumentException("VIN inválido");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Identificador vazio");
        }
        set.add(id);
    }

    public static List<String> dtcs(String command, int i, boolean flag) {
        return dtcs(messages(command), i, flag);
    }

    private static List<String> dtcs(List<byte[]> list, int i, boolean flag) {
        if (i != 3 && i != 7 && i != 10) {
            throw new IllegalArgumentException("Serviço DTC inválido");
        }
        LinkedHashSet seen = new LinkedHashSet();
        for (byte[] bytes : list) {
            if (bytes.length != 0) {
                if ((bytes[0] & 255) != i + 64) {
                    continue;
                } else {
                    if (flag && bytes.length < 2) {
                        throw new IllegalArgumentException("Contagem CAN ausente");
                    }
                    if (!flag && bytes.length < 3) {
                        throw new IllegalArgumentException("DTC incompleto");
                    }
                    int value = flag ? 2 : 1;
                    int length = flag ? bytes[1] & 255 : (bytes.length - 1) / 2;
                    if (flag && bytes.length < (length * 2) + value) {
                        throw new IllegalArgumentException("DTCs CAN truncados");
                    }
                    if (!flag && (bytes.length - 1) % 2 != 0) {
                        throw new IllegalArgumentException("DTC incompleto");
                    }
                    for (int i2 = 0; i2 < length; i2++) {
                        int value2 = (i2 * 2) + value;
                        byte b = bytes[value2];
                        int value3 = b & 255;
                        int value4 = bytes[value2 + 1] & 255;
                        if (value3 != 0 || value4 != 0) {
                            seen.add("PCBU".charAt(value3 >> 6) + String.format(Locale.ROOT, "%01X%01X%02X", Integer.valueOf((value3 >> 4) & 3), Integer.valueOf(b & 15), Integer.valueOf(value4)));
                        }
                    }
                }
            }
        }
        return new ArrayList(seen);
    }

    public static final class DecodedDiagnostic<T> {
        public final String detail;
        public final List<DtcReadResult.NegativeResponse> negativeResponses;
        public final DtcReadResult.Outcome outcome;
        public final int positiveResponseCount;
        public final List<T> values;

        private DecodedDiagnostic(ExpectedResponse expectedResponse, List<T> list) {
            this.outcome = expectedResponse.outcome;
            this.values = Collections.unmodifiableList(new ArrayList(list));
            this.positiveResponseCount = expectedResponse.positives.size();
            this.negativeResponses = Collections.unmodifiableList(new ArrayList(expectedResponse.negatives));
            this.detail = expectedResponse.detail;
        }
    }

    private static final class ExpectedResponse {
        String detail;
        final List<DtcReadResult.NegativeResponse> negatives;
        DtcReadResult.Outcome outcome;
        final List<byte[]> positives;

        private ExpectedResponse() {
            this.positives = new ArrayList();
            this.negatives = new ArrayList();
            this.detail = "";
        }
    }

    private static ExpectedResponse expectedResponse(String command, int i) {
        ExpectedResponse expectedResponse = new ExpectedResponse();
        for (byte[] message : messages(command, true)) {
            byte b = message[0];
            if ((b & 255) == 127) {
                require(message, 3);
                if ((message[1] & 255) != i) {
                    throw new IllegalArgumentException("Resposta negativa de outro serviço");
                }
                expectedResponse.negatives.add(new DtcReadResult.NegativeResponse(i, message[2] & 255));
            } else {
                if ((b & 255) != i + 64) {
                    throw new IllegalArgumentException("Resposta positiva de outro serviço");
                }
                expectedResponse.positives.add(message);
            }
        }
        boolean flag = Pattern.compile("(?im)^\\s*NO DATA\\s*$").matcher(command.replace(">", "")).find();
        if (!expectedResponse.positives.isEmpty()) {
            expectedResponse.outcome = (!expectedResponse.negatives.isEmpty() || flag) ? DtcReadResult.Outcome.PARTIAL : DtcReadResult.Outcome.POSITIVE;
            if (expectedResponse.outcome == DtcReadResult.Outcome.PARTIAL) {
                expectedResponse.detail = "Respostas positivas acompanhadas de resposta negativa ou NO DATA";
            }
        } else if (!expectedResponse.negatives.isEmpty()) {
            expectedResponse.outcome = DtcReadResult.Outcome.NEGATIVE;
            if (flag) {
                expectedResponse.detail = "Resposta negativa acompanhada de NO DATA";
            }
        } else if (flag) {
            expectedResponse.outcome = DtcReadResult.Outcome.NO_DATA;
        } else {
            throw new IllegalArgumentException("Resposta sem confirmação do serviço ou NO DATA");
        }
        return expectedResponse;
    }

    public static DecodedDiagnostic<String> decodeDtcs(String command, int i, boolean can) {
        if (i != 3 && i != 7 && i != 10) {
            throw new IllegalArgumentException("Serviço DTC inválido");
        }
        ExpectedResponse expectedResponse = expectedResponse(command, i);
        return new DecodedDiagnostic<>(expectedResponse, dtcs(expectedResponse.positives, i, can));
    }

    public static DecodedDiagnostic<Integer> decodeMil(String command) {
        ExpectedResponse expectedResponse = expectedResponse(command, 1);
        ArrayList items = new ArrayList();
        for (byte[] positive : expectedResponse.positives) {
            require(positive, 6);
            if ((positive[1] & 255) != 1) {
                throw new IllegalArgumentException("Resposta MIL de outro PID");
            }
            items.add(Integer.valueOf(positive[2] & 255));
        }
        return new DecodedDiagnostic<>(expectedResponse, items);
    }

    public static DecodedDiagnostic<SupplementalReadResult.ReadinessBits> decodeReadiness(String command) {
        ExpectedResponse expectedResponse = expectedResponse(command, 1);
        ArrayList items = new ArrayList();
        for (byte[] positive : expectedResponse.positives) {
            require(positive, 6);
            if ((positive[1] & 255) != 1) {
                throw new IllegalArgumentException("Prontidão de outro PID");
            }
            items.add(readinessBits(positive));
        }
        return new DecodedDiagnostic<>(expectedResponse, items);
    }

    public static DecodedDiagnostic<Double> decodeFreezePid(String command, int i) {
        if (!SupplementalReadResult.FRAME_PIDS.contains(Integer.valueOf(i))) {
            throw new IllegalArgumentException("PID fora do escopo do quadro");
        }
        ExpectedResponse expectedResponse = expectedResponse(command, 2);
        ArrayList items = new ArrayList();
        for (byte[] positive : expectedResponse.positives) {
            require(positive, 3);
            if ((positive[1] & 255) != i || positive[2] != 0) {
                throw new IllegalArgumentException("Resposta de outro PID/quadro");
            }
            Double signal = signal((List<byte[]>) Collections.singletonList(positive), 66, i, true);
            if (signal == null) {
                throw new IllegalArgumentException("Quadro sem valor");
            }
            items.add(signal);
        }
        return new DecodedDiagnostic<>(expectedResponse, items);
    }

    public static boolean hasService(String command, int i) {
        for (byte[] message : messages(command)) {
            if (message.length > 0 && (message[0] & 255) == i + 64) {
                return true;
            }
        }
        return false;
    }
}