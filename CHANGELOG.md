# Histórico de alterações

## Em desenvolvimento

- App em português e inglês: novo item Idioma em Ajustes (Automático, Português ou English). Telas, diálogos, notificações de lembrete, relatórios TXT/PDF e catálogo de códigos traduzidos; números seguem o idioma (12,4 ou 12.4). A análise por IA responde no idioma escolhido. Casos salvos não mudam: a tradução acontece só na exibição.

## 1.1.0 / Confiabilidade — 29/10/2026

- Lembretes de manutenção controlados por serviço: um serviço novo não esconde mais o lembrete de outro (troca de óleo + rodízio de pneus), registrar o mesmo serviço sem novo prazo conclui o lembrete e a tela Hoje mostra o próximo a vencer, não o último cadastrado.
- Release só é publicada com certificado: sem os segredos de assinatura o workflow falha antes de criar a versão, e a assinatura é conferida com `apksigner` antes da publicação. Execução manual gera só o APK como artefato.
- Diário descreve os lembretes como funcionam hoje (antes dizia que não havia notificações); prontidão dos monitores e quadro congelado ganham explicação curta na confirmação da leitura guiada, no complemento e nas ferramentas do scanner.
- Análise por IA remove telefones, e-mails, CPF, CNPJ e placas dos sintomas antes do envio; a prévia mostra o texto exato que sai do celular e o que foi removido. Textos e documentos deixam de prometer anonimização completa: nomes e endereços escritos livremente não são detectados.
- Servidor de IA reserva a cota antes de chamar o provedor e grava o consumo do dia em arquivo: pedidos simultâneos não passam do limite e reiniciar o servidor não zera a contagem.
- Servidor de IA com testes de comportamento (provedor falso, sem rede): contrato do pedido, token, cota, erro e demora do provedor, resposta inválida ou com evidência inventada; o CI roda `npm test`. Resposta da IA que não é JSON passa a voltar como 502, não como "JSON inválido" do pedido, e o provedor tem tempo-limite.
- Análise por IA com mensagens próprias de erro: servidor fora do ar não é mais descrito como problema no adaptador Wi-Fi, e resposta em formato inesperado não mostra mais o erro técnico da biblioteca. Novos testes de falha: conexão caindo no meio da leitura de códigos e da gravação, respostas inválidas do servidor de IA e permissão de Bluetooth negada.

## 1.0.2 / Ferramentas — 28/10/2026

- Android Gradle Plugin 8.13.2 e Gradle 8.14.6; o lint mais rígido dessa versão passa sem erros, com as chamadas Bluetooth marcadas onde a permissão já foi pedida pela tela de conexão.
- Testes com Robolectric 4.17 e org.json 20260814. 62 testes.
- Nenhuma mudança visível no app.

## 1.0.1 / Manutenção — 28/10/2026

- Lint do Android sem nenhum aviso: gráfico e ilustração reaproveitam objetos de desenho em vez de criá-los a cada quadro; regras de backup explícitas mantêm casos e diário só no aparelho.
- CI com as versões atuais das actions (fim do Node 20 no GitHub) e rodando também em `develop`.
- Fluxo de trabalho com `develop`, branches de feature/correção e release documentado no README; Dependabot abre as atualizações contra `develop`.

## 1.0.0 / Profissional — 27/10/2026

- Lembretes de manutenção: notificação diária quando o serviço mais recente com lembrete no Diário está a até 7 dias ou 500 km de vencer (ou passou); uma vez por lembrete, sem sair do celular. Liga/desliga em Ajustes; Android 13+ pede permissão.
- Consumo oficial do Inmetro (PBE Veicular 2019) para as 516 versões do catálogo; a tela Hoje mostra cidade/estrada do combustível do último abastecimento e se o consumo medido está abaixo, dentro ou acima da faixa. Perfis escritos à mão e combustível misto não comparam.
- Testes de tela com Robolectric: Activity real, todas as telas principais e um cenário com perfil, Diário e leitura; capturas usadas para ajustar a interface ("Explorar demonstração" no Diagnóstico, ilustração compacta, menos repetição de "simulado" no Ao vivo).
- Código reorganizado: listeners como lambdas, nomes descritivos em variáveis e métodos, `switch` por enum, constantes do Android no lugar de números soltos; lint do Android sem erros.
- Integração contínua no GitHub Actions; aviso de privacidade (LGPD) em `docs/PRIVACIDADE.md` e em Ajustes; release assinado por `keystore.properties` fora do Git. 62 testes.

