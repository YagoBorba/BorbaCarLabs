# Checkpoint 4.3 — Garagem Foco e snapshot de identificação

Data local: 20/10/2026, America/Sao_Paulo. Logs UTC de 21/10 correspondem à noite de 20/10 no Brasil.

## Entrega

**BorbaCar Labs 0.6.2 / versionCode 9**, mesmo pacote `io.rea.obd` e certificado de desenvolvimento. Garagem nativa com busca, identificação por atributo e cadastro guiado em três passos. Novo esquema 4 de investigação preserva os metadados do perfil. A versão não inclui redesenho completo de Painel/Falhas/Mais, IA, novos transportes, comandos de codificação, atuadores ou tuning.

O catálogo embarcado **continua vazio: zero fontes e zero configurações**. O fluxo informa a cobertura, oferece “Não sei” e permite declaração opcional. Seleção de configurações foi validada com dados originais sintéticos exclusivamente em testes. Não há especificações comerciais novas nem conteúdo proprietário do Carista na APK.

## Comportamento entregue

- Garagem mostra seleção, nome, identificação parcial, atributos pendentes e compatibilidade específica desconhecida. Buscar filtra os perfis locais sem escrever dados.
- “Ver identificação” exibe cada atributo conhecido/desconhecido e distingue declaração, documentação de configuração e origem legada pendente. “Fontes e cobertura” mostra a base, limites e metadados disponíveis.
- Identidade → configuração opcional → revisão. Apenas apelido é obrigatório. A revisão mostra o que será salvo antes da escrita.
- “Não sei” mantém valor/proveniência nulos. Declaração explícita pode confirmar um valor legado como informado pelo usuário, sem transformá-lo em confirmação documental nem reclassificar os demais atributos.
- Alterar fabricante/modelo/ano ou outro ancestral limpa dependentes, com aviso e revisão. O seletor filtra linhas reais da base fornecida e não monta produtos cartesianos de motores/câmbios.
- Perfil editado mantém ID e seleção. Alterações intactas preservam origem/data. Recriar um editor sem mudanças não fabrica nova revisão.
- Rascunho, etapa e texto de declaração ainda aberta sobrevivem à recriação da Activity. Cancelar não grava. Isso não é uma base permanente de rascunhos após encerrar o app.
- Botões da nova UI crescem com o texto e têm alvo mínimo de 48dp. Formulário rola; ações finais ficam fora da área rolável. Navegação principal passa a crescer em altura com fonte ampliada.
- Alterações de perfil são bloqueadas durante aquisição/demonstração; a condição e a revisão esperada são verificadas novamente ao salvar.

## Integridade dos registros

`vehicle_store_v2` continua com o mesmo esquema; não houve nova migração destrutiva de garagem. A origem v1 permanece arquivada. A API de gravação valida fatos novos de catálogo contra a base revisada fornecida, recusa referências indisponíveis, valores incoerentes e mistura de configurações. Metadados históricos intactos podem ser preservados sem exigir que uma base atual ainda ofereça a opção antiga.

Novas capturas da UI congelam o `VehicleRecord` no momento da solicitação de leitura. O esquema 4 contém a projeção legada e o registro completo, cuja consistência é validada. A origem da medição continua independente da origem dos atributos do perfil: demonstração não se torna medição real por usar um perfil declarado.

O novo relatório `vehicle-profile-1` é congelado em `VehicleProfileReportV1`, separado da apresentação editável da UI. Inclui os anos separados, revisão do perfil, origens e referências. As referências novas de catálogo conservam título, editor, localização, edição, seção, consulta e metadados declarados de revisão/permissão. Esses metadados não são prova automática de autorização ou exatidão mecânica.

O motor `investigation-1` continua operando na sua projeção esquema 3; o adaptador 4 acrescenta o relatório de identificação. `CaseReview`, motor, catálogo DTC e suas regras não foram alterados. JSON/TXT e as linhas do PDF das investigações usam os fatos salvos. O PDF de relatórios recentes também recebe a identificação contida no seed 4. Não há consulta ao perfil atual para atualizar um caso antigo.

Documentos 1/2/3 mantêm reprodução canônica e revisão no próprio esquema. Seeds antigos não ganham proveniência retroativa. SQLite físico permanece versão 1 e aceita coexistência dos quatro formatos. Uma revisão de sintomas não pode alterar o snapshot, inclusive proveniência. O builder legado continua produzindo esquema 3; a UI usa o builder completo para novas capturas.

