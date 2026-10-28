# R1b — aparência, sensores e acesso ao histórico

23/10/2026 · Versão 0.6.5 (12). Continuação da reformulação sobre 0.6.4.

## Entrega

- Ajustes → Aparência: Sistema, Claro e Escuro. Instalações sem preferência acompanham o sistema. A preferência é local e preservada ao reabrir; valor desconhecido retorna ao sistema. Cancelar não altera nada.
- Paleta por contexto, sem cores globais mutáveis. Tema aplicado às telas, formulários da Garagem, diálogos, gráficos e barras do sistema. Botões usam estados visuais de habilitação/foco e resposta ao toque.
- Troca manual exige aquisição/demonstração/gravação encerradas e verifica novamente ao aplicar. A recriação mantém a rota. Uma mudança de configuração feita pelo Android continua sujeita ao ciclo de vida existente; esta versão não acrescenta coleta em segundo plano.
- Ícones vetoriais próprios nas quatro abas e no acesso aos detalhes. Rótulos permanecem visíveis e os destinos são focáveis por teclado.
- Ao vivo: valores principais menores, título do sensor como alvo de detalhes com ícone, descrição acessível e altura mínima. Origem, horário e estado continuam visíveis; zero e ausência preservam o contrato anterior. “Sem curva inventada” foi substituído por “Aguardando leituras”.
- Histórico: Mostrar mais leituras carrega mais oito registros por vez, sem modificar os dados. A quantidade exibida sobrevive à recriação. Exportação continua incluindo o conjunto original.

## Arquivos de aplicativo

Novos: ui/ThemePalette.java; res/values/light-theme.xml; drawables ic_home, ic_diagnosis, ic_history, ic_garage e ic_info. Alterados: MainActivity.java, ChartView.java, ui/UiKit.java, ui/VehicleEditor.java, ui/GarageScreen.java e ui/DashboardScreen.java. Versão em version.properties.

Persistência veicular, casos, serializadores, CSV, protocolos, fórmulas e catálogo não foram alterados. Apenas uma preferência independente de aparência foi acrescentada. Não existe migração de dados históricos. Fontes e APKs anteriores continuam arquivadas.

## Verificações

AppearanceIntegrationTest cobre preferência do sistema/override, aplicação/cancelamento, rota restaurada, bloqueio durante demonstração, renderização dos dois temas, detalhes do sensor, editor, paginação sem mutação, SDK 26 e contraste matemático das combinações de texto da paleta. A suíte anterior continua verificando fonte 200%, dados ausentes, zero, histórico, CSV e navegação. Os testes dos detalhes passaram a clicar no título do sensor, que substitui o antigo botão.

As verificações de contraste têm alvo 4,5:1 para texto principal, secundário, acento e atenção contra as superfícies previstas, texto do botão primário e texto desabilitado. Isso não certifica automaticamente cada pixel/diálogo do sistema ou uso com TalkBack real.

Resultados e hashes finais no manifesto dist/checkpoint-R1b-validation.json. Capturas são renderizações Robolectric com fixtures, não fotografias nem medições em veículo.

## Limites e continuidade

Sem ensaio físico de instalação, atualização, adaptador ou veículo. Não foi alterado o transporte para tentar melhorar conexão sem evidência. Catálogo veicular real continua vazio. Seleção automática de sinais, leitura guiada, comparação de capturas, diário de manutenção, abastecimento, backup e IA permanecem etapas futuras.

Próximo conjunto: leitura guiada por resultados de cada serviço e preparação do ensaio piloto. Necessário identificar adaptador, Android e configuração do veículo antes de declarar cobertura física.

## Identificação automática (orientação do usuário)

Ajustes → Identificação automática lê fabricante/modelo/Android/API do próprio sistema. Em conexão ativa, mostra o tipo usado pelo transporte e as respostas de identidade/protocolo já guardadas na sessão; abrir a tela não executa novos comandos. Nome anunciado e identidade ELM não comprovam modelo comercial/autenticidade. Demonstração não reutiliza esses campos como identificação real. VIN/configuração permanecem sem confirmação automática nesta etapa. Não são lidos IMEI, número de série ou identificadores de publicidade.


Resultado final: 1.104 verificações Java, 118 testes Android Robolectric, 10 verificações do pipeline e 2 testes Android separados de recursos aprovados. A tabela de recursos corresponde à APK final. Uma falha inicial no teste de preferência foi corrigida aguardando a fila de eventos do diálogo antes da asserção; não exigiu alteração no aplicativo. Avisos Java obsoleto/unchecked e mensagens Robolectric de ID zero não impediram a aprovação final.

APK SHA-256: 89b8cdbfa910987b6daa804dabf6f1383a38eddabce4689f84228bdf11dd62eb. Capturas em design/reformulacao-v1/android-R1b/. Sem validação física.
