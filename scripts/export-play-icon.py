#!/usr/bin/env python3
"""Render store-assets/icon.svg to a 512x512 Play Console PNG."""

from pathlib import Path

import cairosvg

ROOT = Path(__file__).resolve().parents[1]
SVG = ROOT / "store-assets" / "icon.svg"
OUT = ROOT / "store-assets" / "play-store-icon-512.png"


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(
        url=str(SVG),
        write_to=str(OUT),
        output_width=512,
        output_height=512,
    )
    size = OUT.stat().st_size
    print(f"Wrote {OUT} ({size:,} bytes)")


if __name__ == "__main__":
    main()
