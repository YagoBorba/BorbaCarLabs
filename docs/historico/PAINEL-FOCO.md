# Painel Foco — checkpoint 4.4

Entrega: 0.6.3, código 10, em 21/10/2026. Java nativo; mesmo pacote e certificado. A apresentação foi separada em DashboardState (snapshot imutável) e DashboardScreen (componentes). MainActivity mantém aquisição e ciclo de vida.

## Contrato de apresentação

- Cada PID mostra nome e precisão do SensorCatalog, unidade, valor, estado, origem e horário UTC recebido. O contador de idade usa relógio monotônico; relógio incompatível não inventa idade.
- Ausência, timeout, não suportado, inválido, desconectado e expirado permanecem distintos. Somente VALID recente é apresentado como valor atual. Zero válido continua zero. Após 15 segundos o valor atual vira travessão; detalhes conservam captura e horário originais.
- O resumo informa cobertura parcial, sem concluir saúde do veículo. Presença no catálogo não prova suporte. Resposta sem identificação de ECU não confirma módulo específico.
- Demonstração permanece marcada em todas as abas, fora da área rolável. O perfil exibido é o perfil selecionado; sua descrição não é identificação obtida por OBD. O botão da Garagem leva à proveniência de cada atributo.
- ATRV é alimentação do adaptador, separado do PID 42 de tensão do módulo. O leitor atual só fornece instante monotônico para ATRV: a UI informa que o horário UTC não foi registrado. Não estima saúde da bateria.

## Gráfico e interação

Até 90 capturas válidas recentes recebidas de RPM, com horário original no eixo X, mínimo/máximo calculados das capturas e seleção por toque, botões, teclado ou ações de acessibilidade. Reexibir ou abrir detalhes não produz amostras. Resposta inválida, intervalo superior a 15 segundos ou relógio não crescente interrompem a linha. Não há interpolação através da lacuna nem preenchimento com zeros.

O gráfico vira histórico quando a coleta para, RPM sai da seleção ou a leitura expira. A origem e a sessão acompanham a janela. A seleção mantém o mesmo instante/valor ao avançar a janela; desaparece quando a captura sai dela. Conteúdo não finito é descartado.

## Gravação e persistência

Sessão de aquisição e ID da gravação são mostrados separadamente. O campo session_id do CSV continua sendo o ID da gravação; o ID de aquisição não foi adicionado ao arquivo. Cabeçalho e semântica do CSV permanecem compatíveis. TelemetryRecorder ganhou apenas um getter de identidade.

O callback de gravação verifica primeiro plano e aba ativa antes de escrever. Uma consulta iniciada antes da pausa ainda pode terminar; seu callback não escreve no CSV enquanto a tela está pausada. Isso não cancela comandos de transporte já enviados. Não foi implementado serviço de coleta em segundo plano.

Nenhuma migração adicional. Perfil v2, SQLite e investigações 1/2/3/4 continuam com os mesmos formatos. APK e fontes anteriores permanecem arquivados.

## Layout e limites

Fundo escuro, acento verde-água, cartão principal de RPM e sinais complementares. Duas colunas a partir de 380dp com fonte até 130%; caso contrário, uma coluna. Botões crescem com texto e têm alvo mínimo de 48dp. Acima de 130%, a navegação usa duas linhas para preservar os rótulos. Os avisos de simulação/gravação ficam fixos.

As capturas e testes usam Robolectric com fixtures explícitas. Não comprovam TalkBack em aparelho, ergonomia em condução, BLE/SPP/Wi-Fi reais, instalação ou compatibilidade do Cobalt. Catálogo veicular real continua vazio. Falhas/investigações são a próxima etapa 4.5.
