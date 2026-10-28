# Diário do carro — 0.6.7

24/10/2026 · pacote io.rea.obd · versionCode 14.

## O que foi entregue

Diário acessível por Hoje, Histórico e Garagem, sem scanner. Registros são declarações do usuário, não telemetria, validação de serviço ou diagnóstico. A identidade visual, temas, animações e navegação da 0.6.6 são preservados.

| Registro | Campos |
|---|---|
| Sintoma | Descrição, data/horário e hodômetro opcional |
| Serviço | Descrição, data/horário, hodômetro e valor opcionais; próxima data e próximo hodômetro opcionais, declarados pelo usuário |
| Abastecimento | Data/horário, hodômetro, litros, combustível ou Não sei; valor e observação opcionais; tanque cheio/parcial e confirmação de completude do intervalo |

Todos pertencem a um ID de veículo e conservam o nome informado na criação. Correções criam revisões; anulação e reativação também. Registros anulados continuam visíveis e deixam de entrar nos totais/cálculos. Nenhuma dessas operações apaga as revisões anteriores. Perfis removidos mantêm o diário em uma consulta separada, com detalhes, revisões e exportação, sem reatribuição automática.

O filtro seleciona sintomas, serviços, abastecimentos ou todos. Paginação acrescenta 20 registros por vez. Total de custos usa somente revisões vigentes não anuladas com valor informado; ausência de custo não é zero. Prazos são anotações, sem notificações nem recomendação de intervalo universal.

## Consumo: método tanque cheio v1

Somente o intervalo encerrado no último abastecimento pode gerar média. Exige tanque cheio inicial e final e confirmação explícita no fechamento de que todos os abastecimentos intermediários foram registrados. Soma litros dos abastecimentos parciais e do fechamento, excluindo os litros da abertura. Divide a diferença de hodômetro por esse volume e arredonda a duas casas decimais.

Período aberto, ausência de abertura, confirmação ausente, horários iguais, hodômetro regressivo, distância não positiva e revisões duplicadas não geram média. Não existe interpolação de lacunas. Combustíveis misturados ou desconhecidos são indicados; não há conversão por composição, MAF, autonomia nem consumo instantâneo. Valores, completude e condição de tanque são declarados, não detectados pelo scanner.

O detalhe do cálculo informa distância, litros, método e IDs/revisões utilizados. Anulações no período são identificadas como exclusões. Uma correção recalcula o resumo atual sem reescrever o registro antigo. Isso não comprova consumo físico exato nem que o usuário realmente registrou todos os abastecimentos.

## Persistência e integridade

- Banco novo e independente `vehicle-journal.db`, esquema 1; não migra nem modifica os arquivos históricos de OBD, perfis e investigações.
- Revisões append-only em transação SQLite, com chave composta ID/revisão e checksum de conteúdo. Alteração concorrente, troca de veículo, mudança do tipo ou tentativa de sobrescrever revisão é rejeitada.
- Datas e números mantêm representação explícita; valores opcionais usam null. Decimais são BigDecimal, sem NaN/Infinity, sem separador de milhares e com até 3 casas para km/litros e 2 para reais. O limite técnico por número é 1 bilhão, não uma especificação de capacidade do veículo.
- Falha de leitura/checksum não recria o banco nem descarta conteúdo. Formato futuro desconhecido também é bloqueado.
- IO serial fora da thread de interface. Rascunho, filtro e paginação sobrevivem à recriação; nenhum salvamento é disparado ao cancelar. Recriação durante gravação conserva o ID e uma nova tentativa não duplica o registro.
- Novos registros durante demonstração são bloqueados; registros já declarados continuam consultáveis com indicação específica. Nenhum comando OBD é enviado pelo diário.

## Exportação

JSON próprio `borbacar-journal-export`, versão 1, com todas as revisões dos registros selecionados: um registro ou o diário do veículo ativo. Antes de preparar o arquivo, a interface informa que nomes, notas, valores e revisões anteriores serão incluídos. O usuário escolhe o destino pelo Android. Cancelamento ou término limpa o arquivo temporário daquela operação.

Não envia dados automaticamente, não usa permissões novas e não exporta a base completa de outros módulos. É exportação de declarações, **não backup restaurável do aplicativo**. Importação e restauração ainda não estão implementadas. Um processo encerrado abruptamente pode deixar temporários privados no cache do Android.

## Interface e validação

Editor rolável, seletores de data/horário, campos com rótulos acessíveis, combustível com Não sei, erro visível e alvos de toque nativos. Cabeçalho acompanha a rolagem em fonte ampliada; respeita barras/recortes do sistema e ajuste para teclado. Acesso permanece dentro da aba Histórico, com Voltar para o histórico.

Testes cobrem cálculos, ausência, zero válido, revisão, anulação/reativação, completude, regressão, mistura, isolamento por veículo, corrupção sem reset, cancelamento, rascunho, recriação durante gravação, exportação/cancelamento, temas, fonte 200% e Android 8. Capturas usam fixtures; não são registros de uso real do usuário.

## Arquivos

Novos: journal/JournalEntry.java, FuelSummary.java, JournalJson.java e JournalStore.java; ui/JournalEditor.java e JournalController.java; tests/JournalTest.java e tests/runtime/JournalIntegrationTest.java. Integração em MainActivity.java e ui/NavigationState.java; inclusão dos testes em test.ps1 e tests/runtime/run.ps1; versão e documentação atualizadas.

Não foram alterados transporte, parser, fórmulas OBD, catálogo, exportadores antigos nem banco de investigações. Mesmos pacote e certificado. Artefatos anteriores são preservados.

## Pendências do plano maior

O diário é uma parte de R3/R4/R5, não o encerramento dessas etapas. Faltam linha do tempo unificada com leituras/casos, vínculos entre serviço e investigação, anexos, lembretes/notificações, manutenção documentada por variante, catálogo brasileiro revisado, backup/restauração, sessões duradouras e AutoSense. Catálogo de veículos reais continua vazio. Compatibilidade de scanner/veículo, atualização em telefone, teclado e uso com TalkBack precisam de ensaio físico; não foram declarados como comprovados.

Manifesto da entrega: dist/checkpoint-diario-0.6.7-validation.json. Fontes: dist/checkpoint-diario-0.6.7-fonte.zip.


## Resultado final da validação

1.148 verificações Java, 141 testes Android Robolectric, 10 verificações do pipeline e 2 testes Android separados de recursos aprovados. Os testes finais usam os recursos da APK desta entrega. A revisão visual cobriu os dois temas, formulário de serviço, abastecimento, resumo de consumo e fonte ampliada; os campos ficam bloqueados durante gravação. Avisos Java obsoleto/unchecked e mensagens Robolectric de ID zero foram não fatais.

APK SHA-256: ae3a25a74a7463d7609dbe001b80a3de90f0dbd7bbc2c7ec03cfff2ea5b39ce6. Capturas em design/reformulacao-v1/android-0.6.7-diario/. Não houve validação física.
