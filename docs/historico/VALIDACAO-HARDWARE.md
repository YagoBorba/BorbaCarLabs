# Validação física pendente

Este roteiro registra a etapa que ainda não pode ser executada sem telefone, adaptador e veículo disponíveis. Os testes desta entrega utilizam dados simulados e não enviam comandos à ECU.

## Identificação do ensaio

- APK, versão, SHA-256 e versão Android do telefone.
- Fabricante/modelo do adaptador, conexão SPP/BLE/TCP e configuração necessária.
- Perfil selecionado e configuração confirmada do veículo. Não completar motor/transmissão por suposição.
- Data/hora e condições conhecidas. Não registrar VIN completo em evidência pública.

## Ensaio inicial de leitura

1. Com o veículo parado, verificar a conexão e a identificação do protocolo. Distinguir adaptador conectado de resposta efetiva da ECU.
2. Conferir os PIDs anunciados e consultar poucos parâmetros. Comparar leituras disponíveis com referência independente; dado ausente não pode aparecer como zero.
3. Consultar códigos e prontidão. Registrar respostas sem suporte, negativas ou parciais e manter a diferença entre falha de comunicação e código mecânico.
4. Testar perda e nova conexão do adaptador. A tela deve indicar desconexão e retirar os valores atuais; amostras antigas não podem ganhar novos horários.
5. Gravar e exportar CSV. Conferir ID/nome do veículo, origem real, horários e estados. A pausa ao sair do Painel deve corresponder ao intervalo sem amostras.
6. Conferir histórico e exportação de relatórios do perfil. Relatórios da versão antiga devem continuar na seção sem associação.

O ensaio comum não inclui apagar códigos, codificação, atuadores ou escrita de configuração. A operação `04` existente requer validação própria antes de ser considerada comprovada no veículo. A ausência de DTCs genéricos não estabelece que todos os módulos estejam acessíveis ou sem falhas.

Um resultado deve registrar o que foi observado e a versão exata. Não transformar sucesso em um adaptador/PID em alegação de compatibilidade de todos os recursos do Cobalt.
