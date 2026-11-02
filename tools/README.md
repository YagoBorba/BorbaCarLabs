# Ferramentas de dados

## Catálogo de veículos (`app/src/main/res/raw/vehicle_catalog.json`)

Gerado a partir das tabelas **PBE Veicular — Veículos leves** do Inmetro de 2012 a 2026, uma por
ano-modelo, consultadas em 01/11/2026 na página do programa
(`https://www.gov.br/inmetro/.../veiculos-automotivos-pbe-veicular`). A tabela de 2019 é a mesma da
versão anterior (SHA-256 `d2168ac4bdd266a483468390d501565186e36949eff0c37f61fbc419be5d8804`).

```bash
mkdir pbev && cd pbev
# baixe cada ano como 2012.pdf ... 2026.pdf (links na página do programa)
for y in $(seq 2012 2026); do pdftotext -table -enc UTF-8 $y.pdf t$y.txt; done
cd ..
git show v1.1.1:app/src/main/res/raw/vehicle_catalog.json > pbev/catalogo-anterior.json
node tools/pbev-catalog.mjs pbev pbev/catalogo-anterior.json novo.json relatorio.json
```

`novo.json` vai para `vehicle_catalog.json` e `novo-efficiency.json` para `vehicle_efficiency.json`.
`pdftotext` é o do xpdf 4.x (modo `-table`). Regras de extração:

- Uma linha só entra se as colunas fecham: marca da lista, motor com cilindrada, transmissão
  (`M-6`, `A-4`, `MTA-5`, `DCT-7`, `CVT`) e logo depois ar-condicionado, direção e combustível
  (com a coluna de propulsão, quando o ano tem). Cada ano tem um layout; o extrator acha as colunas
  pela marca e pela transmissão, não pela posição.
- Células que continuam de outra linha do PDF (versão começando ou terminando com `/`, parênteses
  desbalanceados, modelo partido como "TURBO" ou "TOWN &") são **rejeitadas**, não completadas.
- Nomes de modelo são unificados entre os anos: "NOVO", "New" e "Nova" saem do começo; variantes de
  carroceria, tração e cabine ("4X4", "CD", "Sedan", "(Pneus On Road)") vão para a versão; marcas de
  ano ("(MY19)", "(mod.26)") saem; a grafia mais comum vence ("Onix", "HB20S", "T-Cross").
- Versões agrupadas ("Origine / Attraction / Tendance") viram uma configuração cada. Códigos de
  motor e câmbio no nome da versão ("1.4MT LT", "10TAT LT1") saem; na Chevrolet, "1LT", "LT2" e
  "PR2" viram LT e Premier.
- As 16 versões Chevrolet curadas na 0.6.8 ficam como estavam: perfis salvos usam seus ids.
- O formato do arquivo é o `schema_version` 2: cada configuração traz fonte e seção uma vez, e os
  valores ficam em `values`. O app ainda lê o formato 1.
- Em 01/11/2026: 9.541 linhas aceitas e 987 rejeitadas, resultando em 9.362 versões de 504 modelos
  e 43 marcas. Elétricos ficaram de fora por não terem motor a combustão.

O relatório (`relatorio.json`) lista cada linha rejeitada e o motivo. O consumo oficial
(km/l na cidade e na estrada) é lido da mesma linha quando as colunas fecham: 9.124 versões têm.

## Catálogo de códigos (`DtcReference`, generic-dtc-2)

Significados conferidos na tabela pública de códigos genéricos P0 do Ford PCED 1999 (seção 4)
e no boletim VW 24Y2 hospedado pela NHTSA (página 3). Textos curtos escritos para o app; as fontes
são citadas, não reproduzidas. Ficaram de fora códigos de fabricante (P1) e quatro códigos cujo
texto na fonte é repetido ou tem erro (P0141, P0161, P0180, P0553).
