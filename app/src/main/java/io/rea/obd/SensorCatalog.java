package io.rea.obd;

public final class SensorCatalog {
    public static final Sensor[] ALL;

    private SensorCatalog() {
    }

    public static final class Sensor {
        public final int decimals;
        public final double max;
        public final double min;
        public final String name;
        public final int pid;
        public final String unit;

        private Sensor(int i, String name, String unit, double min, double max, int decimals) {
            this.pid = i;
            this.name = name;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
        }
    }

    static {
        int i = 1;
        double min2 = 0.0d;
        double max3 = 100.0d;
        int decimals3 = 1;
        double min3 = -100.0d;
        double max4 = 99.21875d;
        int decimals4 = 0;
        double min4 = 0.0d;
        double min5 = 0.0d;
        int decimals5 = 0;
        double max = 65535.0d;
        int decimals = 0;
        int decimals2 = 0;
        double min = -40.0d;
        double max2 = 215.0d;
        ALL = new Sensor[]{new Sensor(12, "Rotação do motor", "rpm", 0.0d, 16383.75d, 0), new Sensor(13, "Velocidade", "km/h", 0.0d, 255.0d, 0), new Sensor(5, "Temperatura do arrefecimento", "°C", -40.0d, 215.0d, 0), new Sensor(4, "Carga calculada", "%", min2, max3, i), new Sensor(17, "Posição da borboleta", "%", min2, max3, i), new Sensor(15, "Temperatura da admissão", "°C", -40.0d, 215.0d, 0), new Sensor(6, "Correção curta · banco 1", "%", min3, max4, decimals3), new Sensor(7, "Correção longa · banco 1", "%", min3, max4, decimals3), new Sensor(8, "Correção curta · banco 2", "%", min3, max4, decimals3), new Sensor(9, "Correção longa · banco 2", "%", min3, max4, decimals3), new Sensor(10, "Pressão de combustível", "kPa", min4, 765.0d, decimals4), new Sensor(11, "Pressão absoluta da admissão", "kPa", min4, 255.0d, decimals4), new Sensor(14, "Avanço de ignição", "°", -64.0d, 63.5d, 1), new Sensor(16, "Fluxo de ar · MAF", "g/s", min5, 655.35d, 2), new Sensor(31, "Tempo desde a partida", "s", min5, max, decimals5), new Sensor(33, "Distância com luz de falha acesa", "km", min5, max, decimals5), new Sensor(47, "Nível de combustível", "%", min5, 100.0d, 1), new Sensor(49, "Distância desde apagar falhas", "km", min5, 65535.0d, decimals), new Sensor(51, "Pressão barométrica", "kPa", min5, 255.0d, decimals), new Sensor(66, "Tensão do módulo de controle", "V", min5, 65.535d, 2), new Sensor(70, "Temperatura ambiente", "°C", min, max2, decimals2), new Sensor(92, "Temperatura do óleo", "°C", min, max2, decimals2), new Sensor(94, "Vazão de combustível", "L/h", 0.0d, 3276.75d, 2)};
    }

    public static Sensor get(int i) {
        for (Sensor sensor : ALL) {
            if (sensor.pid == i) {
                return sensor;
            }
        }
        return null;
    }
}