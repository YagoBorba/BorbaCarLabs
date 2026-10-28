# BorbaCar Labs — Plano Mestre de Produto, Arquitetura e Desenvolvimento

**Tipo de projeto:** Aplicativo Android de diagnóstico automotivo, monitoramento e análise inteligente de dados veiculares.

**Objetivo acadêmico:** Construir uma aplicação de Inteligência Artificial demonstrável, tecnicamente organizada, testável e documentada, com potencial de evolução para um produto real.

**Direção de produto:** Criar uma plataforma automotiva modular, com experiência visual premium, perfis de veículos, leitura OBD-II, diagnóstico assistido por IA, painéis personalizáveis e módulos avançados que possam ser adicionados progressivamente.

**Veículo inicial de referência:** Chevrolet Cobalt LTZ 2019, sem presumir que todos os recursos específicos desse veículo sejam compatíveis antes da validação.

**Idioma inicial da interface:** Português brasileiro (pt-BR).

**Princípio central:** O aplicativo deve apresentar informações baseadas em dados verificáveis, identificar suas limitações e distinguir fatos, hipóteses e recomendações. Nunca deve fingir que leu um sensor, identificou um módulo ou confirmou uma falha quando isso não aconteceu.

---

# 1. Instruções gerais para a IA que continuará o desenvolvimento

Você será o engenheiro de software responsável por ajudar a desenvolver o AutoSense AI a partir do projeto existente.

Não comece criando outro aplicativo do zero. Primeiro, investigue a base atual, identifique sua arquitetura e determine quais partes estão funcionando.

O desenvolvedor já possui um aplicativo Android funcional para leitura OBD-II conectado a um adaptador real. Antes de qualquer distribuição, confirme que todos os componentes são de implementação própria ou têm reutilização autorizada.

## Regras obrigatórias de desenvolvimento

1. Inspecione o repositório antes de propor mudanças.
2. Identifique as tecnologias, os módulos, as dependências e o fluxo de comunicação OBD-II existente.
3. Não remova funcionalidades que já funcionam sem necessidade técnica demonstrada.
4. Não reescreva a aplicação inteira apenas por preferência tecnológica.
5. Não invente arquivos, classes, APIs, funções ou dependências que não existam.
6. Não afirme que um recurso está implementado até verificar o código e testar seu funcionamento.
7. Trabalhe em pequenas etapas, com entregas verificáveis.
8. Antes de alterar arquivos, explique o objetivo e o impacto da mudança.
9. Execute os testes pertinentes após cada implementação.
10. Preserve o funcionamento da conexão com o adaptador OBD-II.
11. Separe a interface, a comunicação com o veículo, as regras de diagnóstico, a base de conhecimento e a integração com a IA.
12. Use dados simulados nos testes que não exijam comunicação real com o veículo.
13. Não envie comandos de escrita ou alteração de configuração à ECU durante testes comuns.
14. Não contorne licenças, autenticação, DRM ou mecanismos de proteção de software de terceiros.
15. Mantenha documentação atualizada de arquitetura, decisões técnicas, limitações e funcionalidades concluídas.

Quando não houver informação suficiente, pergunte ao desenvolvedor ou investigue o repositório. Não suponha que uma determinada tecnologia, estrutura ou recurso já exista.

---

# 2. Visão do produto

O AutoSense AI deverá ser um aplicativo que permita ao usuário selecionar seu veículo e utilizar as funcionalidades automotivas compatíveis com ele em uma interface única.

A plataforma deverá integrar cinco grandes áreas:

- **Vehicle Intelligence:** catálogo e perfil técnico dos veículos.
- **OBD-II & Telemetry:** comunicação com o veículo, leitura de parâmetros e monitoramento.
- **AI Diagnostics:** análise inteligente de falhas, hipóteses e recomendações.
- **Personalization:** painéis, gráficos, alertas e configurações de experiência.
- **Advanced Automotive Lab:** análise avançada de dados, relatórios e, futuramente, pesquisa de calibração e tuning.

O sistema não deve ser construído como um chatbot genérico. A IA precisa receber informações estruturadas do veículo, consultar fontes técnicas, aplicar regras determinísticas quando apropriado e produzir respostas fundamentadas.

## Proposta de valor

Ajudar o usuário a entender melhor o próprio veículo, interpretar os dados disponíveis, investigar problemas e acompanhar o funcionamento dos sistemas sem exigir que ele seja um especialista em mecânica ou eletrônica automotiva.

O público pode incluir:

- Motoristas que desejam entender códigos de falha.
- Entusiastas automotivos.
- Pessoas que utilizam adaptadores OBD-II.
- Usuários interessados em monitoramento de parâmetros.
- Técnicos que desejam organizar dados e relatórios.
- Desenvolvedores e pesquisadores interessados em diagnóstico veicular.

O aplicativo não substituirá instrumentos profissionais, documentação oficial ou avaliação de um mecânico qualificado.

---

# 3. Direção visual e experiência do usuário

## 3.1 Identidade visual

A interface deve transmitir tecnologia automotiva, precisão e confiabilidade.

Diretrizes visuais:

