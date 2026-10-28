# Checkpoint 4.2 — contratos, perfis e catálogo local

Data local: 20/10/2026, America/Sao_Paulo. Logs em UTC de 21/10 correspondem à noite de 20/10 no Brasil.

## Resultado e escopo

Entregue a versão de desenvolvimento **0.6.1, versionCode 8**, pacote `io.rea.obd`, mesmo certificado. Proveniência por atributo, migração de perfis e núcleo de catálogo local implementados. A Garagem ainda usa a interface antiga. Não foram integrados IA, novos transportes, programação de ECU, atuadores, tuning ou material do Carista.

A base de catálogo tem zero fontes/configurações, com cobertura vazia declarada. Os testes usam configurações sintéticas fora de app/. Os novos metadados de perfil ainda não compõem snapshots/exportações de investigações: documentos 1/2/3 e suas regras foram preservados. Consulte [contratos](CONTRATOS-DE-VEICULOS.md) e [catálogo](CATALOGO-DE-VEICULOS.md).

## Validação executada

| Verificação | Resultado |
|---|---|
| Java | 993 verificações: 957 anteriores + 36 de contratos veiculares |
| Android / Robolectric 4.17 | 60 testes: 54 SDK 35 e seis SDK 26 |
| Migração nova | 11 testes de v1/v0/v2, IDs/seleção, edições, ano de fabricação, falha de commit, revisão obsoleta, tipos/corrupção, relatório/caso antigo e cadastro parcial sem Cobalt |
| Catálogo novo | 12 testes de base vazia, filtros, fontes/escopo/permissões, tipos/tamanho, checksum, atualização/arquivo, rollback recusado, corrupção e falha de substituição |
| Build completo | Recursos atuais, R, Java/DEX e alinhamento; assinatura v2/v3, pacote, versão e certificado verificados |
| Continuidade do pipeline | Dez controles passaram, incluindo dois testes da fixture de recursos nos SDKs 26/35; esses dois não integram os 60 testes do app |
| Recursos da APK | Rodada final Android usou a APK 0.6.1 compilada; tabela resources.arsc comparada às fontes atuais |

As regressões anteriores da UI usam explicitamente uma garagem sintética v1. O teste de instalação nova não usa essa fixture e salva um perfil apenas com apelido no Android 26. Não houve remoção de cobertura de histórico para simular aprovação de uma nova instalação.

Os 993 checks Java somam 44 casos + 116 DTCs + 217 sessão + 250 parser + 38 aquisição + 157 regras + 123 complemento + 12 CSV + 36 contratos veiculares. Contagens de checks, testes JUnit e controles de pipeline são categorias distintas.

## Arquivos e resumo do diff desde 4.1

| Caminho | Mudança |
|---|---|
| app/src/main/java/io/rea/obd/model/FactProvenance.java | Novo contrato de origem/verificação, referências, escopo, método e data |
| app/src/main/java/io/rea/obd/vehicle/VehicleAttribute.java | Dez atributos, desconhecidos nulos e anos separados |
| app/src/main/java/io/rea/obd/vehicle/VehicleRecord.java | Perfil atual imutável, revisão, edição e projeção antiga |
| app/src/main/java/io/rea/obd/vehicle/VehicleRecordJson.java | Serialização v2 e validação estrita de campos/tipos |
| app/src/main/java/io/rea/obd/vehicle/VehicleRepository.java | Única classe de execução existente alterada: migração, garagem vazia e persistência v2 |
| app/src/main/java/io/rea/obd/vehicle/CompatibilityEvidence.java | Contrato de afirmação limitada por função; sem integração/persistência |
| app/src/main/java/io/rea/obd/vehicle/catalog/VehicleCatalog.java | Fontes/configurações imutáveis e filtro por linhas reais |
| app/src/main/java/io/rea/obd/vehicle/catalog/VehicleCatalogJson.java | Validação limitada a 512 KiB, escopo e metadados de permissão/revisão |
| app/src/main/java/io/rea/obd/vehicle/catalog/VehicleCatalogStore.java | Armazenamento local AtomicFile, checksum, versões e arquivo histórico |
| app/src/main/res/raw/vehicle_catalog.json | Bootstrap vazio BR; sem dados comerciais fictícios |
| tests/VehicleContractsTest.java | 36 verificações de proveniência e identidade |
| tests/runtime/VehicleMigrationIntegrationTest.java | 11 testes de migração/edição/integridade/cadastro |
| tests/runtime/VehicleCatalogIntegrationTest.java | 12 testes de catálogo/armazenamento; adaptação explícita do host Windows |
| tests/fixtures/vehicle-store-v1.json e README.md | Garagem sintética e documentação da origem |
| tests/runtime/UiChecks.java | Inicialização explícita da garagem antiga para regressões |
| tests/runtime/MainActivityRuntimeTest.java / MainActivityRegressionTest.java / StructuredDtcIntegrationTest.java / SupplementalIntegrationTest.java / DiagnosticCaseIntegrationTest.java / CatalogIntegrationTest.java / PlatformCompatibilityTest.java | Setup explícito de cenário de atualização, preservando as asserções anteriores |
| tests/runtime/VehicleRepositoryTest.java | Primeira execução agora exige garagem vazia; demais cenários mantidos |
| tests/runtime/run.ps1 e test.ps1 | Incluem os contratos e as novas classes de teste |
| tests/checkpoint/run.ps1 | Testes de sobrescrita usam sentinelas isoladas com versão dinâmica, mantendo artefatos históricos protegidos |
| version.properties | 0.6.1 / código 8 para distinguir alteração funcional da 0.6.0 |
| README.md / CHANGELOG.md / docs/ARQUITETURA.md / docs/CONTINUIDADE-REFORMULACAO.md | Estado atual, limites e sequência |
| docs/CONTRATOS-DE-VEICULOS.md / CATALOGO-DE-VEICULOS.md / VALIDACAO-CHECKPOINT-4.2.md | Contratos, dependências, critérios e evidências |

