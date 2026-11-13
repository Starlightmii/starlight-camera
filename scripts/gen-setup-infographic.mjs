/**
 * Starlight Camera — manual setup infographic generator.
 *
 * Phone-only install: no PC, no ADB, no root. Emits a self-contained
 * animated SVG so GitHub's camo proxy serves it as-is.
 *
 *   node scripts/gen-setup-infographic.mjs
 */

import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'assets');
const FONT = 'Inter,Segoe UI,Helvetica,Arial,sans-serif';

const esc = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const W = 1200;

/** Motion rules. All of it is disabled under prefers-reduced-motion. */
const motion = `
      .trail { stroke-dasharray:6 8; animation: tdash 1.6s linear infinite; }
      @keyframes tdash { to { stroke-dashoffset:-28 } }
      .rise { animation: rise 3.4s ease-in-out infinite; }
      @keyframes rise { 0%,100% { opacity:.62 } 50% { opacity:1 } }
      .r2 { animation-delay:.3s } .r3 { animation-delay:.6s } .r4 { animation-delay:.9s }
      .r5 { animation-delay:1.2s } .r6 { animation-delay:1.5s }
      .r7 { animation-delay:1.8s } .r8 { animation-delay:2.1s }
      .grow { animation: grow 2.8s ease-out infinite; }
      @keyframes grow { 0% { transform:scaleX(.15) } 60%,100% { transform:scaleX(1) } }
      .tick { stroke-dasharray:52; stroke-dashoffset:52; animation: tk 3.2s ease-in-out infinite; }
      @keyframes tk { 0% { stroke-dashoffset:52 } 45%,78% { stroke-dashoffset:0 } 100% { stroke-dashoffset:52 } }
      .t2 { animation-delay:.4s } .t3 { animation-delay:.8s } .t4 { animation-delay:1.2s }
      .t5 { animation-delay:1.6s } .t6 { animation-delay:2s }
      .sweep { animation: sw 5s ease-in-out infinite; }
      @keyframes sw { 0% { transform:translateX(-140px) } 100% { transform:translateX(1200px) } }
      .glow { animation: gl 3.8s ease-in-out infinite; }
      @keyframes gl { 0%,100% { opacity:.3 } 50% { opacity:.85 } }
      @media (prefers-reduced-motion: reduce) { * { animation: none !important } }`;

const defs = `
  <defs>
    <linearGradient id="bgG" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#0b0715"/>
      <stop offset=".5" stop-color="#090612"/>
      <stop offset="1" stop-color="#0b0715"/>
    </linearGradient>
    <linearGradient id="sh" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="#a78bfa" stop-opacity="0"/>
      <stop offset=".5" stop-color="#f0abfc" stop-opacity=".8"/>
      <stop offset="1" stop-color="#a78bfa" stop-opacity="0"/>
    </linearGradient>
    <marker id="ar" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto">
      <path d="M0,0 L0,6 L7,3 z" fill="#7c8aa5"/>
    </marker>
    <style><![CDATA[${motion}]]></style>
  </defs>`;

/** Numbered step card with accent bar and animated check. */
function step({ x, y, w, h, n, title, lines, accent, cls = 'r1', tick = 't1' }) {
  return `
    <g class="rise ${cls}">
      <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="15" fill="#130d22" stroke="#332a52"/>
      <rect x="${x}" y="${y}" width="4" height="${h}" rx="2" fill="${accent}"/>
      <circle cx="${x + 36}" cy="${y + 36}" r="17" fill="${accent}" opacity=".2"/>
      <text x="${x + 36}" y="${y + 42}" text-anchor="middle" font-family="${FONT}"
            font-size="16" font-weight="800" fill="${accent}">${esc(n)}</text>
      <text x="${x + 64}" y="${y + 42}" font-family="${FONT}" font-size="16.5"
            font-weight="700" fill="#f8fafc">${esc(title)}</text>
      ${lines
        .map(
          (l, i) =>
            `<text x="${x + 22}" y="${y + 72 + i * 21}" font-family="${FONT}" font-size="13" fill="#94a3b8">${esc(l)}</text>`,
        )
        .join('')}
      <path class="tick ${tick}" d="M${x + w - 46} ${y + h - 24} l7 8 l15 -19"
            fill="none" stroke="${accent}" stroke-width="3.2"
            stroke-linecap="round" stroke-linejoin="round"/>
    </g>`;
}

