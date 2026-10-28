package io.rea.obd;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CobaltGuide {
    public static final Map<String, String> MESSAGES;

    private CobaltGuide() {
    }

    static {
        LinkedHashMap map = new LinkedHashMap();
        map.put("04", "Ar-condicionado desativado pela temperatura do motor.");
        map.put("35", "Trocar a bateria do controle remoto da chave.");
        map.put("68", "Solicitação de manutenção da direção assistida.");
        map.put("79", "Verificar o nível de óleo do motor.");
        map.put("81", "Solicitação de manutenção da transmissão automática.");
        map.put("84", "Potência do motor reduzida.");
        map.put("89", "Solicitação de manutenção relacionada à luz de avaria.");
        map.put("94", "Selecionar a posição P.");
        map.put("95", "Solicitação de manutenção dos airbags.");
        map.put("134", "Verificar o assistente de estacionamento e limpar os para-choques.");
        MESSAGES = Collections.unmodifiableMap(map);
    }
}