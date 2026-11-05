import fs from "node:fs";

// Environment variables deliberately take precedence over the local wizard file.
const file = new URL("./.env", import.meta.url);
if (fs.existsSync(file)) {
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const match = /^([A-Z_][A-Z0-9_]*)=(.*)$/.exec(line);
    if (match && process.env[match[1]] === undefined) {
      process.env[match[1]] = match[2].startsWith('"') ? JSON.parse(match[2]) : match[2];
    }
  }
}
