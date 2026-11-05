import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { spawn } from "node:child_process";

test("wizard generates a private invitation and does not echo the API key", { timeout: 10000 }, async () => {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), "borbacar-setup-"));
  fs.copyFileSync(new URL("./setup.mjs", import.meta.url), path.join(folder, "setup.mjs"));
  const env = { ...process.env }; delete env.GEMINI_API_KEY; delete env.ANTHROPIC_API_KEY;
  const child = spawn(process.execPath, ["setup.mjs"], { cwd: folder, env, windowsHide: true });
  let output = "", stage = 0;
  child.stdout.on("data", chunk => {
    output += chunk.toString();
    if (stage === 0 && output.includes("Provedor:")) { stage++; child.stdin.write("1\n"); }
    if (stage === 1 && output.includes("Chave de API:")) { stage++; child.stdin.write("dummy-key-not-a-credential\n"); }
    if (stage === 2 && output.includes("Modelo (")) { stage++; child.stdin.write("\n"); }
  });
  try {
    assert.equal(await new Promise((resolve, reject) => { child.once("error", reject); child.once("close", resolve); }), 0);
    assert.equal(stage, 3);
    const content = fs.readFileSync(path.join(folder, ".env"), "utf8");
    assert.match(content, /BORBACAR_TOKEN="[a-f0-9]{48}"/);
    assert.match(content, /GEMINI_API_KEY="dummy-key-not-a-credential"/);
    assert.equal(output.includes("dummy-key-not-a-credential"), false);
  } finally {
    child.kill();
    if (path.dirname(path.resolve(folder)) === path.resolve(os.tmpdir()) && path.basename(folder).startsWith("borbacar-setup-")) {
      fs.rmSync(folder, { recursive: true, force: true });
    }
  }
});
