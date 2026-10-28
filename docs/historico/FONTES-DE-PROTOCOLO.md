# Referências de protocolo — protocol-links-1

Consulta registrada em 19/10/2026. O aplicativo distribui somente metadados e links em `SourceReference`/`KnowledgeRepository`; nenhum PDF, CSV/DBC externo ou texto integral foi incorporado. Esse registro inicia K01 e os metadados de K02; não é um catálogo de causas mecânicas.

| ID | Publicador / versão | Escopo e uso | Distribuição |
|---|---|---|---|
| `elm327-dsk` | Elm Electronics / ELM327DSK, v2.2 | Comunicação ELM e significado geral dos serviços; seções Talking to the Vehicle e Interpreting Trouble Codes | Somente link/metadados |
| `css-pid-table` | CSS Electronics / página sem versão editorial, consulta datada | Referência de codificação dos PIDs existentes e exemplo de conversão de RPM; consulta por PID no registro | Somente link/metadados |

O datasheet descreve os serviços genéricos e deixa explícito que suporte a modos/PIDs varia. Essa referência não comprova o transporte ou o hardware do usuário. [Elm Electronics, ELM327 datasheet](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf).

A tabela CSS é referência das conversões já presentes no parser; unidades/faixas de codificação não são limites mecânicos normais ou críticos. Não se cria um segundo conversor para os casos. [CSS Electronics, OBD2 PID Overview](https://www.csselectronics.com/pages/obd2-pid-table-on-board-diagnostics-j1979).

A origem de referência de um PID não estabelece suporte naquele veículo, regras de diagnóstico ou configuração confirmada do Cobalt. As fórmulas/decodificações existentes continuam sujeitas às regressões e à validação física; o registro de links não substitui revisão técnica independente, em especial dos bits de prontidão. O tipo de ignição indicado numa resposta seleciona o layout dos monitores, sem completar os campos desconhecidos do perfil.

A liberação para redistribuir os documentos completos não foi estabelecida. O código não os inclui. Uma fonte de outro fabricante, motor, mercado ou geração não será transformada em especificação confirmada. O catálogo de DTCs com definições/causas e o motor de hipóteses continuam pendentes; `sources_used` de causas permanece vazio. `protocol_sources` identifica separadamente o escopo dos links de protocolo/codificação.
