# Checkpoint 4.1 — base reproduzível e regressões

Data local: 20/10/2026, America/Sao_Paulo. Execuções registradas em UTC em 21/10, correspondentes à noite do dia 20 no Brasil.

## Resultado

Checkpoint concluído no escopo de infraestrutura. Composição Foco registrada. Não houve nova versão funcional nem substituição da interface Android. Pacote io.rea.obd, versionCode 7, versionName 0.6.0 e certificado de desenvolvimento preservados. Os arquivos de execução em app/ e version.properties não foram alterados.

| Verificação executada | Resultado |
|---|---|
| Build independente do app completo | Passou com SDK 35/JDK 21; R gerado compilado, DEX, alinhamento e assinatura v2/v3 |
| Continuidade | Certificado SHA-256 9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3 confirmado antes e após assinar |
| Java | 957 verificações passaram nos oito grupos existentes |
| Regressões Android | 37 testes passaram (33 SDK 35, quatro SDK 26), incluindo documento original esquema 3 junto aos esquemas 1/2 |
| Fixture isolada de recursos | Dois testes passaram, SDK 26/35; inflação de XML/R.id/R.string, leitura R.raw e assets |
| Pipeline do checkpoint | Dez verificações de continuidade/recursos/integridade passaram; incluem os testes de recursos acima, não uma segunda contagem independente deles |
| Sintaxe PowerShell | Seis scripts verificados sem erros de parse |

Os dez cenários do pipeline são: chave ausente sem substituição; APK existente sem sobrescrita; ZIP existente sem sobrescrita; fontes novas com validação antiga recusadas; certificado diferente recusado antes da compilação; fixture com R compilado e assinatura original; recursos executados nos dois SDKs; APK de árvore de recursos diferente recusada; fixture esquema 3 idêntica à entrada do ZIP original; APK/ZIP distribuídos preservados.

## Alterações e resumo do diff

| Arquivo | Alteração |
|---|---|
| build.ps1 | Remove geração automática de chave; verifica política/certificado; compila R; usa saída nova por execução; verifica pacote/versão/alinhamento/assinaturas |
| build-policy.json | Registro público do pacote, alias e fingerprint esperado; não contém chave privada |
| scripts/build-common.ps1 | Compilação compartilhada de recursos/manifesto/R/assets e hash de entrada ZIP |
| test.ps1 | Classes isoladas por execução e metadados do resultado |
| tests/runtime/run.ps1 | Recursos atuais e R; recusa tabela divergente; isolamento; temporários curtos para SQLite; metadados finais |
| scripts/package.ps1 | Destino novo obrigatório; inclusão de design/política; impede misturar fontes atuais com validação/APK antiga |
| tests/runtime/CatalogIntegrationTest.java | Acrescenta documento original esquema 3 ao ensaio de coexistência/revisão sem modificar o original |
| tests/fixtures/diagnostic-case-v3.json e README.md | Documento original extraído do ZIP 0.6.0 e registro de proveniência/hashes |
| tests/checkpoint/run.ps1 | Cenários de bloqueio, recursos e preservação dos artefatos |
| tests/checkpoint/resource-fixture/* | App mínimo exclusivamente de teste, fora de app/ |
| tests/runtime/ResourceFixtureTest.java | Dois ensaios Android da fixture de recursos |
| README.md, CHANGELOG.md e docs/CONTINUIDADE-REFORMULACAO.md | Procedimentos atuais, escopo e sequência de checkpoints |
| design/reformulacao-v1/ESPECIFICACAO-VISUAL.md | Escolha Foco e continuidade registradas |

Nenhuma classe de transporte, parser, domínio, persistência ou análise da aplicação foi alterada. A fixture mínima não é uma reconstrução do app nem uma APK para instalar sobre o aplicativo real.

## Artefatos e evidências

- APK distribuída 0.6.0 preservada: 2419cbd156b69b8d7310f4d5b4e4f0ff2fc11d4c4dac6591188bfb451721e200.
- ZIP distribuído 0.6.0 preservado: 28012a124067dd17c080c0c48d0d5a321336ebe00c7324447e8e6167d565c28e.
- Build local do app completo: SHA-256 63ab3aa9fb90af49c44c300d2f4da2fa4383c2c7b50a657322399174b044c0e0. É um rebuild de desenvolvimento, não substitui a release arquivada. Sua diferença binária não é apresentada como nova funcionalidade.
- Metadados por execução e ponteiros locais: .local/build/latest.json, .local/unit-tests/latest.json, .local/runtime/latest.json e .local/checkpoint-4.1/latest.json. Não são empacotados com chaves/caches.
- Logs finais Java: 44 casos + 116 DTCs + 217 sessão + 250 parser + 38 aquisição + 157 regras + 123 complemento + 12 CSV = 957.

## Falha encontrada e corrigida

A primeira execução Android após introduzir diretórios longos por execução falhou ao abrir bancos SQLite de testes com nomes extensos no Windows. Foi mantido o isolamento, encurtando somente a raiz dos temporários para .local/rt/<id>. A execução final completa passou. Nenhum teste foi removido/desabilitado, nenhum banco do usuário foi apagado e nenhuma correção foi inserida no código do app para esconder a falha de ambiente.

## Riscos e limites

- Gradle/Android Studio permanece sem ensaio. Repetibilidade do procedimento não significa build determinístico byte a byte.
- Avisos Java de obsolescência/unchecked e mensagens Robolectric Invalid ID 0x00000000 permanecem; não houve falha de asserção na execução final.
- Chave de desenvolvimento continua fora do pacote. Foi criada somente uma chave descartável em .local/ para testar a rejeição de certificado diferente; ela não é utilizada para assinar a aplicação real.
- A versão não foi incrementada porque não há nova entrega funcional. Um rebuild/fixture em .local/ não é uma atualização a distribuir ao usuário.
- O catálogo brasileiro, novos contratos Java, migração, cadastro sem Cobalt inicial e UI Foco ainda não foram implementados. Nenhuma fonte/direito de uso foi presumido.
- Não houve instalação/atualização física, scanner, ECU, Cobalt, PDF nativo em telefone, IA ou revisão mecânica independente.

## Critérios atendidos e próximo checkpoint

Build completo verificado; certificado original obrigatório; artefatos distribuídos imutáveis; recursos preparados para a futura UI; fixture histórica 3 preservada; regressões passaram; código e dados de execução da aplicação preservados.

Próximo checkpoint 4.2: contratos/proveniência e catálogo local com migração compatível. A inclusão de dados depende de fonte, escopo e autorização de uso estabelecidos. A interface seguirá Foco após esses contratos e a migração serem verificados.