/* ---------------- Section: three files ---------------- */
function fileCards(y) {
  const items = [
    {
      name: 'Starlight-Camera-vMax.51.apk',
      meta: '132.4 MB · the app',
      accent: '#a78bfa',
      to: 'Download/',
    },
    {
      name: 'Starlight_Master.xml',
      meta: '154 KB · the tuning',
      accent: '#34d399',
      to: 'LMC8.4/',
    },
    {
      name: 'Starlight_EOS_R10_DSLR.so',
      meta: '94.5 MB · optional',
      accent: '#f59e0b',
      to: 'LMC8.4/libs/',
    },
  ];
  const w = 360;
  const gap = 24;
  return items
    .map((it, i) => {
      const x = 40 + i * (w + gap);
      return `
    <g class="rise r${i + 1}">
      <rect x="${x}" y="${y}" width="${w}" height="124" rx="15" fill="#120c1f" stroke="#2f2549"/>
      <rect class="glow" x="${x}" y="${y}" width="${w}" height="4" rx="2" fill="${it.accent}"/>
      <text x="${x + 22}" y="${y + 40}" font-family="${FONT}" font-size="13.5"
            font-weight="700" fill="${it.accent}">${esc(it.meta)}</text>
      <text x="${x + 22}" y="${y + 66}" font-family="${FONT}" font-size="14"
            font-weight="700" fill="#f8fafc">${esc(it.name)}</text>
      <text x="${x + 22}" y="${y + 96}" font-family="${FONT}" font-size="12.5" fill="#8b93a7">put it in</text>
      <text x="${x + 82}" y="${y + 96}" font-family="${FONT}" font-size="12.5"
            font-weight="700" fill="#cbd5e1">/sdcard/${esc(it.to)}</text>
    </g>`;
    })
    .join('');
}

/* ---------------- Section: steps ---------------- */
function steps(startY) {
  const steps = [
    {
      n: '1',
      title: 'Download all three files',
      lines: [
        'On the phone, open the release page in your browser.',
        'Tap each file. They land in /sdcard/Download/.',
        'Nothing else needed — no PC involved.',
      ],
      accent: '#a78bfa',
    },
    {
      n: '2',
      title: 'Remove the old camera',
      lines: [
        'Settings → Apps → Starlight Camera (or LMC / GCam).',
        'Tap Uninstall. Same package name, so it must go first.',
        'Skip this if no GCam port is installed yet.',
      ],
      accent: '#22d3ee',
    },
    {
      n: '3',
      title: 'Install the APK',
      lines: [
        'Open Files → Download → tap the .apk → Install.',
        'If blocked, allow installs from your file manager.',
        'No root, no ADB, no developer options.',
      ],
      accent: '#818cf8',
    },
    {
      n: '4',
      title: 'Move the config into place',
      lines: [
        'Make folders LMC8.4 and StarlightCamera in /sdcard/.',
        'Copy Starlight_Master.xml into both.',
        'Copy the .so into LMC8.4/libs and StarlightCamera/libs.',
      ],
      accent: '#34d399',
    },
    {
      n: '5',
      title: 'Open it and grant permissions',
      lines: [
        'Launch Starlight Camera from the app drawer.',
        'Allow Camera, Photos/media, and Microphone.',
        'You should land on the viewfinder.',
      ],
      accent: '#2dd4bf',
    },
    {
      n: '6',
      title: 'Import the config',
      lines: [
        'Double-tap the black area beside the shutter.',
        'Tap IMPORT, go to /sdcard/LMC8.4/, pick the XML.',
        'The app restarts itself with tuned settings.',
      ],
      accent: '#f0abfc',
    },
    {
      n: '7',
      title: 'Pick the DSLR library (optional)',
      lines: [
        'Settings → Libs. Set libs_key to -1.',
        'Set pref_libs_fromdir_key to Starlight_EOS_R10_DSLR.so.',
        'Restart the camera so the library loads.',
      ],
      accent: '#f59e0b',
    },
    {
      n: '8',
      title: 'Turn on the good stuff',
      lines: [
        'Settings → Starlight Camera: auto upscale ON.',
        'Video upscale ON when you want 4K. Potato mode if slow.',
        'Shoot. Enhanced copies save next to the originals.',
      ],
      accent: '#ec4899',
    },
  ];

  const colW = 558;
  const h = 152;
  const gapY = 18;
  return steps
    .map((s, i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = 40 + col * (colW + 24);
      const y = startY + row * (h + gapY);
      return step({ x, y, w: colW, h, ...s, cls: `r${(i % 4) + 1}`, tick: `t${(i % 6) + 1}` });
    })
    .join('');
}

