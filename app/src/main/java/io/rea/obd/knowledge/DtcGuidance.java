package io.rea.obd.knowledge;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DtcGuidance {
    public enum Level {
        STOP,
        AVOID,
        SCHEDULE,
        UNKNOWN
    }

    public final Level level;
    public final String advice;
    public final List<String> causes;

    private DtcGuidance(Level level, String advice, String... causes) {
        this.level = level;
        this.advice = advice;
        this.causes = Collections.unmodifiableList(Arrays.asList(causes));
    }

    public boolean urgent() {
        return this.level == Level.STOP || this.level == Level.AVOID;
    }

    public String headline() {
        switch (this.level) {
            case STOP:
                return "Pare assim que for seguro";
            case AVOID:
                return "Evite rodar até a oficina";
            case SCHEDULE:
                return "Pode rodar com atenção · agende a oficina";
            default:
                return "Sem orientação geral para este código";
        }
    }

    private static boolean in(int number, int from, int to) {
        return number >= from && number <= to;
    }

    public static DtcGuidance of(String code) {
        if (code == null || !code.matches("P0[0-9A-F]{3}")) {
            return new DtcGuidance(Level.UNKNOWN, "Códigos de fabricante e de outros sistemas variam de carro para carro. Se houver sintoma, procure uma oficina.");
        }
        int number;
        try {
            number = Integer.parseInt(code.substring(2));
        } catch (NumberFormatException e) {
            return new DtcGuidance(Level.UNKNOWN, "Se houver sintoma, procure uma oficina.");
        }
        if (number == 217 || number == 298 || in(number, 520, 524)) {
            return new DtcGuidance(Level.STOP, "Temperatura ou pressão do óleo fora do normal pode danificar o motor em pouco tempo. Confira o painel e não continue se a luz ou o ponteiro indicarem problema.",
                    "Nível baixo de líquido de arrefecimento ou de óleo", "Ventoinha ou válvula termostática", "Bomba d'água ou de óleo", "Sensor ou chicote");
        }
        if (in(number, 300, 316)) {
            return new DtcGuidance(Level.AVOID, "Falha de combustão pode superaquecer e danificar o catalisador. Se a luz de injeção piscar, pare.",
                    "Velas, cabos ou bobinas de ignição", "Bicos injetores", "Entrada falsa de ar", "Combustível de má qualidade", "Compressão baixa no cilindro");
        }
        if (in(number, 335, 339)) {
            return new DtcGuidance(Level.AVOID, "Sem o sinal de rotação o motor pode apagar ou não ligar.",
                    "Sensor de rotação", "Conector ou chicote do sensor", "Roda fônica danificada");
        }
        if (in(number, 230, 234) || number == 87 || number == 88) {
            return new DtcGuidance(Level.AVOID, "Pode haver perda de força ou o motor pode apagar.",
                    "Bomba ou relé de combustível", "Filtro de combustível", "Pressão do turbo fora do controle");
        }
        if (number == 171 || number == 174) {
            return new DtcGuidance(Level.SCHEDULE, "Mistura pobre por muito tempo pode causar falhas de combustão.",
                    "Entrada falsa de ar (mangueiras, coletor)", "Sensor de fluxo de ar ou de pressão sujo", "Pressão de combustível baixa (filtro, bomba)", "Bicos injetores sujos");
        }
        if (number == 172 || number == 175) {
            return new DtcGuidance(Level.SCHEDULE, "Mistura rica aumenta o consumo e pode prejudicar o catalisador.",
                    "Bico injetor gotejando", "Sensor de temperatura do motor", "Filtro de ar entupido", "Pressão de combustível alta");
        }
        if (number == 420 || number == 430 || in(number, 421, 424) || in(number, 431, 434)) {
            return new DtcGuidance(Level.SCHEDULE, "O carro costuma rodar normalmente, mas as emissões sobem.",
                    "Catalisador desgastado", "Sensor de oxigênio depois do catalisador", "Vazamento no escapamento", "Falhas de combustão anteriores");
        }
        if (in(number, 440, 457)) {
            return new DtcGuidance(Level.SCHEDULE, "Não afeta a dirigibilidade; o vapor de combustível escapa para o ar.",
                    "Tampa do tanque mal fechada", "Válvula de purga do cânister", "Mangueiras do sistema de vapores");
        }
        if (number == 125 || number == 128) {
            return new DtcGuidance(Level.SCHEDULE, "O motor demora a esquentar: aumenta o consumo e o desgaste.",
                    "Válvula termostática presa aberta", "Sensor de temperatura do motor");
        }
        if (in(number, 115, 119)) {
            return new DtcGuidance(Level.SCHEDULE, "Sem a temperatura certa, a ventoinha e a mistura podem não funcionar como deveriam. Acompanhe o ponteiro de temperatura.",
                    "Sensor de temperatura do motor", "Conector ou chicote do sensor");
        }
        if (in(number, 505, 507)) {
            return new DtcGuidance(Level.SCHEDULE, "A marcha lenta pode oscilar ou o motor apagar ao parar.",
                    "Corpo de borboleta sujo", "Atuador de marcha lenta", "Entrada falsa de ar");
        }
        if (in(number, 500, 503)) {
            return new DtcGuidance(Level.SCHEDULE, "O velocímetro e as trocas de marcha podem ficar estranhos.",
                    "Sensor de velocidade", "Conector ou chicote do sensor");
        }
        if (in(number, 340, 349) || in(number, 10, 25)) {
            return new DtcGuidance(Level.SCHEDULE, "Pode haver perda de força ou partida difícil.",
                    "Sensor de fase do comando", "Sincronismo da correia ou corrente", "Válvula do comando variável");
        }
        if (in(number, 700, 799)) {
            return new DtcGuidance(Level.SCHEDULE, "Se o câmbio der trancos ou entrar em modo de emergência, evite rodar.",
                    "Nível ou estado do fluido do câmbio", "Sensor ou solenoide do câmbio", "Conector ou chicote");
        }
        if (in(number, 100, 199) || in(number, 130, 167) || in(number, 400, 499) || in(number, 600, 699)) {
            return new DtcGuidance(Level.SCHEDULE, "Costuma afetar consumo ou emissões antes de afetar a dirigibilidade.",
                    "Conector ou chicote do componente", "O próprio sensor ou atuador", "Alimentação ou aterramento");
        }
        return new DtcGuidance(Level.SCHEDULE, "Se houver sintoma, procure uma oficina.", "Conector ou chicote do componente", "O próprio sensor ou atuador");
    }
}
