# Continuidade do plano mestre

> Plano anterior preservado. A prioridade atual está em [Reformulação de produto — outubro de 2026](REFORMULACAO-PRODUTO-2026-10.md). As funcionalidades propostas ali ainda não estão na APK 0.6.3.

Atualizado em 20/10/2026 para a versão 0.6.0. O plano segue com evolução incremental. O [plano mestre](PLANO-MESTRE.md) é o documento de partida do projeto.

| Frente | Estado nesta entrega | Critério para a próxima entrega |
|---|---|---|
| Auditoria e estabilidade | Defeitos A01/A02 corrigidos com regressões; código duplicado `ElmConnection` removido | Validar conexão/reconexão em adaptador identificado |
| Build e testes | Versão central em `version.properties`; runner Android no repositório; mesma assinatura de desenvolvimento da 0.2.0 | Exercitar também o caminho Gradle antes de declará-lo validado |
| Veículos | Perfis editáveis, ID ativo, migração, escopo dos relatórios e compatibilidade desconhecida explícita | Complementar identidade de ECU/VIN com confirmação e proveniência |
| Aquisição | Casos de uso fora dos widgets; amostras tipadas, CSV com origem e horário de captura | Controlador de sessão independente da Activity, pausa/frequência configuráveis e ensaio de hardware |
| Conhecimento | 23 sensores com referência de codificação; `generic-dtc-1` com cinco significados e fontes | Causas/procedimentos aplicáveis e revisão mecânica independente; referências genéricas não confirmam especificações do Cobalt |
| Diagnóstico offline | DTCs tipados, casos/sintomas, revisões SQLite e prontidão/quadro estruturados e roteiro `investigation-1` em casos novos; casos antigos preservam suas regras; sem hipóteses mecânicas | Fontes de causas e procedimentos aplicáveis, avaliação técnica e contrato de modelo |
| AutoSense AI | Não integrado | Contratos validados, fontes recuperadas, provedor/configuração definidos, consentimento de envio e fallback offline |
| RAG | Não implementado | Documentos utilizáveis, metadados e avaliação da sustentação de cada afirmação |
| Painel avançado | Sensores selecionáveis e gráfico interativo | Perfis de painel e alertas com limites documentados ou personalizados explícitos |
| Histórico e laboratório | Histórico por veículo, casos/revisões consultáveis e exportações JSON/TXT; PDF nativo pendente de ensaio | Comparação de logs em modo somente leitura e validação física das exportações |

A próxima fatia de produto é a base de conhecimento e o diagnóstico offline: construir casos de diagnóstico a partir dos dados já tipados, incluir sintomas e separar evidência de hipótese. A integração de modelo virá sobre esse contrato. Não apresentar regras determinísticas como uso de IA e não conceder ao analisador acesso à sessão ELM.

O [plano de execução detalhado](PLANO-DE-EXECUCAO.md) organiza as próximas entregas por dependência, arquivos, testes e critérios de aceitação. Os [contratos](CONTRATOS-DE-DIAGNOSTICO.md) distinguem o resultado de leitura já implementado das partes ainda propostas. A 0.4.0 agrupa o núcleo D02, D03 e o fluxo inicial U01 com revisão de cobertura. A 0.5.0 completa D02 para o escopo 0101/quadro 00 e inicia K01/K02 com metadados de protocolo. A 0.6.0 acrescenta cinco significados genéricos e roteiro offline com evidências em esquema 3. O catálogo de causas/procedimentos aplicáveis e a avaliação mecânica continuam pendentes antes de hipóteses ou integração de IA.

O objetivo acadêmico de 05/11/2026 orienta a prioridade do MVP, mas não torna uma função concluída sem implementação e validação. Os adaptadores físicos, Android do usuário, motor/transmissão e cobertura dos módulos GM continuam sem confirmação.
