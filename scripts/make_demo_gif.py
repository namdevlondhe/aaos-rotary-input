#!/usr/bin/env python3
"""Turns the frames rendered by DemoFramesTest into docs/demo.gif.

Usage:  ./gradlew :sample:recordRoborazziDebug && python3 scripts/make_demo_gif.py
"""
import glob
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRAMES = sorted(glob.glob(os.path.join(ROOT, "sample", "build", "demo-frames", "frame-*.png")))
OUT = os.path.join(ROOT, "docs", "demo.gif")
WIDTH = 960
FRAME_MS = 1100

if not FRAMES:
    sys.exit("No frames found. Run ./gradlew :sample:recordRoborazziDebug first.")

images = []
for path in FRAMES:
    image = Image.open(path).convert("RGB")
    height = round(image.height * WIDTH / image.width)
    images.append(image.resize((WIDTH, height), Image.LANCZOS))

# One shared palette keeps colours stable between frames and the file small.
palette = images[0].quantize(colors=128, method=Image.MEDIANCUT)
frames = [image.quantize(palette=palette, dither=Image.NONE) for image in images]
durations = [FRAME_MS] * len(frames)
durations[0] = durations[-1] = FRAME_MS * 2

frames[0].save(OUT, save_all=True, append_images=frames[1:], duration=durations, loop=0, optimize=True)
print(f"{OUT}: {len(frames)} frames, {os.path.getsize(OUT) // 1024} KB")
