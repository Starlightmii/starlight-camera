/**
 * Real geometry measurement via headless Chromium.
 * Estimates lie; getBBox() does not.
 */
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, '..', 'out');
const PAGE = pathToFileURL(join(HERE, '..', 'measure.html')).href;

const svgs = {};
for (const f of readdirSync(OUT).filter((n) => n.endsWith('.svg')).sort())
  svgs[f] = readFileSync(join(OUT, f), 'utf8');

const { chromium } = await import('playwright');
const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto(PAGE);
const results = await page.evaluate((s) => window.__measure(s), svgs);
await browser.close();

let bad = 0;
for (const r of results) {
  const flag = r.problems.length ? '✗' : '✓';
  if (r.problems.length) bad++;
  console.log(`${flag} ${r.name.padEnd(20)} ${r.W}×${r.H}  texts=${String(r.texts).padStart(3)}  minFont=${r.minFont}`);
  for (const p of r.problems.slice(0, 14)) console.log(`     ${p}`);
  if (r.deadBands.length) console.log(`     DEAD bands: ${r.deadBands.join(', ')}  coverage=${r.coverage.join(' ')}`);
}
console.log(bad ? `\n${bad} file(s) with problems` : '\nall measured clean');
process.exit(bad ? 1 : 0);
