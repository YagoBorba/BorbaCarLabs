# Catálogo e roteiro offline — 0.6.0

`generic-dtc-1` contém cinco anotações factuais breves escritas para o app. As fontes foram consultadas em 20/10/2026; os documentos, tabelas completas, causas, limiares e procedimentos dos fabricantes não foram incorporados. O registro de protocolo `protocol-links-1` permanece congelado para reproduzir casos antigos.

| Código | Resumo factual | Referência de significado |
|---|---|---|
| P0133 | Resposta lenta no circuito do sensor de oxigênio | Elm Electronics, ELM327DSK, p. 35 |
| P0171 | Mistura indicada como pobre no banco 1 | Ford PCED 1999, seção 4, linha P0171 |
| P0300 | Falhas de combustão aleatórias ou em múltiplos cilindros | Ford PCED 1999, seção 4, linha P0300 |
| P0301 | Falha de combustão indicada no cilindro 1 | Volkswagen 24Y2, p. 3, linha P0301 |
| P0420 | Eficiência do sistema catalítico indicada abaixo do limiar no banco 1 | Ford PCED 1999, seção 4, linha P0420 |

O exemplo de P0133 foi conferido na seção de interpretação de códigos do [datasheet ELM327](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf). Os outros significados foram conferidos nas linhas indicadas do [Ford PCED 1999](https://www.fordservicecontent.dealerconnection.com/pubs/content/~WVX2/~MUS~LEN/14/VX24002.HTM) e na tabela da página 3 do [boletim Volkswagen 24Y2 hospedado pela NHTSA](https://static.nhtsa.gov/odi/tsbs/2012/SB-10072632-3639.pdf).

O escopo é `GENERIC_CODE_MEANING_ONLY`. O Ford PCED é de outra configuração; o boletim Volkswagen é para Routan 2012. Seus limites e procedimentos não se tornam aplicáveis ao Cobalt. O conteúdo do catálogo tem política `ORIGINAL_BRIEF_FACT_SUMMARY`: os resumos acima não reproduzem procedimentos, e os arquivos externos não são redistribuídos. Os metadados das fontes continuam `LINK_METADATA_ONLY`. A aplicação de reparo é `NOT_ESTABLISHED` em todas as definições.

## Uso no aplicativo

Na aba Falhas, cada código recebe **Consultar código**. Em Mais → **Catálogo OBD offline**, consulte a lista ou digite um código válido. A consulta não precisa de conexão e não gera comandos OBD. Um código ausente retorna uma lacuna explícita; não é completado por semelhança com outro código.

Ao salvar uma nova investigação, `RuleDiagnosticEngine` executa `investigation-1`. O roteiro reúne definições conhecidas, referências às observações e perguntas de contexto. As perguntas são de autoria do aplicativo para organizar a coleta de informações; não são procedimentos copiados das fontes. As sugestões são tipadas como declaração do usuário, conferência da captura ou consulta de documentação. Não existe campo de comando executável.

O estado permanece `INSUFFICIENT_EVIDENCE`. `hypotheses` e `sources_used` de causas ficam vazios; `definition_sources` registra separadamente as fontes dos significados. Mesmo com todos os cinco códigos conhecidos, o app não confirma uma peça, uma causa ou a saúde geral do veículo. Não há probabilidades nem classificação mecânica de urgência nesta versão.

## Rastreabilidade e compatibilidade

Novos casos e documentos usam esquema 3, com catálogo/regras fixados. Observações e categorias são preservadas; um mesmo código armazenado/pendente aparece como uma definição com referências a ambas as observações, sem fundir seus estados. Dados ausentes e consultas parciais continuam explícitos.

Documentos das versões 0.4.0 e 0.5.0 permanecem esquemas 1/2 e conservam `coverage-1`/`coverage-2`, inclusive ao revisar sintomas. O SQLite físico continua em esquema 1. A integridade confere a reprodução completa do documento. A montagem de um relatório novo também verifica que o resultado pertence ao mesmo snapshot imutável, além de caso/revisão.

Os testes verificam os cinco significados, desconhecido, ausência de códigos, consultas incompletas, fontes/referências inválidas, declarações contendo instruções, persistência e exportação. Isso é revisão estrutural e conferência de significado; avaliação mecânica independente, procedimentos aplicáveis ao Cobalt, hardware e modelo de IA continuam pendentes.
