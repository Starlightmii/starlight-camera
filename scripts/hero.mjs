import { color as C, defs, frame, text as T, sweepRail, canvas, type } from './design.mjs';

/**
 * Hero banner.
 *
 * Designed at exactly the GitHub content width so nothing is downscaled:
 * 1 SVG unit = 1 CSS pixel on the rendered page.
 */

function rng(seed) {
  let s = seed;
  return () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
}

export function hero() {
  const W = canvas(880);
  const H = 420;
  const r = rng(20261113);

  const stars = Array.from({ length: 40 }, (_, i) => {
    const x = Math.round(r() * (W - 60)) + 30;
    const y = Math.round(r() * 180) + 16;
    const rad = +(0.7 + r() * 1.1).toFixed(2);
    return `<circle class="tw d${(i % 8) + 1}" cx="${x}" cy="${y}" r="${rad}" fill="#fff"/>`;
  }).join('');

  const cx = 132;
  const cy = 208;

  const blades = Array.from({ length: 6 }, (_, i) => {
    const a = (i * 60 * Math.PI) / 180;
    return `<line x1="${(Math.cos(a) * 20).toFixed(1)}" y1="${(Math.sin(a) * 20).toFixed(1)}"
                  x2="${(Math.cos(a) * 42).toFixed(1)}" y2="${(Math.sin(a) * 42).toFixed(1)}"
                  stroke="${C.violet}" stroke-width="2.2" stroke-linecap="round" opacity=".32"/>`;
  }).join('');

  const chips = [
    ['4K video', C.mint],
    ['40 MP photos', C.violet],
    ['100% offline', C.sky],
  ];
  let x = 252;
  const chipsSvg = chips
    .map(([label, accent], i) => {
      const w = label.length * 8.6 + 34;
      const here = x;
      x += w + 10;
      return `
    <g class="lift d${i + 1}">
      <rect x="${here}" y="330" width="${w}" height="38" rx="19" fill="${C.raised}" stroke="${accent}"/>
      <text x="${here + w / 2}" y="${354}" text-anchor="middle" font-family="Inter, 'Segoe UI', sans-serif"
            font-size="${type.micro}" font-weight="700" fill="${accent}">${label}</text>
    </g>`;
    })
    .join('');

  const body = `
  <g filter="url(#soft)">
    <circle class="drift" cx="${cx}" cy="${cy}" r="120" fill="url(#halo)"/>
    <circle class="drift d4" cx="760" cy="120" r="110" fill="url(#haloMint)"/>
  </g>
  ${stars}

  <g transform="translate(${cx} ${cy})">
    <circle class="spin" r="74" fill="none" stroke="url(#photo)" stroke-width="6"
            stroke-linecap="round" stroke-dasharray="250 90" opacity=".9"/>
    <circle class="spin" r="60" fill="none" stroke="url(#photo)" stroke-width="2.5"
            stroke-linecap="round" stroke-dasharray="150 210" opacity=".45"/>
    <circle r="52" fill="${C.panelAlt}" stroke="${C.line}" stroke-width="2.5"/>
    ${blades}
    <circle class="ring d2" r="30" fill="none" stroke="${C.orchid}" stroke-width="2.5"/>
    <path d="M-13 4 L-4 15 L17 -13" fill="none" stroke="${C.mint}" stroke-width="7"
          stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="60" stroke-dashoffset="60">
      <animate attributeName="stroke-dashoffset" values="60;0;0;60" dur="5.4s" repeatCount="indefinite"/>
    </path>
  </g>

  <g class="lift">
    ${T(252, 116, 'Starlight Camera', { size: type.hero, weight: 800 })}
    ${T(254, 148, 'vMax .51', { size: type.h3, weight: 700, fill: C.violet, spacing: 1.6 })}
    ${T(254, 186, 'On-device photo & video super-resolution', { size: type.h2, fill: C.orchid })}
    <line x1="254" y1="206" x2="560" y2="206" stroke="${C.line}" stroke-width="1.5"/>
    ${T(254, 232, 'Redmi Note 10 Pro Max  ·  sweetin', { size: type.body, fill: C.inkMute })}
    ${T(254, 256, 'Nothing is ever uploaded.', { size: type.small, fill: C.inkFaint })}
    ${T(254, 288, 'Available for the Redmi Note 10 Pro Max on GitHub', { size: type.small, fill: C.inkMute })}
  </g>

  ${chipsSvg}

  ${sweepRail(40, H - 44, W - 80)}
  ${T(40, H - 20, 'Built on Google Camera (GCam) and LMC 8.4 R17', { size: type.micro, fill: C.inkFaint })}
  `;

  return frame(W, H).replace('__LABEL__', 'Starlight Camera vMax .51') + defs + body + '</svg>';
}