- Tema escuro como opção padrão inicial.
- Fundo grafite ou carvão, com superfícies de contraste moderado.
- Uma cor de destaque consistente para ações primárias.
- Verde para estados confirmados de funcionamento normal.
- Amarelo ou âmbar para avisos e informações que precisam de atenção.
- Vermelho para falhas críticas ou alertas de segurança devidamente fundamentados.
- Tipografia legível e números alinhados para parâmetros.
- Gráficos limpos, sem excesso de elementos decorativos.
- Cartões com hierarquia visual clara.
- Ícones consistentes.
- Animações discretas, sem prejudicar desempenho ou legibilidade.

Não utilizar cores como substituto de informação textual. Todos os estados importantes devem possuir rótulos e, quando necessário, explicações.

## 3.2 Princípios de experiência

1. O usuário deve entender rapidamente se o aplicativo está conectado ao veículo.
2. A tela deve distinguir dados atuais, históricos, simulados e indisponíveis.
3. O usuário deve conseguir acessar as funções básicas sem precisar conversar com a IA.
4. As recomendações da IA devem ser explicáveis e verificáveis.
5. Os recursos avançados não devem aparecer como disponíveis quando não forem compatíveis.
6. O aplicativo deve ser utilizável em telas móveis de tamanhos diferentes.
7. A interface deve apresentar estados de carregamento, erro, ausência de dados e desconexão.
8. As telas de diagnóstico não devem incentivar o uso do celular durante a condução.

## 3.3 Estrutura principal de navegação

Organizar a navegação de acordo com os recursos que realmente estiverem implementados:

1. **Início:** resumo do veículo e estado da conexão.
2. **Meu veículo:** perfil, identificação e compatibilidade.
3. **Diagnóstico:** códigos, sintomas, hipóteses e investigação guiada.
4. **Monitoramento:** sensores, gráficos, painéis e sessões de coleta.
5. **Histórico:** diagnósticos, sessões e relatórios anteriores.
6. **Laboratório:** ferramentas avançadas disponíveis para o veículo.
7. **Configurações:** personalização, privacidade, conexão e preferências.

A navegação poderá ser reorganizada após a análise do aplicativo existente. Não implementar todas as telas imediatamente.

---

# 4. Arquitetura geral

A arquitetura deverá separar as responsabilidades em módulos independentes.

## 4.1 Camada Android

Responsável por:

- Interface do usuário.
- Seleção do veículo.
- Gerenciamento da conexão OBD-II.
- Coleta e apresentação de parâmetros.
- Gravação e consulta do histórico.
- Envio dos dados necessários ao módulo de IA.
- Apresentação dos relatórios.
- Preferências do usuário.

Preservar a tecnologia atual enquanto for tecnicamente adequada. Se o projeto for Android nativo, manter a linguagem e os padrões existentes. Se for Flutter ou React Native, seguir a arquitetura já adotada.

## 4.2 Camada de comunicação veicular

Responsável por:

- Descoberta e conexão com o adaptador.
- Envio e recebimento de comandos de leitura.
- Interpretação de respostas.
- Conversão de dados brutos em valores físicos.
- Controle de timeout.
- Tratamento de respostas inválidas.
- Reconexão e desconexão.
- Identificação de parâmetros não suportados.
- Controle da frequência de consulta.

Não misturar os comandos do adaptador diretamente com os componentes visuais.

A camada deve possuir uma interface interna bem definida, permitindo substituir ou simular o adaptador sem reescrever o aplicativo.

## 4.3 Camada de conhecimento automotivo

Responsável por armazenar:

- Perfis de veículos.
- Características conhecidas das ECUs.
- Definições de PIDs.
- Unidades e fórmulas de conversão.
- Códigos de falha.
- Documentação técnica.
- Regras de compatibilidade.
- Procedimentos de diagnóstico.
- Fontes e evidências de cada informação.

## 4.4 Camada de diagnóstico

Responsável por:

- Validar os dados recebidos.
- Aplicar regras determinísticas.
- Relacionar códigos, sintomas e parâmetros.
- Identificar hipóteses possíveis.
- Detectar contradições ou informações ausentes.
- Determinar quais informações adicionais seriam úteis.
- Preparar um contexto estruturado para a IA.

## 4.5 Camada de inteligência artificial

Responsável por:

- Interpretar o contexto técnico.
- Consultar a base de conhecimento.
- Explicar os resultados em linguagem acessível.
- Comparar hipóteses.
- Sugerir verificações adequadas.
- Identificar quando a evidência é insuficiente.
- Gerar relatórios estruturados.

A IA não deve ser responsável por inventar fórmulas de PIDs, interpretar arbitrariamente bytes ou executar comandos veiculares diretamente.

## 4.6 Backend opcional

Se a arquitetura escolhida utilizar um backend, ele poderá oferecer:

- API para análise de diagnósticos.
- Consulta ao catálogo técnico.
- Integração com modelos de linguagem.
- Recuperação de documentação por RAG.
- Armazenamento sincronizado de sessões.
- Autenticação e gerenciamento de usuários, quando necessário.

