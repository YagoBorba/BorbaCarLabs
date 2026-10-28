import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const [dir, pageCount, currentPath, outPath, reportPath] = process.argv.slice(2);
const SOURCE = "inmetro-pbev-2019";

const CATEGORIES = new Set(["Sub Compacto", "Compacto", "Médio", "Grande", "Extra Grande", "Esportivo", "Fora de Estrada", "Minivan", "Comercial", "Picape", "Utilitário Esportivo"]);
const BRANDS = {
  "AUDI": "Audi", "BMW": "BMW", "CAOA CHERY": "CAOA Chery", "CHEVROLET": "Chevrolet", "CHRYSLER": "Chrysler",
  "CITROËN": "Citroën", "DODGE": "Dodge", "FERRARI": "Ferrari", "FIAT": "Fiat", "FORD": "Ford", "HONDA": "Honda",
  "HYUNDAI": "Hyundai", "JAC": "JAC", "JAGUAR": "Jaguar", "JEEP": "Jeep", "KIA": "Kia", "LAMBORGHINI": "Lamborghini",
  "LAND ROVER": "Land Rover", "LEXUS": "Lexus", "LIFAN": "Lifan", "MASERATI": "Maserati", "MCLAREN": "McLaren",
  "MINI": "MINI", "MITSUBISHI": "Mitsubishi", "NISSAN": "Nissan", "PEUGEOT": "Peugeot", "PORSCHE": "Porsche",
  "RENAULT": "Renault", "SSANGYONG": "SsangYong", "SUBARU": "Subaru", "SUZUKI": "Suzuki", "TOYOTA": "Toyota",
  "TROLLER": "Troller", "VOLVO": "Volvo", "VW": "Volkswagen",
};
const TRANSMISSION = /^(M|A|MTA|DCT|CVT)-?(\d{1,2})?$/;
const TRANSMISSION_NAME = { M: "Manual", A: "Automático", MTA: "Automatizado", DCT: "Dupla embreagem", CVT: "CVT" };
const AC = new Set(["S", "N"]);
const STEERING = new Set(["H", "E", "M", "E-H"]);
const FUEL = { F: "Flex", G: "Gasolina", E: "Etanol", D: "Diesel" };

const titleWord = (word) => (/^[A-ZÀ-Ý]{4,}$/.test(word) ? word[0] + word.slice(1).toLowerCase() : word);
const display = (text) => text.split(" ").map(titleWord).join(" ");
const slug = (text) => text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

function engineName(printed) {
  const m = printed.match(/^(\d\.\d)\s*(.*)$/);
  if (!m) return null;
  const rest = m[2].replace(/^L\b\s*/i, "").replace(/^[-–]\s*/, "").trim();
  const parts = [`${m[1]} L`];
  const valves = rest.match(/(?:^|\s|-)(\d{1,2})\s*V\b/i);
  const extra = rest.replace(/(?:^|\s|-)\d{1,2}\s*V\b/i, " ").replace(/[\s/–-]+/g, " ").trim();
  if (extra) parts.push(extra);
  if (valves) parts.push(`${valves[1]}V`);
  return parts.join(" · ");
}

