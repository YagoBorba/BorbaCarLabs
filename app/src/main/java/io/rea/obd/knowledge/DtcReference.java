package io.rea.obd.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DtcReference {
    public static final String VERSION = "generic-dtc-2";

    static final SourceReference FORD_GENERIC = new SourceReference("ford-pced-1999-generic-p0",
            "PCED On Board Diagnostics II", "Ford Motor Company", "1999 PCED / Section 4",
            "https://www.fordservicecontent.dealerconnection.com/pubs/content/~WVX2/~MUS~LEN/14/VX24002.HTM",
            "Section 4, DTC Descriptions, códigos genéricos P0", "2026-10-27",
            "CODE_MEANING_REFERENCE_ONLY_OTHER_CONFIGURATION",
            "Somente conferência do significado genérico; causas, limites e procedimentos Ford não se aplicam a outros veículos. Códigos P1 do fabricante não usados.");
    static final SourceReference VW_GENERIC = new SourceReference("vw-24y2-generic",
            "Technical Bulletin 24Y2", "Volkswagen Group of America; hospedado por NHTSA", "24 12 07 / 2030699 / 2012-09-05",
            "https://static.nhtsa.gov/odi/tsbs/2012/SB-10072632-3639.pdf",
            "Página 3, tabela DTC/Description", "2026-10-27",
            "CODE_MEANING_REFERENCE_ONLY_ROUTAN_2012",
            "Somente conferência do significado genérico; boletim/procedimento Routan não se aplica a outros veículos.");

    private static final String Q_SENSOR = "O alerta aparece sempre ou só em certas condições (frio, chuva, trepidação)? Houve serviço elétrico recente perto do sensor?";
    private static final String Q_MIXTURE = "Qual combustível foi usado e quando o comportamento foi percebido?";
    private static final String Q_OXYGEN = "Em que condição o alerta apareceu e quais outros códigos foram registrados?";
    private static final String Q_MISFIRE = "O sintoma ocorre na partida, em marcha lenta ou na aceleração? A posição física do cilindro exige documentação do motor.";
    private static final String Q_TEMPERATURE = "O indicador de temperatura do painel mudou? Em que condição (trânsito parado, estrada, subida)?";
    private static final String Q_FUEL = "Há dificuldade de partida, falhas ou perda de força? Em que condição e com qual combustível?";
    private static final String Q_IGNITION = "O sintoma ocorre na partida, em marcha lenta ou na aceleração? Há outros códigos de ignição registrados?";
    private static final String Q_EMISSIONS = "Há outros códigos e sintomas registrados na mesma investigação?";
    private static final String Q_EVAP = "O alerta apareceu depois de um abastecimento? A tampa do tanque estava bem fechada?";
    private static final String Q_SPEED = "O velocímetro ou a marcha lenta se comportaram de forma diferente quando o alerta apareceu?";
    private static final String Q_MODULE = "O alerta apareceu após troca de bateria, reprogramação ou serviço elétrico?";
    private static final String Q_TRANSMISSION = "Houve trancos, trocas de marcha estranhas ou alerta de câmbio no painel junto com o código?";
    private static final String Q_SWITCH = "O alerta coincide com o uso do pedal, alavanca ou direção? Alguma função ligada a esse comando falhou?";
    private static final String Q_BOOST = "Este motor tem turbo ou compressor? O sintoma aparece em acelerações fortes?";
    private static final String Q_OIL = "O alerta de óleo do painel acendeu? Em que condição de uso?";

    private static final List<DtcDefinition> ENTRIES;
    private static final Map<String, DtcDefinition> BY_CODE = new HashMap<>();

    static {
        List<DtcDefinition> entries = new ArrayList<>(DtcCatalog.all());
        entries.addAll(Arrays.asList(
                ford("P0102", "Sinal baixo no circuito do sensor de fluxo de massa de ar (MAF).", Q_SENSOR),
                ford("P0103", "Sinal alto no circuito do sensor de fluxo de massa de ar (MAF).", Q_SENSOR),
                ford("P0106", "Desempenho do circuito do sensor de pressão barométrica (BARO) fora do esperado.", Q_SENSOR),
                ford("P0107", "Tensão baixa detectada no sensor de pressão barométrica (BARO).", Q_SENSOR),
                ford("P0108", "Tensão alta detectada no sensor de pressão barométrica (BARO).", Q_SENSOR),
                ford("P0109", "Circuito do sensor de pressão barométrica (BARO) intermitente.", Q_SENSOR),
                ford("P0112", "Sinal baixo no circuito do sensor de temperatura do ar de admissão (IAT).", Q_SENSOR),
                ford("P0113", "Sinal alto no circuito do sensor de temperatura do ar de admissão (IAT).", Q_SENSOR),
                ford("P0117", "Sinal baixo no circuito do sensor de temperatura do líquido de arrefecimento (ECT).", Q_SENSOR),
                ford("P0118", "Sinal alto no circuito do sensor de temperatura do líquido de arrefecimento (ECT).", Q_SENSOR),
                ford("P0121", "Desempenho do circuito do sensor de posição da borboleta (TP) fora do esperado.", Q_SENSOR),
                ford("P0122", "Sinal baixo no circuito do sensor de posição da borboleta (TP).", Q_SENSOR),
                ford("P0123", "Sinal alto no circuito do sensor de posição da borboleta (TP).", Q_SENSOR),
                ford("P0125", "Temperatura do líquido de arrefecimento insuficiente para o controle de combustível em malha fechada.", Q_TEMPERATURE),
                ford("P0127", "Temperatura do ar de admissão alta demais.", Q_TEMPERATURE),
                ford("P0131", "Tensão baixa fora da faixa no circuito do sensor de oxigênio aquecido (banco 1, sensor 1).", Q_OXYGEN),
                ford("P0135", "Falha no circuito do sensor de oxigênio aquecido (banco 1, sensor 1).", Q_OXYGEN),
                ford("P0136", "Falha no circuito do sensor de oxigênio aquecido (banco 1, sensor 2).", Q_OXYGEN),
                ford("P0151", "Tensão baixa fora da faixa no circuito do sensor de oxigênio aquecido (banco 2, sensor 1).", Q_OXYGEN),
                ford("P0153", "Resposta lenta no circuito do sensor de oxigênio aquecido (banco 2, sensor 1).", Q_OXYGEN),
                ford("P0155", "Falha no circuito do sensor de oxigênio aquecido (banco 2, sensor 1).", Q_OXYGEN),
                ford("P0156", "Falha no circuito do sensor de oxigênio aquecido (banco 2, sensor 2).", Q_OXYGEN),
                ford("P0172", "Mistura indicada como rica no banco 1.", Q_MIXTURE),
                ford("P0174", "Mistura indicada como pobre no banco 2.", Q_MIXTURE),
                ford("P0175", "Mistura indicada como rica no banco 2.", Q_MIXTURE),
                ford("P0176", "Falha no circuito do sensor de combustível flexível (FF).", Q_FUEL),
                ford("P0181", "Faixa ou desempenho do circuito A do sensor de temperatura do combustível (EFT) fora do esperado.", Q_SENSOR),
                ford("P0182", "Sinal baixo no circuito A do sensor de temperatura do combustível (EFT).", Q_SENSOR),
                ford("P0183", "Sinal alto no circuito A do sensor de temperatura do combustível (EFT).", Q_SENSOR),
                ford("P0186", "Faixa ou desempenho do circuito B do sensor de temperatura do combustível (EFT) fora do esperado.", Q_SENSOR),
                ford("P0187", "Sinal baixo no circuito B do sensor de temperatura do combustível (EFT).", Q_SENSOR),
                ford("P0188", "Sinal alto no circuito B do sensor de temperatura do combustível (EFT).", Q_SENSOR),
                ford("P0190", "Falha no circuito do sensor de pressão do tubo distribuidor de combustível (FRP).", Q_FUEL),
                ford("P0191", "Desempenho do circuito do sensor de pressão do tubo distribuidor (FRP) fora do esperado.", Q_FUEL),
                ford("P0192", "Sinal baixo no circuito do sensor de pressão do tubo distribuidor (FRP).", Q_FUEL),
                ford("P0193", "Sinal alto no circuito do sensor de pressão do tubo distribuidor (FRP).", Q_FUEL),
                ford("P0217", "Condição de superaquecimento do líquido de arrefecimento do motor.", Q_TEMPERATURE),
                ford("P0230", "Falha no circuito primário da bomba de combustível.", Q_FUEL),
                ford("P0231", "Sinal baixo no circuito secundário da bomba de combustível.", Q_FUEL),
                ford("P0232", "Sinal alto no circuito secundário da bomba de combustível.", Q_FUEL),
                ford("P0234", "Condição de sobrepressão do compressor (sobrealimentação).", Q_BOOST),
                ford("P0243", "Falha no circuito do solenoide de alívio (bypass) do compressor.", Q_BOOST),
                ford("P0298", "Condição de superaquecimento do óleo do motor.", Q_OIL),
                ford("P0320", "Falha no circuito de entrada de rotação do motor para a ignição.", Q_IGNITION),
                ford("P0325", "Falha no circuito do sensor de detonação 1 (banco 1).", Q_IGNITION),
                ford("P0326", "Faixa ou desempenho do circuito do sensor de detonação 1 (banco 1) fora do esperado.", Q_IGNITION),
                ford("P0330", "Falha no circuito do sensor de detonação 2 (banco 2).", Q_IGNITION),
                ford("P0331", "Faixa ou desempenho do circuito do sensor de detonação 2 (banco 2) fora do esperado.", Q_IGNITION),
                ford("P0340", "Falha no circuito do sensor de posição do comando de válvulas (CMP).", Q_IGNITION),
                ford("P0350", "Problema no circuito primário ou secundário de bobina de ignição (bobina não determinada).", Q_IGNITION),
                ford("P0401", "Fluxo insuficiente detectado no sistema de recirculação de gases de escape (EGR).", Q_EMISSIONS),
                ford("P0402", "Fluxo excessivo detectado no sistema de recirculação de gases de escape (EGR).", Q_EMISSIONS),
                ford("P0411", "Fluxo incorreto a montante no sistema de injeção de ar secundário (AIR).", Q_EMISSIONS),
                ford("P0412", "Falha no circuito do sistema de injeção de ar secundário (AIR).", Q_EMISSIONS),
                ford("P0430", "Eficiência do sistema catalítico indicada abaixo do limiar no banco 2.", Q_EMISSIONS),
                ford("P0442", "Pequeno vazamento detectado no sistema de controle de emissões evaporativas (EVAP).", Q_EVAP),
                ford("P0443", "Falha no circuito da válvula de purga do cânister do sistema EVAP.", Q_EVAP),
                ford("P0451", "Ruído no circuito do sensor de pressão do tanque de combustível (FTP).", Q_EVAP),
                ford("P0452", "Tensão baixa detectada no circuito do sensor de pressão do tanque (FTP).", Q_EVAP),
                ford("P0453", "Tensão alta detectada no circuito do sensor de pressão do tanque (FTP).", Q_EVAP),
                ford("P0455", "Vazamento detectado no sistema EVAP (sem fluxo de purga ou vazamento grande).", Q_EVAP),
                ford("P0457", "Vazamento detectado no sistema EVAP relacionado à tampa do tanque de combustível.", Q_EVAP),
                ford("P0460", "Falha no circuito do sensor de nível de combustível.", Q_SENSOR),
                ford("P0500", "Falha no sensor de velocidade do veículo (VSS).", Q_SPEED),
                ford("P0501", "Faixa ou desempenho do sensor de velocidade do veículo (VSS) fora do esperado.", Q_SPEED),
                ford("P0503", "Sinal intermitente do sensor de velocidade do veículo (VSS).", Q_SPEED),
                ford("P0505", "Falha no sistema de controle de ar da marcha lenta.", Q_SPEED),
                ford("P0552", "Falha no circuito do sensor de pressão da direção hidráulica (PSP).", Q_SWITCH),
                ford("P0602", "Erro de programação do módulo de controle.", Q_MODULE),
                ford("P0603", "Erro no teste da memória mantida (KAM) do módulo de controle do trem de força.", Q_MODULE),
                ford("P0605", "Erro na memória somente de leitura (ROM) do módulo de controle do trem de força.", Q_MODULE),
                ford("P0703", "Falha na entrada do circuito do interruptor do freio.", Q_SWITCH),
                ford("P0704", "Falha no interruptor de posição do pedal da embreagem.", Q_SWITCH),
                ford("P0720", "Sinal insuficiente do sensor de rotação do eixo de saída da transmissão.", Q_TRANSMISSION),
                ford("P0721", "Interferência (ruído) no sinal do sensor de rotação do eixo de saída da transmissão.", Q_TRANSMISSION),
                ford("P0722", "Sem sinal do sensor de rotação do eixo de saída da transmissão.", Q_TRANSMISSION),
                ford("P0723", "Falha intermitente no circuito do sensor de rotação do eixo de saída da transmissão.", Q_TRANSMISSION),
                ford("P0812", "Falha no circuito de entrada do interruptor de marcha à ré.", Q_SWITCH),
                vw("P0302", "Falha de combustão indicada no cilindro 2.", Q_MISFIRE),
                vw("P0303", "Falha de combustão indicada no cilindro 3.", Q_MISFIRE),
                vw("P0304", "Falha de combustão indicada no cilindro 4.", Q_MISFIRE),
                vw("P0305", "Falha de combustão indicada no cilindro 5.", Q_MISFIRE),
                vw("P0306", "Falha de combustão indicada no cilindro 6.", Q_MISFIRE),
                vw("P0441", "Desempenho do sistema de purga de emissões evaporativas (EVAP) fora do esperado.", Q_EVAP),
                vw("P06DA", "Problema no circuito de controle da pressão do óleo do motor.", Q_OIL),
                vw("P06DE", "Circuito de controle da pressão do óleo do motor travado em ligado.", Q_OIL)));
        for (DtcDefinition entry : entries) {
            if (BY_CODE.put(entry.code, entry) != null) {
                throw new IllegalStateException("Código duplicado: " + entry.code);
            }
            for (String source : entry.sourceIds) {
                if (source(source) == null) {
                    throw new IllegalStateException("Fonte não resolvida: " + source);
                }
            }
        }
        entries.sort((a, b) -> a.code.compareTo(b.code));
        ENTRIES = Collections.unmodifiableList(entries);
    }

    private DtcReference() {
    }

    private static DtcDefinition ford(String code, String meaning, String question) {
        return new DtcDefinition(VERSION, code, meaning, question, "1999 PCED Section 4, linha " + code, Collections.singletonList(FORD_GENERIC.id));
    }

    private static DtcDefinition vw(String code, String meaning, String question) {
        return new DtcDefinition(VERSION, code, meaning, question, "Boletim 24Y2, página 3, linha " + code, Collections.singletonList(VW_GENERIC.id));
    }

    public static List<DtcDefinition> all() {
        return ENTRIES;
    }

    public static DtcDefinition find(String code) {
        return code == null ? null : BY_CODE.get(code);
    }

    public static SourceReference source(String id) {
        if (FORD_GENERIC.id.equals(id)) {
            return FORD_GENERIC;
        }
        if (VW_GENERIC.id.equals(id)) {
            return VW_GENERIC;
        }
        return DtcCatalog.source(id);
    }

    public static List<SourceReference> sources(DtcDefinition definition) {
        List<SourceReference> sources = new ArrayList<>();
        for (String id : definition.sourceIds) {
            SourceReference source = source(id);
            if (source == null) {
                throw new IllegalArgumentException("Fonte de definição não resolvida");
            }
            sources.add(source);
        }
        return Collections.unmodifiableList(sources);
    }
}
