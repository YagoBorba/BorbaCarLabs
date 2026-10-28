# Catálogo local de veículos — contrato inicial

## Estado real da base

O recurso `app/src/main/res/raw/vehicle_catalog.json` contém esquema 1, versão `br-bootstrap-1`, sequência 1 e mercado BR. **São zero fontes e zero configurações veiculares publicadas.** A Garagem Foco carrega essa base e informa cobertura/fontes. O cadastro guiado oferece “Não sei” e declaração opcional; não apresenta opções comerciais fictícias. Não foi incorporado conteúdo do Carista, nem um catálogo estrangeiro apresentado como prova de configurações brasileiras.

Bases não vazias usadas nos testes são sintéticas: código de teste e `tests/fixtures/vehicle-catalog-synthetic.json`. `TEST_MAKE`, `TEST_MODEL` e `TEST_ENGINE_*` não são veículos comerciais e não entram na APK. A URL example.org desses testes é um identificador fictício, não uma fonte consultada.

## Formato e publicação

| Nível | Campos |
|---|---|
| Raiz | schema_version, catalog_version, sequence, coverage_note, markets, sources, configurations |
| Fonte | id, title, publisher, document_version, url, section, accessed_on, permission, permission_reference, reviewed_by, reviewed_on, markets, fields |
| Configuração | id, market, attributes |
| Atributo documentado | value, source_id, section |

`catalog_version` é um identificador textual de até 80 caracteres; `sequence` é inteiro positivo. O parser limita o arquivo a 512 KiB e rejeita campos/esquemas desconhecidos, tipos incorretos, fontes/configurações duplicadas, anos inválidos e atributos sem fonte aplicável.

A fonte registra título, editor, edição, localização HTTPS, seção, data de consulta, escopo de mercado e campos cobertos. Há três estados de permissão: PENDING, METADATA_ONLY e APPROVED_EMBEDDED_FACTS. Somente o último pode sustentar fatos embarcados, com referência do direito de uso, revisor e data. Esses metadados são declarações auditáveis: o parser não determina direitos autorais, não consulta a URL e não comprova a qualidade mecânica de uma fonte.

Cada configuração exige fabricante, modelo e mercado documentados. Outros atributos podem permanecer ausentes. Uma fonte de mercado US não sustenta uma configuração BR; nem uma fonte que cobre só fabricante pode sustentar motor ou câmbio. A abrangência é verificada por atributo. O campo NOTES não pertence ao catálogo.

As referências internas registram versão do catálogo, ID da fonte, edição e seção; o escopo inclui mercado/ID da configuração. No 4.3, também congelam título, editor, localização, consulta e metadados declarados de permissão/revisão. Assim, o novo snapshot de caso conserva essas referências mesmo sem carregar a base futura. DOCUMENTED significa documentação da configuração, sem confirmar que o usuário possui essa configuração e sem comprovar suporte OBD.

`matching()` filtra as linhas publicadas por opções conhecidas. Não combina arbitrariamente todos os motores com todos os câmbios. Um filtro nulo significa desconhecido e não restringe o resultado; não preenche os campos omitidos nem escolhe uma variante automaticamente.

O editor filtra fabricante → modelo → ano-modelo → versão → motor → câmbio → combustível. Atributos escolhidos são remapeados à mesma configuração coerente; os não escolhidos continuam desconhecidos. Uma declaração de mercado restringe as opções. Alterar um ancestral limpa dependentes com aviso e revisão. A referência de uma configuração que sustenta um atributo parcial não significa seleção automática dos demais atributos dessa linha. A inclusão futura de outros mercados deve ampliar a escolha explícita de mercado antes das opções de configuração; a entrega atual publica somente o escopo BR vazio.

## Instalação e preservação

`VehicleCatalogStore` lê o recurso embarcado ou um arquivo local válido `files/vehicle-catalog.json`. Sua API recebe um stream local e SHA-256 esperado; não baixa arquivos e não tem tela de importação nesta etapa.

- Validar integralmente o pacote e o hash antes da escrita.
- Exigir sequência maior e versão textual diferente da instalada.
- Arquivar os bytes instalados anteriormente em `vehicle-catalog-<sequência>-<sha256>.json`, recusando arquivo histórico divergente.
- Gravar com `AtomicFile` e conferir o hash após concluir. Uma falha não é apresentada como atualização bem-sucedida.
- Catálogo local corrompido é preservado; a leitura retorna a base embarcada com aviso. Nova instalação fica bloqueada até revisão desse conteúdo.
- Nenhum perfil é alterado por instalar um catálogo. Atualização não muda a seleção, as investigações ou fontes históricas do usuário.

O hash detecta divergência de bytes; não autentica a autoria do pacote. Um mecanismo de distribuição confiável/assinada e sua política de revisão continuam pendentes. Arquivos antigos são preservados, mas ainda não há API de resolução histórica de referências nem política de retenção. Não houve ensaio de interrupção de energia ou atualização física em telefone.

## Inclusão futura de dados brasileiros

Antes de publicar qualquer configuração real, registrar a fonte aplicável ao ano-modelo/mercado, quais atributos ela comprova, limitações da cobertura e direito de redistribuição. Disponibilidade pública ou possibilidade de consulta não concedem automaticamente direito de embarcar uma base. Uma lista comercial pode ajudar a escolher nomes sem comprovar especificações ou compatibilidade OBD.

As avaliações de fontes feitas na auditoria continuam como pesquisa; nenhuma delas foi convertida automaticamente em dados aprovados nesta entrega. A revisão técnica e a autorização de uso são dependências para popular a base. O produto deve funcionar com perfil parcial e “Não sei” enquanto essas dependências não forem atendidas.