## 0.8.0 / Dados — 27/10/2026

- Catálogo de veículos `br-pbev-2019-2` (sequência 3): 516 versões, 225 modelos e 34 marcas do ano-modelo 2019, extraídas da tabela PBE Veicular 2019 do Inmetro com fonte e página por atributo. Linhas ambíguas do PDF rejeitadas, não completadas; as 16 versões Chevrolet curadas mantêm seus ids. Elétricos e híbridos ficam de fora. Limite do catálogo passa de 512 KiB para 2 MiB. Escolha de versão mostra também o combustível.
- Catálogo de códigos `generic-dtc-2` (`DtcReference`): 91 códigos genéricos P0 com texto curto original e fonte por entrada (Ford PCED 1999 e boletim VW 24Y2/NHTSA), usado na tela, na consulta (agrupada por área do sistema), na análise por IA e num apêndice rotulado dos relatórios. Códigos de fabricante e entradas com erro na fonte ficam de fora.
- Casos salvos continuam com o `generic-dtc-1` e o motor de regras congelados, porque a abertura recalcula a análise e a compara byte a byte.
- Relatórios na tela e em TXT/PDF com horário local (fuso indicado uma vez) e plurais corretos ("1 código", "2 códigos"); documento salvo e JSON exportado inalterados.
- Geração do catálogo reproduzível em `tools/` (regras, hash do PDF e contagens). 51 testes JVM, incluindo o catálogo real passando pelo validador do app e a validação de casos salvos após a formatação.

## 0.7.1 / Relatório e demonstração — 26/10/2026

- Etapa 7 do plano. PDF com título, seções destacadas com divisória, detalhes recuados, título de seção nunca isolado no fim da página e rodapé "página X de Y" com data de geração. O texto continua o mesmo do TXT.
- Relatório com dados simulados leva a faixa "DADOS SIMULADOS · nenhum veículo real foi consultado" em todas as páginas.
- Layout separado do desenho (`ReportLayout`), com testes JVM de estilos, margens, paginação e faixa.
- Roteiro de apresentação em `docs/DEMONSTRACAO.md` (preparação, ordem das telas, falas, plano B e perguntas prováveis) e README reorganizado como visão geral do projeto, arquitetura, estado do plano, evidências e limitações. 40 testes JVM. PDF não renderizado em aparelho.

## 0.7.0 / IA de demonstração — 26/10/2026

- Etapa 6 do plano: análise por IA opcional nas investigações, para demonstrações. O app envia ao servidor intermediário (`server/`) só o caso anonimizado: tipo do veículo, códigos, MIL, leituras e sintomas; nome do perfil, VIN, notas, adaptador, identificadores e horários não saem do celular. Prévia do conteúdo e confirmação a cada envio.
- Servidor Node com o SDK oficial da Anthropic guarda a chave, exige token, limita tamanho e quantidade diária, usa Claude Opus 5.5 com esforço baixo, saída em JSON estruturado e fallback em recusas.
- Contrato `borbacar-ai-request-1`: cada fato tem referência sintética; servidor e app rejeitam respostas fora do esquema ou que citem evidências não enviadas. Resultado rotulado como gerado por IA e não salvo no caso.
- Endereço e token em Ajustes › Análise por IA. HTTP simples só no build de teste; o release exige HTTPS.
- Provedor alternativo gratuito para testes: `BORBACAR_PROVIDER=gemini` usa a API do Gemini (nível gratuito do Google AI Studio, modelo padrão `gemini-3.5-flash-lite`) com o mesmo contrato e as mesmas verificações. Teste real ponta a ponta em 26/10: análise de um caso de demonstração validada em 3,5 s; `gemini-3.8-flash` estava sobrecarregado (HTTP 503) no mesmo momento. No nível gratuito o Google pode usar o conteúdo enviado.
- Sem IA configurada, nada muda: regras, catálogo e relatórios continuam offline. Testes: 36 casos JVM, incluindo anonimização e rejeição de evidência inventada. Sem chamada real à API nesta entrega.

## 0.6.9 / Clareza — 26/10/2026

