import { color as C, defs, frame, text as T, panel, canvas, type } from './design.mjs';

/* ==================================================================== */
/* Pipeline — what the app actually does                                 */
/* ==================================================================== */

export function pipeline() {
  const W = canvas(880);
  const H = 520;

  const node = (x, y, w, h, title, sub, accent, d) => `
    <g class="rise d${d}">
      ${panel(x, y, w, h, { accent, r: 11 })}
      <text x="${x + 14}" y="${y + 26}" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.small}" font-weight="700" fill="${C.ink}">${title}</text>
      ${sub
        .map(
          (l, i) =>
            `<text x="${x + 14}" y="${y + 47 + i * 16}" font-family="Inter, 'Segoe UI', sans-serif"
                   font-size="${type.micro}" fill="${C.inkMute}">${l}</text>`,
        )
        .join('')}
    </g>`;

  const x0 = 40;
  const nw = 156;
  const gap = 12;

  const row = (list, y, d0) =>
    list
      .map(([t, s, a], i) => {
        const x = x0 + i * (nw + gap);
        const parts = [node(x, y, nw, 84, t, s, a, ((i + d0) % 8) + 1)];
        if (i < list.length - 1)
          parts.push(
            `<path class="dash d${i + 1}" d="M${x + nw + 1} ${y + 42} H${x + nw + gap - 3}"
                   stroke="#7c8aa5" stroke-width="2" fill="none" marker-end="url(#ar)"/>`,
          );
        return parts.join('');
      })
      .join('');

  const photo = [
    ['Capture', ['JPEG · DNG'], C.violet],
    ['StarlightSR', ['1024px tiled', 'GPU readback'], C.violet],
    ['Lanczos3', ['colour matrix', 'edge sharpen'], C.sky],
    ['24 / 40 MP', ['JPEG q98', 'EXIF upright'], C.mint],
    ['Gallery', ['named _Starlight'], C.mint],
  ];

  const video = [
    ['MediaCodec', ['hardware decode'], C.mint],
    ['Pass 1 clean', ['chroma bilateral', 'BT.709 luma'], C.mint],
    ['Pass 2 detail', ['de-blur, dual-band', 'IGN dither'], C.cyan],
    ['1:1 cadence', ['no frame blending'], C.cyan],
    ['4K out', ['AAC audio kept'], C.lime],
  ];

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="140" cy="150" r="110" fill="url(#halo)"/>
    <circle class="drift d4" cx="760" cy="420" r="110" fill="url(#haloMint)"/>
  </g>

  ${T(40, 52, 'What happens to your photo and video', { size: type.h1, weight: 800 })}
  ${T(40, 78, 'Every stage runs on the phone. No network calls, ever.', { size: type.small, fill: C.inkMute })}

  ${T(40, 122, 'PHOTO', { size: type.micro, weight: 800, fill: C.violet, spacing: 2 })}
  ${row(photo, 136, 1)}

  ${T(40, 264, 'VIDEO', { size: type.micro, weight: 800, fill: C.mint, spacing: 2 })}
  ${row(video, 278, 3)}

  ${panel(40, 398, W - 80, 100, { fill: C.panelAlt, sheen: true })}
  ${T(60, 426, 'Controls and safety', { size: type.small, weight: 800, fill: C.orchid })}
  ${[
    ['Denoise · Detail · Sharpen', 'Off is truly zero', C.sky],
    ['Potato mode', '16 MP · 1080p30', C.amber],
    ['Capability probe', 'picks the video tier', C.mint],
    ['Thermal + storage checks', 'paced, so clips are safe', C.cyan],
    ['Separate :sr process', 'never blocks the UI', C.violet],
    ['Rolling starlight.log', 'plus exit-reason dump', C.rose],
  ]
    .map(([k, v, a], i) => {
      const col = i % 3;
      const r = Math.floor(i / 3);
      const x = 60 + col * 268;
      const y = 454 + r * 34;
      return (
        `<circle cx="${x + 4}" cy="${y - 4}" r="3.6" fill="${a}"/>` +
        `<text x="${x + 16}" y="${y}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" font-weight="700" fill="${C.inkSoft}">${k}</text>` +
        `<text x="${x + 16}" y="${y + 15}" font-family="Inter, 'Segoe UI', sans-serif" font-size="${type.micro}" fill="${C.inkFaint}">${v}</text>`
      );
    })
    .join('')}
  `;

  return frame(W, H).replace('__LABEL__', 'Animated diagram of the Starlight photo and video pipeline') + defs + body + '</svg>';
}

/* ==================================================================== */
/* Highlights — four feature cards                                       */
/* ==================================================================== */

export function highlights() {
  const W = canvas(880);
  const H = 232;

  const cards = [
    {
      big: '40',
      unit: 'MP',
      title: 'Photo upscale',
      lines: ['24 MP by default.', 'Portrait DNG upright.'],
      accent: C.violet,
    },
    {
      big: '4K',
      unit: 'video',
      title: 'Two-pass rebuild',
      lines: ['1080p becomes 4K.', 'Audio kept in sync.'],
      accent: C.mint,
    },
    {
      big: '0',
      unit: 'uploads',
      title: 'Fully offline',
      lines: ['No account, no cloud.', 'Photos stay yours.'],
      accent: C.amber,
    },
    {
      big: '1',
      unit: 'switch',
      title: 'Potato mode',
      lines: ['16 MP, 1080p30.', 'Fast on hot phones.'],
      accent: C.orchid,
    },
  ];

  const cw = 196;
  const gap = 12;

  const body = cards
    .map((c, i) => {
      const x = 40 + i * (cw + gap);
      return `
    <g class="lift d${i + 1}">
      ${panel(x, 20, cw, H - 40, { sheen: true })}
      <rect x="${x}" y="20" width="${cw}" height="4" rx="2" fill="${c.accent}"/>
      <text x="${x + 18}" y="98" font-family="Inter, 'Segoe UI', sans-serif" font-size="46"
            font-weight="800" fill="${C.ink}">${c.big}</text>
      <text x="${x + 22 + c.big.length * 27}" y="98" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.h3}" font-weight="700" fill="${c.accent}">${c.unit}</text>
      <text x="${x + 18}" y="132" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.small}" font-weight="700" fill="${C.ink}">${c.title}</text>
      ${c.lines
        .map(
          (l, j) =>
            `<text x="${x + 18}" y="${158 + j * 19}" font-family="Inter, 'Segoe UI', sans-serif"
                   font-size="${type.micro}" fill="${C.inkMute}">${l}</text>`,
        )
        .join('')}
      <rect x="${x + 18}" y="${H - 40}" width="${cw - 36}" height="3" rx="1.5" fill="${c.accent}" opacity=".55"/>
    </g>`;
    })
    .join('');

  return frame(W, H).replace('__LABEL__', 'Feature highlights: 40 MP photos, 4K video, zero uploads, potato mode') + defs + body + '</svg>';
}
