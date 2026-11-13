/**
 * Starlight Camera — visual guide generator.
 *
 * Produces the animated SVGs used by the README:
 *   lib-setup.svg     - how to select the .so for every sensor
 *   setup-manual.svg  - whole phone-only install, start to finish
 *
 * All artwork is self-contained (no external fonts, scripts or images) so
 * GitHub's camo proxy serves it unchanged and CSS/SMIL animation runs.
 *
 *   node scripts/gen-guides.mjs
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'assets');
const FONT = 'Inter,Segoe UI,Helvetica,Arial,sans-serif';

const esc = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

/* Every animation is switched off for reduced-motion users. */
const motion = `
      .dash { stroke-dasharray:6 8; animation: dash 1.6s linear infinite; }
      @keyframes dash { to { stroke-dashoffset:-28 } }
      .soft { animation: soft 3.4s ease-in-out infinite; }
      @keyframes soft { 0%,100% { opacity:.6 } 50% { opacity:1 } }
      .s2 { animation-delay:.28s } .s3 { animation-delay:.56s } .s4 { animation-delay:.84s }
      .s5 { animation-delay:1.12s } .s6 { animation-delay:1.4s } .s7 { animation-delay:1.68s }
      .s8 { animation-delay:1.96s }
      .tick { stroke-dasharray:54; stroke-dashoffset:54; animation: tick 3.4s ease-in-out infinite; }
      @keyframes tick { 0% { stroke-dashoffset:54 } 42%,74% { stroke-dashoffset:0 } 100% { stroke-dashoffset:54 } }
      .t2 { animation-delay:.4s } .t3 { animation-delay:.8s } .t4 { animation-delay:1.2s }
      .t5 { animation-delay:1.6s } .t6 { animation-delay:2s }
      .glow { animation: glow 3.8s ease-in-out infinite; }
      @keyframes glow { 0%,100% { opacity:.28 } 50% { opacity:.9 } }
      .sweep { animation: sweep 5s ease-in-out infinite; }
      @keyframes sweep { 0% { transform:translateX(-160px) } 100% { transform:translateX(1260px) } }
      .lift { animation: lift 5s ease-in-out infinite; }
      @keyframes lift { 0%,100% { transform:translateY(0) } 50% { transform:translateY(-5px) } }
      .ring { animation: ring 4.2s ease-in-out infinite; }
      @keyframes ring { 0%,100% { opacity:.35; transform:scale(1) } 50% { opacity:.85; transform:scale(1.05) } }
      @media (prefers-reduced-motion: reduce) { * { animation: none !important } }`;

const head = (w, h, label) => `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}"
     viewBox="0 0 ${w} ${h}" role="img" aria-label="${esc(label)}">`;

const defs = `
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#0b0715"/><stop offset=".55" stop-color="#090613"/>
      <stop offset="1" stop-color="#0b0715"/>
    </linearGradient>
    <linearGradient id="rail" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#a78bfa" stop-opacity="0"/>
      <stop offset=".5" stop-color="#f0abfc" stop-opacity=".85"/>
      <stop offset="1" stop-color="#a78bfa" stop-opacity="0"/>
    </linearGradient>
    <linearGradient id="gv" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#34d399"/><stop offset="1" stop-color="#22d3ee"/>
    </linearGradient>
    <marker id="ar" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto">
      <path d="M0,0 L0,6 L7,3 z" fill="#7c8aa5"/>
    </marker>
    <style><![CDATA[${motion}]]></style>
  </defs>`;

/** Rounded card with a coloured left edge. */
const card = ({ x, y, w, h, fill = '#120c1f', stroke = '#2f2549', accent }) =>
  `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="15" fill="${fill}" stroke="${stroke}"/>` +
  (accent ? `<rect x="${x}" y="${y}" width="4" height="${h}" rx="2" fill="${accent}"/>` : '');

const label = (x, y, text, { size = 15.5, weight = 800, fill = '#f8fafc' } = {}) =>
  `<text x="${x}" y="${y}" font-family="${FONT}" font-size="${size}" font-weight="${weight}" fill="${fill}">${esc(text)}</text>`;

const body = (x, y, text, { size = 13, fill = '#94a3b8' } = {}) =>
  `<text x="${x}" y="${y}" font-family="${FONT}" font-size="${size}" fill="${fill}">${esc(text)}</text>`;