Para o primeiro MVP, evitar dependências desnecessárias. O diagnóstico básico e a leitura OBD-II devem continuar úteis mesmo quando a IA ou a internet estiverem indisponíveis.

---

# 5. Módulo 0 — Auditoria do projeto existente

## Objetivo

Entender a base atual antes de adicionar novas funcionalidades.

## Tarefas

1. Identificar a estrutura do repositório.
2. Identificar linguagem, framework e versão do ambiente.
3. Mapear as telas existentes.
4. Mapear o fluxo de conexão com o adaptador.
5. Identificar o protocolo e os comandos de leitura utilizados.
6. Localizar a lógica que interpreta respostas OBD-II.
7. Identificar como os parâmetros são armazenados e exibidos.
8. Identificar dependências externas.
9. Identificar funcionalidades que dependem de código proprietário.
10. Avaliar a segurança, a estabilidade e a facilidade de manutenção.

## Entregáveis

- Inventário dos módulos existentes.
- Diagrama da arquitetura atual.
- Lista das funcionalidades comprovadamente operacionais.
- Lista dos problemas conhecidos.
- Lista dos componentes que precisam ser substituídos ou reimplementados.
- Plano de mudanças de baixo risco.

## Critério de conclusão

O desenvolvedor deve conseguir explicar o funcionamento da aplicação e executar uma leitura OBD-II reproduzível antes de iniciar mudanças estruturais.

Não iniciar uma reescrita completa sem uma justificativa documentada.

---

# 6. Módulo 1 — Perfil e seleção de veículos

## Objetivo

Permitir que o usuário selecione seu veículo e receba um perfil técnico coerente com a configuração identificada.

## Funcionalidades

### 6.1 Seleção inicial

Criar um fluxo de seleção por:

- Fabricante.
- Modelo.
- Ano.
- Mercado ou país.
- Motorização.
- Versão.
- Tipo de combustível, quando pertinente.

Solicitar apenas os campos necessários. Se houver várias configurações possíveis, pedir esclarecimento ao usuário.

### 6.2 Identificação pela conexão

Quando tecnicamente disponível, permitir complementar o perfil por meio de informações obtidas do veículo, como VIN e identificadores da ECU.

A identificação automática não deve substituir a confirmação do usuário quando houver ambiguidade.

### 6.3 Perfis múltiplos

Permitir salvar mais de um veículo, alternar entre perfis e manter o histórico separado.

### 6.4 Níveis de compatibilidade

Cada recurso deve possuir um estado explícito:

- Confirmado por teste.
- Confirmado por documentação aplicável.
- Detectado, mas não completamente validado.
- Teoricamente aplicável.
- Não suportado.
- Desconhecido.

Não exibir compatibilidade presumida como se fosse comprovada.

## Modelo inicial de dados

VehicleProfile:

- id
- make
- model
- model_year
- market
- trim
- engine_code, quando conhecido
- engine_displacement, quando conhecido
- fuel_type
- transmission_type, quando conhecido
- vin, opcional e protegido
- ecu_profiles
- compatibility_status
- source_references
- created_at
- updated_at

Criar estruturas separadas para motores, ECUs, protocolos e recursos, em vez de transformar VehicleProfile em uma estrutura gigante com campos específicos para cada fabricante.

## Critérios de aceitação

- O usuário consegue selecionar e salvar um veículo.
- O perfil pode ser alterado.
- A aplicação não presume recursos não verificados.
- A seleção não interfere na comunicação OBD-II.
- Os dados de veículos diferentes permanecem isolados.

---

# 7. Módulo 2 — Base de conhecimento automotivo

## Objetivo

Criar uma fonte estruturada de conhecimento para alimentar a interface, as regras de diagnóstico e a IA.

Este módulo é uma das partes mais importantes do produto.

## 7.1 Catálogo de veículos

Armazenar os veículos e suas configurações conhecidas, começando por uma cobertura pequena e validada.

## 7.2 Catálogo de PIDs

Cada PID deve possuir:

- Identificador.
- Nome técnico.
- Descrição.
- Serviço ou modo correspondente.
- Fórmula de conversão, quando aplicável.
- Unidade física.
- Valores válidos.
- Restrições.
- Requisitos de identificação.
- Fonte da definição.
- Tipo de suporte.
- Estado de validação.

Separar os PIDs padronizados daqueles específicos de fabricantes.

## 7.3 Catálogo de DTCs

Cada código de falha deve possuir:

- Código.
- Descrição padronizada.
- Sistema relacionado.
- Possíveis causas, sem tratá-las como certezas.
- Sintomas associados, quando documentados.
- Parâmetros que podem ajudar na investigação.
- Procedimentos de verificação.
- Fonte técnica.
- Restrições de aplicação.
- Avisos de segurança, quando pertinentes.

## 7.4 Catálogo de ECUs e módulos

Registrar apenas módulos identificados ou documentados de maneira confiável.

Não afirmar que ABS, airbag, transmissão ou outros módulos podem ser acessados por qualquer adaptador OBD-II.

## 7.5 Proveniência dos dados

Todo conteúdo técnico deve registrar sua origem.

Dar preferência a:

