import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { test } from "node:test";
import { DailyQuota, localDay } from "./quota.mjs";

function tempFile() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "borbacar-quota-"));
  return path.join(dir, "usage.json");
}

test("never reserves more than the daily limit", async () => {
  const quota = new DailyQuota({ limit: 3, file: tempFile() });
  const results = await Promise.all(Array.from({ length: 10 }, async () => quota.reserve()));
  assert.equal(results.filter(Boolean).length, 3);
  assert.equal(quota.remaining(), 0);
});

test("released reservations can be used again", () => {
  const quota = new DailyQuota({ limit: 1, file: tempFile() });
  assert.equal(quota.reserve(), true);
  assert.equal(quota.reserve(), false);
  quota.release();
  assert.equal(quota.reserve(), true);
});

test("usage survives a restart on the same day", () => {
  const file = tempFile();
  const first = new DailyQuota({ limit: 5, file });
  first.reserve();
  first.reserve();
  const second = new DailyQuota({ limit: 5, file });
  assert.equal(second.remaining(), 3);
});

test("usage resets on the next day", () => {
  const file = tempFile();
  let now = new Date(2026, 9, 29, 23, 50);
  const quota = new DailyQuota({ limit: 2, file, now: () => now });
  quota.reserve();
  quota.reserve();
  assert.equal(quota.reserve(), false);
  now = new Date(2026, 9, 30, 0, 5);
  assert.equal(quota.reserve(), true);
  assert.deepEqual(JSON.parse(fs.readFileSync(file, "utf8")), { day: "2026-10-30", used: 1 });
  const restarted = new DailyQuota({ limit: 2, file, now: () => now });
  assert.equal(restarted.remaining(), 1);
});

test("an unreadable usage file starts from zero", () => {
  const file = tempFile();
  fs.writeFileSync(file, "{not json");
  const quota = new DailyQuota({ limit: 2, file });
  assert.equal(quota.remaining(), 2);
});

test("local day uses the local calendar date", () => {
  assert.equal(localDay(new Date(2026, 0, 5, 23, 59)), "2026-01-05");
});
