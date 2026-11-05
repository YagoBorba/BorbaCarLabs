import fs from "node:fs";
import crypto from "node:crypto";
import { createInterface } from "node:readline/promises";
import { stdin, stdout } from "node:process";
import { Writable } from "node:stream";

const file = new URL("./.env", import.meta.url);
let muted = false;
const output = new Writable({ write(chunk, encoding, callback) { if (!muted) stdout.write(chunk, encoding); callback(); } });
output.isTTY = stdout.isTTY;
const prompt = createInterface({ input: stdin, output, terminal: Boolean(stdin.isTTY && stdout.isTTY) });
try {
  console.log("BorbaCar Labs — configuração da IA. A chave fica somente neste computador.");
  if (fs.existsSync(file) && (await prompt.question("Já existe configuração. Substituir? [s/N] ")).toLowerCase() !== "s") {
    process.exitCode = 0;
  } else {
    const choice = (await prompt.question("Provedor: 1 Gemini / 2 Claude [1]: ")).trim() || "1";
    if (!["1", "2"].includes(choice)) throw new Error("Escolha 1 ou 2.");
    const provider = choice === "1" ? "gemini" : "claude";
    const existing = provider === "gemini" ? process.env.GEMINI_API_KEY : process.env.ANTHROPIC_API_KEY;
    console.log("Cole uma chave de API do seu provedor; uma assinatura do chat não substitui a API." + (existing ? " Enter usa a chave já presente no ambiente." : ""));
    // Suppress terminal echo while the secret is entered.
    muted = true;
    stdout.write("Chave de API: ");
    let key;
    try { key = (await prompt.question("")).trim() || existing; }
    finally { muted = false; stdout.write("\n"); }
    if (!key || /[\r\n]/.test(key)) throw new Error("Informe uma chave válida.");
    const model = (await prompt.question("Modelo (Enter usa o padrão do servidor): ")).trim();
    const values = {
      BORBACAR_PROVIDER: provider,
      [provider === "gemini" ? "GEMINI_API_KEY" : "ANTHROPIC_API_KEY"]: key,
      BORBACAR_TOKEN: crypto.randomBytes(24).toString("hex"),
      BORBACAR_DAILY_LIMIT: "20",
      ...(model ? { BORBACAR_MODEL: model } : {}),
    };
    fs.writeFileSync(file, Object.entries(values).map(([name, value]) => `${name}=${JSON.stringify(value)}`).join("\n") + "\n", { mode: 0o600 });
    console.log("Configuração salva em .env, excluído do Git. Rode npm start e copie o convite para o app. Cada envio pode consumir a cota ou créditos do provedor.");
  }
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
} finally {
  prompt.close();
}