- Documentação oficial e manuais de serviço obtidos legitimamente.
- Normas e especificações aplicáveis.
- Documentação de protocolos.
- Fontes técnicas confiáveis.
- Conjuntos de dados cuja licença permita a utilização pretendida.

Não copiar bases proprietárias de aplicativos concorrentes sem autorização.

## Critérios de aceitação

- A base pode ser consultada pelo código.
- Cada definição técnica possui sua origem registrada.
- Dados ausentes não são substituídos por suposições.
- O sistema diferencia informação padronizada de informação específica de fabricante.
- A IA consegue recuperar somente os registros pertinentes ao veículo e ao problema analisado.

---

# 8. Módulo 3 — Comunicação OBD-II e telemetria

## Objetivo

Transformar a leitura já existente em uma camada de aquisição de dados robusta e independente da interface.

## Funcionalidades

### 8.1 Gerenciamento da conexão

- Seleção do adaptador.
- Conexão e desconexão.
- Estado da comunicação.
- Detecção de falhas.
- Tratamento de timeout.
- Reconexão controlada.
- Mensagens claras quando o adaptador não responde.

### 8.2 Descoberta de capacidades

Consultar os parâmetros padronizados suportados pelo veículo, quando disponíveis.

Não consultar indiscriminadamente todos os parâmetros em intervalos muito curtos.

### 8.3 Leitura de parâmetros

Implementar uma camada reutilizável para ler os parâmetros suportados, converter as respostas e produzir dados tipados.

### 8.4 Sessões de monitoramento

Permitir iniciar, pausar e encerrar uma sessão.

Registrar:

- Veículo selecionado.
- Adaptador utilizado, quando identificado.
- Horário.
- Parâmetros coletados.
- Unidades.
- Erros de comunicação.
- Intervalos de coleta.
- Origem dos dados.
- Condições de teste conhecidas.

### 8.5 Qualidade dos dados

Cada amostra deverá distinguir:

- Valor válido.
- Parâmetro não suportado.
- Timeout.
- Erro de interpretação.
- Dado antigo.
- Ausência de leitura.
- Dado simulado.

Nunca representar uma falha de leitura como valor zero.

## Critérios de aceitação

- A conexão existente continua funcionando.
- Leituras inválidas não contaminam os resultados.
- O sistema registra a origem e a validade das amostras.
- O monitoramento não trava a interface.
- A frequência de coleta é ajustável e respeita as limitações do adaptador.

---

# 9. Módulo 4 — Painel de monitoramento e personalização

## Objetivo

Oferecer uma experiência visual semelhante a um painel automotivo avançado, sem exigir alterações na ECU.

## 9.1 Painel inicial

Exibir:

- Veículo selecionado.
- Estado da conexão.
- Parâmetros favoritos.
- Resumo de códigos de falha.
- Atalhos para diagnóstico e monitoramento.
- Última sessão registrada.

## 9.2 Componentes personalizáveis

Permitir adicionar, remover, mover e redimensionar cartões de informação, conforme a plataforma permitir.

Componentes possíveis:

- Indicador numérico.
- Medidor circular.
- Gráfico de linha.
- Histórico de um parâmetro.
- Indicador de estado.
- Lista de códigos de falha.
- Comparação entre sessões.
- Alertas configuráveis.

## 9.3 Perfis de painel

Disponibilizar modelos como:

- Básico.
- Diagnóstico.
- Viagem.
- Desempenho.
- Técnico.

Esses perfis são configurações de visualização. Não devem sugerir que o aplicativo alterou o desempenho mecânico do veículo.

## 9.4 Alertas

Permitir que o usuário configure alertas sobre parâmetros efetivamente disponíveis.

Cada alerta deve definir:

- Parâmetro.
- Condição.
- Limite.
- Duração ou persistência, se necessário.
- Mensagem.
- Estado habilitado.
- Origem do limite.

Não criar limites críticos arbitrários. Utilizar especificações documentadas ou permitir configurações claramente identificadas como personalizadas.

## Critérios de aceitação

- O usuário consegue personalizar e salvar seu painel.
- Os valores exibidos correspondem às leituras reais.
- Unidades e horários são apresentados corretamente.
- Dados indisponíveis são identificados.
- A interface permanece responsiva durante a coleta.

---

# 10. Módulo 5 — Diagnóstico convencional

## Objetivo

Criar uma camada confiável de diagnóstico antes de depender da IA generativa.

## Funcionalidades

- Leitura de códigos de falha suportados.
- Descrição dos códigos.
- Identificação do sistema associado, quando possível.
- Consulta à base técnica.
- Registro de códigos anteriores.
- Associação com sintomas relatados.
- Consulta de dados congelados, quando disponíveis.
- Registro de observações do usuário.
- Relatório inicial.

## Regras

Um código de falha não comprova automaticamente a falha de um componente.

Por exemplo, um código associado à mistura de combustível pode exigir a investigação de diferentes causas. A aplicação deve explicar as possibilidades, sem recomendar a substituição imediata de uma peça como conclusão definitiva.

A remoção de códigos deve ser uma operação separada, explícita e confirmada pelo usuário. Antes da remoção, explicar que dados úteis podem ser perdidos. Não apagar códigos automaticamente durante o diagnóstico.

