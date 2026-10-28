# BorbaCar Labs — direção de produto revisada

21/10/2026 · Base inspecionada: 0.6.3 / checkpoint 4.4. Esta revisão substitui a prioridade das etapas futuras dos planos anteriores. É uma especificação de produto e implementação; nenhuma funcionalidade nova descrita aqui está sendo declarada entregue na APK.

## 1. A proposta central

**Entender o que aconteceu com o carro, guardar a evidência e acompanhar o que mudou.**

O público inicial é o proprietário brasileiro que quer entender alertas e conversar melhor com a oficina. O usuário técnico encontra sinais e exportações em profundidade. A oficina é destinatária do relatório, não um segundo produto a construir agora.

O diferencial proposto é a continuidade: sintoma → captura → investigação → serviço informado → nova captura comparável. Isso conecta funções que, isoladamente, seriam apenas medidores, listas de códigos ou lembretes. É uma hipótese de produto a validar com usuários, não uma alegação de superioridade comercial comprovada.

BorbaCar permanece a marca principal; Labs pode identificar o canal experimental. AutoSense será uma camada futura de explicação, com fontes e limites, dentro dos casos. Não deve dominar a navegação nem prometer diagnóstico automático antes de existir.

## 2. O que muda na direção atual

| Situação observada ou limite atual | Reformulação recomendada |
|---|---|
| A tela abre com conexão, perfil e grande destaque de RPM | Abrir em Hoje, com a próxima ação útil; painel de sinais dentro de Diagnóstico |
| Repetição de origem, horário e botão longo em cada cartão | Origem/idade compactas no cartão; detalhe completo ao tocar, sem ocultar estado vencido |
| Quatro abas técnicas e funções espalhadas por Mais | Hoje, Diagnóstico, Histórico, Garagem; configurações no cabeçalho |
| Layout ampliado exige muita rolagem e navegação em duas linhas | Rever hierarquia; prototipar expansão sem sobreposição antes de adotar o novo padrão |
| Dados ausentes podem parecer apenas falta de conteúdo | Explicar o motivo e oferecer a ação que consegue resolvê-lo |
| Catálogo veicular real vazio | Liberar cobertura por configurações brasileiras revisadas, exibindo escopo de cada pacote |
| Casos/revisões já existem, mas não são o centro da experiência | Unificar captura, sintomas, investigação e resultado informado numa linha do tempo |
| Aquisição e ciclo de vida ainda coordenados pela Activity | Separar controlador de sessão antes de adicionar gravação duradoura ou viagens |
| Muitos testes automatizados, nenhum ensaio físico desta etapa | Tornar hardware um critério anterior às promessas de produto, não a última etapa |
| Documentos de roadmap ainda descrevem base 0.6.0 | Este documento passa a ser a referência de prioridade; preservar os anteriores como histórico |

Preservar Java, pacote, assinatura, IDs, proveniência, estados de leitura, snapshots, exportações antigas e o que já funciona. Não recomeçar o projeto.

## 3. Experiência completa

### Primeiro uso

Entrar sem conta. Escolher “Meu carro” ou “Conhecer em demonstração”, em ambientes separados. No cadastro, perguntar marca/modelo/ano quando houver opções verificadas; permitir “Ainda não sei” e perfil parcial. Motor, câmbio, versão e ano de fabricação são opcionais. Não exigir VIN, placa ou adaptador para registrar um sintoma ou serviço.

O Cobalt LTZ 2019 informado pelo usuário é o piloto de pesquisa e ensaio. Seu motor, transmissão, ECU e adaptador continuam pendentes. O manual de uma família não identifica automaticamente a configuração de uma unidade.

### Hoje

Cabeçalho compacto com veículo e conexão. Uma ação principal contextual: primeira leitura, continuar investigação ou revisar pendência. Abaixo: último evento relevante, cuidados registrados e atalhos para abastecimento/serviço. Sem nota de “saúde 92%”, aprovação global ou “carro perfeito”.

Estado sem adaptador deve ser útil: registrar sintoma, consultar registros e abrir manual. A última leitura permanece datada e identificada como anterior. Ao conectar, não executar automaticamente apagar códigos ou consultas avançadas.

### Diagnóstico

Três entradas locais: Leitura guiada, Ao vivo e Investigações. A leitura guiada explica quais serviços serão consultados e apresenta progresso real por etapa concluída; nunca anima porcentagem inventada.