const fragment = (text) =>
  /^[/–(-]|[/–-]$/.test(text) || (text.match(/\(/g) ?? []).length !== (text.match(/\)/g) ?? []).length;

function parse(raw, page) {
  let cells = raw.trim().split(/\s{2,}/);
  if ((cells[0] === "Sub" && cells[1] === "Compacto") || (cells[0] === "Extra" && cells[1] === "Grande")) {
    cells = [cells[0] + " " + cells[1], ...cells.slice(2)];
  }
  if (!CATEGORIES.has(cells[0])) return { reason: "categoria desconhecida" };
  if (cells[1]?.toUpperCase() === "LAND" && /^ROVER\b/i.test(cells[2] ?? "")) {
    const rest = cells[2].replace(/^ROVER\s*/i, "");
    cells = [cells[0], "LAND ROVER", ...(rest ? [rest] : []), ...cells.slice(3)];
  }
  const t = cells.findIndex((cell, i) => i >= 3 && TRANSMISSION.test(cell));
  if (t < 0) return { reason: "sem transmissão" };
  if (!AC.has(cells[t + 1]) || !STEERING.has(cells[t + 2]) || !FUEL[cells[t + 3]]) return { reason: "colunas após a transmissão fora do padrão" };
  const left = cells.slice(1, t);
  if (left.length >= 3 && /^\d{1,2}V$/i.test(left[left.length - 1])) {
    let n = left[left.length - 2] === "-" ? 3 : 2;
    const head = left[left.length - n].match(/^(.*?)\s*(\d\.\d\s*L?)\s*[-–]?$/);
    if (head) {
      left.splice(left.length - n, n, ...(head[1] ? [head[1]] : []), `${head[2]}-${left[left.length - 1]}`);
    }
  }
  if (left.length < 3) return { reason: "modelo e versão não separáveis" };
  const [makePrinted, modelPrinted] = left;
  const enginePrinted = left[left.length - 1];
  const version = left.slice(2, left.length - 1).join(" ").trim();
  if (fragment(version) || fragment(modelPrinted)) return { reason: "versão fragmentada em várias linhas do PDF" };
  const make = BRANDS[makePrinted.toUpperCase()];
  if (!make) return { reason: "marca fora da lista" };
  const engine = engineName(enginePrinted);
  if (!engine) return { reason: "motor sem cilindrada" };
  const efficiency = consumption(cells.slice(t + 4));
  const [, kind, gears] = cells[t].match(TRANSMISSION);
  const model = display(modelPrinted.replace(/\s*\(MY19\)\s*$/i, "").trim());
  return {
    row: {
      page, make, makePrinted, model, modelPrinted, version, engine, enginePrinted,
      transmission: gears ? `${TRANSMISSION_NAME[kind]} · ${gears} marchas` : TRANSMISSION_NAME[kind],
      transmissionPrinted: cells[t], fuel: FUEL[cells[t + 3]], efficiency,
    },
  };
}

function consumption(after) {
  const [nmhc, co, nox, reduction, co2e, co2g, ec, eh, gc, gh] = after;
  const pollutant = /^\d,\d{3}$/;
  const kml = (cell) => (/^\d{1,2},\d$/.test(cell ?? "") ? Number(cell.replace(",", ".")) : /^[\\-]$/.test(cell ?? "") ? null : undefined);
  if (![nmhc, co, nox].every((cell) => pollutant.test(cell ?? "")) || !/^[A-E]$/.test(reduction ?? "")) return null;
  if (![co2e, co2g].every((cell) => /^(\d{1,3}|[\\-])$/.test(cell ?? ""))) return null;
  const values = [ec, eh, gc, gh].map(kml);
  if (values.includes(undefined) || values.every((v) => v === null)) return null;
  const [ethanolCity, ethanolHighway, otherCity, otherHighway] = values;
  return { ethanolCity, ethanolHighway, otherCity, otherHighway };
}

const current = JSON.parse(readFileSync(currentPath, "utf8"));
const curatedVersions = new Set(current.configurations.map((c) => {
  const section = c.attributes.MODEL.section;
  return `${c.attributes.MAKE.value}|${c.attributes.MODEL.value}|${(section.match(/versão ([^;]+);/) ?? [])[1]}`.toUpperCase();
}));

const accepted = [];
const rejected = [];
for (let page = 1; page <= Number(pageCount); page++) {
  for (const raw of readFileSync(join(dir, `p${page}.txt`), "utf8").split(/\r?\n/)) {
    if (!/\b\d,\d{3}\b/.test(raw)) continue;
    const result = parse(raw, page);
    if (result.row) accepted.push(result.row);
    else rejected.push({ page, reason: result.reason, raw: raw.trim().replace(/\s{2,}/g, " | ").slice(0, 170) });
  }
}

const configurations = [...current.configurations];
const ids = new Set(configurations.map((c) => c.id));
const curatedIds = new Map(current.configurations.map((c) => [
  `${c.attributes.MAKE.value}|${c.attributes.MODEL.value}|${(c.attributes.MODEL.section.match(/versão ([^;]+);/) ?? [])[1]}`.toUpperCase(), c.id,
]));
const efficiency = {};
const record = (id, r) => {
  if (r.efficiency && !efficiency[id]) {
    efficiency[id] = { ...r.efficiency, fuel: r.fuel, page: r.page };
  }
};
let skippedCurated = 0;
let skippedDuplicate = 0;
for (const r of accepted) {
  const curatedKey = `${r.make}|${r.model}|${r.version}`.toUpperCase();
  if (curatedVersions.has(curatedKey)) {
    record(curatedIds.get(curatedKey), r);
    skippedCurated++;
    continue;
  }
  const id = `br-${slug(r.make)}-${slug(r.model)}-my19-${slug(r.version)}-${slug(r.enginePrinted)}-${slug(r.transmissionPrinted)}-${slug(r.fuel)}`.slice(0, 120);
  if (ids.has(id)) {
    skippedDuplicate++;
    continue;
  }
  ids.add(id);
  record(id, r);
  const section =`PDF p. ${r.page}; ${r.makePrinted} ${r.modelPrinted} ${r.version}`.slice(0, 160);
  const attr = (value) => ({ value, source_id: SOURCE, section });
  configurations.push({
    id,
    market: "BR",
    attributes: {
      MAKE: attr(r.make), MODEL: attr(r.model), MODEL_YEAR: attr("2019"), MARKET: attr("BR"),
      ...(r.version ? { TRIM: attr(r.version) } : {}), ENGINE: attr(r.engine), TRANSMISSION: attr(r.transmission), FUEL_TYPE: attr(r.fuel),
    },
  });
}

const makes = new Set(configurations.map((c) => c.attributes.MAKE.value));
const source = current.sources.find((s) => s.id === SOURCE);
source.section = "PDF, páginas 1 a 46; linhas de motores a combustão extraídas em modo tabela e validadas coluna a coluna";
source.accessed_on = "2026-10-27";
source.reviewed_by = "Conferência textual e visual das 16 linhas Chevrolet; extração da tabela (pdftotext -table) com rejeição de linhas ambíguas";
source.reviewed_on = "2026-10-27";
const catalog = {
  ...current,
  configurations,
  catalog_version: "br-pbev-2019-2",
  sequence: current.sequence + 1,
  coverage_note: `Base PBE Veicular 2019 (Inmetro): ${configurations.length} versões de ${makes.size} marcas com motor a combustão. Outros modelos podem ser cadastrados como perfil simples.`,
};
const out = JSON.stringify(catalog);
writeFileSync(outPath, out);
writeFileSync(outPath.replace(/\.json$/, "") + "-efficiency.json", JSON.stringify({
  schema_version: 1,
  source_id: SOURCE,
  title: "PBE Veicular — Veículos leves 2019 · Quilometragem por litro",
  publisher: "Inmetro",
  url: source.url,
  note: "Valores de laboratório do ciclo de etiquetagem (cidade e estrada), não medição do seu carro. Colunas Gasolina valem para gasolina ou diesel, conforme o combustível da versão.",
  entries: efficiency,
}));
console.log(`consumo oficial em ${Object.keys(efficiency).length} versões`);
writeFileSync(reportPath, JSON.stringify({ accepted: accepted.length, rejected, skippedCurated, skippedDuplicate }, null, 1));
const reasons = {};
rejected.forEach((r) => (reasons[r.reason] = (reasons[r.reason] ?? 0) + 1));
console.log(`aceitas ${accepted.length} · rejeitadas ${rejected.length} ${JSON.stringify(reasons)}`);
console.log(`curadas mantidas ${current.configurations.length} · equivalentes puladas ${skippedCurated} · duplicadas ${skippedDuplicate}`);
console.log(`configurações ${configurations.length} · marcas ${makes.size} · ${(out.length / 1024).toFixed(0)} KB`);
