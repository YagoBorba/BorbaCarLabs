package io.rea.obd;

import java.io.IOException;

public final class EmissionCodeReset {
    private EmissionCodeReset() {
    }

    public static String executeAfterUserConfirmation(ElmSession elmSession) throws IOException {
        Double pid = ObdParser.pid(elmSession.command("010D", 7000L), 13);
        Double pid2 = ObdParser.pid(elmSession.command("010C", 7000L), 12);
        if (pid == null || pid2 == null || pid.doubleValue() != 0.0d || pid2.doubleValue() != 0.0d) {
            throw new IOException("Operação bloqueada: não foi possível confirmar velocidade zero e motor desligado.");
        }
        if (!ObdParser.hasService(elmSession.command("04", 12000L), 4)) {
            throw new IOException("A ECU não confirmou o apagamento.");
        }
        return "A ECU confirmou o comando. Consulte novamente os códigos e os monitores.";
    }
}