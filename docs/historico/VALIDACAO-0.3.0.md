# Validação — BorbaCar Labs 0.3.0

Data: 19/10/2026. Pacote Android `io.rea.obd`, versionCode 3. APK: `dist/BorbaCar-Labs-0.3.0.apk`.

SHA-256 da APK final: `e38808ddd92157c52f7989351ee015b49f5cff33d2573680538cf5c53abf215f`.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| `build.ps1` com JDK 21 e Android SDK 35 | Compilou recursos, Java e DEX; alinhou e assinou APK |
| apksigner | Assinaturas v2 e v3 verificadas |
| Comparação com certificado da 0.2.0 | Mesmo SHA-256 público: `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3` |
| Identificador e versão | Mesmo pacote da versão anterior, versão incrementada, nome BorbaCar Labs |
| `ElmSessionTest` | 217 verificações passaram |
| `ObdParserTest` | 250 verificações passaram |
| `ObdReaderTest` | 38 verificações de aquisição e qualidade passaram |
| `TelemetryRecorderTest` | 12 verificações do conteúdo/proveniência CSV passaram |
| `MainActivityRuntimeTest` | Um teste do fluxo Android 35 passou: navegação, demonstração, DTCs simulados, gravação e retorno do segundo plano |
| `MainActivityRegressionTest` | Quatro testes Android 35 passaram: RPM vencido, zero/horário real sem duplicação, falha de conexão encerrando gravação e relatórios isolados ao mudar veículo |
| `VehicleRepositoryTest` | Sete testes Android 35 passaram: migração, identidade desconhecida, persistência/edição, dados corrompidos, históricos sem associação, retenção/exclusão por perfil |
| `PlatformCompatibilityTest` | Um teste Android 26 passou: abertura, perfil e demonstração |
| Interface | Capturas do fluxo em 412 × 915 dp conferidas; demonstração explicitamente sinalizada |

Total: **517 verificações de Java puro e 13 testes Android simulados**. Os dois testes de auditoria que antes constatavam A01/A02 foram convertidos em regressões do comportamento correto: desconectar após perda de transporte e impedir que RPM antigo se torne ponto novo.

Os ensaios utilizam transportes roteirizados ou a demonstração; não houve conexão com adaptador nem envio de comandos a uma ECU real. O comando de apagar DTCs não foi exercitado fisicamente. A validação de instalação/atualização e de SPP/BLE/TCP em telefone permanece pendente, apesar de pacote e certificado coincidirem com a versão anterior.

## Limites e reprodução

Execute `test.ps1` e `tests/runtime/run.ps1` conforme o README. Dependências do harness estão no POM; caches, resultados e imagens ficam em `.local/`. O teste mínimo usa Android 26 e o restante Android 35. Isso não equivale à cobertura de todas as versões/telefones ou de todas as mudanças de configuração.

O build usa nível de linguagem Java 8 e APIs de compatibilidade, gerando avisos de obsolescência no JDK 21. O Robolectric emite mensagens `Invalid ID 0x00000000` durante os testes, sem falha das asserções ou perda das telas conferidas. O atributo de navegação que gerava erro no Android 26 foi movido para recursos `values-v27`, e o último ensaio não apresentou esse erro de tema.

Gradle/Android Studio não foi executado. O ciclo completo de exportar e abrir PDF/CSV pelo seletor de documentos não foi ensaiado em telefone. O conteúdo CSV foi validado em Java; a navegação exercitou seus controles. Veja `VALIDACAO-HARDWARE.md` para os critérios de teste físico.

O ZIP do código exclui `.local`, chaves e caches. A continuidade da assinatura depende da chave preservada localmente; ela não acompanha o pacote do projeto.
