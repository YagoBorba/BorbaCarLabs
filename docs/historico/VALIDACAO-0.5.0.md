# Validação — BorbaCar Labs 0.5.0

Data local: 19/10/2026 (America/Sao_Paulo). Pacote `io.rea.obd`, versionCode 6, Android mínimo 26 e target 35. APK `dist/BorbaCar-Labs-0.5.0.apk`.

SHA-256: `0759cde64f16de8a7eb822d3f680ce55be2d4aa672192f3f0f4d7f9619c9a889`.

## O que foi entregue

D02 passa a incluir prontidão 0101 e cinco PIDs do quadro congelado 00. O botão de coleta é explícito, restrito ao contexto dos DTCs, e o caso congela os resultados antes de salvá-los. Estados de consulta, múltiplas respostas e horários são preservados. O instante original do congelamento e a associação com código/ECU continuam desconhecidos.

Novos documentos usam esquema 2 / `coverage-2`; documentos da 0.4.0 preservam esquema 1 / `coverage-1`. O esquema SQLite permanece 1 e mantém ambos. O registro `protocol-links-1` inicia os metadados de K01/K02, com referências de protocolo/codificação e consulta por PID. Não inclui causas mecânicas, modelo de IA ou conteúdo integral de terceiros.

## Verificações executadas

| Grupo | Resultado |
|---|---|
| Regressões Java da 0.4.0 | 677 verificações passaram: 44 casos/sintomas, 116 DTCs, 217 sessão, 250 parser, 38 aquisição/qualidade, 12 CSV |
| `SupplementalReadTest` | 123 verificações passaram: bits/monitores, layouts, múltiplas respostas, PID/quadro incorretos, estados, zero, protocolo roteirizado, negativa, falha parcial, timeout, vínculo de contexto, divergência MIL e referências por PID |
| Regressões Android anteriores | 26 passaram |
| `SupplementalIntegrationTest` | Seis passaram: esquema 2/JSON e integridade, documento original 0.4.0 no SQLite sem reescrita, fluxo demo com complemento/reabertura/exportação, perda de transporte parcial, nova leitura/mudança de sessão e Android 26 |

Total: **800 verificações Java e 32 testes Android simulados**, sendo 29 no SDK 35 e três no SDK 26. A execução Android final recompilou todo o código após as alterações finais. O teste Java mantém as conversões e a apresentação anteriores cobertas por regressão. Timeout do leitor foi exercitado com o prazo real sobre transporte falso; não envolve veículo.

Build independente com JDK 21/SDK 35 concluído. Assinaturas v2/v3 verificadas; `aapt2 dump badging` confirmou pacote, versão e SDK. Certificado de desenvolvimento preservado: SHA-256 `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.

## Evidências e limites

- Casos anteriores são conferidos com um fixture original e coexistem com documentos novos no SQLite. Editar sintomas conserva a versão e os fatos. Não se completam prontidão/quadro de uma captura antiga com valores atuais.
- O serviço 02 reutiliza a conversão numérica existente; não recebe valores do serviço 01 quando não responde. O suporte anunciado em 01 não determina o suporte ao quadro. Falha de transporte mantém fatos anteriores e marca as consultas seguintes NOT_READ sem horários inventados.
- Respostas de prontidão e de cada PID não têm identidade de ECU. Não são somadas ou unidas por posição entre consultas. Monitores não suportados são diferentes de concluídos; não concluído não equivale a peça defeituosa. Divergência MIL é relatada com seus limites.
- O JSON exportado pela interface é comparado com o documento salvo. [Exemplo simulado JSON](examples/investigacao-complementar.json) e [TXT correspondente](examples/investigacao-complementar.txt) foram produzidos pelo teste. A [interface da coleta](images/coleta-complementar.png) foi renderizada/inspecionada em Robolectric.
- Referências são somente links/metadados, com escopo genérico. A conferência de localização/conversão não substitui revisão mecânica independente ou documento aplicável ao motor/mercado do Cobalt. [Registro de fontes](FONTES-DE-PROTOCOLO.md).
- Mensagens Robolectric `Invalid ID 0x00000000` e avisos Java de obsolescência/unchecked permanecem; o resultado final não teve falha de asserções.
- Não houve scanner, veículo real, instalação/atualização em telefone, build Gradle ou inferência de modelo. PDF nativo e seletor de arquivos em telefone permanecem pendentes, como registrado em 0.4.0. Os testes não substituem ensaio Bluetooth/BLE/TCP ou compatibilidade GM.
- A coleta complementar só é persistida quando um caso é salvo. Uma nova leitura de DTCs limpa o complemento da sessão. O formulário continua sem rascunho automático; dados já salvos e revisões são preservados.

Próximo bloco: fontes de definições/causas de DTCs, aplicabilidade e avaliação técnica, antes de hipóteses ou integração de modelo. K01/K02, R01 e E01 ainda têm partes pendentes; o registro de protocolo não conclui o diagnóstico mecânico.