Resultado organizado por serviço: códigos armazenados, pendentes, permanentes, prontidão, identificação e complemento disponível. Mostrar resposta vazia válida, erro, não executado e não anunciado como resultados diferentes. “Nenhum código retornado nesta consulta” não vira “nenhum defeito”. Um DTC aponta uma observação, não determina a peça a trocar.

Após a leitura, oferecer “Investigar esta captura” e “Preparar resumo para oficina”. Apagar DTCs fica em ação secundária explícita, preservando as verificações existentes; não é botão principal nem parte de uma verificação automática. ABS, airbag, câmbio e outros módulos só aparecem como funções disponíveis depois de cobertura comprovada.

### Ao vivo

Até quatro sinais visíveis por padrão, escolhidos segundo disponibilidade observada e objetivo do usuário; isso não aumenta o lote atual de aquisição. Não fixar RPM como o sinal mais importante para todos. Oferecer layouts salvos por veículo e contexto: básico ou seleção pessoal, sem prometer medir parâmetros indisponíveis.

Valor e unidade dominam; abaixo ficam estado e idade. A origem real/simulada fica sempre perceptível. Ao tocar: horário UTC, PID, origem, módulo quando conhecido, motivo da ausência e detalhes técnicos. O gráfico usa capturas reais, permite selecionar instantes e comparar sinais em eixos separados quando unidades diferem. Não comparar amostras como simultâneas quando foram obtidas sequencialmente.

Adicionar “Marcar acontecimento” numa gravação: nota curta, instante e vínculo à sessão. No primeiro lançamento, uso parado ou por passageiro; não incentivar interação ao dirigir. Gravação com tela apagada é uma etapa própria, condicionada a serviço Android adequado e ensaio físico.

### Histórico

Linha do tempo por veículo: leitura, sintoma, investigação, revisão, serviço e abastecimento. Filtros simples por tipo e período. Selecionar dois registros para comparar: destacar fatos novos, ausentes e incomparáveis. “Não retornou nesta captura” não significa “defeito resolvido”. Serviço informado pelo usuário não confirma causa nem eficácia.

Reabrir um caso conserva a captura. Uma leitura posterior vira novo registro vinculado, não substitui a evidência antiga. Revisões de sintomas continuam imutáveis. Registros de veículo removido continuam acessíveis.

### Garagem

Identidade e origem por atributo, foto opcional do usuário ou ilustração genérica própria, documentos, manutenção, cobertura OBD e configurações do veículo. Não usar imagem de outra versão como se fosse o carro exato. Nenhum logotipo ou foto de terceiro sem origem/direito definido.

Painel de cobertura por função: implementada no app; anunciada naquela sessão; resposta observada com data; ensaio físico identificado; ainda desconhecida. O estado de uma sessão não certifica todos os carros do mesmo modelo.

## 4. Funções que agregam valor

| Função | Benefício | Entrada mínima | Limite da conclusão |
|---|---|---|---|
| Diário de sintomas | Registrar falha intermitente antes de esquecer | Relato e horário; contexto opcional | Relato não é medição |
| Marcação de evento | Localizar quando um comportamento ocorreu no log | Sessão e relógio da captura | Correlação não é causa |
| Comparação antes/depois | Organizar o acompanhamento de serviço | Duas capturas e condições | Mudança não prova reparo; informar condições diferentes |
| Resumo para oficina | Entregar evidência legível e reutilizável | Caso selecionado | Sem diagnóstico ou troca de peça presumidos |
| Diário de manutenção | Guardar data, km, comprovante e item | Informação do usuário | Item registrado não é serviço auditado |
| Lembretes | Reduzir esquecimento | Prazo/km documentados ou definidos pelo usuário | Nunca inventar intervalo universal; sem km recente, previsão fica pendente |
| Abastecimentos | Acompanhar custo e consumo observado | Litros, valor, hodômetro, tanque completo/parcial | Cálculo depende da consistência dos registros |
| Assistente de conexão | Separar permissão, pareamento, adaptador e resposta do carro | Estado de cada etapa | Conectar ao adaptador não confirma comunicação com ECU |
| Manual e recall | Direcionar à fonte aplicável | Modelo/ano; consulta externa quando necessária | Abrir link não comprova ausência de recall |
| Backup local e restauração | Preservar histórico ao trocar aparelho | Pacote íntegro e versão suportada | Importação exige validação, revisão e tratamento de duplicatas |

Consumo por abastecimento: calcular distância dividida pela soma dos litros entre dois fechamentos consistentes de tanque completo; incluir parciais intermediários. Se faltar registro, hodômetro regredir ou houver inconsistência, marcar período incompleto. Informar que hodômetro e litros foram declarados. Correções recalculam resumos sem apagar registros anteriores. Separar combustível misturado, preço pago e referência regional.

