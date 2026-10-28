# Catálogo MY19 — primeira base factual

26/10/2026 · catálogo `br-my19-1`, sequência 2 · aplicativo 0.6.8.

A base deixou de ser vazia. Contém um recorte de **16 configurações Chevrolet MY19**: seis Cobalt, cinco Onix e cinco Prisma. Não é um catálogo completo do Brasil nem de toda a linha Chevrolet. O cadastro informa essa cobertura antes de salvar; veículos ausentes continuam com cadastro simples. Nenhum perfil antigo recebe dados automaticamente.

## Evidência e proveniência

Fonte primária: [Inmetro — PBE Veicular, veículos leves 2019](https://www.gov.br/inmetro/pt-br/assuntos/regulamentacao/avaliacao-da-conformidade/programa-brasileiro-de-etiquetagem/tabelas-de-eficiencia-energetica/veiculos-automotivos-pbe-veicular/veiculos-leves-2019/@@download/file), PDF de 46 páginas, cabeçalho 36 marcas / 684 modelos. Conferência textual e visual das páginas 2 e 6, contadas a partir de 1. Consulta em 26/10/2026. SHA-256 do PDF consultado: `d2168ac4bdd266a483468390d501565186e36949eff0c37f61fbc419be5d8804`.

Foram usados somente marca, modelo, ano-modelo explicitamente indicado por MY19, versão, descrição resumida do motor, tipo/número de marchas e combustível. O mercado BR decorre do programa brasileiro e da publicação do Inmetro. M-6 e A-6 seguem a legenda de transmissão; F significa Flex. A expressão MY19 foi normalizada para ano-modelo 2019; o ano da tabela isoladamente **não** atribui ano-modelo. Não foi atribuído ano de fabricação.

Cada atributo conserva documento, URL, localização da linha, edição, data de consulta, mercado e revisão de origem. O gerador determinístico `scripts/catalog-my19.py` contém o recorte conferido e produz o JSON embarcado. Ele não consulta a internet, extrai novas linhas nem aprova novas fontes.

No Cobalt LTZ, a fonte distingue três configurações: 1.4 manual, 1.8 manual e 1.8 automática. Por isso, informar “Cobalt LTZ 2019” não escolhe automaticamente motor/câmbio. O usuário deve selecionar a versão que corresponde ao carro ou continuar sem configuração. A escolha de uma linha carrega seus atributos juntos, com o mesmo escopo de origem; não combina listas independentes.

## Direitos e limites de incorporação

O [portal do Inmetro](https://www.gov.br/inmetro/pt-br/assuntos/regulamentacao/avaliacao-da-conformidade/programa-brasileiro-de-etiquetagem/tabelas-de-eficiencia-energetica/veiculos-automotivos-pbe-veicular) informa [CC BY-ND 3.0](https://creativecommons.org/licenses/by-nd/3.0/). Isso não é tratado como licença para transformar e redistribuir o PDF ou como autorização específica concedida ao aplicativo.

A decisão desta curadoria é incorporar somente fatos de identificação em uma estrutura própria, sem tabelas gráficas, textos explicativos, imagens, selos, logotipos, consumo/emissões ou o PDF original. A distinção entre a expressão de uma compilação e seus dados técnicos foi conferida nos arts. 7º, XIII e §§ 2º–3º da [Lei 9.610/1998](https://www.planalto.gov.br/ccivil_03/leis/l9610.htm). Essa avaliação limitada não equivale a parecer jurídico ou autorização para importar uma base inteira. A atribuição e o link permanecem disponíveis no app.

`APPROVED_EMBEDDED_FACTS` é o estado interno desta revisão factual limitada. Não significa aprovação do app pelo Inmetro, autorização individual do titular ou verificação por um especialista automotivo humano. O manual da Chevrolet foi consultado como referência, mas não foi incorporado nem usado para ampliar o recorte.

## O que esta base não comprova

- Identidade física do carro, autenticidade de VIN, alterações/modificações ou compatibilidade OBD.
- PIDs suportados, protocolos, ABS, airbag, atuadores, codificação ou programação.
- Consumo real, autonomia, potência, manutenção, pressão de pneus ou capacidade de fluidos.
- Outros anos/modelos, versões não selecionadas da própria tabela ou toda a frota brasileira.

As fontes são históricas. Não há promessa de atualização automática: nova publicação exige conferência, nova sequência e novo pacote. O catálogo local anterior válido continua preservado; uma base embarcada mais recente pode ser usada sem acusá-lo de corrupção. Perfis e documentos históricos mantêm seus próprios valores e fontes congeladas.