- Build migrado para Gradle (AGP 8.7, wrapper incluído).
- Veredito da leitura: "Nenhum código de falha", "N códigos encontrados" ou "Leitura incompleta", com data, MIL e próximo passo. Ressalva de cobertura OBD2 dita uma vez, no veredito.
- Diagnóstico: "Ler só os códigos de falha" visível junto da leitura guiada; "Investigar esta leitura" no veredito. Resultado por serviço, resumo técnico e complemento agrupados em "Detalhes técnicos da leitura".
- Hoje mostra o veredito da última leitura salva e atalho para o Histórico com contagens no singular/plural corretos. Leituras anteriores à 0.3.1 mantêm a linha de texto.
- Datas na tela em horário local ("hoje às 14:32"). Relatórios TXT/PDF/JSON/CSV mantêm seus formatos.
- Confirmações (PDF/CSV salvo, investigação exportada, gravação concluída) em aviso não bloqueante; resultados de prontidão, VIN e quadro congelado com o nome da consulta no título.
- Hoje resume o Diário: consumo do último intervalo cheio a cheio e próximo serviço do lembrete mais recente (data com dias restantes/vencidos e/ou hodômetro), com atalho para o Diário.
- Cartões de código mostram a área do sistema lida da estrutura SAE J2012 (ex.: "Motor e transmissão · código genérico · ignição ou falha de combustão"), também para códigos fora do catálogo. Nenhum significado, causa ou peça é presumido.
- Histórico usa o mesmo título do veredito por leitura; leituras antigas mantêm o resumo de texto. Histórico vazio leva ao Diagnóstico.
- Falhas de Wi-Fi/Bluetooth do sistema (timeout, conexão recusada, socket encerrado, permissão) explicadas em português; mensagens próprias do app inalteradas.
- Valores do Diário em reais formatados ("R$ 1.234,56") e contagens no singular/plural na tela.
- Corrigidos quatro defeitos encontrados pelos novos testes: velocidade (0D), carga (04), posição da borboleta (11) e pressão do coletor (0B) eram calculadas como temperatura (A−40); resposta de quadro congelado de outro quadro não era ignorada; e o leitor do catálogo de veículos recusava toda lista com itens e todo atributo com fonte válida, impedindo carregar o catálogo br-my19-1.
- Testes JVM reproduzíveis (`./gradlew testDebugUnitTest`, 31 casos): sessão ELM com adaptador simulado, parser OBD, ObdReader (poll e complemento), catálogo embarcado real, ida e volta de leitura/complemento/diário, consumo cheio a cheio, estrutura de DTC e formatação.
- Build de teste instala ao lado do app publicado (`io.rea.obd.teste`). Sem teste físico.

## 0.6.8 / Foco — 26/10/2026

- Redução de atalhos repetidos e uma função principal por aba.
- Histórico com coleções separadas; editor/remover nos detalhes do veículo; demonstração em Ajustes.
- Ações secundárias recolhidas, setas de navegação/expansão e apenas uma ação para novo registro no Diário.
- Catálogo factual br-my19-1: 16 configurações Chevrolet Cobalt, Onix e Prisma MY19, com Inmetro por atributo.
- Escolha de versão preenche motor/câmbio/combustível juntos; cadastro parcial continua disponível sem mosaico de campos desconhecidos.
- Catálogo anterior válido preservado e perfis antigos sem enriquecimento automático. Mesmo pacote e assinatura.
- Ver docs/historico/FOCO-0.6.8.md e docs/historico/CATALOGO-MY19-0.6.8.md. Sem teste físico.

## 0.6.7 / Diário — 24/10/2026

- Diário offline por veículo: sintomas, serviços e abastecimentos, datas/horários, valores e hodômetro declarados.
- Revisões transacionais, correção, anulação/reativação, filtros, paginação e preservação após remover perfil.
- Consumo por intervalo cheio a cheio confirmado, incluindo parciais e bloqueando inconsistências; custos ausentes permanecem desconhecidos.
- Rascunhos restaurados e salvamento sem duplicação após recriação; campos bloqueados durante gravação.
- Exportação JSON com revisão prévia do escopo, todas as revisões dos registros selecionados e destino escolhido pelo usuário.
- Banco separado, sem migração de históricos existentes. Mesmos pacote e certificado. Ver docs/historico/DIARIO-0.6.7.md.

## 0.6.6 / R1c e R2a — 24/10/2026

