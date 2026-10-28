# Fontes pesquisadas para a reformulação

Consulta: 21/10/2026. Fontes acessadas por pesquisa/web; nenhuma base foi importada ou aprovada para redistribuição nesta revisão. Fonte encontrada não equivale a atributo validado. As decisões abaixo são de produto, não parecer jurídico.

| Fonte e proveniência | Uso proposto | Cobertura e atualização | Limitações e direitos |
|---|---|---|---|
| [Manual Cobalt MY19, Chevrolet Brasil](https://meu.chevrolet.com.br/content/dam/gmownercenter/gmsa/gmbr/dynamic/manuals/2019/chevrolet/Cobalt/pt/om_ng-chevrolet_Cobalt_my19-pt_BR.pdf.pdf) | Referência de documentação aplicável à família; pesquisa posterior por seção/variante | Documento MY19; registrar edição, seção/página, hash e data na futura extração | Não identifica o carro do usuário nem comprova PIDs/módulos. PDF acessível não concede automaticamente redistribuição; usar link e manter permissão pendente para conteúdo embarcado. Nenhuma especificação numérica extraída nesta revisão |
| [PBE Veicular, Inmetro](https://www.gov.br/inmetro/pt-br/assuntos/avaliacao-da-conformidade/programa-brasileiro-de-etiquetagem/tabelas-de-eficiencia-energetica/veiculos-automotivos-pbe-veicular) | Pesquisar configurações participantes e referências de consumo com edição identificada | Tabelas por ciclo, incluindo 2019 e 2026; revisar revisões da tabela de interesse | Não é todo o mercado, não é consumo do usuário nem prova de OBD. Verificar cabeçalhos/metodologia e condições de cada linha antes de associar variantes. Registrar termos/licença aplicáveis ao arquivo antes de embarcar fatos; pendente |
| [FIPE oficial](https://www.fipe.org.br/pt-br/indices/veiculos) | Eventual consulta externa de preço de referência, baixa prioridade | Consulta por modelo e mês/ano de referência; ano é ano-modelo | A página afirma não oferecer API nem download da base. Não planejar API oficial inexistente nem raspagem como dependência. Fornecedores terceiros exigem análise própria de origem/contrato. Preço não comprova configuração técnica |
| [vPIC, NHTSA](https://vpic.nhtsa.dot.gov/) e [API](https://vpic.nhtsa.dot.gov/api/Home/Index) | Eventual consulta auxiliar de identidade dentro do escopo aplicável | Dados enviados por fabricantes; foco em veículos destinados a venda/importação nos EUA; API com controle de tráfego | Pode retornar informação limitada para veículos fora desse mercado. Não preencher automaticamente ficha brasileira. Avaliar condições de uso/redistribuição antes de copiar base; não aprovado nesta revisão |
| [Consulta de recall, Senatran](https://www.gov.br/pt-br/servicos/realizar-consulta-sobre-recall-de-veiculos) | Atalho ao serviço oficial, sem guardar credenciais | Página descreve dados RENAVAM divulgados pelas montadoras a partir de 17/03/2011 | Consultas em volume dependem de autorização/contratação descritas no serviço. Sem integração automática nesta fase. Visitar a página não autoriza exibir “sem recall”; resultado precisa de veículo, fonte e data |
| [Preços de combustíveis, ANP](https://www.gov.br/anp/pt-br/centrais-de-conteudo/dados-abertos/serie-historica-de-precos-de-combustiveis) e [painel](https://www.gov.br/anp/pt-br/centrais-de-conteudo/paineis-dinamicos-da-anp/painel-dinamico-de-precos-de-combustiveis-e-derivados-do-petroleo) | Referência regional opcional ao diário de abastecimento | Série de levantamento e agregados semanais/mensais; informar local, produto e período | Não é preço em tempo real de todo posto nem preço pago pelo usuário. Publicada como dados abertos; registrar licença/metadados específicos do recurso escolhido e atribuição antes de redistribuir. Nenhum recurso incorporado |
| [Acessibilidade Android Views](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views?hl=en) | Critérios de componentes e testes nativos | Documentação oficial para Views | Referência recomenda alvos de toque de pelo menos 48dp; não substitui ensaio com fonte ampliada/leitor de tela |

## Publicação de um pacote real

1. Identificar recurso exato, editor, edição, mercado e atributos cobertos.
2. Registrar limites de uso e a evidência de permissão; não converter estado pendente em aprovado por mera disponibilidade pública.
3. Extrair somente os fatos necessários, mantendo localização e relação entre configuração e atributo.
4. Revisar divergências entre fontes e não unir variantes pelo nome aproximado.
5. Revisão técnica independente para instruções mecânicas e limites; manter desconhecidos quando a fonte não sustenta a conclusão.
6. Validar esquema, duplicatas, escopo, assinatura do pacote e compatibilidade dos leitores.
7. Publicar número de configurações e cobertura honesta; conservar edição anterior e snapshots.

Nenhuma dessas fontes, isoladamente, autoriza declarar que o Cobalt do usuário suporta determinada função. Essa evidência precisa ser vinculada à função, ao contexto e ao ensaio ou documento específico.
