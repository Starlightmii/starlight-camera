/**
 * Starlight Camera — animated install guide generator.
 *
 * Emits self-contained, animated SVGs for the README. No external fonts,
 * scripts or images, so GitHub's camo proxy serves them as-is and CSS/SMIL
 * animation runs in the browser.
 *
 *   node scripts/gen-install-guide.mjs
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'assets');
const FONT = 'Inter,Segoe UI,Helvetica,Arial,sans-serif';

/** Shared motion rules. Every animation is disabled under reduced-motion. */
const motion = `
      .trail { stroke-dasharray:7 9; animation: trail 1.5s linear infinite; }
      @keyframes trail { to { stroke-dashoffset:-32 } }
      .step  { animation: breathe 3s ease-in-out infinite; }
      @keyframes breathe { 0%,100% { opacity:.6 } 50% { opacity:1 } }
      .s2 { animation-delay:.35s } .s3 { animation-delay:.7s }
      .s4 { animation-delay:1.05s } .s5 { animation-delay:1.4s }
      .pop  { animation: pop 2.6s ease-in-out infinite; }
      @keyframes pop { 0%,100% { transform:scale(1) } 50% { transform:scale(1.06) } }
      .p2 { animation-delay:.4s } .p3 { animation-delay:.8s } .p4 { animation-delay:1.2s }
      .sweep { animation: sweep 4.6s ease-in-out infinite; }
      @keyframes sweep { 0% { transform:translateX(0); opacity:0 }
                         12% { opacity:1 } 88% { opacity:1 } 100% { transform:translateX(940px); opacity:0 } }
      .tick { stroke-dasharray:48; stroke-dashoffset:48; animation: tick 3s ease-in-out infinite; }
      @keyframes tick { 0% { stroke-dashoffset:48 } 45%,75% { stroke-dashoffset:0 } 100% { stroke-dashoffset:48 } }
      .k2 { animation-delay:.5s } .k3 { animation-delay:1s } .k4 { animation-delay:1.5s }
      @media (prefers-reduced-motion: reduce) { * { animation: none !important } }`;

const esc = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/** Numbered step card with an animated accent bar and optional tick. */
function stepCard({ x, y, w, h, n, title, lines, accent, delay = '' }) {
  return `
    <g class="step ${delay}">
      <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="15" fill="#130d22" stroke="#332a52"/>
      <rect x="${x}" y="${y}" width="${w}" height="4" rx="2" fill="${accent}" opacity=".85"/>
      <circle cx="${x + 30}" cy="${y + 40}" r="16" fill="${accent}" opacity=".18"/>
      <text x="${x + 30}" y="${y + 46}" text-anchor="middle" font-family="${FONT}"
            font-size="16" font-weight="800" fill="${accent}">${esc(n)}</text>
      <text x="${x + 58}" y="${y + 46}" font-family="${FONT}" font-size="16"
            font-weight="700" fill="#f8fafc">${esc(title)}</text>
      ${lines
        .map(
          (l, i) =>
            `<text x="${x + 20}" y="${y + 76 + i * 21}" font-family="${FONT}" font-size="13" fill="#94a3b8">${esc(l)}</text>`,
        )
        .join('')}
      <path class="tick ${delay.replace('s', 'k')}" d="M${x + w - 44} ${y + h - 26} l7 8 l14 -18"
            fill="none" stroke="${accent}" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"/>
    </g>`;
}

/** One numbered bar inside the settings mock. */
function settingRow({ x, y, w, label, value, fill, accent, delay = '' }) {
  return `
    <g class="step ${delay}">
      <text x="${x}" y="${y}" font-family="${FONT}" font-size="13.5" font-weight="600" fill="#e2e8f0">${esc(label)}</text>
      <text x="${x + w}" y="${y}" text-anchor="end" font-family="${FONT}" font-size="13"
            font-weight="700" fill="${accent}">${esc(value)}</text>
      <rect x="${x}" y="${y + 10}" width="${w}" height="8" rx="4" fill="#241b3d"/>
      <rect x="${x}" y="${y + 10}" width="${fill}" height="8" rx="4" fill="${accent}">
        <animate attributeName="width" values="0;${fill}" dur="2.8s" begin="0.2s" fill="freeze"/>
      </rect>
    </g>`;
}