- Identidade visual própria, destaque de veículo genérico, navegação selecionada, diálogos temáticos e hierarquia de ações.
- Movimento de navegação e pressão; preferência de movimento reduzido e respeito à configuração Android. Sem animação de valores medidos.
- Entrada de conexão com identificação do celular, busca e configuração Wi-Fi sem valores presumidos. Ciclo de vida da busca BLE protegido.
- Leitura guiada com progresso por grupo, interrupção por falha e complemento preservado no seed da investigação.
- Comparação de duas leituras estruturadas, por serviço, sem confundir dado ausente com defeito resolvido.
- Formatos históricos, pacote, assinatura e transportes preservados. Ver docs/historico/EXPERIENCIA-0.6.6.md. Sem validação física.

## 0.6.5 / R1b — 23/10/2026

- Identificação automática do celular/Android e apresentação das respostas disponíveis do adaptador; demonstração e identificação não confirmada permanecem explícitas.
- Temas Sistema/Claro/Escuro, paleta por contexto, formulários/diálogos/gráficos e barras do sistema. Preferência local; troca manual bloqueada durante aquisição.
- Ícones vetoriais próprios e navegação focável; estados de botão e cores com contraste testado.
- Detalhes pelo título do sensor; informações de origem/horário/qualidade preservadas.
- Mais oito leituras por acionamento no Histórico; quantidade exibida restaurada sem modificar registros.
- Pacote, certificado, transporte, fórmulas, perfis e casos preservados. Sem validação física. Ver docs/historico/VALIDACAO-R1B.md.

## 0.6.4 / R1a — 21/10/2026

- Hoje como abertura, com perfil e registros reais já existentes; nenhuma pontuação de saúde.
- Navegação Hoje/Diagnóstico/Histórico/Garagem, Ao vivo dentro de Diagnóstico e Ajustes no cabeçalho.
- Históricos e investigações transferidos para destino próprio, preservando consultas, exportações e perfis removidos.
- Rotas legadas restauradas; Voltar respeita a hierarquia. CSV continua pausado fora de Ao vivo. Botões antigos crescem com o texto.
- Sem mudança nos transportes, fórmulas, formatos ou catálogo. R1 parcial: tema claro e refinamentos seguintes ainda pendentes. Validação detalhada em docs/historico/VALIDACAO-R1A.md.

## 0.6.3 / checkpoint 4.4 — 21/10/2026

- Painel Foco extraído em DashboardState/DashboardScreen: estados, origem e horário individual; ausência sem zero fictício, detalhes da captura e cobertura parcial.
- Gráfico com até 90 capturas recebidas, lacunas, seleção por toque/teclado/ações acessíveis, instante UTC e contexto histórico explícito.
- Sessão de aquisição distinta do ID de gravação; formato CSV preservado. Escrita bloqueada fora do Painel/primeiro plano, inclusive callback tardio durante pausa.
- ATRV separado da tensão do módulo, sem inventar horário UTC nem saúde da bateria. Avisos fixos de demonstração/gravação e navegação em duas linhas com fonte ampliada.
- 1.104 verificações Java, 102 testes Android automatizados, dez controles de pipeline e dois testes separados de recursos. Sem validação física.
- Mesmo pacote/certificado; código 10. Persistência, diagnósticos, catálogo, transportes e fórmulas preservados. Próxima etapa: 4.5 Falhas/investigações.

## 0.6.2 / checkpoint 4.3 — 20/10/2026

- Garagem Foco extraída em componentes Java próprios, com busca, identificação por atributo, cobertura/fontes e bloqueio de alterações durante aquisição/demonstração.
- Editor em três passos, “Não sei”, declaração opcional, confirmação explícita de legado, revisão final e rascunho restaurado na recriação da Activity, inclusive texto manual ainda aberto. Cancelar não grava.
- Seletor encadeado usa configurações reais da base fornecida; limpa escolhas dependentes ao mudar ancestral. Validação recusa fontes forjadas, mistura de configurações e revisão obsoleta. Base comercial continua vazia.
- Casos novos da UI em esquema 4, com snapshot completo de perfil e metadados de fonte; revisão não pode alterar esses fatos. Formato histórico vehicle-profile-1 separado da UI. Documentos 1/2/3 e regras existentes preservados; SQLite físico permanece 1.
- JSON/TXT e linhas de PDF usam os metadados capturados. Nenhuma leitura física ou validação de PDF em telefone é declarada.
- 1.026 verificações Java, 87 testes Android simulados (79 SDK 35, oito SDK 26), dez controles de pipeline e dois testes separados de recursos passaram. Seis capturas nativas inspecionadas, incluindo 320px/fonte ampliada.
- Mesmo pacote/certificado, código de versão 9. Próximo checkpoint: Painel Foco. Sem novo transporte, IA, codificação, tuning ou dados proprietários do Carista.