## Testes e evidências

| Verificação | Resultado |
|---|---|
| Java | **1.026 verificações**, dez grupos; 993 anteriores + 33 de rascunho guiado |
| Android / Robolectric 4.17 | **87 testes**, 79 no SDK 35 e oito no SDK 26 |
| Garagem nova | 18 testes: cancelamento, perfil parcial, legado, declaração explícita, anos, voltar/recriar, campo manual aberto, revisão obsoleta, opções/fontes, combinação inválida, busca, cobertura, bloqueio, compactação e capturas |
| Snapshot novo | Nove testes: formato 4, origens/anos/fontes, tampering, projeção divergente, revisão imutável, coexistência 1/2/3/4, seed capturado e reabertura Android 8 |
| Regressões anteriores | 60 testes mantidos; cadastro antigo adaptado ao novo fluxo e expectativa de novas capturas atualizada para esquema 4 |
| Build e assinatura | Recursos/R/Java/DEX, alinhamento, v2/v3, pacote, versão e certificado verificados |
| APK de recursos | Rodada final usou a APK 0.6.2 compilada e comparou sua tabela às fontes atuais |
| Pipeline | Dez controles passaram; incluem dois testes separados da fixture de recursos, SDK 26/35, fora dos 87 do app |
| Capturas | Seis PNGs da UI Android simulada em design/reformulacao-v1/android-4.3, inspecionados visualmente |

Os checks Java somam 44 casos + 116 DTCs + 217 sessão + 250 parser + 38 aquisição + 157 regras + 123 complemento + 12 CSV + 36 contratos + 33 rascunho = 1.026. Checks Java, testes JUnit e controles de pipeline são contagens diferentes.

