# BorbaCar Labs 0.6.6 — identidade, interação e leitura

24/10/2026 · pacote io.rea.obd · versionCode 13.

## Entrega

A solicitação de melhorar a identidade visual passa à frente da expansão funcional. Esta versão entrega uma revisão nativa e três avanços funcionais delimitados. Não declara concluído todo o plano R1–R7.

- Identidade própria: monograma B, grafismo vetorial de veículo genérico, superfície de destaque, bordas, tipografia, ritmo e seleção de navegação consistentes nos dois temas. O desenho não representa a marca, modelo ou versão do perfil.
- Hoje: veículo e próxima ação em destaque, estado de conexão e acesso direto a Ao vivo. Garagem utiliza o mesmo grafismo e componentes.
- Diagnóstico: atalhos, conexão, leitura principal, resultados por serviço e consultas avulsas em grupos. Contagem ausente permanece traço; zero só aparece em resposta positiva vazia.
- Ao vivo: em sessão ativa, sinais vêm antes dos controles extensos de conexão. Origem, estado e horário permanecem acessíveis.
- Diálogos claros/escuros, botões com ripple e transição de pressão de 90/160 ms. Navegação desloca o conteúdo 12 dp em 220 ms, sem ocultar valores ou interpolar medições. Sem loop decorativo nem progresso artificial.
- Ajustes permite reduzir animações sem encerrar a sessão. Desativação de animações pelo Android também é respeitada. Toque não consome o clique; cancelamento/desanexação restaura escala. Leitor de tela mantém nomes, foco e ações nativas.
- Posição de rolagem por destino durante a vida da Activity. Isso não é persistência após encerramento do processo.

## Funções efetivas

### Entrada de conexão

Mostra fabricante, modelo e Android lidos do sistema, e oferece dispositivos pareados, busca BLE ou configuração Wi-Fi. Nomes não certificam compatibilidade. Wi-Fi não recebe mais IP/porta presumidos: valores informados são lembrados como configuração do usuário. Descoberta automática de endereço Wi-Fi e identificação comercial do scanner permanecem pendentes. A busca BLE encerra ao fechar o diálogo; callbacks e temporizadores de buscas antigas não encerram uma busca nova.

### Leitura guiada

Após confirmação, executa dois grupos: códigos/MIL; prontidão/quadro 00. O progresso acompanha grupos encerrados e não porcentagem de sucesso. Falha no primeiro grupo impede a coleta complementar. Estado específico por serviço vem dos contratos existentes. Não inclui VIN/calibração nesse fluxo; continuam consultas avulsas.

GuidedReadController é independente de Android e recebe fonte e observador de progresso. A Activity captura veículo, origem e sessão antes de executar. Resultados de uma sessão encerrada são descartados da interface e não gravados. Complemento de outro contexto é rejeitado. Demonstração usa apenas cenários locais identificados.

Um relatório é salvo com os resultados e, quando disponível, um seed de investigação contendo o complemento. Não há comando 04 na sequência. Não foram alterados ElmSession, Transports, ObdReader, ObdParser ou definições do catálogo.

### Comparação

Histórico permite escolher duas capturas estruturadas. Compara somente o mesmo veículo e origem, ordenando por horário. Por categoria, compara conjuntos de códigos apenas quando as duas consultas possuem resposta positiva completa. Retorna novos, presentes nas duas e não retornados. Ausência de resposta torna a categoria incomparável. Não certifica reparo nem igualdade de condições ou ECU. Não altera as capturas nem converte registros antigos.

## Integridade e critérios

- Pacote, certificado, formatos existentes, veículos, casos e exportadores mantidos. Preferência de movimento e configuração Wi-Fi são independentes dos registros.
- Não existem dados reais adicionados ao catálogo, IA, novas permissões, módulos proprietários ou programação de ECU.
- Testes anteriores e novos exercitam temas, 320/412 dp, fonte 200%, SDK 26/35, clique/pressão, movimento reduzido, fim da animação, cancelamento da leitura, sessão substituída, falha de transporte, contexto do complemento e comparação incomparável.
- Inspeção visual usa renderizações Robolectric. Não equivale a avaliação de fluidez, TalkBack, consumo, conexão ou atualização em telefone.

## Continuidade

O plano completo vai além desta versão. Permanecem abertos: diário de sintomas independente, linha do tempo unificada, manutenção, abastecimento/cálculos, lembretes, backup/restauração, sessões duradouras, identificação veicular assistida, catálogo brasileiro revisado e AutoSense. Leitura guiada e comparação nesta versão são partes de R2/R3; não encerram essas etapas.

Tratar aquisição em segundo plano, migração/restauração e cálculos em entregas com testes próprios. Catálogo depende de revisão de fontes/escopo/direitos; compatibilidade física depende de respostas coletadas no aparelho/carro, preferencialmente identificadas pelo app. IA exige fontes aplicáveis, avaliação e fluxo de consentimento. Não promover essas dependências a recursos concluídos.

Resultados e hashes finais: dist/checkpoint-R1c-R2a-validation.json. Fontes da entrega: dist/checkpoint-R1c-R2a-fonte.zip.


Resultado final: 1.104 verificações Java, 129 testes Android Robolectric, 10 verificações do pipeline e 2 testes Android separados de recursos aprovados. A tabela de recursos corresponde à APK final. Durante a validação, a inicialização de animações por StateListAnimator causou um loop no Choreographer do teste SDK 26; o aplicativo passou a iniciar animações somente por interação, com cancelamento na desanexação. Uma fixture de teste CAN foi corrigida para incluir o byte de contagem exigido pelo protocolo. Avisos Java obsoleto/unchecked e mensagens Robolectric de ID zero não impediram a aprovação final.

APK SHA-256: 2979bccc44b760943e11e7d3877813cf9e1f3ee3160e6a27abb31ae69ff9b0af. Capturas em design/reformulacao-v1/android-R1c-R2a/. Sem validação física.
