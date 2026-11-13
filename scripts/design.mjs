/**
 * Starlight Camera — design system for the README artwork.
 *
 * One place for colour, type, spacing and the shared motion vocabulary, so
 * every generated SVG looks like it came from the same hand.
 *
 * All output is self-contained: no external fonts, scripts or images. That
 * matters because GitHub serves these through its camo proxy, which will not
 * resolve relative or remote dependencies from inside an SVG.
 */

/* ------------------------------------------------------------------ */
/* Tokens                                                              */
/* ------------------------------------------------------------------ */

export const color = {
  void: '#070510',
  base: '#0b0716',
  panel: '#120c20',
  panelAlt: '#160f27',
  raised: '#1b1230',
  line: '#2a1f45',
  lineSoft: '#221a3a',

  ink: '#f8fafc',
  inkSoft: '#e2e8f0',
  inkMute: '#94a3b8',
  inkFaint: '#6b7280',

  violet: '#a78bfa',
  violetDeep: '#7c3aed',
  orchid: '#f0abfc',
  mint: '#34d399',
  cyan: '#22d3ee',
  amber: '#f59e0b',
  rose: '#f472b6',
  sky: '#60a5fa',
  lime: '#a3e635',
};

export const font =
  "Inter, 'Segoe UI', -apple-system, Helvetica, Arial, sans-serif";

export const space = (n) => n * 4;
export const radius = { sm: 9, md: 13, lg: 17, xl: 24 };

/* ------------------------------------------------------------------ */
/* Motion                                                              */
/* ------------------------------------------------------------------ */

export const motion = `
      .rise   { animation: rise 3.4s ease-in-out infinite; }
      @keyframes rise   { 0%,100% { opacity:.62 } 50% { opacity:1 } }
      .d1{animation-delay:.3s}.d2{animation-delay:.6s}.d3{animation-delay:.9s}
      .d4{animation-delay:1.2s}.d5{animation-delay:1.5s}.d6{animation-delay:1.8s}
      .d7{animation-delay:2.1s}.d8{animation-delay:2.4s}

      .dash   { stroke-dasharray:6 8; animation: dash 1.7s linear infinite; }
      @keyframes dash { to { stroke-dashoffset:-28 } }

      .tick   { stroke-dasharray:56; stroke-dashoffset:56;
                animation: tick 3.6s cubic-bezier(.65,0,.35,1) infinite; }
      @keyframes tick { 0%{stroke-dashoffset:56} 40%,74%{stroke-dashoffset:0}
                        100%{stroke-dashoffset:56} }
      .k1{animation-delay:.35s}.k2{animation-delay:.7s}.k3{animation-delay:1.05s}
      .k4{animation-delay:1.4s}.k5{animation-delay:1.75s}.k6{animation-delay:2.1s}

      .glow  { animation: glow 4s ease-in-out infinite; }
      @keyframes glow { 0%,100%{opacity:.26} 50%{opacity:.92} }

      .sweep { animation: sweep 5.2s cubic-bezier(.4,0,.6,1) infinite; }
      @keyframes sweep { 0%{transform:translateX(-200px)} 100%{transform:translateX(1300px)} }

      .drift { animation: drift 17s ease-in-out infinite; }
      @keyframes drift { 0%,100%{transform:translate(0,0)} 50%{transform:translate(38px,-22px)} }

      .lift  { animation: lift 5.4s ease-in-out infinite; }
      @keyframes lift { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-5px)} }

      .spin  { animation: spin 30s linear infinite; }
      @keyframes spin { to { transform: rotate(360deg) } }

      .ring  { animation: ring 4.4s ease-in-out infinite; }
      @keyframes ring { 0%,100%{opacity:.32;transform:scale(1)}
                        50%{opacity:.8;transform:scale(1.06)} }

      .tw    { animation: tw 3.6s ease-in-out infinite; }
      @keyframes tw { 0%,100%{opacity:.14} 50%{opacity:.92} }

      @media (prefers-reduced-motion: reduce) { * { animation: none !important } }`;

/* ------------------------------------------------------------------ */
/* Primitives                                                          */
/* ------------------------------------------------------------------ */

export const esc = (s) =>
  String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

export function text(x, y, value, o = {}) {
  const {
    size = 13,
    weight = 400,
    fill = color.ink,
    anchor,
    spacing,
    opacity,
    cls,
  } = o;
  const attrs = [
    `x="${x}"`,
    `y="${y}"`,
    `font-family="${font}"`,
    `font-size="${size}"`,
    `font-weight="${weight}"`,
    `fill="${fill}"`,
    anchor ? `text-anchor="${anchor}"` : '',
    spacing ? `letter-spacing="${spacing}"` : '',
    opacity ? `opacity="${opacity}"` : '',
    cls ? `class="${cls}"` : '',
  ]
    .filter(Boolean)
    .join(' ');
  return `<text ${attrs}>${esc(value)}</text>`;
}

