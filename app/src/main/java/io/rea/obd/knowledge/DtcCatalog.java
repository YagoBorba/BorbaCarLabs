package io.rea.obd.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class DtcCatalog {
    private static final List<DtcDefinition> ENTRIES;
    public static final String VERSION = "generic-dtc-1";
    private static final SourceReference FORD = new SourceReference("ford-pced-1999-meanings", "PCED On Board Diagnostics II", "Ford Motor Company", "1999 PCED / Section 4", "https://www.fordservicecontent.dealerconnection.com/pubs/content/~WVX2/~MUS~LEN/14/VX24002.HTM", "DTC Descriptions: P0171, P0300, P0420", "2026-10-20", "CODE_MEANING_REFERENCE_ONLY_OTHER_CONFIGURATION", "Somente conferência do significado; causas, limites e procedimentos Ford não são aplicados ao Cobalt");
    private static final SourceReference VW = new SourceReference("vw-24y2-meanings", "Technical Bulletin 24Y2", "Volkswagen Group of America; hospedado por NHTSA", "24 12 07 / 2030699 / 2012-09-05", "https://static.nhtsa.gov/odi/tsbs/2012/SB-10072632-3639.pdf", "Página 3, tabela DTC/Description, linha P0301", "2026-10-20", "CODE_MEANING_REFERENCE_ONLY_ROUTAN_2012", "Somente conferência do significado de P0301; boletim/procedimento Routan não se aplica ao Cobalt");

    static {
        List<DtcDefinition> unmodifiableList = Collections.unmodifiableList(Arrays.asList(entry("P0133", "Resposta lenta no circuito do sensor de oxigênio.", "Em que condição o alerta apareceu e quais outros códigos foram registrados?", "ELM327DSK, página 35, exemplo P0133", "elm327-dsk"), entry("P0171", "Mistura indicada como pobre no banco 1.", "Qual combustível foi usado e quando o comportamento foi percebido?", "1999 PCED, linha P0171", "ford-pced-1999-meanings"), entry("P0300", "Falhas de combustão aleatórias ou em múltiplos cilindros.", "O sintoma ocorre na partida, em marcha lenta ou na aceleração?", "1999 PCED, linha P0300", "ford-pced-1999-meanings"), entry("P0301", "Falha de combustão indicada no cilindro 1.", "Quando a falha é percebida? A posição física do cilindro exige documentação do motor.", "Boletim 24Y2, página 3, linha P0301", "vw-24y2-meanings"), entry("P0420", "Eficiência do sistema catalítico indicada abaixo do limiar no banco 1.", "Há outros códigos e sintomas registrados na mesma investigação?", "1999 PCED, linha P0420", "ford-pced-1999-meanings")));
        ENTRIES = unmodifiableList;
        HashSet seen = new HashSet();
        for (DtcDefinition dtcDefinition : unmodifiableList) {
            if (!seen.add(dtcDefinition.code)) {
                throw new IllegalStateException("Código duplicado");
            }
            Iterator<String> it = dtcDefinition.sourceIds.iterator();
            while (it.hasNext()) {
                if (source(it.next()) == null) {
                    throw new IllegalStateException("Fonte não resolvida");
                }
            }
        }
    }

    private DtcCatalog() {
    }

    private static DtcDefinition entry(String code, String meaning, String question, String sourceSection, String text) {
        return new DtcDefinition(code, meaning, question, sourceSection, Collections.singletonList(text));
    }

    public static List<DtcDefinition> all() {
        return ENTRIES;
    }

    public static DtcDefinition find(String text) {
        if (text == null) {
            return null;
        }
        for (DtcDefinition dtcDefinition : ENTRIES) {
            if (dtcDefinition.code.equals(text)) {
                return dtcDefinition;
            }
        }
        return null;
    }

    public static SourceReference source(String text) {
        SourceReference sourceReference = FORD;
        if (sourceReference.id.equals(text)) {
            return sourceReference;
        }
        SourceReference sourceReference2 = VW;
        return sourceReference2.id.equals(text) ? sourceReference2 : KnowledgeRepository.find(text);
    }

    public static List<SourceReference> sources(DtcDefinition dtcDefinition) {
        ArrayList items = new ArrayList();
        Iterator<String> it = dtcDefinition.sourceIds.iterator();
        while (it.hasNext()) {
            SourceReference source = source(it.next());
            if (source == null) {
                throw new IllegalArgumentException("Fonte de definição não resolvida");
            }
            items.add(source);
        }
        return Collections.unmodifiableList(items);
    }
}