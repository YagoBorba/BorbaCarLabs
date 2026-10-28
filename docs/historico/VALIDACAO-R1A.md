# R1a — navegação e entrada do produto

Data: 21/10/2026. Entrega 0.6.4 (11). Primeira fatia da reformulação R1; não representa a conclusão de toda a estratégia.

## Implementado

- Hoje é a abertura padrão. Usa perfil selecionado, quantidade de registros e investigações e data/origem do último relatório disponível. Não calcula saúde do carro nem identifica configuração por suposição.
- Navegação: Hoje, Diagnóstico, Histórico, Garagem. Ao vivo é uma tela interna de Diagnóstico; o destaque da aba acompanha essa relação. Ajustes e informações ficam no cabeçalho.
- Botão Voltar: Ao vivo → Diagnóstico → Hoje. Demais destinos retornam a Hoje; sair do app mantém o comportamento Android a partir de Hoje.
- Histórico reúne as investigações e os relatórios existentes, incluindo exportação JSON/PDF, leituras para investigar, registros antigos e perfis removidos. Não foi criada uma nova persistência nem unificada a ordenação de todos os tipos numa linha do tempo.
- Diagnóstico conserva consultas, complemento, investigação da captura e ações secundárias existentes. Históricos completos foram retirados dessa tela. Ajustes preserva demonstração, fontes e catálogo, retirando cartões promocionais de codificação e serviços ainda inexistentes.
- Botões antigos da Activity passam a crescer com conteúdo. Fonte ampliada mantém navegação em duas linhas. O desenho continua escuro; tema claro e ícones vetoriais ficam para a próxima fatia.

## Integridade

Os IDs internos antigos 0/1/2/3 foram mantidos (Ao vivo/Diagnóstico/Garagem/Ajustes); Hoje e Histórico usam 4/5. Estado restaurado desconhecido volta a Hoje. A coleta continua restrita a Ao vivo, preservando as guardas existentes. Navegar não faz nova leitura nem reinicia gravação automaticamente.

MainActivity.java é o único arquivo antigo do aplicativo alterado. Novos componentes: ui/HomeScreen.java e ui/NavigationState.java. Transporte, parser, fórmulas, catálogo, perfis, banco, contratos e exportadores não mudaram. Mesmos pacote e certificado. A APK e o arquivo de fontes da 0.6.3 permanecem intactos.

Os testes anteriores foram adaptados para caminhos reais da nova interface; preservam as asserções de domínio. O helper de clique passa a procurar controles clicáveis, pois cabeçalho e aba agora podem compartilhar o mesmo nome. Não há redirecionamento oculto no helper: openLive clica em Diagnóstico e depois Ao vivo.

## Validação e limites

Resultados finais e hashes ficam no manifesto dist/checkpoint-R1a-validation.json. Novos testes cobrem instalação vazia, navegação/Voltar, IDs legados, recriação, histórico simulado, pausa/retomada de CSV, SDK 26 e fonte 200%. Capturas são renderizações Robolectric, não fotografias de aparelho. Não houve leitura física, ensaio de atualização no telefone ou confirmação da compatibilidade do Cobalt.

## Pendências explícitas de R1

Tema claro, ícones vetoriais, apresentação compacta dos sensores, escolha de sinais conforme contexto, protótipo e testes de usabilidade. R2 fará leitura guiada e ensaio de hardware. Manutenção, abastecimento, diário independente de sintomas, comparação antes/depois, backup e IA continuam planejados, sem botões que simulem recursos entregues.

O histórico mantém a apresentação existente de até oito relatórios recentes, além das investigações. Exportação preserva o conjunto completo. Paginação/busca de relatórios e linha do tempo unificada pertencem à revisão de Histórico em R3.

Resultado final: 1.104 verificações Java e 110 testes Android passaram (100 SDK 35, dez SDK 26). Pipeline: dez controles e dois testes separados de recursos. A tabela de recursos foi verificada contra a APK final. O teste de largura usa getLineMax, que exclui o espaço em branco final da linha; a primeira medição contava esse espaço como conteúdo cortado. Avisos Java obsoleto/unchecked e mensagens Robolectric de ID zero persistem sem falha final.

APK SHA-256: 656a3d3d9b4f0e105f83529ca891cbf2f0ee185a3831226148501a06c0457f3f. Capturas em design/reformulacao-v1/android-R1a/.
