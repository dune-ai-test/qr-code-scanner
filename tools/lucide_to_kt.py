"""Convert downloaded Lucide SVGs into a Kotlin file of stroke-only ImageVectors.

Lucide is stroke-only on a 24x24 grid, which maps cleanly onto Compose's
PathBuilder. Shapes other than <path> (circle/rect/line/polyline) are first
rewritten as SVG path data, then the whole `d` string is parsed into absolute
Compose calls.
"""

import os
import re
import sys

SRC = sys.argv[1] if len(sys.argv) > 1 else "/tmp/lucide"
OUT = sys.argv[2] if len(sys.argv) > 2 else "LucideIcons.kt"

# ---------------------------------------------------------------- primitives

def fmt(v):
    v = round(v + 0.0, 4)
    if v == int(v):
        return f"{int(v)}f"
    return f"{v}f"


def circle_to_path(cx, cy, r):
    return (f"M {fmt(cx - r)} {fmt(cy)} "
            f"a {fmt(r)} {fmt(r)} 0 1 0 {fmt(2 * r)} 0 "
            f"a {fmt(r)} {fmt(r)} 0 1 0 {fmt(-2 * r)} 0")


def ellipse_to_path(cx, cy, rx, ry):
    return (f"M {fmt(cx - rx)} {fmt(cy)} "
            f"a {fmt(rx)} {fmt(ry)} 0 1 0 {fmt(2 * rx)} 0 "
            f"a {fmt(rx)} {fmt(ry)} 0 1 0 {fmt(-2 * rx)} 0")


def rect_to_path(x, y, w, h, rx):
    rx = min(rx, w / 2, h / 2) if rx else 0
    if rx <= 0:
        return (f"M {fmt(x)} {fmt(y)} L {fmt(x + w)} {fmt(y)} "
                f"L {fmt(x + w)} {fmt(y + h)} L {fmt(x)} {fmt(y + h)} Z")
    return (f"M {fmt(x + rx)} {fmt(y)} "
            f"L {fmt(x + w - rx)} {fmt(y)} "
            f"A {fmt(rx)} {fmt(rx)} 0 0 1 {fmt(x + w)} {fmt(y + rx)} "
            f"L {fmt(x + w)} {fmt(y + h - rx)} "
            f"A {fmt(rx)} {fmt(rx)} 0 0 1 {fmt(x + w - rx)} {fmt(y + h)} "
            f"L {fmt(x + rx)} {fmt(y + h)} "
            f"A {fmt(rx)} {fmt(rx)} 0 0 1 {fmt(x)} {fmt(y + h - rx)} "
            f"L {fmt(x)} {fmt(y + rx)} "
            f"A {fmt(rx)} {fmt(rx)} 0 0 1 {fmt(x + rx)} {fmt(y)} Z")


def points_to_path(points, close=False):
    nums = [float(n) for n in re.split(r"[,\s]+", points.strip()) if n]
    pairs = list(zip(nums[0::2], nums[1::2]))
    if not pairs:
        return ""
    out = f"M {fmt(pairs[0][0])} {fmt(pairs[0][1])}"
    for x, y in pairs[1:]:
        out += f" L {fmt(x)} {fmt(y)}"
    return out + (" Z" if close else "")


ATTR = re.compile(r'([a-zA-Z][a-zA-Z0-9-]*)\s*=\s*"([^"]*)"')

SHAPES = re.compile(
    r"<(path|circle|ellipse|rect|line|polyline|polygon)\b([^>]*?)/?>",
    re.S,
)