const tickPath = (x, y, accent, cls) =>
  `<path class="tick ${cls}" d="M${x} ${y} l7 8 l15 -19" fill="none" stroke="${accent}"
         stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>`;

/* ==================================================================== */
/* lib-setup.svg — the .so per sensor                                   */
/* ==================================================================== */
function libSetup() {
  const W = 1180;
  const H = 640;

  const lenses = [
    { key: 'Main camera', code: 'libs_key', sub: '108 MP S5KHM2', accent: '#a78bfa' },
    { key: 'Tele camera', code: 'libs_key_2', sub: '2× zoom', accent: '#818cf8' },
    { key: 'Wide angle', code: 'libs_key_3', sub: 'IMX355 ultra-wide', accent: '#22d3ee' },
    { key: 'Front camera', code: 'libs_key_front', sub: 'IMX471 selfie', accent: '#f0abfc' },
  ];

  const y0 = 268;
  const rowH = 78;
  const rows = lenses
    .map((l, i) => {
      const y = y0 + i * rowH;
      return `
    <g class="soft s${i + 2}">
      ${card({ x: 40, y, w: 640, h: 64, accent: l.accent })}
      <circle cx="86" cy="${y + 32}" r="16" fill="${l.accent}" opacity=".18" class="ring"/>
      <circle cx="86" cy="${y + 32}" r="7" fill="${l.accent}" opacity=".9"/>
      ${label(120, y + 28, l.key, { size: 15 })}
      ${body(120, y + 50, l.sub, { size: 12 })}
      <text x="${654}" y="${y + 36}" text-anchor="end" font-family="${FONT}" font-size="12.5"
            font-weight="700" fill="#c4b5fd">${esc(l.code)}</text>
      ${tickPath(40 + 640 - 40, y + 32, l.accent, `t${(i % 4) + 2}`)}
    </g>`;
    })
    .join('');

  const arrows = lenses
    .map(
      (_, i) =>
        `<path class="dash" d="M684 ${y0 + i * rowH + 32} H724" stroke="#7c8aa5"
               stroke-width="2" fill="none" marker-end="url(#ar)"/>`,
    )
    .join('');

  return `${head(W, H, 'How to set the Starlight DSLR library for every camera lens')}
  ${defs}
  <rect width="${W}" height="${H}" rx="24" fill="url(#bg)"/>
  <rect x="1" y="1" width="${W - 2}" height="${H - 2}" rx="23" fill="none" stroke="#241a3d"/>

  <g transform="translate(40 44)">
    <circle class="ring" cx="30" cy="30" r="30" fill="#a78bfa" opacity=".18"/>
    <circle cx="30" cy="30" r="21" fill="none" stroke="#a78bfa" stroke-width="3"/>
    <circle cx="30" cy="30" r="9" fill="#a78bfa" opacity=".85"/>
  </g>
  ${label(126, 62, 'Set the library for every lens', { size: 31 })}
  ${body(126, 90, 'One file, four slots. Takes about a minute.', { size: 15, fill: '#a78bfa' })}
  ${body(126, 116, 'Skip this and the camera still works — it just uses the default processing.', { size: 13.5, fill: '#8b93a7' })}

  <rect x="852" y="46" width="288" height="3" rx="1.5" fill="#241b3d"/>
  <rect class="sweep" x="852" y="45" width="140" height="5" rx="2.5" fill="url(#rail)"/>

  <!-- how to get there -->
  ${label(40, 172, 'Get there', { size: 15, fill: '#f0abfc' })}
  <g>
    ${['Open Starlight Camera', 'Second from top row settings', 'Photo processing', 'Library'].map((t, i) => {
      const x = 40 + i * 286;
      return `
      ${card({ x, y: 190, w: 264, h: 54 })}
      <text x="${x + 18}" y="${215}" font-family="${FONT}" font-size="12.5" font-weight="800"
            fill="#a78bfa">${i + 1}</text>
      ${body(x + 40, 215, t, { size: 13, fill: '#e2e8f0' })}
      ${body(x + 40, 233, ['from the app drawer', 'gear icon, top right', 'tap it', 'tap it'][i], { size: 11.5, fill: '#7c8aa5' })}
      ${i < 3 ? `<path class="dash" d="M${x + 268} 217 H${x + 282}" stroke="#7c8aa5" stroke-width="2" fill="none" marker-end="url(#ar)"/>` : ''}`;
    }).join('')}
  </g>

  ${label(40, 296 - 30, 'Then pick the file in every lens row', { size: 15, fill: '#34d399' })}

  ${rows}
  ${arrows}

  <!-- the single target file -->
  <g class="soft s3">
    ${card({ x: 728, y: y0, w: 412, h: 300, fill: '#0d1a16', stroke: '#2f6b56' })}
    ${label(756, y0 + 38, 'Pick this file', { size: 15, fill: '#34d399' })}
    <rect x="756" y="${y0 + 56}" width="356" height="46" rx="11" fill="#0a1712" stroke="#2f6b56"/>
    <text x="774" y="${y0 + 85}" font-family="${FONT}" font-size="13.5" font-weight="700"
          fill="#a7f3d0">Starlight_EOS_R10_DSLR.so</text>
    ${body(756, y0 + 132, 'It is already in place if you copied it', { size: 12.5 })}
    ${body(756, y0 + 152, 'into /sdcard/LMC8.4/libs/', { size: 12.5 })}
    <rect x="756" y="${y0 + 170}" width="356" height="1" fill="#2f6b56" opacity=".5"/>
    ${body(756, y0 + 198, 'Do the same for every lens you shoot with.', { size: 12.5 })}
    ${body(756, y0 + 218, 'You can leave a lens on Default if you', { size: 12.5 })}
    ${body(756, y0 + 238, 'want the stock processing for it.', { size: 12.5 })}
    ${body(756, y0 + 268, 'Restart the camera after choosing.', { size: 12.5, fill: '#fbbf24' })}
  </g>

  <rect x="40" y="${H - 66}" width="1100" height="42" rx="12" fill="#0f0a1c" stroke="#241a3d"/>
  ${body(62, H - 40, 'Optional. The camera works without it — this simply swaps in the Starlight-tuned processing recipe.', { size: 12.5, fill: '#8b93a7' })}
</svg>`;
}

