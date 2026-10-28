# Investigações offline — 0.6.2

Novas leituras na interface congelam também a identificação completa do perfil no esquema 4: origens por atributo, revisão, anos separados e referências disponíveis. Esse snapshot entra nas exportações e não acompanha edições posteriores da Garagem. “Declarado por você” difere de configuração documentada; nenhum deles comprova compatibilidade OBD. Casos e seeds antigos mantêm o formato original e não recebem dados atuais por suposição.

1. Selecione o veículo na Garagem e conecte um adaptador ou ative a demonstração.
2. Se quiser anexar sensores, deixe o Painel coletar as leituras desejadas antes de ir para Falhas. O caso conserva os horários reais de cada amostra.
3. Em Falhas, leia os códigos. Opcionalmente, toque em **Ler dados complementares** para consultar prontidão e quadro congelado 00. Confira os resultados e então toque em **Investigar esta leitura**. Informe descrição, quando ocorre, frequência e condições; todos os campos são opcionais.
4. Toque em **Salvar caso**. A revisão offline separa dados observados, significados genéricos conhecidos, informações ausentes, roteiro de verificação, fontes e limitações. Nesta versão, não são geradas hipóteses mecânicas: faltam fontes técnicas revisadas.
5. Abra o caso na lista **Investigações offline**, mesmo sem scanner ou internet. **Editar sintomas / criar revisão** mantém as medições e cria uma nova revisão. Uma nova leitura deve gerar uma nova investigação.
6. Use **Consultar revisões anteriores** para comparar relatos. Uma revisão antiga pode ser consultada/exportada, mas não substitui a mais recente: uma tentativa de edição antiga é rejeitada.
7. Exporte JSON para preservar toda a estrutura ou TXT para leitura/compartilhamento. Ambos preservam os mesmos fatos e o fingerprint. PDF usa a plataforma Android; o arquivo precisa ser conferido em telefone, pois o gerador nativo não é fornecido por este ambiente simulado.

Os relatórios de leitura e as investigações são registros diferentes, vinculados pelo ID da captura. Os relatórios da 0.3.1 permanecem acessíveis, sem converter texto antigo em evidência presumida. Somente relatórios novos com `investigation_seed` permitem criar uma investigação a partir do snapshot salvo.

Campos desconhecidos permanecem desconhecidos. Sintomas são declarações suas, não medições. Dados antigos não ficam atuais ao reabrir um caso. Prontidão e quadro entram no caso somente se você usar a coleta complementar antes de salvá-lo, na mesma sessão e perfil. Se não coletados, continuam ausentes. As ferramentas avulsas não anexam dados. O horário do quadro indica quando o app consultou os dados, sem afirmar o instante do congelamento. O caso não associa automaticamente um quadro congelado a um código.

Remover um perfil mantém os casos na área de perfis removidos. Excluir uma investigação exige confirmação e remove apenas esse caso e suas revisões. Apagar relatórios do veículo não exclui as investigações. Corrupção ou esquema não reconhecido geram aviso; o app não apaga o banco para tentar recuperá-lo.

Não há modelo de IA integrado, análise de peças ou comprovação de compatibilidade física nesta entrega. O modo demonstração usa dados fictícios identificados. O formulário de sintomas da investigação ainda não salva rascunho automaticamente: conclua o salvamento antes de sair ou girar a tela. A restauração de rascunho implementada na 0.6.2 é do cadastro de veículo na Garagem.

Exemplo completo produzido pelos testes: [JSON simulado](examples/investigacao-simulada.json) e [texto correspondente](examples/investigacao-simulada.txt). São dados fictícios, com revisão de sintomas e origem identificada.

## Complementos na 0.5.0

A coleta consulta 0101 e os PIDs 0C, 0D, 05, 04 e 11 do serviço 02, quadro 00. Cada consulta distingue resposta positiva, NO DATA, negativa, inválida, timeout, desconexão e não executada. Uma falha de transporte interrompe as consultas restantes; as observações anteriores continuam disponíveis para salvar um caso parcial. O serviço 01 não prova suporte aos mesmos PIDs no serviço 02.

Monitores não suportados não são marcados como concluídos. “Complemento coletado” indica respostas positivas às consultas, não aprovação de inspeção nem ausência de defeitos. Múltiplas respostas são preservadas separadamente, sem tratá-las como ECUs identificadas ou somar contagens. Indicações MIL divergentes entre consultas são apontadas sem inventar uma causa.

Uma nova leitura de DTCs limpa o complemento anterior. Desconectar/trocar o perfil também limpa o complemento da sessão; salve uma investigação para preservá-lo. Um complemento novo não altera um caso já salvo. Crie uma nova investigação para reunir novas medições. O formulário continua sem salvamento automático de rascunho.

Casos da 0.4.0 conservam documento e análise originais (`coverage-1`), inclusive ao revisar sintomas. Casos da 0.5.0 usam esquema 2 e `coverage-2`; novos casos da 0.6.0 usam esquema 3 com `investigation-1` e `generic-dtc-1`. Nenhum resultado antigo recebe prontidão ou quadro presumidos. Referências de protocolo são genéricas; não equivalem a fontes de causas mecânicas ou compatibilidade específica.

Exemplo da 0.5.0 com complemento: [JSON](examples/investigacao-complementar.json) e [TXT](examples/investigacao-complementar.txt). O exemplo anterior permanece como referência de esquema 1.

## Catálogo e roteiro na 0.6.0

**Consultar código** na aba Falhas mostra o significado disponível e suas fontes. Em **Mais → Catálogo OBD offline**, a busca funciona sem scanner ou internet. Cinco códigos estão definidos; os demais permanecem sem descrição presumida.

Novas investigações incluem perguntas vinculadas aos códigos observados e às respectivas referências. Elas ajudam a organizar os sintomas e os dados que faltam. Não confirmam causas, peças ou procedimentos do Cobalt. Documentos antigos mantêm sua análise original. Consulte os [limites e fontes do catálogo](CATALOGO-DTC.md).

Exemplo da 0.6.0: [JSON com definições e roteiro](examples/investigacao-catalogo.json) e [TXT correspondente](examples/investigacao-catalogo.txt), produzidos em demonstração pelos testes.
