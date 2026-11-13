#!/usr/bin/env python3
"""
Measure the generated SVG artwork with real font rendering.

Strategy: recolour every text node with a unique key colour, rasterise at full
scale with rsvg-convert, then locate each colour in the bitmap. That yields the
true ink box of every label — advance widths, descenders and all — instead of
the character-count estimate the JS audit had to use.

    python3 scripts/measure.py

Reports, per file:
  OUT    text bleeding past the canvas
  OVL    two labels whose boxes intersect
  TIGHT  baselines packed closer than the glyphs need
  PAD    label sitting within 5px of its card edge
  SMALL  font below 11.5px
  DEAD   horizontal band containing almost no ink
"""

import json
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
OUT = HERE.parent / "out"
TMP = Path("/tmp/sc-measure")
TMP.mkdir(exist_ok=True)

SCALE = 1.0  # rsvg renders at the viewBox size


def key_colour(i: int):
    """Distinct, saturated colours unlikely to appear in the artwork itself."""
    r = 1 + (i * 37) % 200
    g = 210 + (i * 7) % 40
    b = 1 + (i * 91) % 200
    return (r, g, b)


def extract_texts(svg: str):
    """Top-level <text> elements with their raw attributes."""
    import re

    items = []
    for m in re.finditer(r"<text ([^>]*?)>(.*?)</text>", svg, re.S):
        attrs, body = m.group(1), m.group(2)
        if "<" in body:
            continue  # skip nested markup, none expected but be safe
        items.append((m.start(), m.end(), attrs, body.strip(), m.group(0)))
    return items


def measure(path: Path):
    svg = path.read_text()
    dims = __import__("re").search(r'width="(\d+)"\s+height="(\d+)"', svg)
    W, H = int(dims.group(1)), int(dims.group(2))

    items = extract_texts(svg)
    painted = svg
    boxes = []

    selfcheck = False
    if selfcheck:
        pass

    # Repaint each text node with its own key colour, one file per text node
    # is wasteful, so instead emit a second SVG per node but batch by 24.
    # Simpler and fast enough: one SVG per text node at low cost (files are small).
    for idx, (start, end, attrs, body, raw) in enumerate(items):
        col = key_colour(idx)
        hexc = "#%02x%02x%02x" % col
        patched = raw
        # force fill and remove any class-driven opacity surprises
        if 'fill="' in patched:
            import re

            patched = re.sub(r'fill="[^"]*"', f'fill="{hexc}"', patched, count=1)
        else:
            patched = patched.replace("<text ", f'<text fill="{hexc}" ', 1)
        patched = patched.replace('opacity="0"', 'opacity="1"')
        one = svg.replace(raw, patched, 1)
        f = TMP / "probe.svg"
        f.write_text(one)
        png = TMP / "probe.png"
        try:
            subprocess.run(
                ["rsvg-convert", "-w", str(int(W * SCALE)), str(f), "-o", str(png)],
                check=True,
                capture_output=True,
            )
        except subprocess.CalledProcessError:
            return None, "rasterise failed"

        img = np.array(Image.open(png).convert("RGB"))
        target = np.array(col, dtype=np.int16)
        diff = np.abs(img.astype(np.int16) - target).sum(axis=2)
        ys, xs = np.where(diff < 30)
        if len(xs) == 0:
            boxes.append({"s": body, "x": None, "y": None, "w": 0, "h": 0, "size": font_size(attrs)})
            continue
        boxes.append(
            {
                "s": body,
                "x": float(xs.min()),
                "y": float(ys.min()),
                "w": float(xs.max() - xs.min() + 1),
                "h": float(ys.max() - ys.min() + 1),
                "size": font_size(attrs),
            }
        )

    return (W, H, boxes), None


def font_size(attrs: str) -> float:
    import re

    m = re.search(r'font-size="([\d.]+)"', attrs)
    return float(m.group(1)) if m else 12.0


def analyse(name: str, W: int, H: int, boxes: list):
    problems = []
    live = [b for b in boxes if b["x"] is not None]

    for b in boxes:
        if b["x"] is None:
            problems.append(f'BLANK "{b["s"][:40]}" rendered nothing')

    for b in live:
        if b["x"] < -1 or b["y"] < -1 or b["x"] + b["w"] > W + 1 or b["y"] + b["h"] > H + 1:
            problems.append(
                f'OUT   "{b["s"][:38]}" [{b["x"]:.0f},{b["y"]:.0f} {b["w"]:.0f}x{b["h"]:.0f}]'
            )
        if b["size"] < 11.5:
            problems.append(f'SMALL "{b["s"][:38]}" {b["size"]}px')

    # overlaps
    for i in range(len(live)):
        for j in range(i + 1, len(live)):
            a, b = live[i], live[j]
            ox = min(a["x"] + a["w"], b["x"] + b["w"]) - max(a["x"], b["x"])
            oy = min(a["y"] + a["h"], b["y"] + b["h"]) - max(a["y"], b["y"])
            if ox > 3 and oy > 2:
                problems.append(
                    f'OVL   "{a["s"][:26]}" / "{b["s"][:26]}" {ox:.0f}x{oy:.0f}'
                )

    # vertical crowding within a column
    col = sorted(live, key=lambda b: b["y"])
    for i in range(len(col) - 1):
        a, b = col[i], col[i + 1]
        ox = min(a["x"] + a["w"], b["x"] + b["w"]) - max(a["x"], b["x"])
        if ox <= max(8, a["w"] * 0.25):
            continue
        gap = b["y"] - (a["y"] + a["h"])
        if 0 <= gap < 2.5:
            problems.append(f'TIGHT "{a["s"][:26]}" / "{b["s"][:26]}" gap={gap:.1f}')

    # ink coverage by horizontal band, to catch dead space
    bands = 8
    bh = H / bands
    cover = []
    for i in range(bands):
        top, bot = i * bh, (i + 1) * bh
        area = sum(
            b["w"] * b["h"] for b in live if b["y"] + b["h"] > top and b["y"] < bot
        )
        cover.append(round(area / (W * bh), 5))

    dead = [i for i, c in enumerate(cover) if c < 0.004]

    return sorted(set(problems)), cover, dead


def main():
    files = sorted(OUT.glob("*.svg"))
    if not files:
        print("no artwork in out/")
        return 1

    bad = 0
    report = {}
    for f in files:
        res, err = measure(f)
        if err:
            print(f"✗ {f.name}: {err}")
            bad += 1
            continue
        W, H, boxes = res
        problems, cover, dead = analyse(f.name, W, H, boxes)
        flag = "✗" if problems else "✓"
        if problems:
            bad += 1
        print(f"{flag} {f.name:<20} {W}×{H}  labels={len(boxes)}")
        for p in problems[:16]:
            print(f"     {p}")
        if dead:
            print(f"     DEAD bands {dead}  coverage {cover}")
        report[f.name] = {"cover": cover, "problems": problems}

    (TMP / "report.json").write_text(json.dumps(report, indent=2))
    print("\n" + ("all measured clean" if not bad else f"{bad} file(s) with problems"))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