/* ==================================================================== */
/* setup-manual.svg — whole install, phone only                          */
/* ==================================================================== */
function step({ x, y, w, h, n, title, lines, accent, cls, tick }) {
  return `
    <g class="soft ${cls}">
      ${card({ x, y, w, h, accent })}
      <circle cx="${x + 36}" cy="${y + 34}" r="16" fill="${accent}" opacity=".2"/>
      <text x="${x + 36}" y="${y + 40}" text-anchor="middle" font-family="${FONT}"
            font-size="15.5" font-weight="800" fill="${accent}">${n}</text>
      ${label(x + 62, y + 40, title, { size: 15.5 })}
      ${lines.map((l, i) => body(x + 22, y + 68 + i * 20, l, { size: 12.5 })).join('')}
      ${tickPath(x + w - 42, y + h - 24, accent, tick)}
    </g>`;
}

function setupManual() {
  const W = 1180;
  const steps = [
    {
      n: '1', title: 'Download the three files',
      lines: ['Open the release page in your phone browser.', 'Tap each file — they save to Download.', 'No PC involved.'],
      accent: '#a78bfa',
    },
    {
      n: '2', title: 'Remove the old camera',
      lines: ['Settings → Apps → find LMC / GCam / Starlight.', 'Uninstall it. Same package name, so it cannot', 'sit beside the new one.'],
      accent: '#22d3ee',
    },
    {
      n: '3', title: 'Install the APK',
      lines: ['Files → Download → tap the .apk → Install.', 'Allow installs from your file manager if asked.', 'No root, no ADB, no developer options.'],
      accent: '#818cf8',
    },
    {
      n: '4', title: 'Move the two helper files',
      lines: ['Make folders LMC8.4 and StarlightCamera.', 'Put Starlight_Master.xml in both.', 'Put the .so in LMC8.4/libs as well.'],
      accent: '#34d399',
    },
    {
      n: '5', title: 'Open it, allow permissions',
      lines: ['Launch Starlight Camera from the app drawer.', 'Allow Camera, Photos, Microphone.', 'You should land on the viewfinder.'],
      accent: '#2dd4bf',
    },
    {
      n: '6', title: 'Load the config',
      lines: ['Double-tap the black area beside the shutter.', 'Tap IMPORT → /sdcard/LMC8.4/ → the XML.', 'The app restarts itself, already tuned.'],
      accent: '#f0abfc',
    },
    {
      n: '7', title: 'Pick the library for each lens',
      lines: ['Settings → Photo processing → Library.', 'In Main, Tele, Wide and Front choose', 'Starlight_EOS_R10_DSLR.so, then restart.'],
      accent: '#f59e0b',
    },
    {
      n: '8', title: 'Switch the good stuff on',
      lines: ['Settings → Starlight Camera: Auto upscale ON.', 'Video upscale ON for 4K. Potato mode if slow.', 'Shoot. Enhanced copies save alongside.'],
      accent: '#ec4899',
    },
  ];

  const files = [
    { name: 'Starlight-Camera-vMax.51.apk', meta: '132.4 MB', what: 'the camera', to: 'Download/', accent: '#a78bfa' },
    { name: 'Starlight_Master.xml', meta: '154 KB', what: 'the tuning', to: 'LMC8.4/', accent: '#34d399' },
    { name: 'Starlight_EOS_R10_DSLR.so', meta: '94.5 MB', what: 'optional library', to: 'LMC8.4/libs/', accent: '#f59e0b' },
  ];

  const filesSvg = files
    .map((f, i) => {
      const x = 40 + i * 368;
      return `
    <g class="soft s${i + 1}">
      ${card({ x, y: 188, w: 344, h: 116 })}
      <rect class="glow" x="${x}" y="188" width="344" height="4" rx="2" fill="${f.accent}"/>
      ${body(x + 20, 220, f.meta, { size: 12.5, fill: f.accent })}
      ${label(x + 20, 246, f.name, { size: 13.5 })}
      ${body(x + 20, 276, 'goes in', { size: 12 })}
      <text x="${x + 76}" y="276" font-family="${FONT}" font-size="12" font-weight="700"
            fill="#cbd5e1">/sdcard/${esc(f.to)}</text>
    </g>`;
    })
    .join('');

  const stepsSvg = steps
    .map((s, i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      return step({
        x: 40 + col * 572,
        y: 384 + row * 166,
        w: 548,
        h: 150,
        ...s,
        cls: `s${(i % 4) + 1}`,
        tick: `t${(i % 6) + 1}`,
      });
    })
    .join('');

  const settings = [
    ['Auto upscale', 'ON', '#a78bfa'],
    ['Upscale button', 'ON', '#22d3ee'],
    ['Video upscale', 'ON for 4K', '#f0abfc'],
    ['Hardware acceleration', 'ON', '#34d399'],
    ['Potato mode', 'only if slow', '#f59e0b'],
    ['Media opener', 'your gallery', '#f472b6'],
  ];

  const settingsSvg = `
    ${card({ x: 40, y: 1076, w: 536, h: 300 })}
    ${label(64, 1110, 'Settings → Starlight Camera', { size: 15, fill: '#a78bfa' })}
    ${settings
      .map(
        ([k, v, accent], i) => `
      <g class="soft s${(i % 4) + 1}">
        ${body(72, 1150 + i * 38, k, { size: 13, fill: '#e2e8f0' })}
        <rect x="384" y="${1139 + i * 38}" width="${24 + v.length * 7}" height="19" rx="9.5"
              fill="${accent}" opacity=".85"/>
        <text x="${472}" y="${1153 + i * 38}" font-family="${FONT}" font-size="11.5"
              font-weight="700" fill="${accent}">${esc(v)}</text>
      </g>`,
      )
      .join('')}`;

  const sliders = [
    ['Denoise', 150, '#60a5fa', 'low by day'],
    ['Detail', 184, '#34d399', 'medium is safe'],
    ['Sharpen', 112, '#f472b6', 'low for faces'],
  ];

  const slidersSvg = `
    ${card({ x: 604, y: 1076, w: 536, h: 300, fill: '#0d1a16', stroke: '#2f6b56' })}
    ${label(628, 1110, 'The three sliders', { size: 15, fill: '#34d399' })}
    ${body(628, 1130, 'Off on all three is a true bypass.', { size: 12 })}
    ${sliders
      .map(
        ([name, fill, accent, tip], i) => `
      <g class="soft s${i + 2}">
        ${label(628, 1166 + i * 70, name, { size: 13 })}
        <rect x="628" y="${1178 + i * 70}" width="300" height="9" rx="4.5" fill="#241b3d"/>
        <rect x="628" y="${1178 + i * 70}" width="${fill}" height="9" rx="4.5" fill="${accent}">
          <animate attributeName="width" values="0;${fill}" dur="2.8s" begin="0.3s" fill="freeze"/>
        </rect>
        ${body(628, 1206 + i * 70, tip, { size: 11.5 })}
      </g>`,
      )
      .join('')}`;

  const fixes = [
    ['Will not install', 'uninstall the old LMC / GCam first'],
    ['Config does not load', 'XML must be in /sdcard/LMC8.4/, then double-tap → IMPORT'],
    ['Upscale does nothing', 'turn Auto upscale ON in Settings'],
    ['Video upscale fails', 'keep 1.5× the output size free'],
    ['Preview tap is dead', 'pick a media opener app in Settings'],
    ['Crashes on launch', 'Settings → Apps → Clear Data, reopen'],
  ];

  const H = 1740;

  return `${head(W, H, 'Starlight Camera manual setup: phone only, no PC, no ADB, no root')}
  ${defs}
  <rect width="${W}" height="${H}" rx="24" fill="url(#bg)"/>
  <rect x="1" y="1" width="${W - 2}" height="${H - 2}" rx="23" fill="none" stroke="#241a3d"/>

  <g transform="translate(40 40)">
    <circle class="glow" cx="32" cy="32" r="32" fill="#a78bfa" opacity=".2"/>
    <circle cx="32" cy="32" r="23" fill="none" stroke="#a78bfa" stroke-width="3"/>
    ${tickPath(21, 33, '#34d399', 't1')}
  </g>
  ${label(124, 62, 'Setup, phone only', { size: 32 })}
  ${body(124, 90, 'Starlight Camera vMax .51 · Redmi Note 10 Pro Max', { size: 15, fill: '#a78bfa' })}
  ${body(124, 114, 'Everything happens on the phone. No PC, no cable, no root.', { size: 13.5, fill: '#8b93a7' })}

  <g font-family="${FONT}" font-size="12.5" font-weight="700">
    <rect x="828" y="46" width="126" height="32" rx="16" fill="#0f1f18" stroke="#34d399"/>
    <text x="891" y="67" text-anchor="middle" fill="#a7f3d0">no PC</text>
    <rect x="966" y="46" width="174" height="32" rx="16" fill="#0f1f18" stroke="#34d399"/>
    <text x="1053" y="67" text-anchor="middle" fill="#a7f3d0">no ADB, no root</text>
  </g>
  <rect x="828" y="90" width="312" height="3" rx="1.5" fill="#241b3d"/>
  <rect class="sweep" x="828" y="89" width="140" height="5" rx="2.5" fill="url(#rail)"/>

  ${label(40, 166, '1 · Get these on the phone', { size: 15 })}
  ${filesSvg}

  ${label(40, 362, '2 · Install and configure', { size: 15 })}
  ${stepsSvg}

  ${label(40, 1054, '3 · Tune it', { size: 15 })}
  ${settingsSvg}
  ${slidersSvg}

  ${card({ x: 40, y: 1396, w: 1100, h: 292, fill: '#160c12', stroke: '#55293a' })}
  ${label(64, 1430, 'If something goes wrong', { size: 15, fill: '#fca5a5' })}
  ${fixes
    .map(([p, f], i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = 64 + col * 550;
      const y = 1466 + row * 66;
      return `${label(x, y, p, { size: 13, fill: '#fecaca' })}${body(x, y + 19, f, { size: 12, fill: '#b89aa4' })}`;
    })
    .join('')}

  ${body(40, H - 26, 'Unofficial mod built on Google Camera (GCam) and LMC 8.4 R17 · all processing is on-device · nothing is uploaded', { size: 12, fill: '#6b7280' })}
</svg>`;
}

mkdirSync(OUT, { recursive: true });
for (const [name, svg] of [
  ['lib-setup.svg', libSetup()],
  ['setup-manual.svg', setupManual()],
]) {
  writeFileSync(join(OUT, name), svg, 'utf8');
  console.log(`wrote assets/${name} (${Buffer.byteLength(svg)} bytes)`);
}