## Critérios de aceitação

- O código recebido é interpretado corretamente.
- A descrição vem de uma fonte identificada.
- A interface diferencia código presente, histórico e estado desconhecido, quando a informação estiver disponível.
- A aplicação não inventa a causa da falha.
- O resultado pode ser salvo no histórico.

---

# 11. Módulo 6 — AutoSense AI: motor de diagnóstico inteligente

## Objetivo

Transformar dados e sintomas em uma investigação assistida por IA, com hipóteses explicáveis e recomendações fundamentadas.

Este é o principal diferencial de inteligência artificial do projeto.

## 11.1 Entrada estruturada

A IA deve receber um objeto contendo, conforme disponível:

- Perfil do veículo.
- Identificação da ECU.
- Códigos de falha.
- Sintomas relatados.
- Parâmetros atuais.
- Dados congelados.
- Histórico relevante.
- Condições conhecidas da coleta.
- Informações ausentes.
- Fontes técnicas recuperadas.

Não enviar uma sequência desorganizada de texto e esperar que o modelo descubra sozinho todos os detalhes.

## 11.2 Fluxo de processamento

1. Validar o perfil e os dados.
2. Identificar campos ausentes ou inválidos.
3. Consultar as regras determinísticas.
4. Recuperar documentação relevante.
5. Construir o contexto de diagnóstico.
6. Solicitar à IA uma análise estruturada.
7. Validar o formato e os campos da resposta.
8. Verificar se as afirmações possuem evidências adequadas.
9. Exibir as hipóteses e os próximos passos.
10. Registrar o resultado para avaliação posterior.

## 11.3 Formato de resposta

A IA deverá produzir uma estrutura semelhante a:

{
  "summary": "Resumo do problema relatado",
  "evidence": [],
  "hypotheses": [
    {
      "cause": "Possível causa",
      "supporting_evidence": [],
      "contradicting_evidence": [],
      "missing_information": [],
      "recommended_checks": [],
      "confidence": "low"
    }
  ],
  "safety_notes": [],
  "sources": [],
  "limitations": []
}

Este é um contrato ilustrativo, não um diagnóstico real. Definir o esquema final e validá-lo no código.

A confiança não deve ser inventada como porcentagem estatística. Inicialmente, usar categorias como baixa, moderada e alta, com critérios documentados. Só apresentar probabilidades numéricas quando houver um método calibrado e avaliado que as sustente.

## 11.4 Investigação guiada

A IA pode fazer perguntas adicionais quando elas forem relevantes.

Exemplos:

- O problema acontece com o motor frio ou quente?
- A falha ocorre em marcha lenta ou durante aceleração?
- Há algum código adicional?
- O parâmetro necessário está disponível no veículo?

As perguntas devem reduzir incerteza, não prolongar a conversa desnecessariamente.

## 11.5 Limitações

- A IA não pode afirmar que realizou testes físicos.
- Não pode confirmar uma causa sem evidências suficientes.
- Não deve recomendar intervenções perigosas.
- Não deve inventar especificações do fabricante.
- Não deve assumir que um sensor está disponível.
- Deve recomendar avaliação profissional quando a situação exigir.

## Critérios de aceitação

- A IA recebe dados estruturados.
- O resultado contém evidências e limitações.
- A resposta passa por validação de esquema.
- Hipóteses não são apresentadas como fatos.
- Quando faltam dados, o sistema pede informações ou explica a limitação.
- Os resultados podem ser reproduzidos com entradas de teste.

---

# 12. Módulo 7 — RAG e documentação técnica

## Objetivo

Permitir que a IA consulte informações técnicas relevantes antes de responder.

RAG significa Retrieval-Augmented Generation: o sistema recupera documentos pertinentes e fornece seus trechos ao modelo para fundamentar a resposta.

## Etapas

1. Identificar documentos utilizáveis legalmente.
2. Extrair e organizar seu conteúdo.
3. Dividir o conteúdo em trechos coerentes.
4. Registrar fonte, versão e contexto.
5. Indexar os trechos para pesquisa.
6. Recuperar informações relevantes à consulta.
7. Filtrar resultados pela aplicabilidade ao veículo.
8. Fornecer os trechos à IA.
9. Associar as afirmações às fontes.
10. Validar se as referências realmente sustentam as conclusões.

## Metadados recomendados

- document_id
- title
- publisher
- document_version
- publication_date, quando conhecida
- source_location
- license
- vehicle_applicability
- ecu_applicability
- section
- extracted_at

## Regras

- Não citar documentos que não foram recuperados.
- Não inventar páginas, URLs ou trechos.
- Não considerar um documento genérico aplicável automaticamente a todos os veículos.
- Indicar quando a documentação não resolve a questão.
- Não utilizar material protegido sem autorização adequada.

## Critérios de aceitação

Um teste deve comprovar que uma pergunta técnica recupera documentos relevantes e que a resposta não atribui às fontes afirmações que elas não contêm.

---

# 13. Módulo 8 — Histórico, relatórios e manutenção

## Objetivo

Transformar o aplicativo em uma ferramenta contínua de acompanhamento do veículo.