export function panel(x, y, w, h, o = {}) {
  const {
    fill = color.panel,
    stroke = color.line,
    accent,
    accentW = 4,
    r = radius.md,
    sheen = false,
  } = o;
  let s = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}"/>`;
  if (accent)
    s += `<rect x="${x}" y="${y}" width="${accentW}" height="${h}" rx="${accentW / 2}" fill="${accent}"/>`;
  if (sheen)
    s += `<path d="M${x + r} ${y + 1} H${x + w - r} a${r - 1} ${r - 1} 0 0 1 ${r - 1} ${r - 1} V${y + Math.min(30, h / 3)} H${x + 1} V${y + r} a${r - 1} ${r - 1} 0 0 1 ${r - 1} -${r - 1} Z" fill="#ffffff" opacity=".022"/>`;
  return s;
}

export const tick = (x, y, accent = color.mint, cls = 'k1', scale = 1) =>
  `<path class="tick ${cls}" transform="translate(${x} ${y}) scale(${scale})" ` +
  `d="M0 0 l7 8 l15 -19" fill="none" stroke="${accent}" stroke-width="${(3.2 / scale).toFixed(2)}" ` +
  `stroke-linecap="round" stroke-linejoin="round"/>`;

export const badge = (cx, cy, n, accent, o = {}) => {
  const { r = 16, size = 15.5 } = o;
  return (
    `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${accent}" opacity=".18"/>` +
    `<text x="${cx}" y="${cy + size * 0.36}" text-anchor="middle" font-family="${font}" ` +
    `font-size="${size}" font-weight="800" fill="${accent}">${esc(n)}</text>`
  );
};

export function chip(x, y, label, accent, o = {}) {
  const { h = 32, padX = 16, size = 12.5 } = o;
  const w = o.w ?? Math.round(label.length * size * 0.62 + padX * 2);
  return (
    `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${h / 2}" fill="none" stroke="${accent}"/>` +
    `<text x="${x + w / 2}" y="${y + h / 2 + size * 0.36}" text-anchor="middle" font-family="${font}" ` +
    `font-size="${size}" font-weight="700" fill="${accent}">${esc(label)}</text>`
  );
}

export const sweepRail = (x, y, w) =>
  `<rect x="${x}" y="${y}" width="${w}" height="3" rx="1.5" fill="${color.line}"/>` +
  `<rect class="sweep" x="${x}" y="${y - 1}" width="150" height="5" rx="2.5" fill="url(#rail)"/>`;

export const defs = `
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="${color.void}"/>
      <stop offset=".55" stop-color="${color.base}"/>
      <stop offset="1" stop-color="${color.void}"/>
    </linearGradient>

    <linearGradient id="rail" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="${color.violet}" stop-opacity="0"/>
      <stop offset=".5" stop-color="${color.orchid}" stop-opacity=".85"/>
      <stop offset="1" stop-color="${color.violet}" stop-opacity="0"/>
    </linearGradient>

    <linearGradient id="photo" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="${color.violet}"/>
      <stop offset="1" stop-color="${color.sky}"/>
    </linearGradient>

    <linearGradient id="video" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="${color.mint}"/>
      <stop offset="1" stop-color="${color.cyan}"/>
    </linearGradient>

    <linearGradient id="warm" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0" stop-color="${color.amber}"/>
      <stop offset="1" stop-color="${color.rose}"/>
    </linearGradient>

    <radialGradient id="halo" cx=".5" cy=".5" r=".5">
      <stop offset="0" stop-color="${color.violet}" stop-opacity=".5"/>
      <stop offset="1" stop-color="${color.violet}" stop-opacity="0"/>
    </radialGradient>

    <radialGradient id="haloMint" cx=".5" cy=".5" r=".5">
      <stop offset="0" stop-color="${color.mint}" stop-opacity=".42"/>
      <stop offset="1" stop-color="${color.mint}" stop-opacity="0"/>
    </radialGradient>

    <filter id="soft" x="-40%" y="-40%" width="180%" height="180%">
      <feGaussianBlur stdDeviation="20"/>
    </filter>

    <marker id="ar" markerWidth="9" markerHeight="9" refX="7" refY="3" orient="auto">
      <path d="M0,0 L0,6 L7,3 z" fill="#7c8aa5"/>
    </marker>

    <style><![CDATA[${motion}]]></style>
  </defs>`;

export const frame = (w, h) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" role="img" aria-label="__LABEL__">` +
  `<rect width="${w}" height="${h}" rx="${radius.xl}" fill="url(#bg)"/>` +
  `<rect x="1" y="1" width="${w - 2}" height="${h - 2}" rx="${radius.xl - 1}" fill="none" stroke="${color.line}"/>`;
