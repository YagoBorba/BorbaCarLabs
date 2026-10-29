package io.rea.obd.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PersonalDataFilter {
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern CNPJ = Pattern.compile("(?<![\\w./-])\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}(?![\\w/-])");
    private static final Pattern CPF = Pattern.compile("(?<![\\w./-])\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?![\\w/-])");
    private static final Pattern PHONE = Pattern.compile("(?<![\\w+])(\\+?55[\\s-]?)?(\\(?\\d{2}\\)?[\\s-]?)?9?\\d{4}[\\s-]?\\d{4}(?!\\w)");
    private static final Pattern PLATE = Pattern.compile("(?<![\\p{L}\\d])([\\p{L}]{3})-?(\\d[\\p{L}\\d]\\d{2})(?![\\p{L}\\d])");
    private static final Set<String> UNITS = new HashSet<>(Arrays.asList("RPM", "KMH", "KPA", "PSI", "MPH"));

    private final Map<String, Integer> removed = new LinkedHashMap<>();

    public String apply(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String result = replace(EMAIL, text, "e-mail");
        result = replace(CNPJ, result, "CNPJ");
        result = replaceCpf(result);
        result = replace(PHONE, result, "telefone");
        Matcher matcher = PLATE.matcher(result);
        StringBuilder builder = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            if (UNITS.contains(matcher.group(1).toUpperCase(Locale.ROOT))) {
                continue;
            }
            builder.append(result, end, matcher.start()).append("[placa removida]");
            end = matcher.end();
            count("placa");
        }
        return builder.append(result.substring(end)).toString();
    }

    public boolean changed() {
        return !this.removed.isEmpty();
    }

    public String summary() {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : this.removed.entrySet()) {
            parts.add(entry.getValue() + " " + entry.getKey() + (entry.getValue() > 1 ? "s" : ""));
        }
        return String.join(", ", parts);
    }

    private String replace(Pattern pattern, String text, String kind) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            builder.append(text, end, matcher.start()).append("[").append(kind).append(" removido]");
            end = matcher.end();
            count(kind);
        }
        return builder.append(text.substring(end)).toString();
    }

    private String replaceCpf(String text) {
        Matcher matcher = CPF.matcher(text);
        StringBuilder builder = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("\\D", "");
            boolean formatted = digits.length() != matcher.group().length();
            if (!formatted && !validCpf(digits)) {
                continue;
            }
            builder.append(text, end, matcher.start()).append("[CPF removido]");
            end = matcher.end();
            count("CPF");
        }
        return builder.append(text.substring(end)).toString();
    }

    static boolean validCpf(String digits) {
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        for (int position = 9; position <= 10; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) {
                sum += (digits.charAt(i) - '0') * (position + 1 - i);
            }
            int check = sum * 10 % 11 % 10;
            if (check != digits.charAt(position) - '0') {
                return false;
            }
        }
        return true;
    }

    private void count(String kind) {
        this.removed.merge(kind, 1, Integer::sum);
    }
}
