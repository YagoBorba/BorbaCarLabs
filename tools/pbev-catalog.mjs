import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const [dir, currentPath, outPath, reportPath] = process.argv.slice(2);
const FIRST_YEAR = 2012;
const LAST_YEAR = 2026;
const TODAY = "2026-11-01";
const BASE = "https://www.gov.br/inmetro/pt-br/assuntos/regulamentacao/avaliacao-da-conformidade/programa-brasileiro-de-etiquetagem/tabelas-de-eficiencia-energetica/veiculos-automotivos-pbe-veicular/";
const FILES = {
  2022: "pbe-veicular-2022.pdf",
  2023: "pbe-veicular-2023.pdf",
  2024: "pbe-veicular-2024-1.pdf",
  2025: "mascara-pbev-2025-mar-11.pdf",
  2026: "mascara-pbev-2026_19_jan-rev01.pdf",
};
const urlOf = (year) => `${BASE}${FILES[year] ?? `veiculos-leves-${year}`}/@@download/file`;

const BRANDS = {
  "ALFA ROMEO": "Alfa Romeo", "ASTON MARTIN": "Aston Martin", "AUDI": "Audi", "BENTLEY": "Bentley", "BMW": "BMW", "BYD": "BYD",
  "CAOA CHERY": "CAOA Chery", "CHERY": "Chery", "CHEVROLET": "Chevrolet", "GM": "Chevrolet", "CHRYSLER": "Chrysler",
  "CITROËN": "Citroën", "CITROEN": "Citroën", "DODGE": "Dodge", "EFFA": "Effa", "FERRARI": "Ferrari", "FIAT": "Fiat",
  "FORD": "Ford", "GWM": "GWM", "HAVAL": "GWM", "HONDA": "Honda", "HYUNDAI": "Hyundai", "JAC": "JAC", "JAGUAR": "Jaguar",
  "JEEP": "Jeep", "KIA": "Kia", "KIA MOTORS": "Kia", "LAMBORGHINI": "Lamborghini", "LAND ROVER": "Land Rover",
  "LEXUS": "Lexus", "LIFAN": "Lifan", "MASERATI": "Maserati", "MCLAREN": "McLaren", "MERCEDES-BENZ": "Mercedes-Benz",
  "MERCEDES BENZ": "Mercedes-Benz", "MINI": "MINI", "MITSUBISHI": "Mitsubishi", "NISSAN": "Nissan", "PEUGEOT": "Peugeot",
  "PORSCHE": "Porsche", "RAM": "RAM", "RENAULT": "Renault", "ROLLS-ROYCE": "Rolls-Royce", "SMART": "smart",
  "SSANGYONG": "SsangYong", "SUBARU": "Subaru", "SUZUKI": "Suzuki", "TOYOTA": "Toyota", "TROLLER": "Troller",
  "VOLVO": "Volvo", "VW": "Volkswagen", "VOLKSWAGEN": "Volkswagen",
};
const BRAND_KEYS = Object.keys(BRANDS).sort((a, b) => b.length - a.length);
const TRANSMISSION = /^(M|A|MTA|AMT|DCT|CVT|AT|MT)-?(\d{1,2})?$/;
const TRANSMISSION_NAME = { M: "Manual", MT: "Manual", A: "Automático", AT: "Automático", MTA: "Automatizado", AMT: "Automatizado", DCT: "Dupla embreagem", CVT: "CVT" };
const GEARED = /^(M|A|MTA|AMT|DCT|CVT)-\d{1,2}$|^(CVT|DCT)$/;
const AC = new Set(["S", "N"]);
const STEERING = new Set(["H", "E", "M", "E-H", "EH", "HE"]);
const FUEL = { F: "Flex", G: "Gasolina", E: "Etanol", E100: "Etanol", D: "Diesel" };
const PROPULSION = /^(combust[ãa]o|h[íi]brido.*|hev|mhev|phev|plug-in)$/i;
const ENGINE = /^(\d)[.,](\d)L?(?:(T|TB)|\s*(TSI|TFSI|THP|TDI|TURBO|V6|V8))?\s*(?:[-–]?\s*(\d{1,2})\s*V?)?\s*(T|TURBO)?$/i;
const DISPLACEMENT = /^\d,\d(\s+flex)?$/i;
const ACRONYMS = new Set(["GTI", "GTS", "TSI", "AMG", "SUV", "WRX", "STI", "GLA", "GLB", "GLC", "GLE", "GLS", "CLA", "CLS", "SLC", "SLK", "RS", "GT", "BMW", "MPI", "TDI", "HPE", "JAC", "GWM", "BYD", "RAM", "LTZ", "LT", "LS", "SS", "SRT", "SVR", "AT", "MT", "CVT", "DCT", "XEI", "GLI", "GLX", "EX", "EXL", "LX", "DX", "SE", "XLS", "XLT", "XL", "SR", "SRV", "SW4", "ZX", "TS", "GL", "GLS", "CRV", "HRV", "WRV", "STD", "CD", "CS", "CE", "AWD", "4WD", "2WD", "4X4", "4X2", "V6", "V8"]);
const MODEL_ALIASES = {
  "ttrs": "TT RS", "town": "Town & Country", "towncountry": "Town & Country", "epace": "E-Pace", "fpace": "F-Pace", "ipace": "I-Pace", "scross": "S-Cross", "santafe": "Santa Fe",
  "up": "up!", "uptsi": "up! TSI", "ix35": "ix35", "hb20": "HB20", "hb20s": "HB20S", "hb20x": "HB20X", "ka": "Ka", "kaplus": "Ka+",
  "ka+": "Ka+", "tcross": "T-Cross", "hrv": "HR-V", "crv": "CR-V", "wrv": "WR-V", "cr-v": "CR-V", "rio": "Rio", "uno": "Uno",
  "500": "500", "mx5": "MX-5", "c4lounge": "C4 Lounge", "c4cactus": "C4 Cactus", "dsfive": "DS5", "rangerover": "Range Rover",
  "rangeroverevoque": "Range Rover Evoque", "rangeroversport": "Range Rover Sport", "rangerovervelar": "Range Rover Velar",
  "discoverysport": "Discovery Sport", "classea": "Classe A", "classec": "Classe C", "classee": "Classe E", "sw4": "SW4",
  "crossfox": "CrossFox", "spacefox": "SpaceFox", "spacecross": "SpaceCross", "oniplus": "Onix Plus", "onixplus": "Onix Plus",
  "onixjoy": "Onix Joy", "prismajoy": "Prisma Joy", "strada": "Strada", "s10": "S10", "a3sedan": "A3 Sedan", "a3sportback": "A3 Sportback",
};
const GARBAGE_MODELS = new Set(["turbo", "cvt", "jetflex", "town", "chassisc", "chassiscc", "sedan", "flex", "at", "mt", "4x4", "4x2", "novo", "nova", "new", "sportback", "performanceseditionv", "performancesedition", "cabrio", "comptrack", "competition", "coupe", "coupetrack", "cs", "cup", "gccompetition", "grancoupe", "gran"]);
const MAKE_ALIASES = {
  "Land Rover|range": "Range Rover", "Land Rover|sport": "Range Rover Sport", "Land Rover|evoque": "Range Rover Evoque",
  "Land Rover|evoquea9": "Range Rover Evoque", "Land Rover|velar": "Range Rover Velar", "Kia|k2500": "Bongo K2500",
  "Renault|stepway": "Sandero Stepway", "Renault|partrapid": "Partner Rapid", "Peugeot|partrapid": "Partner Rapid",
  "Toyota|hiluxsw4": "SW4", "Toyota|rav4h": "RAV4", "GWM|havalh6": "H6", "GWM|havalh9": "H9", "Chevrolet|cruzenb": "Cruze",
  "Hyundai|ix35ffbr": "ix35", "Hyundai|i20": "i20", "Mitsubishi|asx": "ASX", "Audi|a3spotback": "A3 Sportback",
  "Chery|tiggofl": "Tiggo", "Porsche|boxter": "Boxster", "Lamborghini|huracn": "Huracán", "Lamborghini|huracan": "Huracán",
  "Chrysler|townecountry": "Town & Country", "Hyundai|ix35ff": "ix35", "Renault|masterfugaopro": "Master Furgão Pró", "Chrysler|towncountry": "Town & Country",
  "Hyundai|i30fl": "i30", "Audi|q3suv": "Q3", "Audi|q5suv": "Q5", "BMW|320ibr": "320i", "BMW|x5br": "X5",
};
const PREFIX = /^(all new|novo new|novo|nova|new)\s+/i;
const DROP_SUFFIX = /\s+(\((?:[^)]*\d[^)]*|novo|nova|new|nacional)\)|\d{2} ?MY|MY ?\d{2}(\.\d)?|novo|nova|new|SUV|AT|MT|FL|BR|-\s?P\d|diesel|flex|gasolina|turbo|\d[.,]\d\b.*)$/i;
const VARIANT_SUFFIX = /\s+(4X2|4X4|4WD|AWD|2WD|4M|4MATIC|CD|CE|CS|CC|SC|V6|V8|SW|HB|SD|Sedan|Sedã|Hatch|Hatchback|Hybrid|Hev|Phev|Plug-in|C\.? ?Dupla|C\.? ?Simples|C\.|Cabine Dupla|Cabine Simples|Cab\.? ?Dupla|Cab\.? ?Simples|\([^)]*\))$/i;