Não lançar consumo instantâneo calculado por MAF em carro flex como medição exata: depende de composição/hipóteses e avaliação própria. Não inferir potência, torque, desgaste, vida de bateria, autonomia ou quilometragem real a partir de sinais insuficientes. Não prometer odômetro, nível de combustível ou temperatura do câmbio como disponíveis em todo carro.

## 5. Estratégia de dados reais

Separar cinco conjuntos: identidade/configurações; documentação de uso/manutenção; evidências de compatibilidade; observações do veículo; registros do usuário. Um conjunto não preenche automaticamente o outro.

Cada fato relevante deve carregar: valor/unidade, origem, escopo, momento observado ou edição documental, estado de qualidade e referência. Cálculos guardam método/versão/entradas. Quando existirem informações conflitantes, mostrar ambas e pedir resolução explícita; VIN recebido não sobrescreve perfil declarado silenciosamente.

A interface usa palavras legíveis: “Lido do carro”, “Informado por você”, “Manual do fabricante”, “Calculado”, “Exemplo” e “Ainda não confirmado”. Não transformar esses rótulos numa porcentagem de confiança inventada. Metadados completos ficam a um toque.

O catálogo cresce por pacotes pequenos. Primeiro, pesquisar o Cobalt MY19 BR e suas configurações documentáveis; publicar somente atributos revisados com direito de uso registrado. Depois ampliar segundo veículos realmente utilizados e fontes disponíveis. A tabela comercial é uma ajuda de seleção, não uma lista de suporte OBD. Fontes oficiais encontradas constam de [FONTES-PRODUTO-2026-10.md](FONTES-PRODUTO-2026-10.md).

O pacote de catálogo precisa de autoria autenticada antes de atualização remota; hash sozinho não basta. Edição nova nunca modifica a identidade ou referência congelada de uma investigação. A seleção guiada deve continuar útil quando cobertura, internet ou variante faltarem.

## 6. Sistema visual e linguagem

Manter a identidade escura com verde-água, criar tema claro equivalente para luz externa e respeitar preferência do sistema. Cor de marca não significa condição mecânica boa. Âmbar indica atenção contextual ou pendência, vermelho apenas erro/alerta sustentado; acompanhar com texto e ícone.

Escala proposta: título 24–28sp, subtítulo 18–20sp, corpo 16sp, metadados 12–14sp, valores 32–40sp. Espaçamentos 4/8/12/16/24dp; margens 16–20dp, cantos 12–16dp. Contraste alvo 4,5:1 no corpo e 3:1 em texto grande/controles. Alvos de toque no mínimo 48dp, fonte a 200%, conteúdo rolável sem botões sobrepostos e rótulos completos. São critérios de aceite futuros, não resultados já medidos do redesenho.

Uma ação dominante por tela; ações perigosas fora desse destaque. Ícones vetoriais consistentes substituem símbolos Unicode de aparência variável. Menos títulos em caixa alta e cartões aninhados. Sem botão longo “Detalhes · nome do sensor” repetido: o próprio cartão abre detalhes com ação acessível e indicação visual.

Microtexto: “Conectar scanner”, “Última leitura”, “Ler novamente”, “Salvar no histórico”, “Preparar para oficina”. Trocar “sem curva inventada” por “Aguardando leituras”. Estado sem dados explica o próximo passo, sem linguagem de implementação. Permissão negada mostra recuperação específica; não repetir alertas a cada atualização. Erros técnicos têm copiar detalhes, com identificadores sensíveis removidos por padrão.

Demonstração tem ambiente/indicador fixo e exportações com marca explícita. Não começa sozinha quando a conexão falha. Pausa e reconexão mantêm controles claros. Gráficos têm texto alternativo, seleção por teclado e não anunciam cada atualização ao leitor de tela.

## 7. Arquitetura que sustenta a expansão

Manter domínio Java e migração incremental. Extrair primeiro o estado de navegação e apresentação; depois a coordenação de sessão para componente testável separado da Activity, preservando ElmSession/Transports até ensaio específico. Operações assíncronas carregam veículo/sessão/origem/revisão do pedido para impedir resultados em contexto errado.

Proposta de módulos: ui/screens e componentes; application com casos de uso; telemetry com aquisição/qualidade; diagnostics com evidências; vehicle/catalog; history; maintenance; fuel; export; knowledge. Não é necessário trocar o projeto inteiro de framework ou banco.

