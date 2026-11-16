#!/usr/bin/env python3
"""Regenerate app/src/main/java/com/bobek/tuner/ui/theme/OmarchyPalettes.kt from Omarchy's themes.

Usage (from the repository root, Python 3.11+):
    git clone --depth 1 https://github.com/omacom/omarchy /tmp/omarchy
    head -17 app/src/main/java/com/bobek/tuner/ui/theme/Color.kt > /tmp/header.kt
    python3 scripts/generate_omarchy_palettes.py /tmp/omarchy/themes \
        app/src/main/java/com/bobek/tuner/ui/theme/OmarchyPalettes.kt /tmp/header.kt
"""
import sys, tomllib, pathlib
themes_dir = pathlib.Path(sys.argv[1]); out = pathlib.Path(sys.argv[2]); header = pathlib.Path(sys.argv[3]).read_text()
NAMES = {"rose-pine": "Rosé Pine", "retro-82": "Retro 82"}
KEYS = ["accent","selection","muted","background","dark_background","lighter_background",
        "foreground","dark_foreground","light_foreground","red","yellow","green","cyan","blue","magenta"]
def camel(k): p=k.split("_"); return p[0]+"".join(x.title() for x in p[1:])
def col(v): return "Color(0xFF%s)" % v.lstrip("#").upper()
entries=[]
for d in sorted(themes_dir.iterdir()):
    f = d/"colors.toml"
    if not f.exists(): continue
    t = tomllib.loads(f.read_text())
    name = NAMES.get(d.name, " ".join(w.title() for w in d.name.split("-")))
    args = [f'id = "{d.name}"', f'displayName = "{name}"', f'isDark = {str(t["mode"]=="dark").lower()}']
    args += [f"{camel(k)} = {col(t[k])}" for k in KEYS]
    entries.append("    OmarchyPalette(\n" + ",\n".join("        "+a for a in args) + "\n    )")
out.write_text(header + """
package com.bobek.tuner.ui.theme

import androidx.compose.ui.graphics.Color

// GENERATED from the colors.toml of each theme in https://github.com/omacom/omarchy (themes/, MIT License,
// Copyright (c) David Heinemeier Hansson). Regenerate rather than edit by hand.

val OMARCHY_PALETTES: List<OmarchyPalette> = listOf(
""" + ",\n".join(entries) + "\n)\n")
print(len(entries))
