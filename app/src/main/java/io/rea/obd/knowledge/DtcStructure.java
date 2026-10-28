package io.rea.obd.knowledge;

public final class DtcStructure {
    private DtcStructure() {
    }

    public static String describe(String code) {
        if (code == null || !code.matches("[PCBU][0-3][0-9A-F]{3}")) {
            return null;
        }
        char system = code.charAt(0);
        char owner = code.charAt(1);
        StringBuilder builder = new StringBuilder(system(system)).append(" · ").append(owner(system, owner));
        if (system == 'P' && owner == '0') {
            String group = powertrainGroup(code.charAt(2));
            if (group != null) {
                builder.append(" · ").append(group);
            }
        }
        return builder.toString();
    }

    private static String system(char letter) {
        switch (letter) {
            case 'P':
                return "Motor e transmissão";
            case 'C':
                return "Chassi";
            case 'B':
                return "Carroceria";
            default:
                return "Rede de comunicação entre módulos";
        }
    }

    private static String owner(char system, char digit) {
        if (digit == '0') {
            return "código genérico";
        }
        if (system == 'P' && digit == '2') {
            return "código genérico";
        }
        if (digit == '3') {
            return "faixa reservada / do fabricante";
        }
        return "código específico do fabricante";
    }

    private static String powertrainGroup(char digit) {
        switch (digit) {
            case '0':
            case '1':
            case '2':
                return "mistura ar/combustível";
            case '3':
                return "ignição ou falha de combustão";
            case '4':
                return "controles auxiliares de emissões";
            case '5':
                return "velocidade, marcha lenta ou entradas auxiliares";
            case '6':
                return "computador ou circuitos de saída";
            case '7':
            case '8':
            case '9':
                return "transmissão";
            case 'A':
            case 'B':
            case 'C':
                return "propulsão híbrida";
            default:
                return null;
        }
    }
}