## 0.6.1 / checkpoint 4.2 — 20/10/2026

- Perfil atual `VehicleRecord` com proveniência por atributo, revisão e anos de fabricação/modelo separados. Projeção histórica `VehicleProfile` mantida sem alterações.
- Migração não destrutiva para `vehicle_store_v2`, preservando IDs/seleção e cópias antigas. Instalação nova sem Cobalt presumido; perfil parcial funciona no formulário existente.
- Edição declara somente campos alterados e preserva os demais metadados. Revisão obsoleta e tipos/campos inválidos são rejeitados.
- Núcleo de catálogo local versionado com validação de fontes/escopo/permissão declarada, filtro por configurações existentes, checksum, gravação verificada e arquivo da versão anterior. Base embarcada vazia; sem seleção por catálogo na UI ou dados de terceiros adicionados.
- Contrato de evidência por função separa implementado, documentado, anunciado, observado e validado fisicamente; sem novas alegações de compatibilidade.
- 993 verificações Java, 60 testes Android (54 SDK 35, seis SDK 26), dez controles do pipeline e dois testes de recursos separados passaram. Mesmo pacote/certificado, versionCode 8.
- Nova UI Foco, snapshot ampliado de casos/exportações e validação física continuam pendentes. Casos 1/2/3 e artefatos 0.6.0/4.1 preservados.

## Checkpoint 4.1 — base de reformulação, sem nova versão do app

- Direção visual Foco escolhida; implementação nativa permanece para etapas seguintes.
- Build exige o certificado de desenvolvimento original, não cria chave substituta e bloqueia sobrescrita de APK/ZIP.
- Recursos e R gerado participam do build e dos testes, com suporte explícito a assets e validação contra APK de recursos divergente.
- Saídas de desenvolvimento e classes de teste isoladas por execução. Temporários Robolectric usam caminho curto para SQLite no Windows.
- Fixture original de investigação esquema 3 preservada a partir do ZIP 0.6.0, junto às fixtures 1/2; regressão de coexistência e revisão ampliada.
- Fixture de recursos e verificações de continuidade ficam fora do código da aplicação. Resultados do checkpoint são documentados separadamente da validação histórica 0.6.0.

## 0.6.0 — 20/10/2026

- Catálogo `generic-dtc-1` com cinco significados genéricos, resumos factuais originais e referências localizadas. Causas, procedimentos e limites de outros veículos não são importados.
- Consulta por código nas cartas de falha e busca offline em Mais. Código desconhecido permanece visível sem definição presumida.
- `RuleDiagnosticEngine` gera `DiagnosticResult` com roteiro tipado e referências às observações. Não recebe transporte, não executa comandos e não gera hipótese de peça ou probabilidade.
- Novos documentos usam esquema 3, `investigation-1` e catálogo versionado. Documentos 1/2 mantêm as regras antigas, inclusive em revisões. SQLite físico permanece 1.
- `definition_sources` separa fontes de significado das fontes de causas, ainda vazias. Relatórios JSON/TXT preservam as mesmas definições/evidências do documento salvo.
- Roteiro/relatório vinculado ao snapshot analisado, além de IDs/revisão; resultado de outra entrada com IDs iguais é rejeitado.
- Validação: 957 verificações Java e 37 testes Android simulados (33 SDK 35, quatro SDK 26). Sem ensaio físico, modelo de IA, avaliação mecânica independente ou validação de PDF nativo.

## 0.5.0 — 19/10/2026

- D02 complementado com captura tipada de prontidão 0101 e cinco PIDs do quadro congelado 00. Estados, horários, múltiplas respostas e ausência de dados preservados por consulta; nenhum valor ao vivo substitui o quadro.
- Coleta complementar explícita na aba Falhas, restrita ao contexto da leitura de DTCs. Novos casos congelam o complemento; uma nova leitura limpa o anterior. Transporte perdido preserva o resultado parcial e interrompe o restante.
- Documentos novos usam esquema 2 / `coverage-2`. Documentos originais da 0.4.0 continuam em esquema 1 / `coverage-1`, inclusive após revisão de sintomas. O esquema físico SQLite permanece 1; os fatos antigos não são migrados por suposição.
- Relatório separa horários das consultas e instante de congelamento desconhecido, mantém associação DTC/ECU desconhecida e aponta indicações MIL divergentes sem inventar causa.
- Registro `protocol-links-1` e referências por PID iniciam K01/K02. Somente metadados/links; catálogo de causas, fontes mecânicas aplicáveis e IA continuam pendentes.
- Validação executada: 800 verificações Java e 32 testes Android simulados (29 SDK 35, três SDK 26). Hardware, instalação/atualização e PDF nativo em telefone continuam sem ensaio.

