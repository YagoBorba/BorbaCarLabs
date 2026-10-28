import http from "node:http";
import crypto from "node:crypto";
import Anthropic from "@anthropic-ai/sdk";
import { GoogleGenAI } from "@google/genai";

const PORT = Number(process.env.PORT ?? 8787);
const TOKEN = process.env.BORBACAR_TOKEN ?? "";
const PROVIDER = (process.env.BORBACAR_PROVIDER ?? "claude").toLowerCase();
const MODEL = process.env.BORBACAR_MODEL ?? (PROVIDER === "gemini" ? "gemini-3.5-flash-lite" : "claude-opus-5-5");
const DAILY_LIMIT = Number(process.env.BORBACAR_DAILY_LIMIT ?? 20);
const MAX_BODY = 16 * 1024;
const REQUEST_SCHEMA = "borbacar-ai-request-1";

if (TOKEN.length < 16) {
  console.error("Defina BORBACAR_TOKEN com pelo menos 16 caracteres (veja server/README.md).");
  process.exit(1);
}
if (PROVIDER !== "claude" && PROVIDER !== "gemini") {
  console.error('BORBACAR_PROVIDER deve ser "claude" ou "gemini".');
  process.exit(1);
}

const client = PROVIDER === "claude" ? new Anthropic() : null;
const gemini = PROVIDER === "gemini" && process.env.GEMINI_API_KEY ? new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY }) : null;
if (PROVIDER === "claude" && !process.env.ANTHROPIC_API_KEY && !process.env.ANTHROPIC_AUTH_TOKEN) {
  console.warn("Aviso: ANTHROPIC_API_KEY não definida; as análises só funcionam se houver um perfil `ant auth login`.");
}
if (PROVIDER === "gemini" && !gemini) {
  console.warn("Aviso: GEMINI_API_KEY não definida; as análises vão falhar até ela ser configurada.");
}

const SYSTEM = `Você analisa casos de diagnóstico OBD2 para demonstrações do app BorbaCar Labs.
O usuário envia um JSON com o tipo do veículo, observações (cada uma com um campo "ref"), leituras e sintomas declarados pelo motorista.
- Responda em português do Brasil, em linguagem simples para um motorista leigo.
- Cada hipótese cita em "evidence" somente refs que existem no JSON recebido.
- Use seu conhecimento geral sobre códigos OBD2 e sistemas automotivos para interpretar, apresentando possibilidades, não um diagnóstico confirmado. Não afirme que uma peça está com defeito.
- "confidence" é "baixa", "media" ou "alta".
- No máximo 5 hipóteses, da mais para a menos provável. Se as evidências forem insuficientes, diga isso no resumo e não force hipóteses.
- "missing_information": até 8 itens que ajudariam a investigar.
- "safety_note": só quando houver risco para continuar dirigindo (por exemplo, luz de injeção piscando ou superaquecimento); caso contrário, string vazia.
- Seja breve: resumo com até 3 frases e raciocínio com até 3 frases por hipótese.
- Se "demonstration_data" for true, os dados são simulados: comece o resumo com "Dados simulados:".`;

const ANALYSIS_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: ["summary", "hypotheses", "missing_information", "safety_note"],
  properties: {
    summary: { type: "string" },
    hypotheses: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["title", "reasoning", "evidence", "confidence", "next_check"],
        properties: {
          title: { type: "string" },
          reasoning: { type: "string" },
          evidence: { type: "array", items: { type: "string" } },
          confidence: { type: "string", enum: ["baixa", "media", "alta"] },
          next_check: { type: "string" },
        },
      },
    },
    missing_information: { type: "array", items: { type: "string" } },
    safety_note: { type: "string" },
  },
};

const SUPPORTS_FALLBACK = /^claude-(opus-5|fable-5|sonnet-5-5)/.test(MODEL);

let usageDay = new Date().toISOString().slice(0, 10);
let usedToday = 0;

