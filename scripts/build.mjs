/**
 * Starlight Camera — artwork build.
 *
 *   node scripts/build.mjs             # write SVGs into ./out
 *   node scripts/build.mjs --publish   # also copy into a repo clone
 *
 * `--publish` expects the clone at $SC_REPO (default /tmp/slrepo/assets).
 */

import { mkdirSync, writeFileSync, copyFileSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { hero } from './hero.mjs';
import { pipeline, highlights } from './diagrams.mjs';
import { install, library, sliders } from './guides.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, '..', 'out');

const ART = {
  'hero.svg': hero(),
  'architecture.svg': pipeline(),
  'highlights.svg': highlights(),
  'install-guide.svg': install(),
  'lib-setup.svg': library(),
  'config-guide.svg': sliders(),
};

mkdirSync(OUT, { recursive: true });
for (const [name, svg] of Object.entries(ART)) {
  writeFileSync(join(OUT, name), svg, 'utf8');
  console.log(`${name.padEnd(22)} ${String(Buffer.byteLength(svg)).padStart(6)} B`);
}

const publish = process.argv.includes('--publish');
if (publish) {
  const dest = process.env.SC_REPO || '/tmp/slrepo/assets';
  if (!existsSync(dest)) {
    console.error(`\npublish target missing: ${dest}`);
    process.exit(1);
  }
  for (const name of Object.keys(ART)) copyFileSync(join(OUT, name), join(dest, name));
  console.log(`\npublished ${Object.keys(ART).length} files -> ${dest}`);
}
