import assert from "node:assert/strict";
import { after, test } from "node:test";
import { createApp, failure, REQUEST_SCHEMA } from "./app.mjs";
import { DailyQuota } from "./quota.mjs";

const TOKEN = "test-token-0123456789";
const servers = [];

const CASE = {
  schema: REQUEST_SCHEMA,
  vehicle: { make: "Chevrolet", model: "Cobalt" },
  read_complete: true,
  demonstration_data: true,
  observations: [
    { ref: "D1", type: "dtc", code: "P0300" },
    { ref: "T1", type: "reading", sensor: "RPM", value: 812 },
  ],
  symptoms: { description: "Falha em marcha lenta" },
};

const ANSWER = {
  summary: "Dados simulados: falha de ignição em mais de um cilindro.",
  hypotheses: [
    { title: "Velas ou cabos", reasoning: "P0300 com marcha lenta irregular.", evidence: ["D1", "T1"], confidence: "media", next_check: "Inspecionar velas." },
  ],
  missing_information: ["Quilometragem da última troca de velas"],
  safety_note: "",
};

async function start(analyze, { limit = 5, timeoutMs = 1000 } = {}) {
  const quota = new DailyQuota({ limit, file: null });
  const server = createApp({ token: TOKEN, provider: "fake", model: "fake-1", quota, analyze, timeoutMs, log: { error() {} } });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  servers.push(server);
  const base = `http://127.0.0.1:${server.address().port}`;
  const post = (body, token = TOKEN) =>
    fetch(`${base}/v1/analyze`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: typeof body === "string" ? body : JSON.stringify(body),
    }).then(async (res) => ({ status: res.status, body: await res.json() }));
  const health = () => fetch(`${base}/health`).then((res) => res.json());
  return { post, health, quota };
}

function answering(answer) {
  return async (kase, attempt) => {
    attempt.answered = true;
    return { model: "fake-1", text: typeof answer === "string" ? answer : JSON.stringify(answer) };
  };
}

after(() => servers.forEach((server) => server.close()));

test("valid answer is returned and counted", async () => {
  let received;
  const app = await start(async (kase, attempt) => {
    received = kase;
    return answering(ANSWER)(kase, attempt);
  });
  const res = await app.post(CASE);
  assert.equal(res.status, 200);
  assert.equal(res.body.model, "fake-1");
  assert.deepEqual(res.body.analysis, ANSWER);
  assert.equal(received.symptoms.description, "Falha em marcha lenta");
  assert.equal((await app.health()).remaining_today, 4);
});

test("wrong token is rejected without using quota", async () => {
  const app = await start(answering(ANSWER));
  const res = await app.post(CASE, "wrong-token-0123456789");
  assert.equal(res.status, 401);
  assert.equal(app.quota.remaining(), 5);
});

test("invalid bodies are rejected before reaching the provider", async () => {
  let calls = 0;
  const app = await start(async (kase, attempt) => {
    calls++;
    return answering(ANSWER)(kase, attempt);
  });
  assert.equal((await app.post("{broken")).status, 400);
  assert.equal((await app.post({ schema: "other", observations: [] })).status, 400);
  assert.equal((await app.post({ ...CASE, observations: Array.from({ length: 61 }, (_, i) => ({ ref: `T${i}` })) })).status, 400);
  assert.equal((await app.post({ ...CASE, symptoms: { description: "x".repeat(20_000) } })).status, 413);
  assert.equal(calls, 0);
  assert.equal(app.quota.remaining(), 5);
});

test("exhausted quota answers 429 without calling the provider", async () => {
  let calls = 0;
  const app = await start(async (kase, attempt) => {
    calls++;
    return answering(ANSWER)(kase, attempt);
  }, { limit: 1 });
  assert.equal((await app.post(CASE)).status, 200);
  const res = await app.post(CASE);
  assert.equal(res.status, 429);
  assert.equal(calls, 1);
});

test("provider error before answering returns its quota", async () => {
  const app = await start(async () => {
    throw failure(503, "Provedor ocupado");
  });
  const res = await app.post(CASE);
  assert.equal(res.status, 503);
  assert.equal(res.body.error, "Provedor ocupado");
  assert.equal(app.quota.remaining(), 5);
});

test("unexpected provider exception becomes a generic 500", async () => {
  const app = await start(async () => {
    throw new TypeError("boom");
  });
  const res = await app.post(CASE);
  assert.equal(res.status, 500);
  assert.equal(res.body.error, "Erro interno");
});

test("provider timeout answers 504 and keeps the unit counted", async () => {
  const app = await start(() => new Promise(() => {}), { timeoutMs: 50 });
  const res = await app.post(CASE);
  assert.equal(res.status, 504);
  assert.equal(app.quota.remaining(), 4);
});

test("answer that is not JSON is rejected as 502", async () => {
  const app = await start(answering("Desculpe, não consigo."));
  const res = await app.post(CASE);
  assert.equal(res.status, 502);
  assert.equal(res.body.error, "Resposta da IA fora do contrato");
});

test("answer citing evidence that was not sent is rejected", async () => {
  const invented = { ...ANSWER, hypotheses: [{ ...ANSWER.hypotheses[0], evidence: ["D1", "X9"] }] };
  const res = await (await start(answering(invented))).post(CASE);
  assert.equal(res.status, 502);
});

test("answer outside the contract is rejected", async () => {
  const cases = [
    { ...ANSWER, summary: " " },
    { ...ANSWER, hypotheses: [{ ...ANSWER.hypotheses[0], confidence: "certa" }] },
    { ...ANSWER, hypotheses: [{ ...ANSWER.hypotheses[0], evidence: [] }] },
    { ...ANSWER, hypotheses: Array(6).fill(ANSWER.hypotheses[0]) },
    { ...ANSWER, missing_information: Array(9).fill("x") },
    { ...ANSWER, safety_note: null },
    null,
  ];
  for (const answer of cases) {
    const res = await (await start(answering(JSON.stringify(answer)))).post(CASE);
    assert.equal(res.status, 502, JSON.stringify(answer));
  }
});

test("unknown routes answer 404", async () => {
  const app = await start(answering(ANSWER));
  const res = await fetch(`http://127.0.0.1:${servers.at(-1).address().port}/v1/other`);
  assert.equal(res.status, 404);
  assert.equal((await app.health()).ok, true);
});
