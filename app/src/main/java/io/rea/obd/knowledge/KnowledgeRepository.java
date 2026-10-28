package io.rea.obd.knowledge;

import io.rea.obd.SensorCatalog;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class KnowledgeRepository {
    private static final List<SourceReference> SOURCES = Collections.unmodifiableList(Arrays.asList(new SourceReference("elm327-dsk", "ELM327 datasheet", "Elm Electronics", "ELM327DSK / v2.2", "https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf", "Talking to the Vehicle; Interpreting Trouble Codes", "2026-10-19", "GENERIC_ELM_OBD_PROTOCOL", "Localização e escopo consultados; sem ensaio de compatibilidade do adaptador ou veículo"), new SourceReference("css-pid-table", "OBD2 PID Overview", "CSS Electronics", "Página sem versão editorial; consulta em 2026-10-19", "https://www.csselectronics.com/pages/obd2-pid-table-on-board-diagnostics-j1979", "OBD2 PID table; RPM conversion example", "2026-10-19", "GENERIC_SERVICE01_ENCODING", "Referência de codificação dos PIDs existentes; não contém limites mecânicos confirmados do Cobalt")));
    public static final String VERSION = "protocol-links-1";

    private KnowledgeRepository() {
    }

    public static List<SourceReference> all() {
        return SOURCES;
    }

    public static SourceReference find(String text) {
        for (SourceReference sourceReference : SOURCES) {
            if (sourceReference.id.equals(text)) {
                return sourceReference;
            }
        }
        return null;
    }

    public static SourceReference sourceForPid(int i) {
        if (SensorCatalog.get(i) == null) {
            return null;
        }
        return find("css-pid-table");
    }
}