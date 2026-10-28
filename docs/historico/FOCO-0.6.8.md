# Foco — revisão de navegação e cadastro

26/10/2026 · 0.6.8 / versionCode 15 · io.rea.obd.

## Problema corrigido

A 0.6.7 repetia destinos da barra em botões, colocava funções secundárias no mesmo nível das principais e oferecia um seletor sem dados. Esta revisão reduz a densidade inicial, organiza as funções existentes e publica uma primeira base factual limitada. Não acrescenta novos módulos ao escopo principal.

| Local | Função e caminho |
|---|---|
| Hoje | Veículo ativo, estado da conexão e última atividade. Uma ação contextual: adicionar veículo diretamente ou conectar scanner. Sem atalhos para Histórico, Garagem ou Diário |
| Diagnóstico | Conectar e iniciar leitura guiada. Ao vivo é um destino interno; leitura avulsa, resultado por serviço e ferramentas ficam em grupos recolhidos |
| Histórico | Seletor entre Leituras, Investigações e Diário. Apenas uma coleção por vez; exportação e gerenciamento ficam recolhidos |
| Garagem | Adicionar, selecionar e abrir os detalhes de um veículo. Editar, origem e remover ficam nos detalhes. Busca aparece quando há mais de um perfil |
| Ajustes | Aparência, movimento e identificação do dispositivo. Demonstração tem entrada única aqui; informações de versão/referências ficam recolhidas |
| Diário | Um botão Novo registro abre a escolha Sintoma, Serviço ou Abastecimento; exportações ficam em Opções do diário |

Linhas de ação secundárias usam setas; grupos recolhidos indicam expansão/recolhimento e saem do fluxo visual/acessível enquanto fechados. A barra continua com os mesmos quatro destinos e os IDs de navegação anteriores. O Histórico restaura a coleção de investigações após recriação. Temas, animações com movimento reduzido e identidade gráfica própria permanecem.

## Cadastro com escolhas úteis

1. Marca, modelo e ano-modelo em seletores compactos. O apelido é opcional quando um modelo foi escolhido; um perfil simples sem modelo ainda precisa de nome.
2. Escolha explícita de uma versão documentada preenche motor, transmissão e combustível juntos. Outros detalhes são opcionais e ficam recolhidos. Não saber a versão não impede criar um perfil parcial.
3. Revisão mostra os valores preenchidos e suas origens; a identificação completa com fontes pode ser expandida. Cancelar nunca salva.

“Não sei” continua como opção de escolha, sem ocupar vários cartões no formulário. Ausências continuam null; valores antigos não são apagados nem reclassificados. A seleção do catálogo é feita pelo usuário e não é identificação automática do veículo. Consulte [cobertura, fonte e critérios](CATALOGO-MY19-0.6.8.md).

## Preservação e testes

Mesmos pacote, certificado e bancos. Nenhuma alteração em transporte OBD, comandos, parser, fórmulas de telemetria, diário persistido, investigação ou formato de exportação. Catálogo 1 → 2 não reescreve perfis. O código mantém os formatos históricos e rascunhos; salvar um perfil continua exigindo revisão coerente.

Regressões passam pelos novos caminhos reais: demonstração em Ajustes, edição no detalhe do carro, diário no Histórico, consultas em grupos expansíveis. Novos testes verificam limite de ações visíveis, ausência de destinos duplicados, preenchimento de uma linha factual, fontes por atributo, nulidade do ano de fabricação, invalidação ao trocar modelo, catálogo anterior preservado, temas e fonte ampliada no SDK 26. Capturas são renderizações nativas com fixtures, não uso real do usuário.

Não foi realizado teste físico de instalação/atualização, TalkBack, teclado, scanner ou veículo. Testes simulados não comprovam compatibilidade. A base continua pequena; ampliar marcas/anos exige curadoria, não preenchimento por suposição.

Resultado da suíte: **148 testes Android Robolectric e 1.148 verificações Java aprovados**. Na revisão visual, foi corrigida a quebra do texto Investigações no seletor do Histórico. No pipeline, uma conversão de codificação do script de teste alterou a mensagem esperada de bloqueio; o arquivo foi restaurado da base anterior, preservando somente a adição da nova classe de testes. A tabela de recursos testada corresponde à APK final. Nenhuma dessas falhas foi tratada como aprovação.

## Arquivos e entrega

UI: MainActivity, HomeScreen, GarageScreen, VehicleEditor, DashboardScreen, JournalController, UiKit, três vetores próprios. Dados: VehicleDraft, VehiclePresentation, VehicleCatalogStore, vehicle_catalog.json e scripts/catalog-my19.py. Testes: regressões de caminhos e UsabilityIntegrationTest. Documentação e versão atualizadas.

Manifesto e hashes: dist/checkpoint-foco-0.6.8-validation.json e checkpoint-foco-0.6.8-SHA256SUMS.txt. Capturas: design/reformulacao-v1/android-0.6.8-foco/. O pacote da 0.6.7 permanece arquivado sem alteração.


Empacotamento final: 1.148 verificações Java, 148 testes Android, 10 verificações do pipeline e 2 testes de recursos separados aprovados. APK SHA-256: e9795756879d424cfc4982e41aab4105af658300dc423dbb3f7ab11908199e2c. Preservação de artefatos e verificação do ZIP registradas no manifesto da entrega. Sem validação física.
