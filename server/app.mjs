import http from "node:http";
import crypto from "node:crypto";

export const REQUEST_SCHEMA = "borbacar-ai-request-1";

export function failure(status, message) {
  return Object.assign(new Error(message), { status });
}

function send(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

function authorized(req, token) {
  const given = Buffer.from((req.headers.authorization ?? "").replace(/^Bearer /, ""));
  const expected = Buffer.from(token);
  return given.length === expected.length && crypto.timingSafeEqual(given, expected);
}

async function readJson(req, maxBody) {
  let size = 0;
  const chunks = [];
  for await (const chunk of req) {
    size += chunk.length;
    if (size > maxBody) {
      throw failure(413, "Caso grande demais");
    }
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    throw failure(400, "JSON inválido");
  }
}

export function acceptCase(body) {
  if (body?.schema !== REQUEST_SCHEMA || !Array.isArray(body.observations) || body.observations.length > 60) {
    throw failure(400, "Caso fora do contrato");
  }
  const refs = new Set(body.observations.map((item) => item?.ref).filter((ref) => typeof ref === "string"));
  if (refs.size !== body.observations.length || [...refs].some(ref => !/^[DMT]\d{1,3}$/.test(ref))) {
    throw failure(400, "Referências de evidência inválidas ou duplicadas");
  }
  const question = body.question ?? "";
  const conversation = body.conversation ?? [];
  if (typeof question !== "string" || question.length > 500 || !Array.isArray(conversation) || conversation.length > 6
      || conversation.some(turn => !turn || typeof turn.question !== "string" || turn.question.length > 500
        || typeof turn.answer !== "string" || turn.answer.length > 2000)) {
    throw failure(400, "Conversa fora do contrato");
  }
  const kase = {
    vehicle: body.vehicle ?? {},
    read_complete: body.read_complete === true,
    demonstration_data: body.demonstration_data === true,
    observations: body.observations,
    symptoms: body.symptoms ?? {},
    language: body.language === "en" ? "en" : "pt-BR",
    question,
    conversation,
  };
  return { kase, refs };
}

export function checkAnalysis(text, refs) {
  let analysis;
  try {
    analysis = typeof text === "string" ? JSON.parse(text) : text;
  } catch {
    throw failure(502, "Resposta da IA fora do contrato");
  }
  const short = (value, max) => typeof value === "string" && value.length <= max;
  const ok =
    analysis !== null && typeof analysis === "object" &&
    short(analysis.summary, 1200) && analysis.summary.trim() !== "" &&
    Array.isArray(analysis.hypotheses) && analysis.hypotheses.length <= 5 &&
    analysis.hypotheses.every(
      (h) =>
        short(h?.title, 160) && short(h.reasoning, 1200) && short(h.next_check, 600) &&
        ["baixa", "media", "alta"].includes(h.confidence) &&
        Array.isArray(h.evidence) && h.evidence.length > 0 && h.evidence.every((ref) => refs.has(ref)),
    ) &&
    Array.isArray(analysis.missing_information) && analysis.missing_information.length <= 8 &&
    analysis.missing_information.every((item) => short(item, 300)) &&
    short(analysis.safety_note, 600);
  if (!ok) {
    throw failure(502, "Resposta da IA fora do contrato");
  }
  return analysis;
}

function withTimeout(promise, ms, attempt) {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => {
      attempt.timedOut = true;
      reject(failure(504, "A IA demorou demais para responder"));
    }, ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

export function createApp({ token, provider, model, quota, analyze, maxBody = 16 * 1024, timeoutMs = 60_000, log = console }) {
  return http.createServer(async (req, res) => {
    const attempt = { reserved: false, answered: false, timedOut: false };
    try {
      if (req.method === "GET" && req.url === "/health") {
        return send(res, 200, { ok: true, provider, model, remaining_today: quota.remaining() });
      }
      if (req.method === "GET" && req.url === "/v1/status") {
        if (!authorized(req, token)) return send(res, 401, { error: "Token inválido" });
        return send(res, 200, { ok: true, provider, model, remaining_today: quota.remaining(), conversation: true });
      }
      if (req.method !== "POST" || req.url !== "/v1/analyze") {
        return send(res, 404, { error: "Rota inexistente" });
      }
      if (!authorized(req, token)) {
        return send(res, 401, { error: "Token inválido" });
      }
      const { kase, refs } = acceptCase(await readJson(req, maxBody));
      if (!quota.reserve()) {
        return send(res, 429, { error: "Limite diário de demonstração atingido" });
      }
      attempt.reserved = true;
      const answer = await withTimeout(analyze(kase, attempt), timeoutMs, attempt);
      const analysis = checkAnalysis(answer.text, refs);
      return send(res, 200, { model: answer.model, analysis });
    } catch (error) {
      if (attempt.reserved && !attempt.answered && !attempt.timedOut) {
        quota.release();
      }
      if (error.status) {
        return send(res, error.status, { error: error.message });
      }
      log.error(error);
      return send(res, 500, { error: "Erro interno" });
    }
  });
}