## 0.4.0 — 19/10/2026

- Entrega agrupada: casos com sintomas, snapshots históricos, persistência SQLite, revisões e retomada offline.
- `DiagnosticCase` rejeita mistura de veículo/sessão/origem, preserva zero válido e avalia idade em relação à captura original, sem renovar medições ao abrir o caso.
- Cada revisão de sintomas preserva os fatos. Escritas são transacionais; edições concorrentes antigas e alterações de medições são rejeitadas. Corrupção/esquema futuro não são sobrescritos.
- Aba Falhas inclui criação, lista, detalhes, revisões, exportações e exclusão confirmada por caso. Histórico de leitura liga investigações pelo ID da captura. Perfis removidos mantêm o snapshot acessível.
- `CaseReview` organiza observações, lacunas, verificações e limitações com regras `coverage-1`. Não integra modelo, catálogo de causas ou hipóteses mecânicas. Sintomas continuam declarações do usuário.
- JSON e TXT contêm os mesmos dados/revisão; seletor e cópia de exportação são exercitados em simulação. PDF permanece nativo e não foi validado nesta entrega: Robolectric não fornece a implementação de `PdfDocument` necessária.
- Corrigida a quebra de parágrafos com várias linhas no gerador PDF e a limpeza da leitura atual ao selecionar/remover um perfil.
- Verificação executada: 677 verificações Java e 26 testes Android simulados (24 SDK 35, dois SDK 26). Prontidão e quadro congelado ainda não são anexados aos casos em formato estruturado.

## 0.3.1 — 19/10/2026

- D01 entregue: `DtcReadResult` e `DtcObservation` substituem os textos como fonte dos dados de diagnóstico.
- MIL e categorias de DTC têm resultados próprios: positivo, NO DATA, negativa, inválida, timeout, conexão encerrada, não executada ou parcial.
- Respostas negativas preservam serviço e código sem inventar sua interpretação. Consultas restantes são interrompidas após perder o transporte; fatos anteriores permanecem na tela e no histórico.
- Horários, captura, veículo/sessão e origem são persistidos em `dtc_read`, esquema 1. Campos anteriores são mantidos para compatibilidade; relatórios antigos não ganham fatos presumidos.
- `DataOrigin` passa a ser compartilhado entre amostras e capturas. Demonstração usa a mesma estrutura de dados e continua sinalizada como simulação.
- Cobertura executada: 633 verificações Java e 17 testes Android simulados. Nenhum comando de alteração enviado nos cenários de diagnóstico.

## 0.3.0 — 19/10/2026

- Base própria migrada para a pasta BorbaLab 2 e nome de apresentação BorbaCar Labs; pacote Android e certificado de desenvolvimento preservados.
- Estado da conexão passa a vir da sessão ELM. Erros de consulta encerram a conexão visual e a gravação.
- Gráfico de RPM recebe somente amostras novas e recentes, com horário de aquisição.
- Casos de uso somente leitura em `ObdReader`; ação de apagar códigos separada em `EmissionCodeReset`.
- Amostras tipadas com origem, qualidade e tempos; CSV registra veículo, adaptador, sessão e estados sem converter ausência em zero.
- Garagem com perfil ativo e campos editáveis; histórico por veículo, migração e registros antigos sem associação preservados.
- Versão centralizada e testes Android incluídos no repositório. Tema e insets ajustados para o Android mínimo.
- Cobertura executada: 517 verificações Java e 13 testes Android simulados. Hardware e integração de IA continuam pendentes.

## 0.2.0 — Base importada

Leitor Android independente com SPP/BLE/TCP, PIDs, DTCs, prontidão, VIN/calibração, quadro congelado, gráfico e exportações. Auditoria e plano de evolução preservados em `docs/historico/` como registros históricos.
