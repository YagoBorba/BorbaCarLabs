package io.rea.obd.ui;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

public final class WallClock {
    private static volatile Clock fixed;

    private WallClock() {
    }

    public static long millis() {
        Clock clock = fixed;
        return clock == null ? System.currentTimeMillis() : clock.millis();
    }

    public static ZoneId zone() {
        Clock clock = fixed;
        return clock == null ? ZoneId.systemDefault() : clock.getZone();
    }

    public static LocalDate today() {
        return java.time.Instant.ofEpochMilli(millis()).atZone(zone()).toLocalDate();
    }

    public static void use(Clock clock) {
        fixed = clock;
    }
}