## Funcionalidades

- Histórico de diagnósticos.
- Histórico de sessões de monitoramento.
- Comparação entre leituras.
- Registro de sintomas.
- Observações do usuário.
- Exportação de relatórios.
- Registro das recomendações da IA.
- Registro das ações tomadas pelo usuário.
- Histórico de erros de comunicação.

## Relatório

Cada relatório deve incluir:

1. Identificação do veículo, com os dados disponíveis.
2. Data e hora.
3. Condições conhecidas da coleta.
4. Códigos de falha.
5. Parâmetros relevantes.
6. Hipóteses e evidências.
7. Verificações recomendadas.
8. Fontes consultadas.
9. Limitações.
10. Aviso de que o resultado é uma análise assistida e não uma confirmação definitiva de defeito.

## Privacidade

- Solicitar consentimento antes de sincronizar dados.
- Proteger identificadores pessoais e VIN.
- Permitir apagar históricos do usuário.
- Não enviar telemetria desnecessária ao modelo de IA.
- Não registrar credenciais ou segredos nos logs.
- Explicar onde os dados ficam armazenados.

## Critérios de aceitação

O usuário consegue gerar e consultar um relatório sem depender de uma conexão permanente com a internet, desde que os dados necessários estejam disponíveis localmente.

---

# 14. Módulo 9 — Laboratório avançado e tuning

## Objetivo

Preparar a arquitetura para recursos avançados sem comprometer os módulos de diagnóstico já validados.

Este módulo não faz parte da primeira entrega obrigatória.

## Fase A: análise avançada de dados

- Importar arquivos de log.
- Comparar sessões.
- Visualizar curvas de parâmetros.
- Detectar anomalias.
- Comparar medições em condições semelhantes.
- Gerar relatórios técnicos.
- Identificar dados inconsistentes.

## Fase B: pesquisa de calibrações

Somente em ambiente controlado e com arquivos obtidos legalmente:

- Catalogar formatos conhecidos.
- Documentar metadados.
- Comparar arquivos e versões.
- Identificar diferenças estruturais.
- Estudar documentação de calibração.
- Construir ferramentas de visualização somente de leitura.

Não presumir que todo arquivo de ECU possui um formato conhecido ou que as diferenças binárias possam ser interpretadas automaticamente como alterações de combustível, ignição ou pressão.

## Fase C: funções de escrita

Qualquer recurso futuro que modifique a ECU deverá passar por uma avaliação técnica, legal e de segurança independente.

Requisitos mínimos antes de considerar sua implementação:

- Compatibilidade comprovada com a ECU específica.
- Autorização e documentação adequadas.
- Integridade dos dados e verificação de escrita.
- Procedimentos de recuperação.
- Condições de alimentação elétrica apropriadas.
- Controles de acesso e confirmação.
- Validação profissional.
- Avaliação dos riscos ao veículo e aos ocupantes.

Não implementar escrita genérica de calibrações, desativação de sistemas de emissões ou segurança, nem comandos desconhecidos apenas porque um adaptador aceita enviá-los.

## Critério de aceitação da primeira versão

O módulo avançado deve funcionar como ferramenta de análise de dados em modo somente leitura. A escrita de calibrações permanece fora do escopo do MVP.

---

# 15. Modelo de dados inicial

Criar modelos separados, com relacionamentos explícitos.

## Entidades principais

### VehicleProfile

Identificação do veículo e configuração selecionada.

### ECUProfile

Identificação, fabricante, família, protocolo e informações de compatibilidade conhecidas.

### PIDDefinition

Definição do parâmetro, fórmula, unidade, fonte e aplicabilidade.

### DTCDefinition

Código, descrição, sistema, documentação e procedimentos de investigação.

### VehicleCapability

Relação entre veículo, ECU, função e estado de compatibilidade.

### TelemetrySession

Uma sessão de coleta de dados.

### TelemetrySample

Uma amostra coletada durante a sessão.

### DiagnosticCase

Caso de diagnóstico com sintomas, dados de entrada e resultado.

### DiagnosticHypothesis

Hipótese, evidências favoráveis e contrárias, incertezas e verificações recomendadas.

### KnowledgeDocument

Documento técnico e seus metadados.

### UserDashboard

Configuração de painel e seus componentes.

### DiagnosticReport

Relatório gerado a partir de uma sessão ou caso.

## Regras de modelagem

- Usar identificadores estáveis.
- Armazenar datas em formato consistente.
- Armazenar unidades explicitamente.
- Separar definições técnicas de valores medidos.
- Não misturar dados simulados e reais sem identificação.
- Versionar as regras técnicas relevantes.
- Evitar armazenar dados pessoais desnecessários.
- Definir índices para consultas frequentes.
- Criar migrações de banco de dados.
- Testar a persistência e a recuperação dos registros.

O esquema físico deve ser adaptado à tecnologia encontrada na auditoria do projeto.

---

# 16. Segurança, confiabilidade e limites de atuação

O AutoSense AI deve ser um sistema de diagnóstico e apoio à decisão, não um agente autônomo que executa comandos indiscriminadamente no veículo.

## Requisitos

