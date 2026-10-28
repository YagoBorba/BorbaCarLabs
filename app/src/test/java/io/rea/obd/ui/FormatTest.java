package io.rea.obd.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.Test;

public class FormatTest {
    private static long at(LocalDate day, int hour, int minute) {
        return day.atTime(LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Test
    public void relativeDays() {
        LocalDate today = LocalDate.now();
        assertEquals("hoje às 14:32", Format.when(at(today, 14, 32)));
        assertEquals("ontem às 09:05", Format.when(at(today.minusDays(1), 9, 5)));
        assertTrue(Format.when(at(today.minusYears(2), 8, 0)).matches("\\d{2}/\\d{2}/\\d{4} às 08:00"));
    }

    @Test
    public void pluralsAndNumbers() {
        assertEquals("1 código", Format.count(1, "código", "códigos"));
        assertEquals("0 códigos", Format.count(0, "código", "códigos"));
        assertEquals("R$ 1.234,56", Format.money(new BigDecimal("1234.56")));
        assertEquals("58.210 km", Format.km(new BigDecimal("58210")));
        assertEquals("12,5", Format.decimal(new BigDecimal("12.5")));
    }
}
