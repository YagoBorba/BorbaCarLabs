package io.rea.obd.application;

import io.rea.obd.diagnostics.DtcReadResult;
import io.rea.obd.diagnostics.SupplementalReadResult;
import java.util.Objects;

public final class GuidedReadController {
    public interface Progress {
        void stage(int i, String text);
    }

    public interface Source {
        DtcReadResult codes();

        SupplementalReadResult supplement();
    }

    public static final class Result {
        public final DtcReadResult codes;
        public final SupplementalReadResult supplement;

        Result(DtcReadResult dtcReadResult, SupplementalReadResult supplementalReadResult) {
            this.codes = dtcReadResult;
            this.supplement = supplementalReadResult;
        }

        public boolean incomplete() {
            SupplementalReadResult supplementalReadResult;
            return this.codes.incomplete() || (supplementalReadResult = this.supplement) == null || supplementalReadResult.incomplete();
        }

        public boolean transportFailed() {
            SupplementalReadResult supplementalReadResult;
            return this.codes.transportFailed() || ((supplementalReadResult = this.supplement) != null && supplementalReadResult.transportFailed());
        }
    }

    public static Result read(Source source, Progress progress) {
        progress.stage(0, "1 de 2 · consultando códigos e indicador de emissões");
        DtcReadResult codes = source.codes();
        if (codes.transportFailed()) {
            progress.stage(1, "Interrompida · complemento não executado após falha na conexão");
            return new Result(codes, null);
        }
        progress.stage(1, "2 de 2 · consultando prontidão e quadro congelado 00");
        SupplementalReadResult supplement = source.supplement();
        if (!Objects.equals(codes.vehicleId, supplement.vehicleId) || !codes.sessionId.equals(supplement.sessionId) || codes.origin != supplement.origin) {
            throw new IllegalArgumentException("Complemento pertence a outro contexto");
        }
        progress.stage(2, "Consultas encerradas · confira o resultado de cada serviço");
        return new Result(codes, supplement);
    }
}