/* ---------------- Section: settings ---------------- */
function settings(y) {
  const rows = [
    ['Auto upscale', 'ON', '#a78bfa'],
    ['Upscale button', 'ON', '#22d3ee'],
    ['Video upscale', 'ON for 4K', '#f0abfc'],
    ['Hardware acceleration', 'ON', '#34d399'],
    ['Potato mode', 'OFF unless slow', '#f59e0b'],
    ['Media opener', 'Your gallery', '#f472b6'],
  ];
  const rowSvg = rows
    .map(
      ([label, value, accent], i) => `
    <g class="rise r${(i % 4) + 1}">
      <text x="72" y="${y + 62 + i * 44}" font-family="${FONT}" font-size="13.5" fill="#e2e8f0">${esc(label)}</text>
      <rect x="380" y="${y + 50 + i * 44}" width="118" height="20" rx="10" fill="#241b3d"/>
      <rect x="380" y="${y + 50 + i * 44}" width="${20 + value.length * 7}" height="20" rx="10" fill="${accent}" opacity=".85"/>
      <text x="${512}" y="${y + 65 + i * 44}" font-family="${FONT}" font-size="12"
            font-weight="700" fill="${accent}">${esc(value)}</text>
    </g>`,
    )
    .join('');

  const sliders = [
    ['Denoise', 150, '#60a5fa', 'low by day'],
    ['Detail', 180, '#34d399', 'medium is safe'],
    ['Sharpen', 110, '#f472b6', 'low for faces'],
  ]
    .map(
      ([label, fill, accent, tip], i) => `
    <g class="rise r${i + 2}">
      <text x="620" y="${y + 62 + i * 76}" font-family="${FONT}" font-size="13.5"
            font-weight="700" fill="#f8fafc">${esc(label)}</text>
      <rect x="620" y="${y + 74 + i * 76}" width="300" height="9" rx="4.5" fill="#241b3d"/>
      <rect x="620" y="${y + 74 + i * 76}" width="${fill}" height="9" rx="4.5" fill="${accent}">
        <animate attributeName="width" values="0;${fill}" dur="2.8s" begin="0.3s" fill="freeze"/>
      </rect>
      <text x="620" y="${y + 102 + i * 76}" font-family="${FONT}" font-size="12" fill="#8b93a7">${esc(tip)}</text>
    </g>`,
    )
    .join('');

  return `
    <rect x="40" y="${y}" width="536" height="336" rx="16" fill="#120c1f" stroke="#2a1f45"/>
    <text x="64" y="${y + 32}" font-family="${FONT}" font-size="15.5"
          font-weight="800" fill="#a78bfa">Settings → Starlight Camera</text>
    ${rowSvg}

    <rect x="596" y="${y}" width="564" height="336" rx="16" fill="#0d1a16" stroke="#2f6b56"/>
    <text x="620" y="${y + 32}" font-family="${FONT}" font-size="15.5"
          font-weight="800" fill="#34d399">The three sliders</text>
    <text x="620" y="${y + 52}" font-family="${FONT}" font-size="12" fill="#8b93a7">Off on all three is a true bypass.</text>
    ${sliders}`;
}

/* ---------------- Section: where files land ---------------- */
function locations(y) {
  const rows = [
    ['/sdcard/DCIM/Camera/', 'your shots and enhanced copies'],
    ['/sdcard/LMC8.4/Starlight_Master.xml', 'the config you imported'],
    ['/sdcard/LMC8.4/libs/', 'the DSLR library'],
    ['/sdcard/Android/data/com.google.android.GoogleCameraLMCR17/files/starlight/', 'logs, if you need them'],
  ];
  return `
    <rect x="40" y="${y}" width="1120" height="${52 + rows.length * 34}" rx="16" fill="#0f0a1c" stroke="#241a3d"/>
    <text x="66" y="${y + 32}" font-family="${FONT}" font-size="15.5"
          font-weight="800" fill="#f0abfc">Where things end up</text>
    ${rows
      .map(
        ([p, d], i) => `
      <text x="66" y="${y + 62 + i * 34}" font-family="${FONT}" font-size="12.5"
            fill="#7dd3fc">${esc(p)}</text>
      <text x="66" y="${y + 78 + i * 34}" font-family="${FONT}" font-size="12"
            fill="#8b93a7">${esc(d)}</text>`,
      )
      .join('')}`;
}