1. A IA não pode executar comandos OBD-II diretamente.
2. A leitura deve ser separada da escrita.
3. Comandos que alterem o veículo devem ser explicitamente classificados.
4. Operações destrutivas exigem confirmação e justificativa.
5. O sistema deve evitar consultas excessivas que prejudiquem a comunicação.
6. Falhas de comunicação não podem ser interpretadas como falhas mecânicas.
7. Informações críticas devem apresentar mensagens claras e prudentes.
8. A aplicação não deve incentivar testes perigosos durante a condução.
9. Dados de sensores devem ser validados antes de qualquer conclusão.
10. A IA deve ter acesso apenas às ferramentas e aos dados necessários.
11. Instruções encontradas em documentos recuperados não podem substituir as regras de segurança do sistema.
12. Os registros devem permitir investigar como uma conclusão foi produzida.

---

# 17. Estratégia de testes

## 17.1 Testes unitários

Testar:

- Fórmulas de conversão.
- Interpretação das respostas.
- Validação dos dados.
- Regras de diagnóstico.
- Formatação dos relatórios.
- Validação das respostas da IA.

## 17.2 Testes de integração

Testar:

- Comunicação entre a interface e a camada OBD-II.
- Persistência do histórico.
- Consulta ao catálogo.
- Integração com o backend.
- Recuperação de documentos.
- Geração de diagnósticos completos.

## 17.3 Testes com dados simulados

Criar cenários controlados para:

- Leitura normal.
- Código de falha conhecido.
- Parâmetro não suportado.
- Resposta inválida.
- Timeout.
- Desconexão.
- Dados contraditórios.
- Ausência de informações suficientes.
- Falha de acesso ao modelo de IA.

## 17.4 Testes reais

Executar apenas quando houver veículo e hardware disponíveis, com condições seguras e procedimentos documentados.

Registrar a diferença entre:

- Resultado simulado.
- Resultado reproduzido em veículo.
- Resultado respaldado por documentação.
- Resultado ainda não validado.

## 17.5 Avaliação da IA

Criar um conjunto de casos de referência revisados tecnicamente.

Avaliar:

- Relevância das hipóteses.
- Fidelidade às evidências.
- Qualidade das fontes.
- Adequação dos próximos passos.
- Capacidade de reconhecer incerteza.
- Frequência de afirmações sem suporte.
- Reprodutibilidade.

Não utilizar somente a avaliação subjetiva de que a resposta parece boa.

---

# 18. Roadmap de desenvolvimento

O projeto deve evoluir por entregas pequenas e demonstráveis.

## Etapa 0 — Auditoria e estabilidade

**Objetivo:** entender e preservar a base atual.

Entregáveis:
- Inventário técnico.
- Arquitetura existente.
- Teste de leitura OBD-II.
- Lista de componentes reutilizáveis.
- Plano de refatoração.

## Etapa 1 — MVP de diagnóstico

**Objetivo:** entregar uma demonstração acadêmica funcional.

Entregáveis:
- Perfil de veículo.
- Leitura de códigos.
- Leitura de parâmetros suportados.
- Base técnica inicial.
- Diagnóstico baseado em regras.
- IA com respostas estruturadas.
- Relatório.
- Testes automatizados.

## Etapa 2 — Monitoramento personalizado

Entregáveis:
- Painel configurável.
- Gráficos.
- Sessões de monitoramento.
- Histórico.
- Comparação entre sessões.

## Etapa 3 — RAG e avaliação avançada

Entregáveis:
- Recuperação de documentação.
- Fontes vinculadas às respostas.
- Conjunto de avaliação.
- Métricas de qualidade.
- Tratamento de incerteza.

## Etapa 4 — Expansão da base de veículos

Entregáveis:
- Catálogo ampliado.
- Perfis de compatibilidade.
- Identificação de configurações.
- Processo de validação e versionamento.

## Etapa 5 — Laboratório avançado

Entregáveis:
- Análise de logs.
- Detecção de anomalias.
- Ferramentas de comparação.
- Visualização de arquivos técnicos.
- Pesquisa controlada de calibração.

## Ordem de prioridade

Não iniciar as etapas 2 a 5 enquanto o MVP não estiver estável. A evolução deve ocorrer com testes e entregas incrementais.

---

# 19. Cronograma acadêmico sugerido

Considerando a entrega acadêmica em 5 de novembro de 2026, priorizar um MVP com escopo controlado.

### Fase 1 — Auditoria e definição

- Inspecionar o projeto.
- Definir arquitetura mínima.
- Identificar funcionalidades existentes.
- Selecionar os primeiros casos de teste.

### Fase 2 — Base técnica

- Implementar o perfil inicial de veículo.
- Organizar códigos e parâmetros.
- Separar as regras determinísticas.
- Validar a leitura real existente.

### Fase 3 — Integração de IA

- Definir o contrato de entrada e saída.
- Integrar o modelo escolhido.
- Implementar a recuperação de fontes essenciais.
- Validar as respostas.
- Tratar indisponibilidade do modelo.

### Fase 4 — Interface e relatório

- Construir a tela de diagnóstico.
- Apresentar evidências e hipóteses.
- Gerar o relatório.
- Preparar a demonstração.

