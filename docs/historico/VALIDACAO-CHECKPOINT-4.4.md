# Validação — checkpoint 4.4 / Painel Foco

Data: 21/10/2026. Versão 0.6.3 (10), pacote io.rea.obd. Resultado: verificações automatizadas aprovadas; validação física pendente.

## Entrega e alterações

- `app/src/main/java/io/rea/obd/telemetry/DashboardState.java`: novo snapshot de apresentação, origem/tempo/qualidade e contexto de coleta.
- `app/src/main/java/io/rea/obd/ui/DashboardScreen.java`: nova tela nativa Foco, detalhes e controles de gráfico/CSV.
- `app/src/main/java/io/rea/obd/ChartView.java`: capturas finitas, lacunas, seleção estável, teclado/ações acessíveis e eixos.
- `app/src/main/java/io/rea/obd/MainActivity.java`: integração, banners fixos, navegação adaptativa, histórico e guarda de escrita durante pausa.
- `app/src/main/java/io/rea/obd/TelemetryRecorder.java`: somente getter do ID existente.
- `tests/DashboardStateTest.java`, `tests/runtime/DashboardIntegrationTest.java`, `test.ps1`, `tests/runtime/run.ps1`: novos testes e registro nas suítes.
- `version.properties`, README, CHANGELOG, arquitetura/continuidade, PAINEL-FOCO e capturas: versão e documentação.

Comparação com o manifesto 4.3 confirma que apenas os três arquivos antigos de aplicativo acima mudaram. Os dois componentes são novos. Transportes, parser, leitor, catálogo de sensores, fórmulas, fontes do catálogo veicular, perfis, migração, regras, SQLite e serializadores históricos permanecem byte a byte. Nenhum arquivo antigo foi removido. Lista completa e hashes no manifesto externo.

## Critérios de aceitação

| Critério | Evidência automatizada |
|---|---|
| Ausência não vira zero | Estados e limites de idade, zero válido, relógio negativo e leitura parcial nos 78 checks de DashboardStateTest |
| Horários não são atualizados por renderização | Detalhes conservam instante original; refresh e abrir diálogo mantêm contagem do gráfico |
| Gráfico não preenche falhas | Respostas inválidas/intervalos/relógio regressivo geram lacunas; seleção preservada ao deslocar janela |
| Origem/contexto explícitos | Demonstração fixa, gráfico histórico, sensor fora da seleção e ATRV separado do PID 42 |
| CSV compatível | Cabeçalho original, zero válido, campo vazio para NO_DATA, IDs distintos e nenhuma escrita durante pausa |
| Interface compacta/ampliada | SDK 26 em 320dp; SDK 35 com fonte 200%, uma coluna, botões com altura variável e rótulos de navegação sem sobreposição |
| Histórico preservado | Suíte existente de migração/perfis, casos 1/2/3/4, revisões, exportação e corrupção aprovada |
| Continuidade da instalação | Pacote/código/certificado e assinaturas v2/v3 verificados pelo build; instalação física ainda não executada |

## Execuções

- Java: **1.104 verificações**, incluindo 78 novas de apresentação.
- Android: **102 testes Robolectric 4.17** (93 SDK 35, nove SDK 26), incluindo 15 novos do Painel. Recursos verificados contra a APK final.
- Pipeline: **dez controles** de chave/assinatura/sobrescrita/integridade; **dois testes** separados do aplicativo mínimo de recursos. A chave temporária negativa pertence apenas ao ensaio; certificado do app preservado.
- Quatro capturas nativas finais inspecionadas. Corrigida a sobreposição da navegação a 200%; teste passou após correção. Uma asserção inicial esperava texto de origem OBD em fixture simulada: ajustada para conferir a ressalva real sobre identificação de ECU.
- Avisos conhecidos: opções Java 8 obsoletas/deprecated/unchecked e mensagens Robolectric `Invalid ID 0x00000000`; nenhuma falha final de teste.

APK SHA-256: `5c67f808527307645d5eee61f5213b4dec93219f1e25f4eb50d34fc53daa9442`.
Certificado SHA-256: `9217a602ff2c1a5125d8ab1dbbf530f129ae0cdafb4ae6fd57caf47c64630ac3`.

Artefatos: `dist/BorbaCar-Labs-0.6.3.apk`, `dist/checkpoint-4.4-fonte.zip`, `dist/checkpoint-4.4-validation.json`, `dist/checkpoint-4.4-SHA256SUMS.txt`. O manifesto relaciona fontes e execuções. As entregas anteriores são preservadas, incluindo a validação histórica 0.6.0.

## Limites e sequência

Sem teste em telefone/adaptador/veículo, TalkBack físico, PDF físico ou integração de IA. O catálogo comercial permanece vazio. O ID de aquisição é mostrado na interface, mas não foi acrescentado ao CSV; session_id mantém seu significado histórico de gravação. O bloqueio de escrita avalia o estado da tela no callback e não cancela consultas já enviadas. Consulte [contrato do Painel](PAINEL-FOCO.md).

Próxima entrega: 4.5, apresentação de Falhas e investigações. Preservar os fatos e formatos; separar resultados por serviço, histórico, revisão e evidências. Depois: relatórios/exportações e validação física documentada.