Novos registros: SymptomEvent, TimelineEntry, ServiceRecord, FuelEntry e SessionEvent. Referenciam IDs estáveis e origem, sem mudar formatos históricos. A linha do tempo começa como projeção sobre repositórios existentes; novos eventos entram em armazenamento versionado próprio. Anexos ficam privados com tamanho limitado, tipo verificado, referência e política de remoção explícita.

Importação: pré-validar tudo, apresentar conteúdo/duplicatas, fazer backup, gravar transacionalmente e verificar resultado. Versão desconhecida ou arquivo corrompido não pode substituir dados existentes. Exportação antiga mantém reprodutibilidade. PDF humano e JSON técnico devem refletir a mesma revisão; CSV legado continua disponível quando surgir um formato novo.

Sem conta/servidor obrigatório no núcleo. Dados locais por padrão; compartilhamento com seleção prévia e VIN/placa/localização ocultos por padrão. Revisar backup Android, logs e permissões antes de alegar privacidade garantida. Geolocalização só em função futura explícita, nunca requisito de leitura OBD. Não cobrar nem definir assinatura nesta etapa; hipótese futura de receita não deve interferir nas evidências.

## 8. Papel futuro da AutoSense

Explicar uma captura congelada, resumir diferenças e sugerir perguntas sustentadas por fontes aplicáveis. Separar fato, hipótese e ausência; responder “não há dados suficientes” quando necessário. Exibir fontes, revisão da entrada e versão do analisador. Não estimar porcentagem de diagnóstico sem calibração validada.

Nenhum acesso da IA a comandos do veículo. Envio externo exige prévia visualização/consentimento e minimização de dados. Falta de internet ou de serviço mantém leitura, histórico e relatórios utilizáveis. Documentos recuperados são dados, não instruções. Avaliação exige exemplos revisados, testes adversariais e checagem de afirmações não sustentadas; regras existentes não são rotuladas como IA.

## 9. Ordem de implementação e arquivos

As fatias R1–R7 substituem a ordem linear antiga 4.5–4.8; checkpoints anteriores continuam concluídos. Agrupar itens de baixo risco; separar aquisição, migração e novos cálculos para revisão e testes próprios. Não prometer data antes de resolver hardware e fontes.

