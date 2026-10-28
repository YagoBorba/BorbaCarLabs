# Origem e dependências

O código inicial deste repositório é o do leitor REA OBD 0.2.0, implementação própria e independente. A APK de referência 0.2.0 possui SHA-256 `c520044b1d93f109d4cf1f2fe59196d80f0f0b060481c0d7d965584801c30210`.

O aplicativo não usa bibliotecas nativas nem código de terceiros em tempo de execução.

## Inventário de desenvolvimento

| Item | Uso |
|---|---|
| Android SDK plataforma 35 e build-tools 35 | Compilação, recursos, DEX e assinatura no script independente |
| JDK 21 | Compilação Java e ferramentas de build/testes; código usa nível de linguagem 8 |
| AGP 8.9.2 | Declarado para Android Studio; build Gradle ainda não exercitado nesta entrega |
| Robolectric 4.17 | Testes Android simulados; dependências declaradas em `tests/runtime/pom.xml` |
| JUnit 4.13.2 | Execução dos testes Android; fora da APK |

O aplicativo usa APIs da plataforma Android e código local; não declara bibliotecas externas de runtime no Gradle. O POM de testes resolve dependências de desenvolvimento, que mantêm suas próprias licenças e avisos. SDK/JDK e ferramentas não são distribuídos no ZIP do projeto.

O catálogo de sensores conserva a referência técnica em `SensorCatalog.java`; o parser conserva a referência do datasheet ELM. O guia Cobalt contém mensagens selecionadas parafraseadas do manual oficial MY19, com contexto explícito de consulta manual. A próxima etapa deverá registrar fontes/aplicabilidade por entrada; não presumir permissão para republicar documentos completos.

A licença de distribuição do código próprio ainda deve ser definida por seu titular. Nenhuma licença de terceiros foi atribuída automaticamente ao aplicativo ou aos conteúdos citados.

As chaves de assinatura e a configuração local ficam em `.local/`, ignorada pelo Git e excluída do empacotamento. A chave anterior foi preservada nesta máquina para atualizações. O certificado público de desenvolvimento mantém o SHA-256 `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.

Na 0.5.0, o registro [protocol-links-1](FONTES-DE-PROTOCOLO.md) mantém título/publicador/versão/localização/consulta/escopo e política de metadados. Os 23 PIDs têm uma referência consultável; nenhum documento externo foi redistribuído e nenhuma licença de conteúdo mecânico foi presumida.

Na 0.6.0, [generic-dtc-1](CATALOGO-DTC.md) acrescenta cinco resumos factuais originais. Os documentos Ford/Volkswagen/ELM são referenciados por metadados; arquivos/tabelas/procedimentos não são distribuídos. Não há licença presumida para republicar os documentos completos nem aplicação presumida de seus parâmetros ao Cobalt.