const slug = (text) => text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const key = (text) => text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9+!]/g, "").replace(/!/g, "");

function titleWord(word) {
  return word.split("-").map((part) => {
    if (!/^[A-ZÀ-Ý]+$/.test(part) || part.length <= 2 || ACRONYMS.has(part)) return part;
    if (part.length === 3 && !/[AEIOUÀ-Ý]/.test(part)) return part;
    return part[0] + part.slice(1).toLowerCase();
  }).join("-");
}
const display = (text) => text.split(" ").map(titleWord).join(" ");

function cellsOf(raw) {
  const text = raw.replace(/­/g, "-").replace(/\b(M|A|MTA|AMT|DCT|CVT)\s*-\s*(\d)/g, "$1-$2").replace(/\bE -H\b/g, "E-H").trim();
  let cells = text.split(/\s{2,}/).map((cell) => cell.trim()).filter(Boolean);
  const out = [];
  for (const cell of cells) {
    const upper = cell.toUpperCase();
    const brand = BRAND_KEYS.find((b) => upper !== b && upper.startsWith(b + " ") && !BRAND_KEYS.includes(upper));
    if (brand && out.every((c) => !BRANDS[c.toUpperCase()])) {
      out.push(cell.slice(0, brand.length), cell.slice(brand.length).trim());
    } else {
      out.push(cell);
    }
  }
  return out;
}

