import { font, color as C, defs, frame, text as T, sweepRail } from './design.mjs';

/** Deterministic PRNG so repeated builds produce identical files. */
function rng(seed) {
  let s = seed;
  return () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
}

export function hero() {
  const W = 1280;
  const H = 440;
  const r = rng(20261113);

  const stars = Array.from({ length: 48 }, (_, i) => {
    const x = Math.round(r() * (W - 60)) + 30;
    const y = Math.round(r() * (H - 80)) + 24;
    const rad = +(0.8 + r() * 1.3).toFixed(2);
    const d = (i % 8) + 1;
    return `<circle class="tw d${d}" cx="${x}" cy="${y}" r="${rad}" fill="#fff"/>`;
  }).join('');

  const cx = 224;
  const cy = 236;
  const arcs = [96, 82].map((rad, i) =>
    `<circle class="spin" r="${rad}" fill="none" stroke="url(#photo)" stroke-width="${i ? 3 : 7}"
             stroke-linecap="round" stroke-dasharray="${i ? '210 300' : '330 120'}"
             opacity="${i ? 0.45 : 0.92}"/>`,
  ).join('');

  const blades = Array.from({ length: 6 }, (_, i) => {
    const a = (i * 60 * Math.PI) / 180;
    const x0 = Math.cos(a) * 26;
    const y0 = Math.sin(a) * 26;
    const x1 = Math.cos(a) * 54;
    const y1 = Math.sin(a) * 54;
    return `<line x1="${x0.toFixed(1)}" y1="${y0.toFixed(1)}" x2="${x1.toFixed(1)}" y2="${y1.toFixed(1)}"
                  stroke="${C.violet}" stroke-width="2.4" stroke-linecap="round" opacity=".35"/>`;
  }).join('');

  let chipCursor = 356;
  const chipSvg = [
    ['4K video', C.mint],
    ['40 MP photos', C.violet],
    ['Potato mode', C.amber],
    ['100% offline', C.sky],
  ]
    .map(([label, accent], i) => {
      const w = label.length * 8.4 + 36;
      const x = chipCursor;
      chipCursor += w + 14;
      return `
      <g class="lift d${i + 1}">
        <rect x="${x}" y="338" width="${w}" height="42" rx="21" fill="${C.raised}" stroke="${accent}" opacity=".95"/>
        <text x="${x + w / 2}" y="364" text-anchor="middle" font-family=""
              font-size="15.5" font-weight="700" fill="${accent}">${label}</text>
      </g>`;
    })
    .join('');

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="${cx}" cy="${cy}" r="150" fill="url(#halo)"/>
    <circle class="drift d3" cx="1080" cy="116" r="120" fill="url(#haloMint)"/>
  </g>

  ${stars}

  <circle cx="${cx}" cy="${cy}" r="128" fill="url(#halo)" opacity=".55" filter="url(#soft)">
    <animate attributeName="r" values="118;142;118" dur="5.4s" repeatCount="indefinite"/>
  </circle>

  <g transform="translate(${cx} ${cy})">
    ${arcs}
    <circle r="72" fill="${C.panelAlt}" stroke="${C.line}" stroke-width="2.5"/>
    ${blades}
    <circle class="ring d2" r="44" fill="none" stroke="${C.orchid}" stroke-width="2.5"/>
    <circle r="24" fill="none" stroke="${C.mint}" stroke-width="2" opacity=".55">
      <animate attributeName="r" values="21;33;21" dur="3.2s" repeatCount="indefinite"/>
      <animate attributeName="opacity" values=".6;0;.6" dur="3.2s" repeatCount="indefinite"/>
    </circle>
    <path d="M-20 6 L-6 22 L25 -18" fill="none" stroke="${C.mint}" stroke-width="9"
          stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="78" stroke-dashoffset="78">
      <animate attributeName="stroke-dashoffset" values="78;0;0;78" dur="5.4s" repeatCount="indefinite"/>
    </path>
  </g>

  <g class="lift">
    ${T(356, 150, 'Starlight Camera', { size: 62, weight: 800, fill: C.ink })}
    ${T(358, 196, 'vMax .51', { size: 26, weight: 700, fill: C.violet, spacing: 1.6 })}
    ${T(358, 236, 'On-device photo & video super-resolution', { size: 25, fill: C.orchid })}
    <line x1="358" y1="258" x2="700" y2="258" stroke="${C.line}" stroke-width="1.5"/>
    ${T(358, 286, 'Redmi Note 10 Pro Max  ·  sweetin  ·  Android APK', { size: 18, fill: C.inkMute })}
    ${T(358, 312, 'Nothing is ever uploaded.', { size: 15, fill: C.inkFaint })}
  </g>

  ${chipSvg}

  ${sweepRail(40, H - 34, W - 80)}
  ${T(40, H - 14, 'Built on Google Camera (GCam) and LMC 8.4 R17', { size: 12, fill: C.inkFaint })}
  `;

  return frame(W, H).replace('__LABEL__', 'Starlight Camera vMax .51 hero banner') + defs + body + '</svg>';
}