| Fatia | Escopo e arquivos principais | Critérios mensuráveis para concluir |
|---|---|---|
| R1 — experiência | MainActivity.java; ui/UiKit.java; ui/DashboardScreen.java; ui/GarageScreen.java; novos ui/HomeScreen.java, ui/DiagnosisScreen.java, ui/NavigationState.java; res/values e drawables | Quatro destinos acessíveis; nenhum fluxo existente perdido; 320/360/412dp e 200% sem sobreposição; estado offline/erro/simulado/reconectando/pausado revisado; todas ações possuem destino real |
| R2 — leitura guiada e ensaio piloto | Novo application/GuidedReadController.java; ui/DiagnosisScreen; camada de sessão e testes próprios apenas se necessários; docs/VALIDACAO-HARDWARE.md | Resultado por serviço, sem vazio convertido em sucesso; testar permissão negada, perda de conexão e retorno; registrar telefone/Android/adaptador/veículo, versão e log. Só a função ensaiada recebe evidência física |
| R3 — histórico e oficina | Novos history/TimelineRepository.java, ui/TimelineScreen.java; extrair telas de caso da MainActivity; ReportWriter.java e exportadores novos | Abrir todos os esquemas legados, nenhuma reatribuição; comparação preserva origem/tempo; relatório humano e JSON contêm mesmos fatos; exportação sensível revisável; PDF inspecionado e ensaiado em aparelho |
| R4 — cobertura e manutenção | vehicle/catalog/*; res/raw/vehicle_catalog.json; novos maintenance/* e ui/MaintenanceScreen.java | Cada atributo publicado tem fonte/escopo/revisão/direito; casos não se alteram com catálogo; prazo desconhecido não vira atraso; datas/km declarados identificados; conflitos e variante ausente testados |
| R5 — abastecimento e custo | Novos fuel/FuelEntry.java, FuelRepository.java, FuelCalculator.java e ui/FuelScreen.java | Tanque parcial/completo, lacunas, correções, km regressivo, unidades e divisão por zero testados; média calculada conserva entradas; referência ANP nunca substitui preço pago |
| R6 — sessões duradouras e backup | Novo application/AcquisitionController.java; serviço Android condicionado; SessionEvent; import/export de backup | Recuperação de rotação/tela apagada/perda de adaptador/morte de processo ensaiada; sem misturar sessões; arquivo inválido não altera base; interrupção deixa estado recuperável; replay claramente histórico |
| R7 — explicação assistida | Novo módulo analysis isolado; contratos de conhecimento e exportação mínima | Corpus aplicável e revisado; 100% das afirmações factuais verificáveis apontam entrada/fonte; abstenção avaliada; sem execução de comandos; offline e consentimento funcionam; avaliação humana antes de liberação |

R2 inclui hardware cedo e pode revelar bloqueios que reordenem R3–R6. Uma fatia visual pode ser entregue enquanto aguardamos hardware, mas não promove suporte físico. Em R4, manutenção manual pode avançar antes do catálogo comercial; alertas de fabricante esperam a fonte e a variante correta.

## 10. Testes e resultado de produto

A base registrada tem 1.104 verificações Java e 102 testes Android, além dos controles de pipeline. Esses números descrevem a 0.6.3; não validam as funções propostas. Reexecutar regressões quando houver implementação. Testar comportamento, integridade e falhas, não apenas contagem.

Para cada fatia: estado vazio, parcial, erro, perda de permissão, dados antigos, arquivo corrompido, cancelamento e retomada. Formatos antigos têm fixtures congeladas. Nenhum teste sintético deve ser apresentado como suporte a veículo.

Validação de uso proposta com cinco participantes: pelo menos quatro conseguem criar perfil parcial, reconhecer uma demonstração, encontrar a data de uma leitura e produzir um resumo sem ajuda. Todos devem distinguir “sem código retornado” de “carro sem defeito”; se houver confusão, revisar texto antes de ampliar recursos. Medir tempo e erros reais, não atribuir aprovação fictícia.

Indicador principal de utilidade: uma investigação termina com evidência compreensível e próximo passo explícito. Complementares: conexão concluída por combinação ensaiada, tempo até primeira captura válida, taxa de leituras parciais, preservação após interrupção, reabertura de casos e sucesso de exportação. Medição local ou consentida; nunca coleta silenciosa por causa de métricas.

## 11. O que fica fora agora

Programação/codificação/tuning, testes de atuadores, suporte universal a módulos, predição de falha sem validação, diagnóstico por som/foto, catálogo completo do Brasil, rede social, marketplace, mapas e gamificação de condução. São expansões com custo e dependências próprias; nenhuma melhora o núcleo o suficiente para vir antes da leitura confiável e do histórico.

Decisões de produto adotadas neste plano: foco no proprietário, quatro destinos, histórico como eixo, dados locais, cobertura progressiva, hardware antecipado e IA posterior. Dependências que não podem ser inventadas: adaptador/Android/motorização do piloto; direitos de cada fonte; revisão mecânica de procedimentos; comportamento físico e resultados de uso. Compras, contratação de dados, publicação e troca de assinatura continuam fora do escopo desta revisão.

## Orientação do usuário — 23/10/2026

Priorizar descoberta pelo app, sem formulário obrigatório para celular/Android/adaptador. Sistema operacional fornece dados do celular; transporte ativo e respostas disponíveis fornecem evidência do adaptador. A 0.6.5 expõe essas informações em Ajustes. Próxima etapa deve organizar descoberta assistida, permissões e consulta de identidade do veículo, preservando dados ausentes e sem inferir modelo comercial, motor/câmbio ou autenticidade a partir de nome Bluetooth/ELM. Nenhuma função específica é promovida a suporte comprovado apenas pela identificação.

## Progresso — 24/10/2026

0.6.6 entrega R1c (identidade e interação), leitura guiada inicial de R2 e comparação de códigos de R3. R2 continua dependendo de ensaio físico e descoberta assistida mais completa; R3 ainda não é linha do tempo unificada. Escopo entregue, testes e pendências em EXPERIENCIA-0.6.6.md. A autorização do usuário abrange a continuidade de R1–R7, com prioridade visual antes da expansão.


Progresso adicional em 24/10/2026: 0.6.7 entrega registros de sintomas/serviços/abastecimentos, revisões, cálculo condicionado e exportação do diário. Não conclui notificações, linha do tempo unificada, catálogo de manutenção, backup ou validação física. Consulte DIARIO-0.6.7.md.



Atualização 26/10/2026 — 0.6.8: revisão de densidade/navegação Foco, destinos sem atalhos repetidos, opções em detalhes, coleções separadas no Histórico e primeira base factual MY19 (16 configurações). Ver FOCO-0.6.8.md e CATALOGO-MY19-0.6.8.md. Sem mudança em transportes, bancos ou formatos históricos.