function engineOf(cells, start, end) {
  for (let i = start; i < end; i++) {
    for (let n = 1; n <= 3 && i + n <= end; n++) {
      const joined = cells.slice(i, i + n).join(" ").replace(/\s*[-–]\s*/g, "-");
      const m = joined.match(ENGINE);
      if (m) {
        const extra = (m[3] ?? m[4] ?? m[6] ?? "").toUpperCase();
        const parts = [`${m[1]}.${m[2]} L`];
        if (["T", "TB", "TURBO"].includes(extra)) parts.push("Turbo");
        if (["TSI", "TFSI", "THP", "TDI", "V6", "V8"].includes(extra)) parts.push(extra);
        if (m[5]) parts.push(`${m[5]}V`);
        return { index: i, length: n, name: parts.join(" · "), printed: cells.slice(i, i + n).join(" ") };
      }
    }
  }
  return null;
}

const fragment = (text) =>
  /^[/–(-]|[/–-]$/.test(text) || (text.match(/\(/g) ?? []).length !== (text.match(/\)/g) ?? []).length;

const GM_TRIMS = [[/^\d?LS\d?$/, "LS"], [/^\d?LT\d?$/, "LT"], [/^\d?LTZ\d?$/, "LTZ"], [/^PR\d?$/, "Premier"], [/^ELI$/, "Elite"], [/^[TA]$/, ""]];

function cleanTrim(make, model, engine, text) {
  const liters = engine.slice(0, 3).replace(".", "");
  let trim = text
    .replace(/\((produzid|fabricad)[^)]*\)/gi, " ")
    .replace(/\(?\s*(\d{2}\s?MY|MY\s?\d{2,4}(\.\d)?|mod\.?\s?\d{2,4}|ano modelo \d{4})\s*\)?/gi, " ")
    .replace(/\(?MY ?\d{2}(\/\d{2})?\)?/gi, " ")
    .replace(/\b\d{2}\/\d{2}\b/g, " ")
    .replace(/\s*-?\s*\b\d{1,2}V\b/gi, " ")
    .replace(/^\s*\d{2,3}\s*T?\s*(AT|MT)\b\s*/i, "")
    .replace(/^\s*\d[.,]?\d\s*(MT|AT|L|T|TB)?\b\s*/i, "")
    .replace(/^\s*\d[.,]?\d\s*[MA]\b\s*/i, "")
    .replace(/^(T\s+)?[AM]\s+(?=\S)/, "")
    .replace(new RegExp("^" + model.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "(?![\\w])", "i"), "")
    .replace(new RegExp("\\b" + liters + "\\b", "g"), " ")
    .replace(/^[\s-]+|[\s-]+$/g, "")
    .replace(/\s+/g, " ")
    .trim();
  if (make === "Chevrolet") {
    trim = trim.split(" ").map((word) => (GM_TRIMS.find(([pattern]) => pattern.test(word.toUpperCase())) ?? [null, word])[1]).filter(Boolean).join(" ");
  }
  if ((trim.match(/\(/g) ?? []).length !== (trim.match(/\)/g) ?? []).length) trim = trim.replace(/[()]/g, "").trim();
  return trim.replace(/\s+/g, " ");
}

