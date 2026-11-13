/**
 * Layout audit for the generated artwork.
 *
 * Renders each SVG (to prove it rasterises) and checks:
 *   - valid XML
 *   - every text node inside the canvas
 *   - no two text nodes overlapping in the same band
 *   - no unresolved __LABEL__ placeholder
 *
 *   node scripts/audit.mjs
 */

import { execFileSync } from 'node:child_process';
import { readFileSync, readdirSync, mkdtempSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, '..', 'out');
const tmp = mkdtempSync(join(tmpdir(), 'sc-audit-'));

let failures = 0;

for (const name of readdirSync(OUT).filter((f) => f.endsWith('.svg')).sort()) {
  const file = join(OUT, name);
  const svg = readFileSync(file, 'utf8');
  const problems = [];

  const m = svg.match(/width="(\d+)"\s+height="(\d+)"/);
  if (!m) {
    problems.push('no width/height');
  }
  const W = Number(m?.[1] ?? 0);
  const H = Number(m?.[2] ?? 0);

  if (svg.includes('__LABEL__')) problems.push('placeholder left in output');

  // XML well-formedness
  try {
    execFileSync('python3', ['-c', `import xml.dom.minidom,sys;xml.dom.minidom.parse(sys.argv[1])`, file]);
  } catch {
    problems.push('not well-formed XML');
  }

  // Text bounds
  const texts = [...svg.matchAll(/<text ([^>]*)>([^<]*)<\/text>/g)].map((t) => {
    const x = Number(/x="(-?[\d.]+)"/.exec(t[1])?.[1] ?? NaN);
    const y = Number(/y="(-?[\d.]+)"/.exec(t[1])?.[1] ?? NaN);
    const size = Number(/font-size="([\d.]+)"/.exec(t[1])?.[1] ?? 13);
    const anchor = /text-anchor="(\w+)"/.exec(t[1])?.[1] ?? 'start';
    const body = t[2].trim();
    // rough advance width: bold caps a little wider
    const weight = Number(/font-weight="(\d+)"/.exec(t[1])?.[1] ?? 400);
    const factor = weight >= 700 ? 0.58 : 0.545;
    const w = body.length * size * factor;
    const left = anchor === 'middle' ? x - w / 2 : anchor === 'end' ? x - w : x;
    return { left, right: left + w, y, size, body };
  });

  for (const t of texts) {
    if (t.left < -2 || t.right > W + 2) {
      problems.push(`text out of canvas: "${t.body.slice(0, 34)}" [${Math.round(t.left)}..${Math.round(t.right)}] W=${W}`);
    }
    if (t.y > H - 2 || t.y < 0) {
      problems.push(`text outside height: "${t.body.slice(0, 34)}" y=${t.y} H=${H}`);
    }
  }

  // Overlap: same vertical band and horizontal ranges intersecting
  for (let i = 0; i < texts.length; i++) {
    for (let j = i + 1; j < texts.length; j++) {
      const a = texts[i];
      const b = texts[j];
      if (Math.abs(a.y - b.y) > 10) continue;
      const overlap = Math.min(a.right, b.right) - Math.max(a.left, b.left);
      if (overlap > 6 && a.body && b.body) {
        problems.push(
          `overlap: "${a.body.slice(0, 22)}" ∩ "${b.body.slice(0, 22)}" (${Math.round(overlap)}px)`,
        );
      }
    }
  }

  // Rasterise to prove the renderer accepts it
  try {
    execFileSync('rsvg-convert', ['-w', '600', file, '-o', join(tmp, name + '.png')]);
  } catch (e) {
    problems.push('rsvg-convert failed');
  }

  // Shape bounds: rects and circles must stay on canvas.
  for (const r of svg.matchAll(/<rect ([^>]*)\/?>/g)) {
    const a = r[1];
    const x = Number(/\bx="(-?[\d.]+)"/.exec(a)?.[1] ?? 0);
    const y = Number(/\by="(-?[\d.]+)"/.exec(a)?.[1] ?? 0);
    const w = Number(/\bwidth="([\d.]+)"/.exec(a)?.[1] ?? 0);
    const h = Number(/\bheight="([\d.]+)"/.exec(a)?.[1] ?? 0);
    if (x + w > W + 1 || y + h > H + 1 || x < -1 || y < -1) {
      problems.push(`rect overflows canvas: x=${x} y=${y} w=${w} h=${h}`);
    }
  }
  for (const c of svg.matchAll(/<circle ([^>]*)>/g)) {
    const a = c[1];
    const cx = Number(/\bcx="(-?[\d.]+)"/.exec(a)?.[1] ?? 0);
    const cy = Number(/\bcy="(-?[\d.]+)"/.exec(a)?.[1] ?? 0);
    const r = Number(/\br="([\d.]+)"/.exec(a)?.[1] ?? 0);
    // decorative halos are allowed to bleed, they are blurred glows
    // A circle at the group origin is drawn inside a translate(); its
    // absolute position is not knowable from the attributes alone.
    const local = cx === 0 && cy === 0;
    const blurred = /blur|halo|filter/.test(a);
    if (!blurred && !local && (cx - r < -2 || cy - r < -2 || cx + r > W + 2 || cy + r > H + 2)) {
      problems.push(`circle overflows canvas: cx=${cx} cy=${cy} r=${r}`);
    }
  }

  // Vertical crowding: text baselines too close in the same column.
  const sorted = [...texts].sort((a, b) => a.y - b.y || a.left - b.left);
  for (let i = 0; i < sorted.length; i++) {
    for (let j = i + 1; j < sorted.length; j++) {
      const a = sorted[i];
      const b = sorted[j];
      if (b.y - a.y > 26) break;
      const hGap = Math.min(a.right, b.right) - Math.max(a.left, b.left);
      if (hGap <= 0) continue;
      const vGap = b.y - a.y;
      if (vGap > 0 && vGap < Math.max(a.size, b.size) * 0.95) {
        problems.push(
          `crowded: "${a.body.slice(0, 20)}" / "${b.body.slice(0, 20)}" only ${vGap.toFixed(0)}px apart`,
        );
      }
    }
  }

  const unique = [...new Set(problems)];
  if (unique.length) {
    failures++;
    console.log(`✗ ${name}  (${W}×${H})`);
    for (const p of unique.slice(0, 8)) console.log(`    ${p}`);
  } else {
    console.log(`✓ ${name}  (${W}×${H}, ${texts.length} text nodes)`);
  }
}

console.log(failures ? `\n${failures} file(s) with issues` : '\nall artwork clean');
process.exit(failures ? 1 : 0);
