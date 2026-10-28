# Contratos de veículos — checkpoint 4.3 / 0.6.2

## Implementado e limites

O perfil atual usa `VehicleRecord`, separado da projeção antiga `VehicleProfile`. O repositório persiste atributos com proveniência desde o 4.2. No 4.3, Garagem Foco e `VehicleEditor` usam o contrato completo, com identificação por atributo e três passos. O seletor encadeado usa apenas opções existentes no catálogo revisado; a base real continua vazia. Cadastro parcial e declaração opcional estão disponíveis sem catálogo.

`FactProvenance` descreve a origem de um fato. `DataOrigin` continua descrevendo o contexto real/simulado de uma aquisição. Não são enumerações intercambiáveis. Não houve conversão das estruturas antigas de telemetria, DTCs ou casos para o novo contrato.

## Atributos e origem

| Campo | Representação e regra |
|---|---|
| ID | String estável, independente do nome e de eventual configuração de catálogo |
| Nome | Apelido obrigatório do usuário; não comprova especificações |
| Revisão | Inteiro positivo; uma alteração deve avançar uma revisão |
| MAKE / MODEL | Fabricante e modelo; não extraídos automaticamente de texto antigo |
| MANUFACTURE_YEAR | Ano de fabricação, distinto do ano-modelo |
| MODEL_YEAR | Ano-modelo; corresponde ao `year` da projeção antiga |
| MARKET / TRIM / ENGINE / FUEL_TYPE / TRANSMISSION | Mercado, versão, motor, combustível e câmbio; ausência permanece desconhecida |
| NOTES | Texto do usuário ou preservado do legado; nunca especificação do catálogo |

Os dez atributos sempre existem no mapa. Desconhecido é `value=null` e `provenance=null`, não zero, string de exemplo ou especificação padrão. A projeção antiga representa textos desconhecidos como string vazia para manter seus formatos. Anos conhecidos são quatro dígitos entre 1886 e 2100; esses limites validam a entrada, não a existência de uma configuração.

| Origem / verificação | Requisitos e alcance |
|---|---|
| USER_INPUT / DECLARED | Declaração; não recebe fonte documental nem confirmação automática |
| UNKNOWN_LEGACY / PENDING | Valor antigo de origem não comprovada; sem fonte ou data inventada |
| CATALOG / DOCUMENTED | Referência e escopo obrigatórios; documenta uma configuração, não identifica o carro do usuário |
| LIVE_OBD / OBSERVED | Referência e escopo da observação obrigatórios |
| DERIVED / PENDING | Entradas, método e contexto obrigatórios; não promove cálculo a observação |
| SIMULATION / EXAMPLE | Referência ao cenário e escopo obrigatórios |

Os três últimos contratos gerais existem, mas não estão ligados à coleta atual nesta entrega. `VehicleAttribute` aceita somente declaração, legado e catálogo: VIN ou uma resposta OBD não preenchem automaticamente a configuração do perfil. Desconhecido não recebe proveniência fictícia. Data ausente permanece nula.

## Persistência e migração

`vehicle_store_v2` contém `schema_version=2`, `active_id`, `profiles` e `migration_source`. Cada perfil contém `id`, `name`, `revision` e `attributes`; cada atributo contém `value` e `provenance`. A proveniência contém `origin`, `verification`, `references`, `scope`, `method` e `recorded_at_ms`.

1. Se v2 existe, carregá-la ou reportar erro preservando os bytes. Não retornar silenciosamente a uma cópia v1 desatualizada.
2. Se somente `vehicle_store_v1` existe, preservar IDs, nomes, valores explícitos e seleção ativa. Valores conhecidos entram como UNKNOWN_LEGACY/PENDING. O Cobalt de uma instalação antiga é preservado, sem reclassificá-lo como identificado ou documentado.
3. Se somente `vehicles` existe, preservar nome e texto em observações. O ID usa a mesma fórmula histórica; não interpretar o nome comercial como marca, ano ou motor confirmados. Um único perfil mantém a seleção automática histórica dessa migração.
4. Sem dados antigos, criar garagem vazia, sem Cobalt, perfil de exemplo ou seleção presumida.
5. Persistir um snapshot completo com resultado de `commit()` verificado. Só então atualizar a coleção em memória do repositório. A cópia antiga permanece intacta.

Versão desconhecida, campos desconhecidos na v2, IDs duplicados, seleção inexistente, tipos incorretos e revisões inválidas bloqueiam o carregamento. O app já mostra aviso de armazenamento; não apaga nem recria o conteúdo malformado.

