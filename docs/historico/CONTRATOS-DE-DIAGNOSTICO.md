# Contratos propostos para diagnóstico

Atualização 0.6.2: a UI cria casos esquema 4 com `VehicleRecord` congelado e relatório `vehicle-profile-1`. O motor original opera sobre a projeção esquema 3, sem novas hipóteses mecânicas. Documentos 1/2/3 e suas revisões continuam reproduzíveis, sem enriquecimento retroativo. Veja os [contratos de veículos](CONTRATOS-DE-VEICULOS.md) para o snapshot e as [evidências 4.3](VALIDACAO-CHECKPOINT-4.3.md).

Status em 20/10/2026: a leitura da seção 1 foi implementada em 0.3.1. A 0.4.0 implementa o núcleo de casos da seção 2 e a persistência/revisões da seção 5. A 0.5.0 acrescenta prontidão/quadro tipados em esquema 2 e registro de referências de protocolo. A 0.6.0 acrescenta cinco significados e roteiro offline versionado em esquema 3. Hipóteses técnicas, catálogo de causas/procedimentos aplicáveis e modelo continuam propostos. Os exemplos anteriores não substituem os documentos completos emitidos pelo app.

## 1. Resultado de leitura: antes da análise

`DtcReadResult` possui identificador de captura, perfil, sessão, origem, janela de consulta e resultados por serviço. Cada resultado mantém `service`, `outcome`, observações e erro opcional. `outcome` diferencia resposta positiva, ausência de dados, negativa, parsing inválido, timeout, desconexão, consulta não executada e resposta parcial. `model.DataOrigin` é compartilhado com a telemetria, sem duas enumerações da mesma origem.

`history.DtcReadJson` grava o snapshot como `dtc_read`, com `schema_version=1`, nos relatórios novos. Os campos de texto anteriores continuam como saída de compatibilidade; não são a entrada do diagnóstico. Relatórios antigos não recebem uma estrutura presumida.

Horários representam a consulta/observação pelo app, não o instante em que a falha apareceu no veículo. Uma falha de transporte interrompe as consultas seguintes, que ficam `NOT_READ` com horários nulos. Uma resposta negativa ou inválida completa não impede as demais categorias enquanto a sessão continua aberta. Respostas positivas e negativas completas na mesma consulta são preservadas como `PARTIAL`; uma resposta malformada é rejeitada para aquela consulta, sem invalidar os fatos já obtidos nas outras.

Uma resposta positiva com lista vazia confirma apenas que aquela consulta não devolveu códigos. Uma ausência de resposta não prova zero códigos nem falta de suporte. A categoria armazenado/pendente/permanente pertence à observação, não a uma frase traduzida.

Cada `DtcObservation` contém ID, código, categoria, instante e escopo de origem. A mesma sequência de código pode aparecer em categorias diferentes. Como o parser atual opera sem headers e não atribui respostas a módulos identificados, `ecu_id` permanece nulo e `source_scope` indica essa limitação. Não transformar a primeira resposta escalar numa identificação de ECU.

Trecho de uma captura simulada proposta:

```json
{
  "origin": "SIMULATED",
  "vehicle_id": "example-vehicle-001",
  "ecu_id": null,
  "source_scope": "HEADERLESS_OBD",
  "service_results": [
    {"service": "03", "outcome": "POSITIVE", "observations": []},
    {
      "service": "07",
      "outcome": "POSITIVE",
      "observations": [
        {"id": "example-observation-001", "code": "P0301", "category": "PENDING"}
      ]
    },
    {"service": "0A", "outcome": "NO_DATA", "observations": []}
  ]
}
```

O exemplo não inclui descrição técnica do código nem afirmação sobre o Cobalt; uma definição precisa entrar no catálogo por seu próprio processo de fontes e aplicabilidade.

## 2. Entrada: DiagnosticCase

