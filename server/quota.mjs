import fs from "node:fs";
import path from "node:path";

export function localDay(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export class DailyQuota {
  constructor({ limit, file, now = () => new Date() }) {
    this.limit = limit;
    this.file = file;
    this.now = now;
    this.day = localDay(now());
    this.used = 0;
    this.load();
  }

  load() {
    if (!this.file || !fs.existsSync(this.file)) {
      return;
    }
    try {
      const saved = JSON.parse(fs.readFileSync(this.file, "utf8"));
      if (saved.day === this.day && Number.isInteger(saved.used) && saved.used >= 0) {
        this.used = saved.used;
      }
    } catch {
      console.warn(`Uso diário em ${this.file} ilegível; recomeçando do zero.`);
    }
  }

  save() {
    if (!this.file) {
      return;
    }
    const temp = path.join(path.dirname(this.file), `.${path.basename(this.file)}.${process.pid}.tmp`);
    fs.writeFileSync(temp, JSON.stringify({ day: this.day, used: this.used }));
    fs.renameSync(temp, this.file);
  }

  rollOver() {
    const today = localDay(this.now());
    if (today !== this.day) {
      this.day = today;
      this.used = 0;
    }
  }

  reserve() {
    this.rollOver();
    if (this.used >= this.limit) {
      return false;
    }
    this.used++;
    this.save();
    return true;
  }

  release() {
    this.rollOver();
    if (this.used > 0) {
      this.used--;
      this.save();
    }
  }

  remaining() {
    this.rollOver();
    return Math.max(0, this.limit - this.used);
  }
}
