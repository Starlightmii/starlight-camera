import { font, color as C, defs, frame, text as T, panel, badge, sweepRail } from './design.mjs';

/**
 * Phone mock — a small vector device used to show the UI readably.
 * Kept schematic on purpose: this is a diagram, not a screenshot.
 */
export function phone(x, y, w, h, o = {}) {
  const {
    accent = C.violet,
    title = 'STARLIGHT',
    rows = [],
    footer = '',
    notch = true,
  } = o;
  const r = 22;
  const rx = x + 14;
  const innerW = w - 28;

  const notchSvg = notch
    ? `<rect x="${x + w / 2 - 26}" y="${y + 12}" width="52" height="8" rx="4" fill="${C.line}"/>`
    : '';

  const rowsSvg = rows
    .map((row, i) => {
      const ry = y + 92 + i * 46;
      return `
      <g>
        <text x="${rx}" y="${ry}" font-family="" font-size="12.5"
              font-weight="600" fill="${C.inkSoft}">${row.label}</text>
        <rect x="${rx}" y="${ry + 10}" width="${innerW}" height="8" rx="4" fill="${C.lineSoft}"/>
        <rect x="${rx}" y="${ry + 10}" width="${row.fill}" height="8" rx="4" fill="${row.accent ?? accent}"/>
        <text x="${rx + innerW}" y="${ry}" text-anchor="end" font-family=""
              font-size="12" font-weight="700" fill="${row.accent ?? accent}">${row.value}</text>
      </g>`;
    })
    .join('');

  return `
  <g>
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="#0d0818" stroke="${C.line}" stroke-width="2"/>
    <rect x="${x + 5}" y="${y + 5}" width="${w - 10}" height="${h - 10}" rx="${r - 6}" fill="${C.panel}" stroke="${C.lineSoft}"/>
    ${notchSvg}
    <text x="${x + w / 2}" y="${y + 46}" text-anchor="middle" font-family=""
          font-size="13" font-weight="800" fill="${accent}" letter-spacing="2">${title}</text>
    <line x1="${rx}" y1="${y + 62}" x2="${rx + innerW}" y2="${y + 62}" stroke="${C.line}" stroke-width="1.5"/>
    ${rowsSvg}
    ${footer ? `<text x="${x + w / 2}" y="${y + h - 20}" text-anchor="middle" font-family=""
          font-size="12" font-weight="700" fill="${accent}">${footer}</text>` : ''}
  </g>`;
}

/** Horizontal labelled step with an arrow between. */
export function flowStep(x, y, w, h, o) {
  const { n, title, sub, accent, d = 1 } = o;
  return `
  <g class="rise d${d}">
    ${panel(x, y, w, h, { accent })}
    ${badge(x + 32, y + 30, n, accent, { r: 15, size: 14.5 })}
    <text x="${x + 56}" y="${y + 35}" font-family="" font-size="14.5"
          font-weight="700" fill="${C.ink}">${title}</text>
    ${sub
      .map(
        (l, i) =>
          `<text x="${x + 18}" y="${y + 62 + i * 18}" font-family=""
                 font-size="12" fill="${C.inkMute}">${l}</text>`,
      )
      .join('')}
  </g>`;
}

export const connector = (x, y, len, d = 0) =>
  `<path class="dash${d ? ` d${d}` : ''}" d="M${x} ${y} H${x + len}" stroke="#7c8aa5"
         stroke-width="2" fill="none" marker-end="url(#ar)"/>`;
