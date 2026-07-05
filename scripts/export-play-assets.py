#!/usr/bin/env python3
"""Render Play Console store assets from SVG sources."""

from pathlib import Path

import cairosvg

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "store-assets"

EXPORTS = [
    ("icon.svg", "play-store-icon-512.png", 512, 512),
    ("feature-graphic.svg", "play-store-feature-graphic-1024x500.png", 1024, 500),
]


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    for svg_name, png_name, width, height in EXPORTS:
        svg = ASSETS / svg_name
        out = ASSETS / png_name
        cairosvg.svg2png(
            url=str(svg),
            write_to=str(out),
            output_width=width,
            output_height=height,
        )
        print(f"Wrote {out} ({out.stat().st_size:,} bytes, {width}x{height})")


if __name__ == "__main__":
    main()
