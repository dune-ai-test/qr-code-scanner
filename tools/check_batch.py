"""Mirror of the batch geometry and payload rules.

Same reasoning as the other mirrors: the Kotlin tests are only run by CI, and
the failure mode of the layout is arithmetic — a placement off the edge of the
bitmap throws from Canvas.drawBitmap on a device rather than from a test.

Keep in step with QrContactSheet.kt and BatchPayloads.kt. A divergence here is
not a build error, so the file is short on purpose.
"""

import re
import sys

MAX_ITEMS = 60
MAX_COLUMNS = 4
CELL_WIDTH = 520
PADDING = 48
LABEL_HEIGHT = 104

SCHEME = re.compile(r"[a-zA-Z][a-zA-Z0-9+.\-]*\Z")

FAILURES = []


def options_for(count):
    safe = max(0, min(count, MAX_ITEMS))
    if safe <= 1:
        columns = 1
    elif safe <= 4:
        columns = 2
    elif safe <= 9:
        columns = 3
    else:
        columns = MAX_COLUMNS
    rows = 0 if safe == 0 else -(-safe // columns)
    code_size = CELL_WIDTH - PADDING
    return {
        "count": safe,
        "columns": columns,
        "rows": rows,
        "cellWidth": CELL_WIDTH,
        "cellHeight": code_size + LABEL_HEIGHT,
        "codeSize": code_size,
        "padding": PADDING,
        "width": PADDING * 2 + columns * CELL_WIDTH,
        "height": PADDING * 2 + rows * (code_size + LABEL_HEIGHT),
    }


def placements(o):
    out = []
    for index in range(o["count"]):
        row = index // o["columns"]
        column = index % o["columns"]
        cell_left = o["padding"] + column * o["cellWidth"]
        cell_top = o["padding"] + row * o["cellHeight"]
        inset = o["padding"] // 2
        out.append({
            "index": index,
            "codeLeft": cell_left + inset,
            "codeTop": cell_top,
            "codeSize": o["codeSize"],
            "labelLeft": cell_left + inset,
            "labelTop": cell_top + o["codeSize"],
            "labelWidth": o["codeSize"],
        })
    return out


def looks_like_domain(line):
    if any(ch.isspace() for ch in line):
        return False
    host = line.split("/")[0].split("?")[0]
    dot = host.rfind(".")
    return 0 < dot < len(host) - 1


def normalise(line):
    if "://" in line:
        return line
    colon = line.find(":")
    if colon > 0 and SCHEME.match(line[:colon]):
        return line
    return "https://" + line if looks_like_domain(line) else line


def parse(text):
    seen = set()
    items = []
    for line in text.splitlines():
        trimmed = line.strip()
        if not trimmed:
            continue
        content = normalise(trimmed)
        if content in seen:
            continue
        seen.add(content)
        items.append(content)
        if len(items) == MAX_ITEMS:
            break
    return items


def check(name, condition, detail=""):
    if not condition:
        FAILURES.append(name + ((": " + detail) if detail else ""))


def eq(name, actual, expected):
    check(name, actual == expected, "expected %r, got %r" % (expected, actual))


def run():
    # ------------------------------------------------------------- geometry
    eq("no codes: rows", options_for(0)["rows"], 0)
    eq("one: columns", options_for(1)["columns"], 1)
    eq("one: rows", options_for(1)["rows"], 1)
    eq("two: columns", options_for(2)["columns"], 2)
    eq("four: columns", options_for(4)["columns"], 2)
    eq("five: columns", options_for(5)["columns"], 3)
    eq("nine: columns", options_for(9)["columns"], 3)
    eq("ten: columns", options_for(10)["columns"], MAX_COLUMNS)
    eq("two: rows", options_for(2)["rows"], 1)
    eq("four: rows", options_for(4)["rows"], 2)
    eq("five: rows", options_for(5)["rows"], 2)
    eq("ten: rows", options_for(10)["rows"], 3)
    eq("capped count", options_for(500)["count"], MAX_ITEMS)
    check("five is not full", options_for(5)["count"] != options_for(5)["columns"] * options_for(5)["rows"])
    eq("six is full", options_for(6)["count"], options_for(6)["columns"] * options_for(6)["rows"])
    check("one grows wider", options_for(20)["width"] > options_for(1)["width"])
    check("one grows taller", options_for(20)["height"] > options_for(1)["height"])

    for count in [1, 2, 3, 5, 7, 9, 11, 17, 40, 60]:
        o = options_for(count)
        places = placements(o)
        eq("placement count for %d" % count, len(places), count)
        for p in places:
            inside = (p["codeLeft"] >= 0 and p["codeTop"] >= 0 and
                      p["codeLeft"] + p["codeSize"] <= o["width"] and
                      p["codeTop"] + p["codeSize"] + o["cellHeight"] - o["codeSize"] <= o["height"])
            check("code %d inside (%d codes)" % (p["index"], count), inside,
                  "%d,%d %d in %dx%d" % (p["codeLeft"], p["codeTop"], p["codeSize"], o["width"], o["height"]))
            # The label is part of the cell too. Without this, a cellHeight
            # that forgets LABEL_HEIGHT clips every caption and passes.
            label_inside = (p["labelLeft"] + p["labelWidth"] <= o["width"] and
                            p["labelTop"] + LABEL_HEIGHT <= o["height"])
            check("label %d inside (%d codes)" % (p["index"], count), label_inside,
                  "top=%d needs %d, height=%d" % (p["labelTop"], p["labelTop"] + LABEL_HEIGHT, o["height"]))

    o = options_for(12)
    places = placements(o)
    for i in range(len(places)):
        for j in range(i + 1, len(places)):
            a, b = places[i], places[j]
            separated = (a["codeLeft"] + a["codeSize"] <= b["codeLeft"] or
                         b["codeLeft"] + b["codeSize"] <= a["codeLeft"] or
                         a["codeTop"] + a["codeSize"] <= b["codeTop"] or
                         b["codeTop"] + b["codeSize"] <= a["codeTop"])
            check("no overlap %d/%d" % (a["index"], b["index"]), separated)

    # ------------------------------------------------------------- payloads
    eq("two links", parse("https://a.example\nhttps://b.example"),
       ["https://a.example", "https://b.example"])
    eq("blank lines dropped",
       parse("\n  https://a.example  \n\n\n  https://b.example \n"),
       ["https://a.example", "https://b.example"])
    eq("empty paste", parse(""), [])
    eq("spaces only", parse("   \n  \n"), [])
    eq("bare domain", parse("example.com"), ["https://example.com"])
    eq("domain with path", parse("example.com/a"), ["https://example.com/a"])
    eq("domain with query", parse("example.com/a?b=c"), ["https://example.com/a?b=c"])
    eq("geo untouched", parse("geo:51.5,-0.12"), ["geo:51.5,-0.12"])
    eq("mailto untouched", parse("mailto:a@b.com"), ["mailto:a@b.com"])
    eq("tel untouched", parse("tel:+15551234567"), ["tel:+15551234567"])
    eq("sms untouched", parse("sms:+15551234567?body=hi"), ["sms:+15551234567?body=hi"])
    eq("http untouched", parse("http://a.example"), ["http://a.example"])
    eq("https untouched", parse("https://a.example"), ["https://a.example"])
    eq("wifi untouched", parse("WIFI:T:WPA;S:Home;P:secret;;"),
       ["WIFI:T:WPA;S:Home;P:secret;;"])
    eq("text with colon", parse("Hello: world"), ["Hello: world"])
    eq("sentence", parse("Buy milk today"), ["Buy milk today"])
    # No way to tell "notes.txt" from "example.com" without a public-suffix
    # list; the link reading wins and is visible on the sheet.
    eq("dotted token", parse("notes.txt"), ["https://notes.txt"])
    eq("duplicates", parse("https://a.example\nhttps://b.example\nhttps://a.example"),
       ["https://a.example", "https://b.example"])
    eq("duplicate after normalisation", parse("example.com\nhttps://example.com"),
       ["https://example.com"])
    eq("capped", len(parse("\n".join("https://site%d.example" % i for i in range(1, 81)))), MAX_ITEMS)

    return FAILURES


if __name__ == "__main__":
    failed = run()
    if failed:
        print("FAILED:")
        for name in failed:
            print("  -", name)
        sys.exit(1)
    print("batch mirror: all cases pass")