O teste de fonte ampliada inicialmente exigia incorretamente duplicação linear de pixels. A aplicação já usava sp. A asserção foi corrigida para verificar configuração 200%, aumento efetivo do texto, alvo mínimo, crescimento dos controles e ausência de corte no texto dos botões. A escala não linear é comportamento documentado do Android recente: [Android Developers](https://developer.android.com/about/versions/14/features#non-linear-font-scaling). Não se reduziu a fonte do produto para fazer o teste passar.

As capturas usam uma garagem legada sintética, explicitamente descrita nas observações do perfil. Não são evidência de instalação, veículo ou scanner. As fixtures históricas 1/2/3 não foram regeneradas. Os testes antigos de reprodução, falha parcial, corrupção, revisões e exportações continuam ativos.

## Arquivos alterados desde 4.2

| Arquivo ou grupo | Mudança |
|---|---|
| app/src/main/java/io/rea/obd/ui/UiKit.java | Novos componentes com tema Foco, crescimento de texto e alvos de toque |
| app/src/main/java/io/rea/obd/ui/GarageScreen.java | Garagem, busca, identificação e fontes/cobertura |
| app/src/main/java/io/rea/obd/ui/VehicleEditor.java | Editor em três passos, opções/declaração, revisão, cancelamento e estado da Activity |
| app/src/main/java/io/rea/obd/vehicle/VehicleDraft.java | Rascunho, dependências, declaração explícita, filtro e revisão |
| app/src/main/java/io/rea/obd/vehicle/VehiclePresentation.java | Adaptador de rótulos atuais da UI |
| app/src/main/java/io/rea/obd/vehicle/VehicleProfileReportV1.java | Formato histórico congelado separado da UI |
| app/src/main/java/io/rea/obd/model/FactProvenance.java / vehicle/VehicleAttribute.java | Igualdade semântica imutável para restaurar sem criar mudanças fictícias |
| app/src/main/java/io/rea/obd/vehicle/VehicleRepository.java | Gravação de seleção validada e coerência entre fatos de catálogo |
| app/src/main/java/io/rea/obd/vehicle/catalog/VehicleCatalogJson.java | Referências de fonte completas congeladas por atributo |
| app/src/main/java/io/rea/obd/diagnostics/DiagnosticCase.java / DiagnosticCaseBuilder.java | Snapshot opcional apenas para novo esquema 4 e preservação em revisões |
| app/src/main/java/io/rea/obd/history/DiagnosticCaseJson.java | Ramo 4, consistência, integridade e relatório, mantendo ramos 1/2/3 |
| app/src/main/java/io/rea/obd/MainActivity.java | Integra componentes, salva/restaura rascunho, captura perfil completo e encaminha exportações; navegação cresce com fonte |
| tests/VehicleDraftTest.java | 33 checks de rascunho, desconhecidos, dependências e declaração |
| tests/runtime/GarageIntegrationTest.java / VehicleSnapshotIntegrationTest.java | 27 testes novos de UI/contratos e persistência |
| tests/runtime/UiChecks.java | Observação de diálogo e capturas com dimensão explícita |
| tests/runtime/VehicleMigrationIntegrationTest.java / CatalogIntegrationTest.java | Atualizam ações da UI e formato das novas capturas, mantendo documentos antigos |
| tests/runtime/run.ps1 / test.ps1 | Novas classes e fontes de teste |
| tests/fixtures/vehicle-catalog-synthetic.json / README.md | Dados originais de teste e proveniência explícita; não embarcados |
| version.properties | Nome 0.6.2, código 9 |
| README.md / CHANGELOG.md / docs/ARQUITETURA.md / CONTINUIDADE-REFORMULACAO.md / CONTRATOS-DE-VEICULOS.md / CATALOGO-DE-VEICULOS.md / CONTRATOS-DE-DIAGNOSTICO.md / INVESTIGACOES.md | Estado atual, contratos e sequência |
| design/reformulacao-v1/ESPECIFICACAO-VISUAL.md | Vincula a implementação nativa sem confundir prévia ilustrativa e produto |
| design/reformulacao-v1/android-4.3/* / este relatório | Capturas e validação da entrega |

O protótipo HTML e as imagens de referência não foram incorporados ao aplicativo. Transporte, leitor OBD, parser, fórmulas, gravação CSV, esquema físico do SQLite e regras antigas permanecem com os hashes anteriores. A lista exata de arquivos preservados/alterados/adicionados está no manifesto do checkpoint.

## Artefatos

- APK: `dist/BorbaCar-Labs-0.6.2.apk`, SHA-256 `d02a773bb3e7c86611ba1e9ce0ddc6925c6379589c821c37df7b7994aaa75ca0`.
- Fontes: `dist/checkpoint-4.3-fonte.zip`, sem caches, chave ou APK antiga misturada; manifesto externo em `dist/checkpoint-4.3-validation.json` com hashes por arquivo e dos artefatos.
- Certificado preservado: `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.
- Artefatos 0.6.0, 0.6.1, checkpoints 4.1/4.2 e fixtures históricas preservados. O manifesto histórico dist/validation.json não foi substituído.
- Metadados locais por execução em .local/build, .local/unit-tests, .local/runtime e .local/checkpoint-4.1; não entram no ZIP de fontes.

## Riscos e limites

Não houve instalação/atualização física, teste com telefone/adaptador/Cobalt/ECU, ensaio de energia interrompida ou avaliação mecânica independente. PDF nativo, TalkBack, teclado físico e variações de teclado de fabricantes continuam sem ensaio físico. Gradle/Android Studio não foi executado. Mensagens Robolectric Invalid ID 0 e avisos Java permanecem, sem falha de asserção final.

Um catálogo sintético estruturalmente aprovado nos testes não autoriza publicar dados reais. Os direitos de uso e a aplicabilidade de novas fontes exigem revisão humana. A API de atualização é local, sem distribuição autenticada ou tela de importação nesta entrega. A base vazia é intencional e não representa cobertura do mercado brasileiro.

Rascunho de Activity não substitui persistência permanente. Revisão esperada protege a instância atual, não concorrência entre processos. A cópia v1 não é espelho das edições v2; aplicativos antigos também não leem o esquema 4. Não existe downgrade automático suportado.

## Critérios atendidos e sequência

Perfil parcial, “Não sei”, ID estável, revisão explícita, rótulos de origem, seleção filtrada, referências congeladas, cancelamento/restauração, fonte ampliada e regressões foram verificados. Histórico original e certificado foram preservados. Não há leitura ausente convertida em zero nem compatibilidade OBD inferida pelo catálogo.

Próximo checkpoint 4.4: Painel Foco, com extração da apresentação, valor/estado/unidade/horário, identificação do gráfico histórico e contexto de gravação. Manter transporte, fórmulas e CSV, testar compactação/fonte e preservar a distinção real/simulado. Novas configurações comerciais dependem de fontes brasileiras aplicáveis, direitos e revisão técnica, em frente separada.