function send(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

function authorized(req) {
  const given = Buffer.from((req.headers.authorization ?? "").replace(/^Bearer /, ""));
  const expected = Buffer.from(TOKEN);
  return given.length === expected.length && crypto.timingSafeEqual(given, expected);
}

async function readJson(req) {
  let size = 0;
  const chunks = [];
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY) {
      throw Object.assign(new Error("Caso grande demais"), { status: 413 });
    }
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

function acceptCase(body) {
  if (body?.schema !== REQUEST_SCHEMA || !Array.isArray(body.observations) || body.observations.length > 60) {
    throw Object.assign(new Error("Caso fora do contrato"), { status: 400 });
  }
  const refs = new Set(body.observations.map((item) => item?.ref).filter((ref) => typeof ref === "string"));
  const kase = {
    vehicle: body.vehicle ?? {},
    read_complete: body.read_complete === true,
    demonstration_data: body.demonstration_data === true,
    observations: body.observations,
    symptoms: body.symptoms ?? {},
  };
  return { kase, refs };
}

function checkAnalysis(analysis, refs) {
  const short = (value, max) => typeof value === "string" && value.length <= max;
  const ok =
    short(analysis.summary, 1200) && analysis.summary.trim() !== "" &&
    Array.isArray(analysis.hypotheses) && analysis.hypotheses.length <= 5 &&
    analysis.hypotheses.every(
      (h) =>
        short(h.title, 160) && short(h.reasoning, 1200) && short(h.next_check, 600) &&
        ["baixa", "media", "alta"].includes(h.confidence) &&
        Array.isArray(h.evidence) && h.evidence.length > 0 && h.evidence.every((ref) => refs.has(ref)),
    ) &&
    Array.isArray(analysis.missing_information) && analysis.missing_information.length <= 8 &&
    analysis.missing_information.every((item) => short(item, 300)) &&
    short(analysis.safety_note, 600);
  if (!ok) {
    throw Object.assign(new Error("Resposta da IA fora do contrato"), { status: 502 });
  }
}

async function analyze(kase, refs) {
  return PROVIDER === "gemini" ? analyzeWithGemini(kase, refs) : analyzeWithClaude(kase, refs);
}

function withoutClosedObjects(schema) {
  if (Array.isArray(schema)) {
    return schema.map(withoutClosedObjects);
  }
  if (schema && typeof schema === "object") {
    return Object.fromEntries(
      Object.entries(schema).filter(([key]) => key !== "additionalProperties").map(([key, value]) => [key, withoutClosedObjects(value)]),
    );
  }
  return schema;
}

async function analyzeWithGemini(kase, refs) {
  if (!gemini) {
    throw Object.assign(new Error("Servidor de IA sem chave do Gemini configurada"), { status: 500 });
  }
  let interaction;
  try {
    interaction = await gemini.interactions.create({
      model: MODEL,
      system_instruction: SYSTEM,
      input: JSON.stringify(kase),
      response_format: { type: "text", mime_type: "application/json", schema: withoutClosedObjects(ANALYSIS_SCHEMA) },
    });
  } catch (error) {
    console.error("Erro do Gemini:", error?.status ?? "", error?.message ?? error);
    const status = error?.status === 429 ? 503 : 502;
    throw Object.assign(new Error(status === 503 ? "Limite gratuito do Gemini atingido; tente mais tarde" : "Falha na API do Gemini"), { status });
  }
  usedToday++;
  const analysis = JSON.parse(interaction.output_text ?? "");
  checkAnalysis(analysis, refs);
  console.log(`análise ok · ${MODEL} (Gemini)`);
  return { model: MODEL, analysis };
}

async function analyzeWithClaude(kase, refs) {
  const params = {
    model: MODEL,
    max_tokens: 4000,
    output_config: { effort: "low", format: { type: "json_schema", schema: ANALYSIS_SCHEMA } },
    system: SYSTEM,
    messages: [{ role: "user", content: JSON.stringify(kase) }],
  };
  const response = SUPPORTS_FALLBACK
    ? await client.beta.messages.create({ ...params, betas: ["server-side-fallback-2026-07-01"], fallbacks: "default" })
    : await client.messages.create(params);
  usedToday++;

  if (response.stop_reason === "refusal") {
    throw Object.assign(new Error("A IA não aceitou analisar este caso"), { status: 422 });
  }
  if (response.stop_reason === "max_tokens") {
    throw Object.assign(new Error("Resposta da IA incompleta"), { status: 502 });
  }
  const text = response.content.filter((block) => block.type === "text").map((block) => block.text).join("");
  const analysis = JSON.parse(text);
  checkAnalysis(analysis, refs);
  console.log(`análise ok · ${response.model} · entrada ${response.usage.input_tokens} · saída ${response.usage.output_tokens} tokens`);
  return { model: response.model, analysis };
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "GET" && req.url === "/health") {
      return send(res, 200, { ok: true, provider: PROVIDER, model: MODEL, remaining_today: Math.max(0, DAILY_LIMIT - usedToday) });
    }
    if (req.method !== "POST" || req.url !== "/v1/analyze") {
      return send(res, 404, { error: "Rota inexistente" });
    }
    if (!authorized(req)) {
      return send(res, 401, { error: "Token inválido" });
    }
    const today = new Date().toISOString().slice(0, 10);
    if (today !== usageDay) {
      usageDay = today;
      usedToday = 0;
    }
    if (usedToday >= DAILY_LIMIT) {
      return send(res, 429, { error: "Limite diário de demonstração atingido" });
    }
    const { kase, refs } = acceptCase(await readJson(req));
    return send(res, 200, await analyze(kase, refs));
  } catch (error) {
    if (error instanceof Anthropic.RateLimitError) {
      return send(res, 503, { error: "API da Anthropic ocupada; tente em instantes" });
    }
    if (error instanceof Anthropic.AuthenticationError) {
      console.error("Chave da Anthropic inválida ou ausente no servidor.");
      return send(res, 500, { error: "Servidor de IA sem credencial válida" });
    }
    if (error instanceof Anthropic.APIConnectionError) {
      return send(res, 502, { error: "Sem conexão com a API da Anthropic" });
    }
    if (error instanceof Anthropic.APIError) {
      console.error("Erro da API:", error.status, error.message);
      return send(res, 502, { error: "Falha na API da Anthropic" });
    }
    if (/Could not resolve authentication/i.test(error?.message ?? "")) {
      console.error("Sem credencial da Anthropic: defina ANTHROPIC_API_KEY ou faça `ant auth login`.");
      return send(res, 500, { error: "Servidor de IA sem chave da Anthropic configurada" });
    }
    if (error instanceof SyntaxError) {
      return send(res, 400, { error: "JSON inválido" });
    }
    if (error.status) {
      return send(res, error.status, { error: error.message });
    }
    console.error(error);
    return send(res, 500, { error: "Erro interno" });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`BorbaCar AI em http://0.0.0.0:${PORT} · ${PROVIDER} · modelo ${MODEL} · limite ${DAILY_LIMIT}/dia`);
});
