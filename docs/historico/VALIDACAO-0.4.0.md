# Validação — BorbaCar Labs 0.4.0

Data: 19/10/2026. Pacote `io.rea.obd`, versionCode 5, minSdk 26, targetSdk 35. APK `dist/BorbaCar-Labs-0.4.0.apk`.

SHA-256 final: `8bb4e7319d3d736c5a2615da46d37c8386bfc0d918b43b9e791c0b101fe45a2f`.

## Entrega agrupada

Núcleo D02 (casos/sintomas e telemetria contextual), D03 (SQLite/revisões/retomada), fluxo inicial U01 e revisão de cobertura parcial R01, sem hipóteses mecânicas ou IA. Prontidão/quadro congelado estruturados, fontes técnicas e catálogo continuam pendentes. O guia de uso está em [INVESTIGACOES.md](INVESTIGACOES.md).

## Verificações executadas

| Grupo | Resultado |
|---|---|
| `DiagnosticCaseTest` | 44 verificações: snapshots imutáveis; contexto de veículo/sessão/origem; zero e dado ausente; idade UTC em relação à captura; revisão de sintomas sem alterar fatos; determinismo; instruções em sintomas tratadas como dados; consultas parciais |
| Regressões D01 e anteriores | 633 verificações Java: 116 DTCs estruturados, 217 sessão, 250 parser, 38 aquisição/qualidade, 12 CSV |
| Regressões Android anteriores | 17 passaram, incluindo 16 SDK 35 e um SDK 26 |
| `DiagnosticCaseIntegrationTest` | Nove passaram: round-trip JSON/integridade, SQLite/reabertura/revisão antiga, corrupção e esquema futuro preservados, exclusão por escopo/legado, falha transacional/alteração de fatos rejeitada, Android 26, perfis removidos/isolamento, recriação durante escrita enfileirada, fluxo UI com sintomas/revisão/exportação |

Total: **677 verificações Java e 26 testes Android simulados**, 24 no SDK 35 e dois no SDK 26. Os testes foram executados após as alterações finais de código. As mensagens `Invalid ID 0x00000000` e avisos Java de obsolescência/unchecked continuam no ambiente; não houve falha de asserções no resultado final.

`build.ps1` concluiu compilação Java/DEX e alinhamento. `apksigner` verificou esquemas v2 e v3. `aapt2 dump badging` confirmou pacote, versão e SDK. O certificado SHA-256 é `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`, preservado das versões anteriores.

## Evidências e limites

- A revisão mantém códigos/categorias, estados, dados nulos, origem e IDs. A alteração de sintomas cria revisão; tentativas antigas de edição ou mudança de medições são rejeitadas. Falha de inserção injetada mantém a revisão anterior.
- A recriação da Activity com escrita ainda enfileirada foi reproduzida por latches. O executor serial com vida de processo garante que a nova leitura do banco ocorra após a gravação anterior. Os auxiliares SQLite são fechados na mesma fila.
- O teste de interface cria um caso simulado, edita sintomas, reinicia a Activity e confere JSON/TXT contra o documento validado. Também verifica restauração de uma exportação pendente, cancelamento e cópia TXT ao destino via ContentResolver de teste. Não é um teste do seletor de arquivos em telefone.
- Os arquivos [JSON simulado](examples/investigacao-simulada.json) e [TXT correspondente](examples/investigacao-simulada.txt) foram gerados pelo teste, sem dados físicos de veículo. A interface foi renderizada em modo gráfico nativo do Robolectric e inspecionada; [captura](images/investigacoes.png).
- PDF permanece com o gerador Android nativo. Uma tentativa em Robolectric resultou em `document is closed!`, pois a implementação nativa necessária de `PdfDocument` não é fornecida nesse ambiente. A geração/renderização de PDF não é declarada validada. O teste final usa JSON/TXT reais de saída; não substitui PDF por um arquivo fictício para anunciar sucesso.
- Não houve scanner, veículo, instalação/atualização em telefone ou ensaio Bluetooth/BLE/TCP nesta entrega. Não houve build Gradle, inferência de modelo ou avaliação mecânica independente.
- Sintomas são declarações, não medições. Campos técnicos desconhecidos continuam vazios. Prontidão/quadro não anexados e ECU não identificada permanecem explícitos. Abrir um caso não renova a leitura.
- O formulário não salva rascunho automaticamente; a persistência é feita ao salvar. A recriação após enfileirar o salvamento está coberta. Os novos casos não convertem relatórios antigos em dados presumidos.

O próximo bloco completa a coleta estruturada complementar e prepara fontes aplicáveis para o catálogo e as regras técnicas. A integração de IA permanece uma etapa posterior, com configuração, consentimento e validação próprios.