### Fase 5 — Validação e entrega

- Executar os testes.
- Corrigir os problemas prioritários.
- Documentar a arquitetura.
- Preparar o README.
- Registrar as limitações.
- Preparar uma apresentação demonstrável.

O escopo final deve ser ajustado ao estado real do repositório e à disponibilidade de hardware. Se houver risco ao prazo, priorizar confiabilidade e avaliação em vez de adicionar funções superficiais.

---

# 20. Organização sugerida do repositório

A estrutura exata depende da tecnologia existente. Usar como referência, não como exigência de reescrita.

autosense-ai/
- mobile/
  - app/
  - vehicle/
  - obd/
  - telemetry/
  - diagnostics/
  - dashboards/
  - history/
- backend/
  - api/
  - vehicles/
  - knowledge/
  - diagnostics/
  - ai/
  - reports/
- data/
  - schemas/
  - seed/
  - fixtures/
- docs/
  - architecture/
  - decisions/
  - compatibility/
  - safety/
- tests/
  - unit/
  - integration/
  - fixtures/
  - evaluation/
- scripts/
- README.md

Se o projeto não utilizar backend, não criar um apenas para seguir esse modelo. Se já houver uma estrutura funcional, evoluí-la gradualmente.

Nunca armazenar credenciais, chaves privadas ou dados pessoais reais em arquivos de exemplo.

---

# 21. Documentação e portfólio

O projeto deverá ter documentação suficiente para que outro desenvolvedor compreenda o sistema.

O README deve apresentar:

- Problema que o projeto resolve.
- Visão geral da solução.
- Arquitetura.
- Funcionalidades implementadas.
- Tecnologias utilizadas.
- Requisitos.
- Instruções de instalação.
- Como executar.
- Como utilizar dados simulados.
- Como conectar um adaptador compatível.
- Como executar os testes.
- Como funciona a IA.
- Como são tratadas as fontes.
- Limitações conhecidas.
- Compatibilidade efetivamente testada.
- Roadmap.
- Créditos e licenças das dependências.

Criar diagramas de arquitetura e fluxos de diagnóstico.

Usar capturas de tela e vídeos de demonstração apenas com dados reais ou claramente identificados como simulados.

Não anunciar compatibilidade com veículos que ainda não foram validados.

---

# 22. Definition of Done

Uma funcionalidade só será considerada concluída quando:

1. O código estiver implementado.
2. O fluxo principal estiver funcional.
3. Erros previsíveis estiverem tratados.
4. Os testes pertinentes tiverem sido executados.
5. A documentação tiver sido atualizada.
6. Os limites da funcionalidade estiverem registrados.
7. Nenhuma funcionalidade existente tiver sido quebrada.
8. Os dados exibidos forem rastreáveis à sua origem.
9. A interface diferenciar estados válidos e indisponíveis.
10. Não houver alegações de compatibilidade ou precisão sem evidências.

Se um requisito não puder ser validado, marcar como pendente, em vez de declará-lo concluído.

---

# 23. Primeira tarefa para a IA de desenvolvimento

Comece exclusivamente pela auditoria do projeto existente.

Não implemente todas as funcionalidades descritas neste documento de uma só vez.

Execute esta sequência:

1. Inspecione os arquivos e a estrutura do repositório.
2. Identifique a tecnologia e o ponto de entrada da aplicação.
3. Localize a implementação da conexão OBD-II.
4. Localize o código responsável por interpretar os dados.
5. Identifique as funcionalidades operacionais.
6. Identifique os testes existentes.
7. Avalie as dependências e os componentes cuja reutilização está autorizada.
8. Produza um mapa da arquitetura atual.
9. Compare o estado atual com os módulos definidos neste documento.
10. Proponha um plano de implementação priorizado, com arquivos envolvidos, riscos e critérios de aceitação.

Ao concluir, apresente:

- O que foi encontrado.
- O que está funcionando.
- O que está incompleto.
- Quais mudanças são realmente necessárias.
- Qual será a primeira implementação.
- Como essa implementação será testada.

Aguarde a validação do plano antes de iniciar uma grande refatoração.

---

# 24. Instrução permanente de trabalho

Trabalhe como um engenheiro de software experiente, com foco em arquitetura, qualidade, diagnóstico automotivo e aplicações de IA.

Priorize:

- Correção técnica.
- Evidências.
- Modularidade.
- Segurança.
- Manutenibilidade.
- Testabilidade.
- Experiência do usuário.
- Evolução incremental.

Não transforme o projeto em uma coleção de funcionalidades desconectadas.

Cada módulo deve contribuir para a visão geral do AutoSense AI, compartilhar modelos de dados consistentes e ter interfaces claras com os demais componentes.

A meta não é apenas fazer a aplicação parecer completa. A meta é construir um sistema que funcione, que possa ser testado, que explique seus resultados e que seja tecnicamente defensável em uma apresentação acadêmica e em um portfólio profissional.

**Primeiro: compreender e preservar o leitor existente. Depois: construir o diagnóstico inteligente. Em seguida: expandir para uma plataforma automotiva modular.**