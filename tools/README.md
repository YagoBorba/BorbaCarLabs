# Data tools

## Vehicle catalog (`app/src/main/res/raw/vehicle_catalog.json`)

Generated from Inmetro's **PBE Veicular — Light vehicles** tables from 2012 to 2026, one per
model year, retrieved on 2026-11-01 from the program page
(`https://www.gov.br/inmetro/.../veiculos-automotivos-pbe-veicular`). The 2019 table is the same as in the
previous version (SHA-256 `d2168ac4bdd266a483468390d501565186e36949eff0c37f61fbc419be5d8804`).

```bash
mkdir pbev && cd pbev
# download each year as 2012.pdf ... 2026.pdf (links on the program page)
for y in $(seq 2012 2026); do pdftotext -table -enc UTF-8 $y.pdf t$y.txt; done
cd ..
git show v1.1.1:app/src/main/res/raw/vehicle_catalog.json > pbev/catalogo-anterior.json
node tools/pbev-catalog.mjs pbev pbev/catalogo-anterior.json novo.json relatorio.json
```

`novo.json` becomes `vehicle_catalog.json` and `novo-efficiency.json` becomes `vehicle_efficiency.json`.
`pdftotext` is the one from xpdf 4.x (`-table` mode); the script needs Node.js 20+. Extraction rules:

- A row is accepted only if its columns line up: a make from the list, an engine with displacement, a transmission
  (`M-6`, `A-4`, `MTA-5`, `DCT-7`, `CVT`) and right after it air conditioning, steering and fuel
  (plus the propulsion column, when the year has one). Each year has its own layout; the extractor finds the columns
  by make and transmission, not by position.
- Cells that continue from another PDF row (a version starting or ending with `/`, unbalanced
  parentheses, a split model such as "TURBO" or "TOWN &") are **rejected**, not completed.
- Model names are unified across years: "NOVO", "New" and "Nova" are dropped from the start; body,
  drive and cab variants ("4X4", "CD", "Sedan", "(Pneus On Road)") move to the version; year marks
  ("(MY19)", "(mod.26)") are dropped; the most common spelling wins ("Onix", "HB20S", "T-Cross").
- Grouped versions ("Origine / Attraction / Tendance") become one configuration each. Engine and
  transmission codes in the version name ("1.4MT LT", "10TAT LT1") are dropped; for Chevrolet, "1LT", "LT2" and
  "PR2" become LT and Premier.
- The 16 Chevrolet versions curated in 0.6.8 stay as they were: saved profiles use their ids.
- The file format is `schema_version` 2: each configuration carries its source and section once, and the
  values live in `values`. The app still reads format 1.
- On 2026-11-01: 9,541 rows accepted and 987 rejected, resulting in 9,362 versions of 504 models
  and 43 makes. Electric vehicles were left out because they have no combustion engine.

The report (`relatorio.json`) lists every rejected row and the reason. The official fuel economy
(km/l city and highway) is read from the same row when the columns line up: 9,124 versions have it.

## Trouble code catalog (`DtcReference`, generic-dtc-3)

Meanings checked against the public table of generic P0 codes in Ford PCED 1999 (section 4)
and the VW 24Y2 bulletin hosted by NHTSA (page 3). Short texts written for the app; the sources
are cited, not reproduced. Manufacturer codes (P1) and four codes whose text in the source is
repeated or wrong (P0141, P0161, P0180, P0553) were left out.

generic-dtc-3 adds 26 common codes that were missing (P0011 to P0019, P0068, P0101, P0128, P0219,
P0335, P0336, P0506, P0507, P0562, P0563, P0627, P0641 and U0001, U0073, U0100, U0101, U0121, U0140,
U0155), checked against the "2014 OBD-II 5 Digit Code Interpretation" table published by Ford Performance
Parts (93-page PDF, SHA-256 `38b56df0e234b89eae1854823cfa4e7b5331f2da3be18ef78102b428305f8627`,
retrieved on 2026-11-01). Each definition cites the page and line.