function trims(r, model) {
  const parts = / ?\/ ?/.test(r.version) ? r.version.split(/ ?\/ ?/) : [r.version];
  const cleaned = parts.map((p) => display(cleanTrim(r.make, model, r.engine, p))).filter((p, i, all) => all.indexOf(p) === i);
  return cleaned.length ? cleaned : [""];
}

function modelOf(make, printed) {
  let model = printed.replace(/\s+/g, " ").trim().replace(PREFIX, "");
  const moved = [];
  for (let i = 0; i < 6; i++) {
    const drop = model.match(DROP_SUFFIX);
    if (drop && drop.index > 0) {
      model = model.slice(0, drop.index).trim();
      continue;
    }
    const m = model.match(VARIANT_SUFFIX);
    if (!m || m.index === 0) break;
    moved.unshift(m[1].replace(/^\(|\)$/g, ""));
    model = model.slice(0, m.index).trim();
  }
  const k = key(model);
  const alias = MAKE_ALIASES[make + "|" + k] ?? MODEL_ALIASES[k];
  return { model: alias ?? display(model), variant: moved.map(display).join(" ") };
}

function kml(year, fuel, cells) {
  const values = cells.filter((c) => /^\d{1,2}[.,]\d$/.test(c)).map((c) => Number(c.replace(",", ".")));
  let ec = null, eh = null, gc = null, gh = null;
  if (fuel === "Flex" && values.length === 4) {
    if (year === 2012) [ec, gc, eh, gh] = values;
    else [ec, eh, gc, gh] = values;
  } else if (fuel === "Flex" && values.length === 6) {
    [ec, eh, , gc, gh] = values;
  } else if ((fuel === "Gasolina" || fuel === "Diesel") && (values.length === 2 || values.length === 3)) {
    [gc, gh] = values;
  } else if (fuel === "Etanol" && (values.length === 2 || values.length === 3)) {
    [ec, eh] = values;
  } else {
    return null;
  }
  const all = [ec, eh, gc, gh].filter((v) => v !== null);
  if (all.some((v) => v < 3 || v > 35)) return null;
  if (fuel === "Flex" && !(ec < gc && eh < gh)) return null;
  return { ethanolCity: ec, ethanolHighway: eh, otherCity: gc, otherHighway: gh };
}