def svg_to_d(svg_text):
    parts = []
    for tag, attrs in SHAPES.findall(svg_text):
        a = dict(ATTR.findall(attrs))
        if tag == "path":
            parts.append(a.get("d", ""))
        elif tag == "circle":
            parts.append(circle_to_path(float(a["cx"]), float(a["cy"]), float(a["r"])))
        elif tag == "ellipse":
            parts.append(ellipse_to_path(float(a["cx"]), float(a["cy"]),
                                         float(a["rx"]), float(a["ry"])))
        elif tag == "rect":
            parts.append(rect_to_path(float(a.get("x", 0)), float(a.get("y", 0)),
                                      float(a["width"]), float(a["height"]),
                                      float(a.get("rx", 0) or 0)))
        elif tag == "line":
            parts.append(f"M {fmt(float(a['x1']))} {fmt(float(a['y1']))} "
                         f"L {fmt(float(a['x2']))} {fmt(float(a['y2']))}")
        elif tag == "polyline":
            parts.append(points_to_path(a.get("points", "")))
        elif tag == "polygon":
            parts.append(points_to_path(a.get("points", ""), close=True))
    return parts


# ------------------------------------------------------------- path parsing

NUM = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?")
CMD = re.compile(r"([MmLlHhVvCcSsQqTtAaZz])")
ARG_COUNT = {"M": 2, "L": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "T": 2, "A": 7, "Z": 0}


def parse_path(d):
    """Yield Compose builder calls for one SVG path string, all absolute."""
    tokens = CMD.split(d)
    out = []
    i = 1
    cx = cy = 0.0          # current point
    sx = sy = 0.0          # subpath start
    prev_cubic_ctrl = None  # for S
    prev_quad_ctrl = None   # for T
    prev_cmd = None

    def nums(s):
        return [float(n) for n in NUM.findall(s)]

    while i < len(tokens):
        cmd = tokens[i].strip()
        args_blob = tokens[i + 1] if i + 1 < len(tokens) else ""
        i += 2
        if not cmd:
            if not args_blob.strip():
                continue
            # Implicit repeat of the previous command.
            cmd = prev_cmd
            if cmd is None:
                continue
            args_blob = cmd + args_blob if cmd.isupper() else cmd + args_blob

        upper = cmd.upper()
        rel = cmd.islower()
        a = nums(args_blob)
        count = ARG_COUNT.get(upper)
        if count is None:
            continue

        idx = 0
        first = True

        # Z takes no arguments, so it can never be reached from the loop below.
        if upper == "Z":
            out.append("close()")
            cx, cy = sx, sy
            prev_cubic_ctrl = prev_quad_ctrl = None
            prev_cmd = cmd
            continue

        while idx < len(a):
            chunk = a[idx:idx + count]
            idx += count
            if len(chunk) < count:
                break

            if upper == "M":
                x, y = chunk
                if rel and not first:
                    x, y = cx + x, cy + y
                elif rel:
                    x, y = cx + x, cy + y
                out.append(f"moveTo({fmt(x)}, {fmt(y)})")
                cx, cy = x, y
                sx, sy = x, y
                # Subsequent pairs are implicit lineto.
                upper, count = "L", 2
                prev_cubic_ctrl = prev_quad_ctrl = None
            elif upper == "L":
                x, y = chunk
                if rel:
                    x, y = cx + x, cy + y
                out.append(f"lineTo({fmt(x)}, {fmt(y)})")
                cx, cy = x, y
                prev_cubic_ctrl = prev_quad_ctrl = None
            elif upper == "H":
                x = cx + chunk[0] if rel else chunk[0]
                out.append(f"horizontalLineTo({fmt(x)})")
                cx = x
                prev_cubic_ctrl = prev_quad_ctrl = None
            elif upper == "V":
                y = cy + chunk[0] if rel else chunk[0]
                out.append(f"verticalLineTo({fmt(y)})")
                cy = y
                prev_cubic_ctrl = prev_quad_ctrl = None
            elif upper == "C":
                x1, y1, x2, y2, x, y = chunk
                if rel:
                    x1, y1, x2, y2, x, y = cx + x1, cy + y1, cx + x2, cy + y2, cx + x, cy + y
                out.append(f"curveTo({fmt(x1)}, {fmt(y1)}, {fmt(x2)}, {fmt(y2)}, {fmt(x)}, {fmt(y)})")
                prev_cubic_ctrl = (x2, y2)
                prev_quad_ctrl = None
                cx, cy = x, y
            elif upper == "S":
                x2, y2, x, y = chunk
                if rel:
                    x2, y2, x, y = cx + x2, cy + y2, cx + x, cy + y
                out.append(f"reflectiveCurveTo({fmt(x2)}, {fmt(y2)}, {fmt(x)}, {fmt(y)})")
                prev_cubic_ctrl = (x2, y2)
                prev_quad_ctrl = None
                cx, cy = x, y
            elif upper == "Q":
                x1, y1, x, y = chunk
                if rel:
                    x1, y1, x, y = cx + x1, cy + y1, cx + x, cy + y
                out.append(f"quadTo({fmt(x1)}, {fmt(y1)}, {fmt(x)}, {fmt(y)})")
                prev_quad_ctrl = (x1, y1)
                prev_cubic_ctrl = None
                cx, cy = x, y
            elif upper == "T":
                x, y = chunk
                if rel:
                    x, y = cx + x, cy + y
                out.append(f"reflectQuadTo({fmt(x)}, {fmt(y)})")
                prev_quad_ctrl = None
                prev_cubic_ctrl = None
                cx, cy = x, y
            elif upper == "A":
                rx, ry, rot, large, sweep, x, y = chunk
                if rel:
                    x, y = cx + x, cy + y
                out.append(
                    f"arcTo({fmt(rx)}, {fmt(ry)}, {fmt(rot)}, "
                    f"{'true' if large >= 0.5 else 'false'}, "
                    f"{'true' if sweep == 1 else 'false'}, {fmt(x)}, {fmt(y)})"
                )
                cx, cy = x, y
                prev_cubic_ctrl = prev_quad_ctrl = None

            first = False
        prev_cmd = cmd
    return out