/* ------------------------------------------------------------------ */
/* 1. Install flow                                                     */
/* ------------------------------------------------------------------ */
function installGuide() {
  const steps = [
    {
      n: '1',
      title: 'Download the APK',
      lines: ['From Releases, grab', 'Starlight-Camera-vMax.51.apk', '(132.4 MB, arm64-v8a)'],
      accent: '#a78bfa',
    },
    {
      n: '2',
      title: 'Install it',
      lines: ['Tap the APK, or run', 'adb install Starlight-Camera-vMax.51.apk', 'No root, no Magisk'],
      accent: '#22d3ee',
    },
    {
      n: '3',
      title: 'Import the config',
      lines: ['Double-tap black near shutter', '→ IMPORT → Starlight_Master.xml', 'App restarts, tuned'],
      accent: '#34d399',
    },
    {
      n: '4',
      title: 'Add the DSLR lib',
      lines: ['Optional: push the .so to', '/sdcard/StarlightCamera/libs/', 'Pick it in Settings → Libs'],
      accent: '#f59e0b',
    },
    {
      n: '5',
      title: 'Verify checksum',
      lines: ['sha256sum the APK and', 'compare with CHECKSUMS.sha256', 'Then shoot something'],
      accent: '#ec4899',
    },
  ];

  const cardW = 196;
  const gap = 20;
  const width = 40 + steps.length * cardW + (steps.length - 1) * gap + 40;
  const y = 118;
  const h = 196;

  const cards = steps
    .map((s, i) => {
      const x = 40 + i * (cardW + gap);
      const delay = `s${i + 1}`;
      const parts = [stepCard({ x, y, w: cardW, h, ...s, delay })];
      if (i < steps.length - 1) {
        const ax = x + cardW + 3;
        parts.push(
          `<path class="trail" d="M${ax} ${y + h / 2} H${ax + gap - 8}" stroke="#7c8aa5"
                 stroke-width="2.2" fill="none" marker-end="url(#ar)"/>`,
        );
      }
      return parts.join('');
    })
    .join('');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="380" viewBox="0 0 ${width} 380"
     role="img" aria-label="Five step install guide for Starlight Camera">
  <defs>
    <marker id="ar" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto">
      <path d="M0,0 L0,6 L7,3 z" fill="#7c8aa5"/>
    </marker>
    <linearGradient id="sh" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#a78bfa" stop-opacity="0"/>
      <stop offset=".5" stop-color="#f0abfc" stop-opacity=".9"/>
      <stop offset="1" stop-color="#a78bfa" stop-opacity="0"/>
    </linearGradient>
    <clipPath id="rail"><rect x="40" y="96" width="${width - 80}" height="4" rx="2"/></clipPath>
    <style><![CDATA[${motion}]]></style>
  </defs>

  <rect width="${width}" height="380" rx="22" fill="#0a0713"/>
  <rect x="1" y="1" width="${width - 2}" height="378" rx="21" fill="none" stroke="#241a3d"/>

  <text x="40" y="46" font-family="${FONT}" font-size="24" font-weight="800" fill="#f8fafc">Install in five steps</text>
  <text x="40" y="72" font-family="${FONT}" font-size="14" fill="#8b93a7">About two minutes. No root, no account, no PC required.</text>

  <rect x="40" y="96" width="${width - 80}" height="4" rx="2" fill="#241b3d"/>
  <g clip-path="url(#rail)"><rect class="sweep" x="40" y="94" width="120" height="8" rx="4" fill="url(#sh)"/></g>

  ${cards}

  <g font-family="${FONT}" font-size="13" fill="#8b93a7">
    <text x="40" y="348">⚠️ Package name is unchanged, so this replaces an existing LMC install rather than sitting beside it.</text>
  </g>
</svg>`;
}

/* ------------------------------------------------------------------ */
/* 2. Configuration guide (settings mock + slider behaviour)           */
/* ------------------------------------------------------------------ */
function configGuide() {
  const width = 1080;
  const rows = [
    { label: 'Photo quality', value: 'Max', fill: 210, accent: '#a78bfa', delay: 's1' },
    { label: 'Upscale scale', value: '2×', fill: 150, accent: '#22d3ee', delay: 's2' },
    { label: 'Smart naming', value: 'On', fill: 260, accent: '#34d399', delay: 's3' },
    { label: 'Video upscale', value: 'On', fill: 260, accent: '#f0abfc', delay: 's4' },
    { label: 'Potato mode', value: 'Off', fill: 70, accent: '#f59e0b', delay: 's5' },
  ];

  const settings = rows
    .map((r, i) => settingRow({ x: 56, y: 168 + i * 62, w: 380, ...r }))
    .join('');

  const sliders = [
    { label: 'Denoise', fill: 130, accent: '#60a5fa', tip: 'Low by day, higher at night' },
    { label: 'Detail', fill: 168, accent: '#34d399', tip: 'Medium is a safe default' },
    { label: 'Sharpen', fill: 100, accent: '#f472b6', tip: 'Low for faces, high for text' },
  ]
    .map(
      (s, i) => `
    <g class="step s${i + 1}">
      <text x="536" y="${182 + i * 76}" font-family="${FONT}" font-size="14" font-weight="700" fill="#f8fafc">${esc(s.label)}</text>
      <rect x="536" y="${194 + i * 76}" width="300" height="10" rx="5" fill="#241b3d"/>
      <rect x="536" y="${194 + i * 76}" width="${s.fill}" height="10" rx="5" fill="${s.accent}">
        <animate attributeName="width" values="0;${s.fill}" dur="2.6s" begin="0.3s" fill="freeze"/>
      </rect>
      <circle class="pop p${i + 1}" cx="${536 + s.fill}" cy="${199 + i * 76}" r="9" fill="#0a0713" stroke="${s.accent}" stroke-width="3"/>
      <text x="536" y="${232 + i * 76}" font-family="${FONT}" font-size="12" fill="#8b93a7">${esc(s.tip)}</text>
    </g>`,
    )
    .join('');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="512" viewBox="0 0 ${width} 512"
     role="img" aria-label="Configuration guide: settings to change and what the three sliders do">
  <defs>
    <linearGradient id="sh2" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#34d399" stop-opacity="0"/>
      <stop offset=".5" stop-color="#a7f3d0" stop-opacity=".9"/>
      <stop offset="1" stop-color="#34d399" stop-opacity="0"/>
    </linearGradient>
    <style><![CDATA[${motion}]]></style>
  </defs>

  <rect width="${width}" height="512" rx="22" fill="#0a0713"/>
  <rect x="1" y="1" width="${width - 2}" height="510" rx="21" fill="none" stroke="#241a3d"/>

  <text x="40" y="46" font-family="${FONT}" font-size="24" font-weight="800" fill="#f8fafc">Configure it properly</text>
  <text x="40" y="72" font-family="${FONT}" font-size="14" fill="#8b93a7">Five settings worth touching, and what each slider actually changes.</text>

  <rect x="40" y="104" width="436" height="356" rx="16" fill="#120c1f" stroke="#2a1f45"/>
  <text x="56" y="136" font-family="${FONT}" font-size="15" font-weight="800" fill="#a78bfa">Settings to change</text>
  ${settings}

  <rect x="512" y="104" width="528" height="356" rx="16" fill="#0d1a16" stroke="#2f6b56"/>
  <text x="536" y="136" font-family="${FONT}" font-size="15" font-weight="800" fill="#34d399">The three sliders</text>
  <text x="536" y="158" font-family="${FONT}" font-size="12" fill="#8b93a7">Off is a genuine bypass on all three.</text>
  ${sliders}

  <g font-family="${FONT}" font-size="13" fill="#8b93a7">
    <text x="40" y="492">💡 40 MP and 4K are heavy jobs on a 732G — use Potato mode when you want the shot fast, Max when you want the print.</text>
  </g>
</svg>`;
}

mkdirSync(OUT, { recursive: true });
for (const [name, svg] of [
  ['install-guide.svg', installGuide()],
  ['config-guide.svg', configGuide()],
]) {
  writeFileSync(join(OUT, name), svg, 'utf8');
  console.log(`wrote assets/${name} (${Buffer.byteLength(svg)} bytes)`);
}