function parse(raw, year, page) {
  const cells = cellsOf(raw);
  const b = cells.findIndex((cell) => BRANDS[cell.toUpperCase()]);
  if (b < 0) return { reason: "marca fora da lista" };
  let t = cells.findIndex((cell, i) => i >= b + 2 && GEARED.test(cell));
  if (t < 0) t = cells.findIndex((cell, i) => i >= b + 2 && TRANSMISSION.test(cell));
  if (t < 0) return { reason: "sem transmissão" };
  const after = cells.slice(t + 1);
  if (!AC.has(after[0]) || !STEERING.has(after[1])) return { reason: "colunas após a transmissão fora do padrão" };
  let f = 2;
  if (PROPULSION.test(after[f] ?? "")) f++;
  after[f] = (after[f] ?? "").toUpperCase();
  if (!FUEL[after[f]]) return { reason: "combustível fora do padrão" };
  const middle = cells.slice(b + 1, t);
  const hybrid = middle.some((c) => /^h[íi]brido/i.test(c));
  if (middle.some((c) => /^el[ée]trico$/i.test(c))) return { reason: "elétrico sem motor a combustão" };
  const filtered = middle.filter((c) => !PROPULSION.test(c) && !DISPLACEMENT.test(c) && !TRANSMISSION.test(c));
  if (filtered.length < 2) return { reason: "modelo e motor não separáveis" };
  const engine = engineOf(filtered, 1, filtered.length);
  if (!engine) return { reason: "motor sem cilindrada" };
  const modelPrinted = filtered[0];
  let version = filtered.filter((_, i) => i > 0 && (i < engine.index || i >= engine.index + engine.length)).join(" ").trim();
  const valves = version.match(/\s*-\s*(\d{1,2})\s*V$/i);
  if (valves) {
    version = version.slice(0, valves.index).trim();
    if (!/\dV$/.test(engine.name)) engine.name += ` · ${valves[1]}V`;
  }
  if (fragment(version) || fragment(modelPrinted)) return { reason: "versão fragmentada em várias linhas do PDF" };
  const { model, variant } = modelOf(BRANDS[cells[b].toUpperCase()], modelPrinted);
  if (GARBAGE_MODELS.has(key(model)) || /[&/]$|^\W|^\d[.,]\d/.test(model) || model.length < 2) return { reason: "modelo partido em várias linhas do PDF" };
  const [, kind, gears] = cells[t].match(TRANSMISSION);
  const fuel = FUEL[after[f]];
  return {
    row: {
      year, page, make: BRANDS[cells[b].toUpperCase()], makePrinted: cells[b], model, modelPrinted, variant, version,
      engine: engine.name, enginePrinted: engine.printed + (hybrid ? " híbrido" : ""), hybrid,
      transmission: gears && !(kind === "A" && gears === "1") ? `${TRANSMISSION_NAME[kind]} · ${gears} marchas` : TRANSMISSION_NAME[kind],
      transmissionPrinted: cells[t], fuel, efficiency: kml(year, fuel, after.slice(f + 1)),
    },
  };
}

