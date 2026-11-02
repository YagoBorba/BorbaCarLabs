package io.rea.obd.vehicle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class VinDecoder {
    public static final class Decoded {
        public final String make;
        public final Integer modelYear;

        Decoded(String make, Integer modelYear) {
            this.make = make;
            this.modelYear = modelYear;
        }
    }

    private static final Map<String, String> MAKERS = new HashMap<>();
    private static final String YEARS = "ABCDEFGHJKLMNPRSTVWXY123456789";

    static {
        String[][] makers = {
            {"9BG", "Chevrolet"}, {"8AG", "Chevrolet"}, {"1G1", "Chevrolet"}, {"1GC", "Chevrolet"}, {"KL1", "Chevrolet"},
            {"9BW", "Volkswagen"}, {"8AW", "Volkswagen"}, {"3VW", "Volkswagen"}, {"WVW", "Volkswagen"}, {"WVG", "Volkswagen"},
            {"9BD", "Fiat"}, {"8AP", "Fiat"}, {"ZFA", "Fiat"}, {"9BF", "Ford"}, {"8AF", "Ford"}, {"1FA", "Ford"}, {"1FT", "Ford"},
            {"93H", "Honda"}, {"JHM", "Honda"}, {"9BR", "Toyota"}, {"8AJ", "Toyota"}, {"JTD", "Toyota"}, {"JTM", "Toyota"},
            {"93Y", "Renault"}, {"8A1", "Renault"}, {"VF1", "Renault"}, {"9BH", "Hyundai"}, {"95P", "Hyundai"}, {"KMH", "Hyundai"},
            {"94D", "Nissan"}, {"3N1", "Nissan"}, {"JN1", "Nissan"}, {"93X", "Mitsubishi"}, {"JMB", "Mitsubishi"}, {"JMY", "Mitsubishi"},
            {"935", "Citroën"}, {"VF7", "Citroën"}, {"936", "Peugeot"}, {"VF3", "Peugeot"}, {"988", "Jeep"}, {"1C4", "Jeep"},
            {"9BM", "Mercedes-Benz"}, {"WDD", "Mercedes-Benz"}, {"W1K", "Mercedes-Benz"}, {"98M", "BMW"}, {"WBA", "BMW"},
            {"93U", "Audi"}, {"WAU", "Audi"}, {"KNA", "Kia"}, {"KND", "Kia"}, {"LVV", "Chery"}, {"LGW", "GWM"}, {"LGX", "BYD"},
            {"LJ1", "JAC"}, {"SAL", "Land Rover"}, {"SAJ", "Jaguar"}, {"YV1", "Volvo"}, {"JF1", "Subaru"}, {"JS2", "Suzuki"},
            {"JS3", "Suzuki"}, {"WP0", "Porsche"}, {"WP1", "Porsche"}, {"WMW", "MINI"},
        };
        for (String[] maker : makers) {
            MAKERS.put(maker[0], maker[1]);
        }
    }

    private VinDecoder() {
    }

    public static Decoded decode(String vin, int currentYear) {
        if (vin == null) {
            return null;
        }
        String text = vin.trim().toUpperCase(Locale.ROOT);
        if (!text.matches("[A-HJ-NPR-Z0-9]{17}")) {
            return null;
        }
        String make = MAKERS.get(text.substring(0, 3));
        int code = YEARS.indexOf(text.charAt(9));
        Integer year = null;
        if (code >= 0) {
            int candidate = 1980 + code;
            while (candidate + 30 <= currentYear + 1) {
                candidate += 30;
            }
            year = candidate;
        }
        return make == null && year == null ? null : new Decoded(make, year);
    }

    public static String describe(String vin, int currentYear, String profileMake, Integer profileYear) {
        Decoded decoded = decode(vin, currentYear);
        if (decoded == null) {
            return "";
        }
        StringBuilder text = new StringBuilder("Pelo VIN: ");
        text.append(decoded.make == null ? "fabricante não reconhecido" : decoded.make);
        if (decoded.modelYear != null) {
            text.append(" · ano-modelo ").append(decoded.modelYear);
        }
        boolean otherMake = decoded.make != null && profileMake != null && !profileMake.isEmpty() && !decoded.make.equalsIgnoreCase(profileMake);
        boolean otherYear = decoded.modelYear != null && profileYear != null && !decoded.modelYear.equals(profileYear);
        if (otherMake || otherYear) {
            text.append("\nO perfil ativo diz ").append(profileMake == null || profileMake.isEmpty() ? "" : profileMake + " ").append(profileYear == null ? "" : profileYear).append(". Confira se é o mesmo carro.");
        } else if (profileMake != null && !profileMake.isEmpty()) {
            text.append("\nCombina com o perfil ativo.");
        }
        return text.toString().replace(" .", ".");
    }
}