Implementado na 0.4.0: caso imutável com perfil histórico, sessão/origem de DTCs e telemetria, adaptador conhecido, sintomas declarados, datas e revisões. `DiagnosticCase.Sample` rejeita veículo/sessão/origem diferentes. A interface congela as amostras antes da leitura de DTCs; idade se refere a essa captura e não ao momento de abrir o caso. Na 0.5.0, `SupplementalReadResult` anexa prontidão 0101 e cinco PIDs do quadro 00, com resultados individuais e IDs. Sem coleta, `supplement=null`; com coleta, associação DTC e instante original do quadro continuam desconhecidos. Não se une a primeira resposta de PIDs diferentes numa ECU presumida.

Os campos abaixo descrevem o alvo completo; referências/fontes mecânicas recuperadas ainda não estão disponíveis; referências de protocolo são metadados separados.

| Campo / grupo | Conteúdo e regra |
|---|---|
| Identidade | `schema_version`, `case_id`, `revision`, data de criação e intervalo da coleta |
| Perfil | ID e snapshot dos dados conhecidos. Motor, mercado, transmissão e ECU ausentes continuam desconhecidos |
| Contexto de captura | ID de sessão, adaptador conhecido e origem real ou simulada; sem credenciais |
| DTCs | Resultados por serviço e observações tipadas, com leituras parciais preservadas |
| Telemetria | Snapshots de `TelemetrySample`, PID, unidade/definição, valor, qualidade, origem e instante; identidade de perfil/sessão validada no contexto de cada captura |
| Prontidão | Estados suportado/concluído/desconhecido provenientes da leitura, com horário e escopo da resposta |
| Quadro congelado | Número do quadro, amostras e associação DTC apenas quando confirmada pela leitura. Falta de associação não é preenchida com o primeiro código da lista |
| Sintomas | Texto e condições declaradas pelo usuário, com proveniência distinta dos valores medidos |
| Lacunas | O que não foi consultado, não respondeu, ficou antigo ou depende de confirmação do usuário |
| Referências | IDs de registros técnicos recuperados e versões; uma fonte não recuperada não é apresentada como utilizada |

O builder recebe snapshots, não referências a widgets ou mapas mutáveis. Leituras feitas em momentos diferentes mantêm seus horários; o caso não afirma simultaneidade. A identificação do veículo é preservada na resposta assíncrona, mesmo após uma mudança de tela.

O relógio monotônico só é comparável dentro de seu contexto de execução. Para persistência e reabertura, preservar a data UTC e a identidade da captura; não recalcular frescor usando um `elapsedMillis` de outra inicialização. Uma amostra pode ser historicamente válida e estar antiga para representar o estado atual.

Rejeitar mistura silenciosa de veículos ou de dados reais e simulados. Cenários comparativos futuros devem modelar duas capturas separadas. Sintomas são dados de entrada; instruções presentes no texto não concedem autoridade ao analisador.

## 3. Conhecimento e aplicabilidade

Separar:

- `SourceReference`: título, autor/publicador conhecido, versão, localização consultável, seção, origem/licença/permissão conhecida ou pendente e estado de revisão.
- `KnowledgeEntry`: definição ou procedimento, IDs das fontes que o sustentam, versão, escopo padronizado/específico e condições de aplicabilidade.
- `KnowledgeRepository`: consulta por código, parâmetros e configuração, retornando somente registros pertinentes e explicitando limites.

Licença/permissão pendente não significa aprovação para distribuir o conteúdo. O catálogo inicial deve ser pequeno o suficiente para revisão. Informações específicas de um motor não se tornam universais por conterem o mesmo código.

O catálogo de PIDs existente permanece responsável por suas definições/fórmulas. Acrescentar metadados ou um índice de fontes; não criar um segundo conversor para a IA. A faixa da codificação não é um limite mecânico normal/crítico.

RAG inicial pode usar pesquisa determinística por IDs e filtros locais. Banco vetorial só será acrescentado quando houver corpus e avaliação que justifiquem essa complexidade.

## 4. Saída: DiagnosticResult

