import { font, color as C, defs, frame, text as T, panel, badge, sweepRail } from './design.mjs';
import { phone, flowStep, connector } from './parts.mjs';

/* ==================================================================== */
/* Pipeline — what the app actually does                                */
/* ==================================================================== */

export function pipeline() {
  const W = 1180;
  const H = 560;

  const node = (x, y, w, h, title, sub, accent, o = {}) => `
    <g class="rise d${o.d ?? 1}">
      ${panel(x, y, w, h, { accent })}
      <text x="${x + 18}" y="${y + 28}" font-family="" font-size="14"
            font-weight="700" fill="${C.ink}">${title}</text>
      ${sub
        .map(
          (l, i) =>
            `<text x="${x + 18}" y="${y + 50 + i * 17}" font-family=""
                   font-size="11.5" fill="${C.inkMute}">${l}</text>`,
        )
        .join('')}
    </g>`;

  const photoY = 132;
  const videoY = 296;
  const nw = 196;
  const gap = 20;
  const x0 = 40;

  const photoRow = [
    ['GCam capture', ['JPEG · DNG · RAW'], C.violet],
    ['StarlightSR', ['1024 px tiled GPU', 'readback'], C.violet],
    ['Lanczos3 fallback', ['colour matrix', 'edge-gated sharpen'], C.sky],
    ['24 / 40 MP', ['JPEG q98', 'EXIF upright'], C.mint],
    ['Gallery', ['named _Starlight'], C.mint],
  ];

  const videoRow = [
    ['MediaCodec', ['hardware decode'], C.mint],
    ['Pass 1 · clean', ['chroma bilateral', 'BT.709 luma'], C.mint],
    ['Pass 2 · detail', ['de-blur · dual-band', 'IGN dithering'], C.cyan],
    ['1:1 cadence', ['no frame blending'], C.cyan],
    ['4K output', ['AAC audio kept'], C.lime],
  ];

  const rows = (list, y, accentLine, d0) =>
    list
      .map(([t, s, a], i) => {
        const x = x0 + i * (nw + gap);
        const parts = [node(x, y, nw, 96, t, s, a, { d: i + d0 })];
        if (i < list.length - 1) parts.push(connector(x + nw + 2, y + 48, gap - 8, i + 1));
        return parts.join('');
      })
      .join('');

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="180" cy="180" r="130" fill="url(#halo)"/>
    <circle class="drift d4" cx="1000" cy="420" r="130" fill="url(#haloMint)"/>
  </g>

  ${T(40, 54, 'Photo & video pipeline', { size: 27, weight: 800 })}
  ${T(40, 80, 'Every stage runs on the phone. No network calls, ever.', { size: 14.5, fill: C.inkMute })}
  ${sweepRail(40, 96, W - 80)}

  ${T(40, 122, 'PHOTO', { size: 13, weight: 800, fill: C.violet, spacing: 2 })}
  ${rows(photoRow, photoY, 'photo', 1)}

  ${T(40, 286, 'VIDEO', { size: 13, weight: 800, fill: C.mint, spacing: 2 })}
  ${rows(videoRow, videoY, 'video', 2)}

  ${panel(40, 424, W - 80, 106, { fill: C.panelAlt, sheen: true })}
  ${T(62, 452, 'Controls and safety', { size: 14.5, weight: 800, fill: C.orchid })}
  ${[
    ['Denoise · Detail · Sharpen', 'full range — Off is truly zero', C.sky],
    ['Capability probe', 'picks a sustainable video tier', C.mint],
    ['Potato mode', '16 MP · 1080p30 · lower bitrate', C.amber],
    ['Thermal + storage prechecks', 'with pacing, so long clips are safe', C.cyan],
    ['Separate :sr process', 'video work never blocks the UI', C.violet],
    ['Rolling starlight.log', 'plus an exit-reason dump', C.rose],
  ]
    .map(([k, v, a], i) => {
      const col = i % 3;
      const row = Math.floor(i / 3);
      const x = 62 + col * 366;
      const y = 480 + row * 34;
      return (
        `<circle cx="${x + 4}" cy="${y - 4}" r="4" fill="${a}"/>` +
        `<text x="${x + 18}" y="${y}" font-family="" font-size="12.5" font-weight="700" fill="${C.inkSoft}">${k}</text>` +
        `<text x="${x + 18}" y="${y + 15}" font-family="" font-size="11" fill="${C.inkFaint}">${v}</text>`
      );
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Animated diagram of the Starlight photo and video pipeline') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Highlights — four feature cards                                      */
/* ==================================================================== */

export function highlights() {
  const W = 1180;
  const H = 268;

  const cards = [
    {
      big: '40',
      unit: 'MP max',
      title: 'Photo super-resolution',
      lines: ['24 MP by default.', 'Tiled GPU readback,', 'portrait DNG upright.'],
      accent: C.violet,
      grad: 'photo',
    },
    {
      big: '4K',
      unit: 'video',
      title: 'Two-pass rebuild',
      lines: ['1080p becomes 3840×2160.', 'Audio kept in sync,', 'BT.709 colour.'],
      accent: C.mint,
      grad: 'video',
    },
    {
      big: '0',
      unit: 'uploads',
      title: 'Fully offline',
      lines: ['No account, no cloud,', 'no subscription.', 'Your photos stay yours.'],
      accent: C.amber,
      grad: 'warm',
    },
    {
      big: '1',
      unit: 'switch',
      title: 'Potato mode',
      lines: ['16 MP, 1080p30,', 'lower bitrate. Fast on', 'hot phones.'],
      accent: C.orchid,
      grad: 'photo',
    },
  ];

  const cw = 259;
  const gapx = 21;

  const body = cards
    .map((c, i) => {
      const x = 40 + i * (cw + gapx);
      return `
    <g class="lift d${i + 1}">
      ${panel(x, 24, cw, H - 48, { fill: C.panel, sheen: true })}
      <rect x="${x}" y="24" width="${cw}" height="4" rx="2" fill="${c.accent}"/>
      <text x="${x + 24}" y="${106}" font-family="" font-size="54"
            font-weight="800" fill="${C.ink}">${c.big}</text>
      <text x="${x + 24 + c.big.length * 32}" y="${106}" font-family=""
            font-size="19" font-weight="700" fill="${c.accent}">${c.unit}</text>
      <text x="${x + 24}" y="${142}" font-family="" font-size="14.5"
            font-weight="700" fill="${C.ink}">${c.title}</text>
      ${c.lines
        .map(
          (l, j) =>
            `<text x="${x + 24}" y="${170 + j * 20}" font-family=""
                   font-size="12.5" fill="${C.inkMute}">${l}</text>`,
        )
        .join('')}
      <rect x="${x + 24}" y="${238}" width="${cw - 48}" height="3" rx="1.5" fill="${c.accent}" opacity=".55"/>
    </g>`;
    })
    .join('');

  return frame(W, H).replace('__LABEL__', 'Feature highlights: 40 MP photos, 4K video, zero uploads, potato mode') + defs + body + '</svg>';
}
