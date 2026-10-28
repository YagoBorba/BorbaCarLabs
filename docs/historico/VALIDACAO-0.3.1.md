# Validação — BorbaCar Labs 0.3.1 / D01

Data: 19/10/2026. Pacote `io.rea.obd`, versionCode 4. APK `dist/BorbaCar-Labs-0.3.1.apk`.

SHA-256 final: `ef43bba7ba0ebe7657a302da4ac5158b37409113cfcdf03ce6bd2038617da993`.

## O que mudou

As leituras de DTCs usam `DtcReadResult`, com categorias e estados separados de texto. O parser continua sendo o proprietário da interpretação de frames. MIL preserva respostas individuais sem atribuir identidade de ECU nem somar contagens como se fossem um único módulo.

O histórico novo recebe `dtc_read` em esquema 1; JSON exporta a estrutura. Texto e lista antiga de códigos são derivados para compatibilidade/PDF, sem serem reinterpretados como entrada de diagnóstico. Não houve migração que atribuísse fatos novos aos registros anteriores.

## Verificações executadas

| Grupo | Resultado |
|---|---|
| `DtcReadTest` | 116 verificações passaram: positivo vazio, NO DATA, negativa, dados inválidos, perda de transporte, timeout real do protocolo sobre transporte falso, categorias distintas, múltiplas respostas MIL, resposta positiva/negativa na mesma consulta, CAN/não CAN, imutabilidade e IDs/contexto |
| `ElmSessionTest` | 217 verificações passaram |
| `ObdParserTest` | 250 verificações passaram, incluindo manutenção do comportamento estrito das APIs escalares |
| `ObdReaderTest` | 38 verificações passaram |
| `TelemetryRecorderTest` | 12 verificações passaram; origem compartilhada não altera o contrato CSV existente |
| Testes Android anteriores | 13 passaram: navegação/demonstração, regressões, perfis/migrações e Android mínimo |
| `StructuredDtcIntegrationTest` | Quatro passaram: snapshot da demonstração restaurado após recriação, falha parcial preservada na tela/histórico, positivo vazio versus NO DATA/negativa, histórico legado preservado ao lado do novo |

Total: **633 verificações Java e 17 testes Android simulados**, sendo 16 no Android 35 e um no Android 26. Build independente com JDK 21/SDK 35 concluído; apksigner verificou esquemas v2 e v3. Pacote mantido e certificado de desenvolvimento preservado.

Os cenários de leitura verificam que `04` não foi enviado. Respostas negativas e inválidas completas permitem consultar as outras categorias enquanto a sessão está aberta. Falha de transporte interrompe o restante, que fica `NOT_READ` sem horários inventados. Resposta malformada invalida aquela consulta; não descarta consultas anteriores válidas.

## Limites

Os ensaios usaram transporte roteirizado ou demonstração. Não houve scanner, veículo ou instalação em telefone real nesta validação. Não foi executado o caminho Gradle nem o ciclo de salvar/abrir documentos pelo seletor Android em telefone. A assinatura de desenvolvimento deve ser preservada para atualizações.

Uma resposta positiva vazia representa somente aquela consulta; não certifica a saúde do veículo. Horários indicam quando o app consultou/observou a resposta, não quando surgiu a falha. NRCs são registrados como código bruto, sem tradução técnica não verificada. Nenhuma hipótese mecânica, base de causas ou integração de IA foi acrescentada em D01.

O framework Robolectric continua emitindo mensagens `Invalid ID 0x00000000` durante a execução, sem falha de asserções. Os avisos Java de obsolescência e os limites de hardware permanecem registrados. Próxima etapa: D02, casos e sintomas sobre esta captura estruturada.