Editar nome ou avançar/voltar não reclassifica atributos intactos. A ação explícita “Declarar informação” pode confirmar um valor legado como declarado, sem convertê-lo em fato documentado e sem reclassificar os outros campos. Alterar fabricante, modelo ou outro ancestral do seletor limpa as escolhas dependentes; o fluxo avisa isso e mostra o resultado na revisão. Ano de fabricação e observações não são inferidos dessas escolhas.

`VehicleDraft` mantém o rascunho sem escrever no repositório. Cancelar não cria perfil. `Bundle` preserva passo, dados e texto de uma declaração ainda aberta durante recriação da Activity; não é um salvamento permanente de rascunhos após encerrar o app. A comparação semântica evita criar revisão só por recriar um editor intacto. `saveSelection` exige revisão esperada e valida novos fatos de catálogo contra a base fornecida, recusando referências forjadas, incoerência e mistura de configurações. Fatos históricos de catálogo intactos podem ser preservados mesmo se uma base nova não os oferecer. A proteção de revisão vale para a instância atual; não é uma transação entre processos ou repositórios independentes.

`vehicle_store_v1`, `vehicles`, históricos, CSVs e bancos não são apagados pela migração. A cópia v1 é uma origem arquivada, **não um espelho atualizado para downgrade**. Voltar à APK antiga não é um procedimento de restauração compatível.

## Casos, relatórios e compatibilidade

Documentos de investigação 1/2/3, suas representações canônicas, regras e SQLite físico 1 permanecem compatíveis. Não ganham origem por atributo, ano de fabricação ou configuração retroativa. Novas capturas da interface produzem **esquema 4**, com a projeção antiga e `vehicle_record` completo, consistência entre ambas, revisão do perfil, origens e referências. O snapshot é obtido na solicitação de leitura, não ao abrir ou exportar a investigação.

O esquema 4 usa o motor esquema 3 sobre sua projeção original, sem alterar `investigation-1`, `CaseReview` ou catálogo DTC. Acrescenta o relatório de identificação `vehicle-profile-1`, congelado em `VehicleProfileReportV1` e separado dos rótulos da UI. JSON/TXT e linhas do PDF da investigação usam o snapshot. O PDF de relatórios recentes também inclui a identificação do seed esquema 4. O renderizador PDF continua sem ensaio físico. Referências de catálogo incluem edição, localização, editor, consulta e metadados declarados de permissão/revisão, preservados dentro do snapshot. Presença desses metadados não comprova sua veracidade nem identifica o veículo.

Editar sintomas de um caso antigo mantém seu esquema; revisar um caso 4 mantém seu perfil capturado. O fingerprint dos fatos inclui a proveniência e impede substituí-la em uma revisão de sintomas. A API antiga `create(VehicleProfile, ...)` continua produzindo esquema 3 para clientes legados; `createWithProfile` produz esquema 4. Um seed antigo sem proveniência não é enriquecido com o perfil atual.

Editar ou remover o perfil atual não reescreve os casos ou relatórios anteriores. Testes carregam documento original esquema 3 no SQLite, migram/alteram/removem o perfil e verificam preservação e acesso ao histórico separado.

`CompatibilityEvidence` é um contrato em memória para afirmações por função: IMPLEMENTED, DOCUMENTED, ANNOUNCED, OBSERVED e PHYSICALLY_VALIDATED. Anúncio/observação exigem veículo, protocolo, adaptador, versão do app e instante; validação física também exige configuração e Android. Não há promoção automática entre categorias. O contrato não produz evidências, não verifica se um teste físico ocorreu e ainda não tem persistência/tela. Nenhuma compatibilidade específica foi adicionada nesta entrega.

## Critérios do checkpoint 4.3

- Seleção encadeada filtra configurações existentes; “Não sei” mantém atributo nulo.
- Perfil parcial disponível mesmo com catálogo vazio; nome/apelido é o único campo obrigatório.
- Seleção/declaração não troca ID nem migra relatórios a outro carro.
- UI distingue declarado, documentado para configuração e legado pendente; não usa um selo global de veículo confirmado.
- Novo snapshot esquema 4 documentado e testado; esquemas antigos e regras preservados.
- Testar troca de etapa, voltar, cancelamento, edição de legado, mudanças de ano/motor, revisão obsoleta e reinício nos SDKs 26/35.
