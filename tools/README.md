# Ferramentas de dados

## Catálogo de veículos (`app/src/main/res/raw/vehicle_catalog.json`)

Gerado a partir da tabela **PBE Veicular — Veículos leves 2019** do Inmetro (PDF de 46 páginas,
SHA-256 `d2168ac4bdd266a483468390d501565186e36949eff0c37f61fbc419be5d8804`, consultado em 27/10/2026).

```bash
curl -L -o pbev-2019.pdf "https://www.gov.br/inmetro/pt-br/assuntos/regulamentacao/avaliacao-da-conformidade/programa-brasileiro-de-etiquetagem/tabelas-de-eficiencia-energetica/veiculos-automotivos-pbe-veicular/veiculos-leves-2019/@@download/file"
mkdir pbev
for p in $(seq 1 46); do pdftotext -f $p -l $p -table -enc UTF-8 pbev-2019.pdf pbev/p$p.txt; done
node tools/pbev-catalog.mjs pbev 46 app/src/main/res/raw/vehicle_catalog.json novo.json relatorio.json
```

`pdftotext` é o do xpdf 4.x (modo `-table`). Regras de extração:

- Uma linha só entra se as colunas fecham: categoria conhecida, marca da lista, motor com
  cilindrada, transmissão (`M-6`, `A-4`, `MTA-5`, `DCT-7`, `CVT`), e logo depois ar-condicionado,
  direção e combustível nessa ordem. Isso evita confundir "E" de direção elétrica com etanol.
- Células que continuam de outra linha do PDF (versão começando ou terminando com `/`, começando
  com parêntese ou com parênteses desbalanceados) são **rejeitadas**, não completadas.
- O motor partido em células ("1.4L" | "-" | "8V") é reunido, porque as partes estão na linha.
- As 16 versões Chevrolet curadas na 0.6.8 ficam como estavam: perfis salvos usam seus ids.
- Em 27/10/2026: 519 linhas aceitas e 165 rejeitadas (de 684), resultando em 516 versões
  de 34 marcas. Elétricos e híbridos ficaram de fora por não terem cilindrada nesse formato.

O relatório (`relatorio.json`) lista cada linha rejeitada e o motivo.

## Catálogo de códigos (`DtcReference`, generic-dtc-2)

Significados conferidos na tabela pública de códigos genéricos P0 do Ford PCED 1999 (seção 4)
e no boletim VW 24Y2 hospedado pela NHTSA (página 3). Textos curtos escritos para o app; as fontes
são citadas, não reproduzidas. Ficaram de fora códigos de fabricante (P1) e quatro códigos cujo
texto na fonte é repetido ou tem erro (P0141, P0161, P0180, P0553).
