package io.rea.obd.ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class Format {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Format() {
    }

    public static String when(long epochMillis) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime time = Instant.ofEpochMilli(epochMillis).atZone(zone);
        LocalDate day = time.toLocalDate();
        LocalDate today = LocalDate.now(zone);
        String clock = time.format(TIME);
        if (day.equals(today)) {
            return "hoje às " + clock;
        }
        if (day.equals(today.minusDays(1))) {
            return "ontem às " + clock;
        }
        return day.format(day.getYear() == today.getYear() ? DAY : FULL_DAY) + " às " + clock;
    }

    public static String clock(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIME_SECONDS);
    }

    public static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    public static String km(java.math.BigDecimal dueOdometer) {
        java.text.NumberFormat format = java.text.NumberFormat.getIntegerInstance(I18n.locale());
        return format.format(dueOdometer) + " km";
    }

    public static String decimal(java.math.BigDecimal bigDecimal) {
        return I18n.english() ? bigDecimal.toPlainString() : bigDecimal.toPlainString().replace('.', ',');
    }

    public static String money(java.math.BigDecimal bigDecimal) {
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(I18n.locale());
        format.setCurrency(java.util.Currency.getInstance("BRL"));
        return format.format(bigDecimal).replace(' ', ' ');
    }
}