O contrato completo desta seção continua proposto. A 0.4.0 fornece `CaseReview` com regras `coverage-1`; a 0.5.0 usa `coverage-2` em casos novos e preserva as regras antigas em documentos esquema 1, com `analyzer_type=RULE_ENGINE`, `catalog_version=none` e estado `INSUFFICIENT_EVIDENCE`. Inclui observações, referências aos IDs de DTC/MIL/telemetria, lacunas, verificações e limitações. `hypotheses` e `sources_used` permanecem vazios. Não representa diagnóstico mecânico nem inferência de IA.

`DiagnosticCaseJson` persiste entrada e revisão de cobertura no mesmo documento versionado (esquemas 1 e 2 aceitos). A impressão digital da entrada usa `canonical-json-sha256-v1`, testado para ordenação de chaves e round-trip. A validação confirma o documento completo contra o contrato e a versão de cobertura conhecida; uma versão futura exigirá tratamento explícito.

| Campo / grupo | Conteúdo e regra |
|---|---|
| Rastreabilidade | ID, caso/revisão, versões de esquema, regras e catálogo; identificação do analisador e modelo efetivamente usado, quando houver |
| Correspondência da entrada | Impressão digital da entrada normalizada por algoritmo versionado, a definir/testar antes da persistência final |
| Observações | Fatos com referências aos IDs de observações/amostras. Não usar um sintoma declarado como teste físico realizado |
| Hipóteses | Causa possível, evidências favoráveis/contrárias, informações ausentes, fontes e verificações pertinentes; array vazio é válido quando faltam fundamentos |
| Confiança | Categoria com justificativa; ausente se não há hipótese sustentada. Percentuais não serão gerados como probabilidade sem método avaliado |
| Próximos passos | Perguntas e verificações descritas, com contexto. Nenhum comando livre para execução pelo app |
| Limitações | Dados ausentes, qualidade, cobertura, configuração desconhecida e limite das fontes |
| Fontes utilizadas | IDs resolvíveis do conjunto recuperado; não aceitar ID inventado pelo modelo |
| Estado | Resultado suficiente para investigação, evidência insuficiente ou análise indisponível; sem um estado que signifique “defeito confirmado” apenas por geração de texto |

Trecho de resultado esperado quando o catálogo não possui definição revisada para o código:

```json
{
  "analyzer_type": "RULE_ENGINE",
  "origin": "SIMULATED",
  "status": "INSUFFICIENT_EVIDENCE",
  "summary": "Há um código pendente no cenário simulado; faltam informações para propor uma causa.",
  "evidence_refs": ["example-observation-001"],
  "hypotheses": [],
  "missing_information": ["Definição técnica revisada para o código", "Sintomas relatados"],
  "sources_used": [],
  "limitations": ["Não houve leitura de veículo real", "Uma categoria de consulta não respondeu"]
}
```

O exemplo demonstra a forma de tratar incerteza; não é um diagnóstico nem uma análise já implementada. A validação do esquema final terá campos obrigatórios e testes próprios em D02/M02.

## 5. Persistência e revisões

Implementado na 0.4.0: armazenamento SQLite da plataforma Android para casos, revisões e revisão de cobertura, encapsulado por `DiagnosticCaseStore`. Não é necessário acrescentar biblioteca de persistência externa ou migrar os perfis existentes para começar.

- Preservar `VehicleRepository` e relatórios legados em SharedPreferences. Não converter registros antigos em leituras tipadas presumidas.
- Guardar caso/revisão, contexto de entrada e resultados de análise em transações do mesmo proprietário. Indexar por veículo, caso e data.
- Manter snapshots históricos imutáveis; alterações de sintomas geram revisão vinculada à anterior. Reanálise registra versões e resultado novo.
- Identificar o relatório de leitura de origem quando houver; exibir “Investigações” e “Relatórios de leitura” com relação explícita, evitando duplicatas aparentes ou confusão entre os dois tipos.
- Remover um perfil mantém seu arquivo histórico acessível, seguindo a política atual. Exclusão de investigações precisa de escopo e confirmação próprios; não anunciar exclusão total se alguma etapa falhar.
- Executar I/O fora da thread da interface. Migrações, falha de gravação e retomada devem ser testadas antes de integrar a tela.

