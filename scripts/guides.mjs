import { color as C, defs, frame, text as T, panel, badge, tick, canvas, type } from './design.mjs';

/* ==================================================================== */
/* Install guide — phone only                                           */
/* ==================================================================== */

export function install() {
  const W = canvas(880);
  const H = 856;

  const steps = [
    ['1', 'Download the files', 'Open Releases on the phone, tap each file.', C.violet],
    ['2', 'Remove the old camera', 'Uninstall LMC / GCam first, same package name.', C.sky],
    ['3', 'Install the APK', 'Tap it in Files, allow installs if asked.', C.violet],
    ['4', 'Move the helper files', 'XML into /sdcard/LMC8.4/, .so into its libs.', C.mint],
    ['5', 'Open it, allow permissions', 'Camera, Photos, Microphone.', C.cyan],
    ['6', 'Load the config', 'Double-tap beside the shutter, tap IMPORT.', C.orchid],
    ['7', 'Choose the library', 'Settings, Photo processing, Library, per lens.', C.amber],
    ['8', 'Switch the good stuff on', 'Auto upscale, video upscale, potato mode.', C.rose],
  ];

  const cw = 404;
  const ch = 92;

  const stepsSvg = steps
    .map(([n, title, line, accent], i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = 40 + col * (cw + 12);
      const y = 200 + row * (ch + 8);
      return `
    <g class="rise d${(i % 8) + 1}">
      ${panel(x, y, cw, ch, { accent, r: 11 })}
      ${badge(x + 30, y + 30, n, accent, { r: 14, size: 13.5 })}
      ${T(x + 54, y + 35, title, { size: type.body, weight: 700 })}
      ${T(x + 20, y + 66, line, { size: type.micro, fill: C.inkMute })}
      ${tick(x + cw - 42, y + ch - 20, accent, `k${(i % 6) + 1}`, 0.85)}
    </g>`;
    })
    .join('');

  const files = [
    ['132.4 MB', 'the camera app', C.violet],
    ['154 KB', 'the tuning config', C.mint],
    ['94.5 MB', 'optional library', C.amber],
  ];

  const filesSvg = files
    .map(([meta, what, accent], i) => {
      const x = 40 + i * 268;
      return `
    <g class="rise d${i + 1}">
      ${panel(x, 96, 256, 68, { fill: C.panelAlt, r: 11 })}
      <text x="${x + 16}" y="124" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.small}" font-weight="800" fill="${accent}">${meta}</text>
      <text x="${x + 16}" y="146" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.micro}" fill="${C.inkSoft}">${what}</text>
    </g>`;
    })
    .join('');

  const fixes = [
    ['Will not install', 'uninstall the old LMC / GCam'],
    ['Config does not load', 'XML must be in /sdcard/LMC8.4/'],
    ['Upscale does nothing', 'turn Auto upscale on'],
    ['Video upscale fails', 'keep 1.5× output size free'],
    ['Preview tap is dead', 'pick a media opener app'],
    ['Crashes on launch', 'Apps, Clear Data, reopen'],
  ];

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="120" cy="140" r="110" fill="url(#halo)"/>
    <circle class="drift d4" cx="780" cy="720" r="110" fill="url(#haloMint)"/>
  </g>

  ${T(40, 52, 'Setup, phone only', { size: type.h1, weight: 800 })}
  ${T(40, 78, 'No PC, no cable, no root. About three minutes.', { size: type.small, fill: C.violet })}

  ${filesSvg}
  ${stepsSvg}

  ${panel(40, 616, W - 80, 176, { fill: C.panelAlt, sheen: true })}
  ${T(60, 644, 'If something goes wrong', { size: type.small, weight: 800, fill: C.rose })}
  ${fixes
    .map(([p, f], i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const x = 60 + col * 404;
      const y = 678 + row * 36;
      return (
        `<circle cx="${x + 4}" cy="${y - 4}" r="3.6" fill="${C.rose}"/>` +
        `<text x="${x + 16}" y="${y}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" font-weight="700" fill="#fecaca">${p}</text>` +
        `<text x="${x + 16}" y="${y + 16}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" fill="${C.inkMute}">${f}</text>`
      );
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Starlight Camera setup, phone only: download, install, configure, tune') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Library setup — the .so for every lens                               */
/* ==================================================================== */

export function library() {
  const W = canvas(880);
  const H = 500;

  const lenses = [
    ['libs_key', 'Main camera', '108 MP S5KHM2', C.violet],
    ['libs_key_2', 'Tele camera', '2× zoom', C.sky],
    ['libs_key_3', 'Wide angle', 'IMX355 ultra-wide', C.cyan],
    ['libs_key_front', 'Front camera', 'IMX471 selfie', C.orchid],
  ];

  const y0 = 214;
  const rh = 58;

  const rows = lenses
    .map(([key, name, sub, accent], i) => {
      const y = y0 + i * rh;
      return `
    <g class="rise d${i + 2}">
      ${panel(40, y, 424, 50, { accent, r: 11 })}
      <circle class="ring" cx="74" cy="${y + 25}" r="13" fill="${accent}" opacity=".16"/>
      <circle cx="74" cy="${y + 25}" r="5.6" fill="${accent}"/>
      ${T(100, y + 22, name, { size: type.small, weight: 700 })}
      ${T(100, y + 40, sub, { size: type.micro, fill: C.inkFaint })}
      <text x="${446}" y="${y + 29}" text-anchor="end" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.micro}" font-weight="700" fill="${C.violet}">${key}</text>
      ${tick(430, y + 25, accent, `k${i + 1}`, 0.8)}
    </g>`;
    })
    .join('');

  const arrows = lenses
    .map(
      (_, i) =>
        `<path class="dash d${i + 2}" d="M468 ${y0 + i * rh + 25} H486" stroke="#7c8aa5"
               stroke-width="2" fill="none" marker-end="url(#ar)"/>`,
    )
    .join('');

  const taps = ['Open the app', 'Settings', 'Photo processing', 'Library'].map((t, i) => {
    const x = 40 + i * 202;
    return `
    <g class="rise d${i + 1}">
      ${panel(x, 150, 190, 46, { fill: C.panelAlt, r: 11 })}
      ${badge(x + 26, 173, `${i + 1}`, C.orchid, { r: 12.5, size: type.micro })}
      ${T(x + 46, 178, t, { size: type.micro, weight: 700, fill: C.inkSoft })}
      ${i < 3 ? `<path class="dash d${i + 1}" d="M${x + 194} 173 H${x + 202}" stroke="#7c8aa5" stroke-width="2" fill="none" marker-end="url(#ar)"/>` : ''}
    </g>`;
  }).join('');

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="150" cy="180" r="110" fill="url(#halo)"/>
    <circle class="drift d4" cx="760" cy="400" r="110" fill="url(#haloMint)"/>
  </g>

  ${T(40, 52, 'Set the library for every lens', { size: type.h1, weight: 800 })}
  ${T(40, 78, 'One file, four slots.', { size: type.small, fill: C.violet })}
  ${T(40, 102, 'Optional — the camera works without it, using default processing.', { size: type.micro, fill: C.inkMute })}

  ${T(40, 138, 'THEN', { size: type.micro, weight: 800, fill: C.orchid, spacing: 2 })}
  ${taps}

  ${T(40, 202, 'PICK THE FILE IN EACH ROW', { size: type.micro, weight: 800, fill: C.mint, spacing: 2 })}
  ${rows}
  ${arrows}

  <g class="lift">
    ${panel(500, y0, 340, 224, { fill: '#0c1a17', stroke: '#2f6b56', accent: C.mint, r: 11 })}
    ${T(522, y0 + 32, 'Pick this file', { size: type.small, weight: 800, fill: C.mint })}
    <rect x="522" y="${y0 + 48}" width="296" height="42" rx="9" fill="#0a1712" stroke="#2f6b56"/>
    <text x="536" y="${y0 + 75}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}"
          font-weight="700" fill="#a7f3d0">Starlight_EOS_R10_DSLR.so</text>
    ${T(522, y0 + 116, 'Already in /sdcard/LMC8.4/libs/', { size: type.micro, fill: C.inkMute })}
    ${T(522, y0 + 136, 'if you copied it earlier.', { size: type.micro, fill: C.inkMute })}
    <line x1="522" y1="${y0 + 154}" x2="818" y2="${y0 + 154}" stroke="#2f6b56" opacity=".5"/>
    ${T(522, y0 + 180, 'Do the same for each lens.', { size: type.micro, fill: C.inkMute })}
    ${T(522, y0 + 200, 'Restart the camera.', { size: type.micro, weight: 700, fill: C.amber })}
  </g>

  ${T(40, H - 20, 'Everything is on-device. Nothing about your photos leaves the phone.', { size: type.micro, fill: C.inkFaint })}
  `;

  return frame(W, H).replace('__LABEL__', 'How to choose the Starlight library for Main, Tele, Wide and Front lenses') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Sliders — what the three controls do                                 */
/* ==================================================================== */

export function sliders() {
  const W = canvas(880);
  const H = 320;

  const settings = [
    ['Auto upscale', 'ON', C.violet],
    ['Video upscale', 'ON for 4K', C.orchid],
    ['Potato mode', 'if slow', C.amber],
    ['Media opener', 'your gallery', C.rose],
  ];

  const rows = [
    ['Denoise', 96, C.sky, 'Low by day, higher at night.'],
    ['Detail', 128, C.mint, 'Medium is a safe default.'],
    ['Sharpen', 74, C.rose, 'Low for faces, higher for text.'],
  ];

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="140" cy="160" r="110" fill="url(#halo)"/>
    <circle class="drift d4" cx="750" cy="180" r="110" fill="url(#haloMint)"/>
  </g>

  ${panel(40, 20, 372, H - 40, { sheen: true })}
  ${T(60, 52, 'Settings worth touching', { size: type.small, weight: 800, fill: C.violet })}
  ${settings
    .map(([k, v, a], i) => {
      const y = 92 + i * 44;
      return (
        `<text x="60" y="${y}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" fill="${C.inkSoft}">${k}</text>` +
        `<rect x="284" y="${y - 13}" width="${20 + v.length * 6.8}" height="18" rx="9" fill="${a}" opacity=".85"/>` +
        `<text x="376" y="${y}" text-anchor="end" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" font-weight="700" fill="${a}">${v}</text>`
      );
    })
    .join('')}
  ${T(60, H - 40, 'Everything else can stay on Default.', { size: type.micro, fill: C.inkFaint })}

  ${panel(444, 20, 396, H - 40, { fill: '#0c1a17', stroke: '#2f6b56', sheen: true })}
  ${T(464, 52, 'The three sliders', { size: type.small, weight: 800, fill: C.mint })}
  ${T(464, 72, 'Off on all three is a true bypass.', { size: type.micro, fill: C.inkFaint })}
  ${rows
    .map(([name, fill, accent, tip], i) => {
      const y = 112 + i * 62;
      const trackW = 260;
      return `
      <g class="rise d${i + 1}">
        <text x="464" y="${y}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}"
              font-weight="700" fill="${C.ink}">${name}</text>
        <rect x="464" y="${y + 12}" width="${trackW}" height="9" rx="4.5" fill="${C.lineSoft}"/>
        <rect x="464" y="${y + 12}" width="${fill}" height="9" rx="4.5" fill="${accent}"/>
        <circle class="ring" cx="${464 + fill}" cy="${y + 16.5}" r="8" fill="${C.base}" stroke="${accent}" stroke-width="3"/>
        <text x="464" y="${y + 40}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}"
              fill="${C.inkMute}">${tip}</text>
      </g>`;
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Which settings to change and what the three sliders do') + defs + body + '</svg>';
}