/* ---------------- Section: fixes ---------------- */
function fixes(y) {
  const rows = [
    ['Will not install', 'uninstall the old LMC / GCam first'],
    ['Config does not load', 'XML must sit in /sdcard/LMC8.4/, then double-tap → IMPORT'],
    ['Upscale does nothing', 'auto upscale must be ON in Settings'],
    ['Video upscale fails', 'keep 1.5× the output size free; avoid 10-bit HDR sources'],
    ['Preview tap is dead', 'pick a media opener app in Settings'],
    ['Crashes on launch', 'Settings → Apps → Clear Data, then reopen'],
  ];
  const colW = 548;
  return `
    <rect x="40" y="${y}" width="1120" height="${64 + Math.ceil(rows.length / 2) * 46}" rx="16"
          fill="#1a0f14" stroke="#5b2b3a"/>
    <text x="66" y="${y + 32}" font-family="${FONT}" font-size="15.5"
          font-weight="800" fill="#fca5a5">When it misbehaves</text>
    ${rows
      .map(([problem, fix], i) => {
        const col = i % 2;
        const row = Math.floor(i / 2);
        const x = 66 + col * (colW + 24);
        const yy = y + 60 + row * 46;
        return `
      <text x="${x}" y="${yy}" font-family="${FONT}" font-size="13"
            font-weight="700" fill="#fecaca">${esc(problem)}</text>
      <text x="${x}" y="${yy + 17}" font-family="${FONT}" font-size="12" fill="#c4a0a8">${esc(fix)}</text>`;
      })
      .join('')}`;
}

/* ---------------- Compose ---------------- */
const Y_FILES = 218;
const Y_STEPS = 384;
const Y_SET = 1064;
const Y_LOC = 1424;
const Y_FIX = 1600;
const H = 1852;

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}"
     role="img" aria-label="Starlight Camera manual setup: phone only, no PC, no ADB, no root">
  ${defs}

  <rect width="${W}" height="${H}" rx="24" fill="url(#bgG)"/>
  <rect x="1" y="1" width="${W - 2}" height="${H - 2}" rx="23" fill="none" stroke="#241a3d"/>

  <g transform="translate(40 40)">
    <circle class="glow" cx="34" cy="34" r="34" fill="#a78bfa" opacity=".22"/>
    <circle cx="34" cy="34" r="24" fill="none" stroke="#a78bfa" stroke-width="3"/>
    <path class="tick" d="M23 35 l8 9 l16 -20" fill="none" stroke="#34d399"
          stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
  </g>

  <text x="126" y="66" font-family="${FONT}" font-size="34" font-weight="800" fill="#f8fafc">Manual setup</text>
  <text x="126" y="96" font-family="${FONT}" font-size="16" fill="#a78bfa">Starlight Camera vMax .51 · Redmi Note 10 Pro Max</text>
  <text x="126" y="122" font-family="${FONT}" font-size="14" fill="#8b93a7">Everything below happens on the phone. No PC, no ADB, no root.</text>

  <g font-family="${FONT}" font-size="13" font-weight="700">
    <rect x="852" y="46" width="132" height="34" rx="17" fill="#0f1f18" stroke="#34d399"/>
    <text x="918" y="68" text-anchor="middle" fill="#a7f3d0">no PC</text>
    <rect x="996" y="46" width="164" height="34" rx="17" fill="#0f1f18" stroke="#34d399"/>
    <text x="1078" y="68" text-anchor="middle" fill="#a7f3d0">no ADB, no root</text>
  </g>
  <rect x="852" y="92" width="308" height="3" rx="1.5" fill="#241b3d"/>
  <rect class="sweep" x="852" y="91" width="120" height="5" rx="2.5" fill="url(#sh)"/>

  <text x="40" y="${Y_FILES - 22}" font-family="${FONT}" font-size="15.5" font-weight="800" fill="#f8fafc">1 · Get the three files on the phone</text>
  ${fileCards(Y_FILES)}

  <text x="40" y="${Y_STEPS - 22}" font-family="${FONT}" font-size="15.5" font-weight="800" fill="#f8fafc">2 · Install and configure</text>
  ${steps(Y_STEPS)}

  <text x="40" y="${Y_SET - 18}" font-family="${FONT}" font-size="15.5" font-weight="800" fill="#f8fafc">3 · Tune it</text>
  ${settings(Y_SET)}

  ${locations(Y_LOC)}
  ${fixes(Y_FIX)}

  <g font-family="${FONT}" font-size="12" fill="#6b7280">
    <text x="40" y="${H - 26}">Unofficial mod built on Google Camera and LMC 8.4 R17 · processing is entirely on-device, nothing is uploaded</text>
  </g>
</svg>`;

mkdirSync(OUT, { recursive: true });
writeFileSync(join(OUT, 'setup-manual.svg'), svg, 'utf8');
console.log(`wrote assets/setup-manual.svg (${Buffer.byteLength(svg)} bytes, ${W}x${H})`);