O banco `diagnostic-cases.db` está no esquema 1, com tabela `case_revisions` e chave `(case_id, revision)`. Cada inserção confere a revisão anterior e impede mudar medições/perfil/contexto. Consultas verificam checksum do documento, fingerprint da entrada e os índices. Migração futura ou corrupção não têm recuperação destrutiva. Relatórios novos incluem `investigation_seed`; casos e relatórios são ligados por `dtc_read.capture_id`. Relatórios anteriores permanecem sem seed presumido. Um executor de I/O serial com vida de processo ordena a gravação do proprietário anterior antes da leitura da Activity recriada; esse cenário tem regressão própria. JSON/TXT são verificados em simulação; PDF nativo e seletor real exigem telefone.

## 6. Limites do analisador

`DiagnosticAnalyzer` receberá um caso validado e referências pertinentes. Não receberá `MainActivity`, `ElmSession`, `ElmTransport`, adaptador Bluetooth, arquivo de chave ou ação de alteração. O adaptador do modelo retorna dados para validação; não chama ferramentas veiculares.

Se remoto, o payload deve omitir VIN completo e outros identificadores desnecessários por padrão. O usuário deve saber o que será enviado e consentir. Credenciais ficam no ambiente do gateway. Limites de tamanho, timeout, cancelamento, retomada e descarte de resposta tardia fazem parte da integração, não de um tratamento posterior de erros.

Resposta formalmente válida ainda pode conter afirmações incorretas. M02 verifica integridade/referências; a avaliação técnica verifica a sustentação das afirmações. Ambas são necessárias antes de anunciar qualidade de diagnóstico.

## Compatibilidade introduzida na 0.5.0

Documento e entrada esquema 2 contêm `supplement`, nulo quando não coletado. A captura complementar usa esquema próprio 1, com contexto, estado/horários por consulta, bytes de prontidão e observações numéricas por PID. O serviço 02 nunca recebe valores do serviço 01 como substituição. Múltiplas respostas ficam separadas e sem ECU identificada; `associated_dtc`/`frozen_at` permanecem nulos.

O esquema físico SQLite permanece 1 e pode conter documentos 1/2. Um fixture original da 0.4.0 é lido, revisado e comparado sem reescrita dos fatos/resultados antigos. Editar sintomas mantém a versão do caso; coletar dados novos exige nova investigação. A lista de referências `protocol_sources` pertence à análise 2 e não à lista de fontes de causas mecânicas, ainda vazia. A distribuição das referências limita-se a metadados e links.

## Catálogo / análise 3 — 0.6.0

Documento e entrada esquema 3 usam `investigation-1` e `generic-dtc-1`. `definitions` contém ID/código/significado genérico, política de resumo factual, escopo, revisão limitada, aplicação de reparo não estabelecida e referências de evidência/fonte. `checks` contém ID, tipo, texto e referências; os tipos só permitem perguntar ao usuário, conferir a captura ou consultar documentação. Não há ação veicular executável.

`definition_sources` pertence aos significados; não constitui prova de uma causa. `sources_used` de causas e `hypotheses` continuam vazios. As regras sempre retornam evidência insuficiente para confirmar causa; ausência de código não confirma saúde total. O catálogo não transforma procedimentos Ford/Routan em recomendações ao Cobalt.

A validação reproduz o documento com sua versão fixa. Esquemas 1/2 são preservados com as regras anteriores e o banco físico permanece 1. Uma mudança de sintomas conserva a versão e a impressão digital dos fatos. O contrato completo de hipóteses/modelo da seção 4 continua alvo futuro; `DiagnosticResult` implementa a parte de definições e roteiro, sem afirmar que esse alvo inteiro está pronto.