const current = JSON.parse(readFileSync(currentPath, "utf8"));
const curatedSection = (c) => (c.section ?? c.attributes?.MODEL?.section ?? "");
const curated = current.configurations.filter((c) => /versão [^;]+;/.test(curatedSection(c))).map((c) => {
  if (c.values) return c;
  const values = {};
  for (const [k, a] of Object.entries(c.attributes)) values[k] = a.value;
  return { id: c.id, market: c.market, source_id: c.attributes.MODEL.source_id, section: c.attributes.MODEL.section, values };
});
const curatedKeys = new Map(curated.map((c) => [`${c.values.MAKE}|${c.values.MODEL}|${(curatedSection(c).match(/versão ([^;]+);/) ?? [])[1]}`.toUpperCase(), c.id]));

const accepted = [];
const rejected = [];
const pages = {};
for (let year = FIRST_YEAR; year <= LAST_YEAR; year++) {
  const text = readFileSync(join(dir, `t${year}.txt`), "utf8");
  const sheets = text.split("\f");
  pages[year] = sheets.length - (sheets[sheets.length - 1].trim() ? 0 : 1);
  sheets.forEach((sheet, index) => {
    for (const raw of sheet.split(/\r?\n/)) {
      if (!/\b(M|A|MTA|AMT|DCT|CVT)\s?-\s?\d|\bCVT\b|[­]/.test(raw) || !BRAND_KEYS.some((b) => raw.toUpperCase().includes(b))) continue;
      const result = parse(raw, year, index + 1);
      if (result.row) accepted.push(result.row);
      else rejected.push({ year, page: index + 1, reason: result.reason, raw: raw.trim().replace(/\s{2,}/g, " | ").slice(0, 170) });
    }
  });
}

const spelling = new Map();
for (const r of accepted) {
  const k = `${r.make}|${key(r.model)}`;
  const counts = spelling.get(k) ?? new Map();
  counts.set(r.model, (counts.get(r.model) ?? 0) + (/[a-z]/.test(r.model) ? 2 : 1));
  spelling.set(k, counts);
}
const canonical = (r) => [...spelling.get(`${r.make}|${key(r.model)}`).entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))[0][0];

const trimSpelling = new Map();
for (const r of accepted) {
  for (const t of trims(r, canonical(r))) {
    const k = r.make + "|" + canonical(r) + "|" + key(t);
    const counts = trimSpelling.get(k) ?? new Map();
    counts.set(t, (counts.get(t) ?? 0) + 1 + (t === t.toUpperCase() ? 0.5 : 0));
    trimSpelling.set(k, counts);
  }
}
const canonicalTrim = (r, model, t) => {
  const counts = trimSpelling.get(r.make + "|" + model + "|" + key(t));
  return counts ? [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))[0][0] : t;
};

const configurations = [...curated];
const ids = new Set(configurations.map((c) => c.id));
const efficiency = {};
const sourceId = (year) => `inmetro-pbev-${year}`;
let skippedCurated = 0;
let skippedDuplicate = 0;
for (const r of accepted) {
  const model = canonical(r);
  if (r.year === 2019) {
    const curatedId = curatedKeys.get(`${r.make}|${r.model}|${r.version}`.toUpperCase());
    if (curatedId) {
      if (r.efficiency && !efficiency[curatedId]) efficiency[curatedId] = { ...r.efficiency, fuel: r.fuel, page: r.page, year: r.year };
      skippedCurated++;
      continue;
    }
  }
  for (const base of trims(r, model)) {
    const trim = [r.variant, canonicalTrim(r, model, base)].filter(Boolean).join(" ");
    const yy = String(r.year).slice(2);
    const id = `br-${slug(r.make)}-${slug(model)}-my${yy}-${slug(trim) || "unica"}-${slug(r.engine)}-${slug(r.transmissionPrinted)}-${slug(r.fuel)}`.slice(0, 120);
    if (ids.has(id)) {
      skippedDuplicate++;
      continue;
    }
    ids.add(id);
    if (r.efficiency) efficiency[id] = { ...r.efficiency, fuel: r.fuel, page: r.page, year: r.year };
    const values = { MAKE: r.make, MODEL: model, MODEL_YEAR: String(r.year), MARKET: "BR" };
    if (trim) values.TRIM = trim;
    Object.assign(values, { ENGINE: r.engine, TRANSMISSION: r.transmission, FUEL_TYPE: r.fuel });
    configurations.push({
      id, market: "BR", source_id: sourceId(r.year),
      section: `PDF p. ${r.page}; ${r.makePrinted} ${r.modelPrinted} ${r.version} ${r.enginePrinted} ${r.transmissionPrinted}`.replace(/\s+/g, " ").slice(0, 160),
      values,
    });
  }
}