A comparação com os hashes arquivados do checkpoint 4.1 preserva as outras classes de execução, inclusive MainActivity, VehicleProfile, transportes, parser, telemetria, diagnósticos, regras, histórico e serializadores de casos. Não houve migração física do SQLite. As fixtures originais 1/2/3 mantêm seus hashes.

## Falhas encontradas durante o trabalho

O teste de cadastro precisava processar o evento de abertura do diálogo antes de acionar seu botão no SDK 26. O teste foi sincronizado com o looper; o app não precisou mudar seu diálogo.

No SDK 35 simulado, AtomicFile usa File.renameTo; o host Windows não substitui um destino existente como o Android/Linux. O teste recebeu um shadow somente da operação de rename, usando Files.move atômico. As demais operações e a serialização reais continuam exercitadas. Um teste injeta falha nessa substituição e verifica o catálogo anterior intacto. Isso é adaptação de ambiente, não validação do sistema de arquivos de um telefone. A aplicação também confere os bytes instalados depois de AtomicFile.finishWrite, que pode apenas registrar uma falha sem lançá-la.

Leituras getInt/getString permissivas foram substituídas por checagens de tipo nos novos documentos, com rejeição explícita de sequência/revisão fracionária, overflow e valor numérico no lugar de texto. Documentos antigos não foram regenerados para os testes passarem.

## Artefatos e rastreabilidade

- APK atual: `dist/BorbaCar-Labs-0.6.1.apk`, SHA-256 `0fbf3731445ce4c2bad8a326260c5c3c055f12be780375fd7e13ae47c468eaaa`.
- Fontes atuais: `dist/checkpoint-4.2-fonte.zip`; hashes por arquivo e do ZIP em `dist/checkpoint-4.2-validation.json`, externo ao ZIP para evitar referência circular.
- Certificado: `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.
- APK 0.6.0 preservada: `2419cbd156b69b8d7310f4d5b4e4f0ff2fc11d4c4dac6591188bfb451721e200`.
- ZIP 0.6.0 preservado: `28012a124067dd17c080c0c48d0d5a321336ebe00c7324447e8e6167d565c28e`.
- ZIP 4.1 preservado: `097402e763837b44529646cace445c1c8264a11ad8918aac25c70344ed7179b3`.
- Os manifests/relatórios históricos não foram substituídos. Metadados locais por execução ficam em `.local/build`, `.local/unit-tests`, `.local/runtime` e `.local/checkpoint-4.1`, excluídos do pacote junto às chaves/caches.

## Riscos, limites e critérios de avanço

Migração automatizada passou em ambiente simulado; não houve atualização de instalação física, teste de scanner/ECU, leitura no Cobalt, interrupção de energia, PDF em telefone ou revisão mecânica independente. Gradle/Android Studio não foi ensaiado. Avisos Java de obsolescência/unchecked e mensagens Robolectric Invalid ID 0 permanecem sem falha de asserção.

As cópias v1 são preservadas como origem, sem sincronizar alterações posteriores da v2. Não existe restauração/downgrade automático. A falha de commit foi injetada antes da persistência; não equivale a simular todos os efeitos de falha de energia ou SharedPreferences entre processos. Revisões protegem o fluxo atual com uma instância, não concorrência entre processos.

Critérios do 4.2 atendidos: identidade estável e seleção preservadas; desconhecidos continuam nulos; nenhuma instalação nova é identificada como Cobalt; edição não fabrica fontes; anos separados; relatórios/casos antigos intactos; parser recusa fontes fora do escopo e metadados inválidos; falhas de instalação preservam dados; certificado e artefatos antigos mantidos. A publicação de configurações reais permanece condicionada a fontes/escopo/direitos e revisão técnica.

Próximo checkpoint 4.3: Garagem Foco com navegação guiada, “Não sei”, perfil parcial e rótulos de origem. Para habilitar dados de catálogo em perfis e casos, definir o snapshot ampliado sem alterar os formatos antigos. Validar o seletor com fixtures sintéticas explicitamente identificadas nos testes; elas não devem virar opções comerciais no produto.