def pascal(kebab):
    return "Lucide" + "".join(p[:1].upper() + p[1:] for p in kebab.split("-"))


HEADER = '''package com.quickscan.core.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/*
 * Lucide v1.54.0 (ISC License, https://lucide.dev). Path data is taken verbatim
 * from the published 24x24 SVGs and emitted as stroke-only ImageVectors, so
 * Icon() tints them the way the mockups tint their inlined SVGs. Stroke width 2,
 * round caps and joins are Lucide's defaults.
 *
 * Generated by tools/lucide_to_kt.py - do not edit by hand.
 */

private fun lucide(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathBuilder().apply(block).nodes,
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
'''

ICON_NAMES = [
    "arrow-right", "bell", "bookmark", "camera", "check", "chevron-down",
    "chevron-left", "chevron-right", "circle-x", "clipboard-check",
    "clipboard-paste", "copy", "download", "ellipsis", "external-link", "eye",
    "eye-off", "globe", "hard-drive", "history", "image", "info", "link",
    "lock", "mail", "moon", "palette", "plus", "qr-code", "repeat", "scan-line",
    "scan-search", "search", "settings", "share-2", "shield-check", "signal",
    "smartphone", "sparkles", "trash-2", "type", "user", "video", "volume-2",
    "wifi", "wifi-off", "zap",
    # Added later for selection controls; Lucide spells these square-check,
    # x and bookmark-check, not check-square or bookmark-check-alt.
    "square-check", "x", "bookmark-check",
    # Deep-link result types: a place, a phone number, a message, an email.
    "map-pin", "phone", "message-square",
    # Batch generate: a sheet is a grid, not a list.
    "layout-grid",
]


def main():
    chunks = [HEADER]
    for kebab in ICON_NAMES:
        path = os.path.join(SRC, kebab + ".svg")
        if not os.path.exists(path):
            print(f"missing: {kebab}", file=sys.stderr)
            continue
        svg = open(path, encoding="utf-8").read()
        calls = []
        for d in svg_to_d(svg):
            calls.extend(parse_path(d))
        prop = pascal(kebab)
        body = "\n".join(f"    {c}" for c in calls)
        chunks.append(f"\nval {prop} = lucide(\"{prop[6:]}\") {{\n{body}\n}}\n")

    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(chunks))
    print(f"wrote {OUT} with {len(ICON_NAMES)} icons")


if __name__ == "__main__":
    main()