const template = current.sources.find((s) => s.id === "inmetro-pbev-2019");
const sources = [];
for (let year = FIRST_YEAR; year <= LAST_YEAR; year++) {
  const count = configurations.filter((c) => c.source_id === sourceId(year)).length;
  sources.push({
    ...template,
    id: sourceId(year),
    title: `PBE Veicular — Veículos leves ${year}`,
    document_version: year === 2019 ? template.document_version : `${year}; recorte factual MY${String(year).slice(2)}`,
    url: urlOf(year),
    section: `PDF, páginas 1 a ${pages[year]}; ${count} linhas de motores a combustão extraídas em modo tabela e validadas coluna a coluna`,
    accessed_on: TODAY,
    reviewed_by: year === 2019
      ? template.reviewed_by
      : "Extração da tabela (pdftotext -table) validada coluna a coluna, com rejeição de linhas ambíguas ou partidas",
    reviewed_on: TODAY,
  });
}

const makes = new Set(configurations.map((c) => c.values.MAKE));
const models = new Set(configurations.map((c) => `${c.values.MAKE}|${c.values.MODEL}`));
const catalog = {
  schema_version: 2,
  catalog_version: `br-pbev-${FIRST_YEAR}-${LAST_YEAR}-1`,
  sequence: current.sequence + 1,
  coverage_note: `Base PBE Veicular ${FIRST_YEAR} a ${LAST_YEAR} (Inmetro): ${configurations.length} versões de ${makes.size} marcas com motor a combustão. Outros modelos podem ser cadastrados como perfil simples.`,
  markets: current.markets,
  sources,
  configurations,
};
const out = JSON.stringify(catalog);
writeFileSync(outPath, out);
writeFileSync(outPath.replace(/\.json$/, "") + "-efficiency.json", JSON.stringify({
  schema_version: 1,
  source_id: "inmetro-pbev",
  title: `PBE Veicular — Veículos leves ${FIRST_YEAR} a ${LAST_YEAR} · Quilometragem por litro`,
  publisher: "Inmetro",
  url: BASE,
  note: "Valores de laboratório do ciclo de etiquetagem (cidade e estrada), não medição do seu carro. Colunas Gasolina valem para gasolina ou diesel, conforme o combustível da versão.",
  entries: efficiency,
}));
writeFileSync(reportPath, JSON.stringify({ accepted: accepted.length, rejected, skippedCurated, skippedDuplicate }, null, 1));
const reasons = {};
rejected.forEach((r) => (reasons[r.reason] = (reasons[r.reason] ?? 0) + 1));
const perYear = {};
configurations.forEach((c) => (perYear[c.values.MODEL_YEAR] = (perYear[c.values.MODEL_YEAR] ?? 0) + 1));
console.log(`aceitas ${accepted.length} · rejeitadas ${rejected.length} ${JSON.stringify(reasons)}`);
console.log(`por ano ${JSON.stringify(perYear)}`);
console.log(`consumo oficial em ${Object.keys(efficiency).length} versões`);
console.log(`curadas ${curated.length} · equivalentes puladas ${skippedCurated} · duplicadas ${skippedDuplicate}`);
console.log(`configurações ${configurations.length} · modelos ${models.size} · marcas ${makes.size} · ${(out.length / 1024).toFixed(0)} KB`);
