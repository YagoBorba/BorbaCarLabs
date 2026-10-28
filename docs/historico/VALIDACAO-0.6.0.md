# Validação — BorbaCar Labs 0.6.0

Data local: 20/10/2026, America/Sao_Paulo. Pacote `io.rea.obd`, versionCode 7, minSdk 26, targetSdk 35. APK `dist/BorbaCar-Labs-0.6.0.apk`.

SHA-256 final: `2419cbd156b69b8d7310f4d5b4e4f0ff2fc11d4c4dac6591188bfb451721e200`.

## Entrega

Catálogo inicial `generic-dtc-1` com cinco significados genéricos e fontes localizadas. Roteiro `investigation-1` com perguntas/verificações vinculadas às observações. Busca offline e consulta em cada cartão de código. Novos documentos usam esquema 3; antigos preservam esquema/regras. Causas, peças e procedimentos aplicáveis ao Cobalt não são confirmados por esta entrega.

## Verificações executadas

| Grupo | Resultado |
|---|---|
| Regressões Java anteriores | 800 verificações passaram: 44 casos/sintomas, 116 DTCs, 217 sessão, 250 parser, 38 aquisição/qualidade, 123 complemento, 12 CSV |
| `RuleDiagnosticEngineTest` | 157 verificações: cinco significados, fontes resolvidas, códigos desconhecidos/vazios, estados incompletos, preservação das categorias, determinismo, sintomas como dados, imutabilidade e resultado de outro snapshot recusado |
| Regressões Android anteriores | 32 testes passaram, conservando os ensaios de leitura/estabilidade/persistência |
| `CatalogIntegrationTest` | Cinco testes passaram: esquema 3/integridade de referências, documentos originais 0.4/0.5 preservados no SQLite, busca offline/código desconhecido, UI/demonstração/reabertura/exportação e SDK 26 |

Total final: **957 verificações Java e 37 testes Android simulados**, sendo 33 no SDK 35 e quatro no SDK 26. As execuções finais ocorreram depois do vínculo entre resultado e snapshot ser acrescentado. O build independente foi concluído com JDK 21/SDK 35. `apksigner` verificou v2/v3; `aapt2 dump badging` confirmou pacote, versão e SDK. Certificado preservado: SHA-256 `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.

## Evidências e limites

- Os testes fixam expectativas dos cinco códigos e exercitam desconhecido, vazio e leituras parciais. Fontes só aparecem para definições efetivamente utilizadas. A conferência de referências e reprodução de JSON não prova a qualidade mecânica de uma hipótese.
- `definition_sources` registra fontes de significado. Fontes de causas e hipóteses permanecem vazias; `repair_applicability=NOT_ESTABLISHED`. As fontes de outras configurações não são tratadas como procedimentos do Cobalt. O [catálogo documenta os locais consultados](CATALOGO-DTC.md).
- Os fixtures originais 0.4/0.5 são comparados, carregados no SQLite e revisados, sem substituir os documentos anteriores. Novos casos são esquema 3; o banco físico continua 1. Não foi adicionada migração destrutiva.
- As referências das definições e verificações apontam às observações originais. Não há acesso do motor a ElmSession, ações de alteração ou modelo. A montagem do relatório exige o mesmo snapshot imutável; a impressão digital do documento preserva a entrada exportada.
- A saída JSON da interface é comparada com o documento salvo. [Exemplo JSON](examples/investigacao-catalogo.json) e [TXT](examples/investigacao-catalogo.txt) foram gerados em demonstração. A [interface de DTCs](images/catalogo-dtc.png) foi renderizada/inspecionada em Robolectric.
- As mensagens `Invalid ID 0x00000000` e avisos Java de obsolescência/unchecked permanecem no ambiente; o resultado final não teve falha de asserções.
- Não houve scanner real, ensaio no Cobalt, instalação/atualização em telefone, build Gradle, inferência de IA ou revisão mecânica independente. PDF nativo e seletor em telefone continuam pendentes. As verificações de software não comprovam compatibilidade SPP/BLE/TCP ou cobertura de módulos GM.
- O catálogo cobre cinco significados e não uma base ampla de diagnóstico. Não indica peça defeituosa, probabilidade, limites mecânicos ou urgência apenas pelo código. Campos de configuração desconhecidos permanecem desconhecidos. Rascunhos do formulário continuam sem salvamento automático.

Próximo bloco: contrato de análise e validação de modelo sobre esta base, configuração de provedor e consentimento quando houver envio, além de fontes de causas/procedimentos aplicáveis e sua avaliação. M01/M02 e o objetivo de IA continuam pendentes, sem apresentar o roteiro offline como inferência de modelo.
