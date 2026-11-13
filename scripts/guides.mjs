import { font, color as C, defs, frame, text as T, panel, badge, tick, sweepRail } from './design.mjs';
import { phone, connector } from './parts.mjs';

/* ==================================================================== */
/* Guided install — 8 steps, phone only                                 */
/* ==================================================================== */

export function install() {
  const W = 1180;
  const H = 1092;

  const steps = [
    {
      n: '1',
      title: 'Download the three files',
      lines: ['Open Releases in your phone browser.', 'Tap each file — they land in Download.', 'No PC involved at all.'],
      accent: C.violet,
    },
    {
      n: '2',
      title: 'Remove the old camera',
      lines: ['Settings → Apps → find LMC / GCam.', 'Uninstall it. The package name is the', 'same, so it cannot sit beside the new one.'],
      accent: C.sky,
    },
    {
      n: '3',
      title: 'Install the APK',
      lines: ['Files → Download → tap the .apk → Install.', 'Allow installs from your file manager', 'if Android asks. No root, no ADB.'],
      accent: C.violet,
    },
    {
      n: '4',
      title: 'Move the two helper files',
      lines: ['Create folders LMC8.4 and StarlightCamera.', 'Drop Starlight_Master.xml into both.', 'Drop the .so into LMC8.4/libs.'],
      accent: C.mint,
    },
    {
      n: '5',
      title: 'Open it, allow permissions',
      lines: ['Launch Starlight Camera from the drawer.', 'Allow Camera, Photos and Microphone.', 'You should land on the viewfinder.'],
      accent: C.cyan,
    },
    {
      n: '6',
      title: 'Load the config',
      lines: ['Double-tap the black area by the shutter.', 'Tap IMPORT → /sdcard/LMC8.4/ → the XML.', 'The app restarts itself, already tuned.'],
      accent: C.orchid,
    },
    {
      n: '7',
      title: 'Choose the library per lens',
      lines: ['Settings → Photo processing → Library.', 'In Main, Tele, Wide and Front pick', 'Starlight_EOS_R10_DSLR.so, then restart.'],
      accent: C.amber,
    },
    {
      n: '8',
      title: 'Switch the good stuff on',
      lines: ['Settings → Starlight Camera → Auto upscale.', 'Video upscale for 4K, Potato mode if slow.', 'Shoot — enhanced copies save alongside.'],
      accent: C.rose,
    },
  ];

  const sw = 548;
  const sh = 158;
  const sx = 40;
  const sy = 214;

  const stepsSvg = steps
    .map((s, i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = sx + col * (sw + 24);
      const y = sy + row * (sh + 18);
      return `
    <g class="rise d${(i % 8) + 1}">
      ${panel(x, y, sw, sh, { accent: s.accent, sheen: true })}
      ${badge(x + 34, y + 34, s.n, s.accent)}
      ${T(x + 60, y + 39, s.title, { size: 15, weight: 700 })}
      ${s.lines
        .map((l, j) => T(x + 22, y + 68 + j * 19, l, { size: 12, fill: C.inkMute }))
        .join('')}
      ${tick(x + sw - 46, y + sh - 22, s.accent, `k${(i % 6) + 1}`)}
    </g>`;
    })
    .join('');

  const files = [
    { name: 'Starlight-Camera-vMax.51.apk', meta: '132.4 MB', what: 'the camera', to: 'Download/', accent: C.violet },
    { name: 'Starlight_Master.xml', meta: '154 KB', what: 'the tuning', to: 'LMC8.4/', accent: C.mint },
    { name: 'Starlight_EOS_R10_DSLR.so', meta: '94.5 MB', what: 'optional library', to: 'LMC8.4/libs/', accent: C.amber },
  ];

  const filesSvg = files
    .map((f, i) => {
      const x = 40 + i * 372;
      return `
    <g class="rise d${i + 1}">
      ${panel(x, 150, 348, 44, { fill: C.panelAlt })}
      <text x="${x + 16}" y="178" font-family="" font-size="12.5"
            font-weight="700" fill="${f.accent}">${f.meta}</text>
      <text x="${x + 96}" y="178" font-family="" font-size="12.5"
            font-weight="600" fill="${C.inkSoft}">${f.name}</text>
    </g>`;
    })
    .join('');

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="150" cy="160" r="140" fill="url(#halo)"/>
    <circle class="drift d4" cx="1040" cy="880" r="140" fill="url(#haloMint)"/>
  </g>

  <g transform="translate(40 36)">
    <circle class="ring" cx="30" cy="30" r="30" fill="${C.violet}" opacity=".16"/>
    <circle cx="30" cy="30" r="21" fill="none" stroke="${C.violet}" stroke-width="3"/>
    ${tick(19, 31, C.mint, 'k2')}
  </g>
  ${T(124, 62, 'Setup, phone only', { size: 32, weight: 800 })}
  ${T(124, 90, 'Starlight Camera vMax .51  ·  Redmi Note 10 Pro Max', { size: 15, fill: C.violet })}
  ${T(124, 114, 'Everything happens on the phone. No PC, no cable, no root.', { size: 13.5, fill: C.inkMute })}

  <g font-family="" font-size="12.5" font-weight="700">
    <rect x="830" y="42" width="128" height="32" rx="16" fill="none" stroke="${C.mint}"/>
    <text x="894" y="63" text-anchor="middle" fill="${C.mint}">no PC</text>
    <rect x="970" y="42" width="170" height="32" rx="16" fill="none" stroke="${C.mint}"/>
    <text x="1055" y="63" text-anchor="middle" fill="${C.mint}">no ADB, no root</text>
  </g>
  ${sweepRail(830, 88, 310)}
  ${T(40, 138, 'Get these three files on the phone', { size: 13, weight: 800, fill: C.orchid, spacing: 1.4 })}

  ${filesSvg}
  ${stepsSvg}

  ${panel(40, 958, W - 80, 118, { fill: C.panelAlt, sheen: true })}
  ${T(64, 988, 'If something goes wrong', { size: 14.5, weight: 800, fill: C.rose })}
  ${[
    ['Will not install', 'uninstall the old LMC / GCam first'],
    ['Config does not load', 'XML must sit in /sdcard/LMC8.4/'],
    ['Upscale does nothing', 'turn Auto upscale on in Settings'],
    ['Video upscale fails', 'keep 1.5× the output size free'],
    ['Preview tap is dead', 'pick a media opener app'],
    ['Crashes on launch', 'Apps → Clear Data, then reopen'],
  ]
    .map(([p, f], i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = 64 + col * 556;
      const y = 1018 + row * 34;
      return (
        T(x, y, p, { size: 12.5, weight: 700, fill: '#fecaca' }) +
        T(x + 176, y, f, { size: 12, fill: C.inkMute })
      );
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Starlight Camera manual setup: phone only, no PC, no ADB, no root') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Library setup — the .so for every lens                               */
/* ==================================================================== */

export function library() {
  const W = 1180;
  const H = 596;

  const lenses = [
    { key: 'libs_key', name: 'Main camera', sub: '108 MP S5KHM2', accent: C.violet },
    { key: 'libs_key_2', name: 'Tele camera', sub: '2× zoom', accent: C.sky },
    { key: 'libs_key_3', name: 'Wide angle', sub: 'IMX355 ultra-wide', accent: C.cyan },
    { key: 'libs_key_front', name: 'Front camera', sub: 'IMX471 selfie', accent: C.orchid },
  ];

  const y0 = 256;
  const rh = 70;

  const rows = lenses
    .map((l, i) => {
      const y = y0 + i * rh;
      return `
    <g class="rise d${i + 2}">
      ${panel(40, y, 636, 60, { accent: l.accent })}
      <circle class="ring" cx="84" cy="${y + 30}" r="15" fill="${l.accent}" opacity=".16"/>
      <circle cx="84" cy="${y + 30}" r="6.5" fill="${l.accent}"/>
      ${T(116, y + 26, l.name, { size: 14.5, weight: 700 })}
      ${T(116, y + 46, l.sub, { size: 11.5, fill: C.inkFaint })}
      <rect x="${636 - 30}" y="${y + 20}" width="0" height="0"/>
      <text x="${40 + 636 - 118}" y="${y + 27}" text-anchor="end" font-family=""
            font-size="12" font-weight="700" fill="${C.violet}">${l.key}</text>
      ${tick(40 + 636 - 34, y + 30, l.accent, `k${i + 1}`, 0.85)}
    </g>`;
    })
    .join('');

  const arrows = lenses
    .map(
      (_, i) =>
        `<path class="dash d${i + 2}" d="M682 ${y0 + i * rh + 30} H718" stroke="#7c8aa5"
               stroke-width="2" fill="none" marker-end="url(#ar)"/>`,
    )
    .join('');

  const tapPath = ['Open Starlight Camera', 'Settings', 'Photo processing', 'Library'].map((t, i) => {
    const x = 40 + i * 288;
    return `
    <g class="rise d${i + 1}">
      ${panel(x, 186, 264, 52, { fill: C.panelAlt })}
      ${badge(x + 30, 212, `${i + 1}`, C.orchid, { r: 14, size: 13.5 })}
      ${T(x + 52, 217, t, { size: 13, weight: 700, fill: C.inkSoft })}
      ${i < 3 ? `<path class="dash d${i + 1}" d="M${x + 268} 212 H${x + 282}" stroke="#7c8aa5" stroke-width="2" fill="none" marker-end="url(#ar)"/>` : ''}
    </g>`;
  }).join('');

  const side = `
    <g class="lift">
      ${panel(756, y0, 384, 280, { fill: '#0c1a17', stroke: '#2f6b56', accent: C.mint })}
      ${T(786, y0 + 36, 'Pick this file', { size: 15, weight: 800, fill: C.mint })}
      <rect x="786" y="${y0 + 54}" width="322" height="46" rx="11" fill="#0a1712" stroke="#2f6b56"/>
      <text x="802" y="${y0 + 83}" font-family="" font-size="13"
            font-weight="700" fill="#a7f3d0">Starlight_EOS_R10_DSLR.so</text>
      ${T(786, y0 + 128, 'It is already in place if you copied it', { size: 12, fill: C.inkMute })}
      ${T(786, y0 + 147, 'into /sdcard/LMC8.4/libs/', { size: 12, fill: C.inkMute })}
      <line x1="786" y1="${y0 + 168}" x2="1110" y2="${y0 + 168}" stroke="#2f6b56" opacity=".5"/>
      ${T(786, y0 + 196, 'Do the same for every lens you shoot with.', { size: 12, fill: C.inkMute })}
      ${T(786, y0 + 215, 'Leave a lens on Default to keep the', { size: 12, fill: C.inkMute })}
      ${T(786, y0 + 234, 'stock processing for it.', { size: 12, fill: C.inkMute })}
      ${T(786, y0 + 262, 'Restart the camera afterwards.', { size: 12, weight: 700, fill: C.amber })}
    </g>`;

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="200" cy="200" r="130" fill="url(#halo)"/>
    <circle class="drift d3" cx="1020" cy="480" r="120" fill="url(#haloMint)"/>
  </g>

  <g transform="translate(40 36)">
    <circle class="ring" cx="30" cy="30" r="30" fill="${C.violet}" opacity=".16"/>
    <circle cx="30" cy="30" r="21" fill="none" stroke="${C.violet}" stroke-width="3"/>
    <circle cx="30" cy="30" r="8" fill="${C.violet}" opacity=".85"/>
  </g>
  ${T(124, 62, 'Set the library for every lens', { size: 31, weight: 800 })}
  ${T(124, 90, 'One file, four slots. About a minute.', { size: 15, fill: C.violet })}
  ${T(124, 114, 'Skip it and the camera still works — it just uses the default processing.', { size: 13, fill: C.inkMute })}

  ${sweepRail(830, 88, 310)}

  ${T(40, 168, 'Get there', { size: 13, weight: 800, fill: C.orchid, spacing: 1.4 })}
  ${tapPath}

  ${T(40, 244, 'Then pick the file in each row', { size: 13, weight: 800, fill: C.mint, spacing: 1.4 })}
  ${rows}
  ${arrows}
  ${side}

  ${panel(40, H - 62, W - 80, 42, { fill: C.panelAlt })}
  ${T(62, H - 36, 'Optional. This swaps in the Starlight-tuned processing recipe; the camera works without it.', { size: 12, fill: C.inkFaint })}
  `;

  return frame(W, H).replace('__LABEL__', 'How to choose the Starlight library for Main, Tele, Wide and Front lenses') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Sliders — what the three controls do                                 */
/* ==================================================================== */

export function sliders() {
  const W = 1180;
  const H = 340;

  const rows = [
    ['Denoise', 44, C.sky, 'Cleaner but softer. Low by day, higher at night.', 'temporal + chroma'],
    ['Detail', 62, C.mint, 'Micro-contrast and texture. Medium is a safe default.', 'dual-band separation'],
    ['Sharpen', 34, C.rose, 'Edge definition. Low for faces, higher for text.', 'directional de-blur'],
  ];

  const settings = [
    ['Auto upscale', 'ON', C.violet],
    ['Video upscale', 'ON for 4K', C.orchid],
    ['Potato mode', 'only if slow', C.amber],
    ['Media opener', 'your gallery', C.rose],
  ];

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="200" cy="170" r="120" fill="url(#halo)"/>
    <circle class="drift d3" cx="1000" cy="200" r="120" fill="url(#haloMint)"/>
  </g>

  ${panel(40, 24, 500, H - 48, { sheen: true })}
  ${T(64, 56, 'Settings worth touching', { size: 15, weight: 800, fill: C.violet })}
  ${settings
    .map(([k, v, a], i) => {
      const y = 96 + i * 46;
      return (
        `${T(72, y, k, { size: 13.5, fill: C.inkSoft })}
         <rect x="368" y="${y - 14}" width="${24 + v.length * 7.4}" height="20" rx="10" fill="${a}" opacity=".85"/>
         ${T(448, y, v, { size: 12, weight: 700, fill: a })}`
      );
    })
    .join('')}
  ${T(72, H - 46, 'Everything else can stay on Default.', { size: 12, fill: C.inkFaint })}

  ${panel(564, 24, 576, H - 48, { fill: '#0c1a17', stroke: '#2f6b56', sheen: true })}
  ${T(588, 56, 'The three sliders', { size: 15, weight: 800, fill: C.mint })}
  ${T(588, 76, 'Off on all three is a genuine bypass.', { size: 12, fill: C.inkFaint })}
  ${rows
    .map(([name, fill, accent, tip, mech], i) => {
      const y = 112 + i * 68;
      const trackW = 300;
      return `
      <g class="rise d${i + 1}">
        ${T(588, y, name, { size: 14, weight: 700 })}
        ${T(588 + trackW + 16, y, mech, { size: 11.5, fill: C.inkFaint })}
        <rect x="588" y="${y + 12}" width="${trackW}" height="9" rx="4.5" fill="${C.lineSoft}"/>
        <rect x="588" y="${y + 12}" width="${fill}" height="9" rx="4.5" fill="${accent}"/>
        <circle cx="${588 + fill}" cy="${y + 16.5}" r="8.5" fill="${C.base}" stroke="${accent}" stroke-width="3"
                class="ring"/>
        ${T(588, y + 42, tip, { size: 11.5, fill: C.inkMute })}
      </g>`;
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Which settings to change and what the three sliders do') + defs + body + '</svg>';
}
