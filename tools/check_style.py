"""Mirror of QrStyleCodec.

The round trip is what makes "a saved code looks the same everywhere" true, and
a throw inside decode would take a result screen down rather than falling back
to a plain code. That is the failure worth executing rather than reading.

Keep in step with QrStyleCodec.kt. A divergence here is not a build error, so
the file is short on purpose.
"""

import sys

DEFAULT_FOREGROUND = 0xFF111318
DEFAULT_BACKGROUND = 0xFFFFFFFF
NONE = "none"
MAX_RADIUS = 0.5
LOGOS = ["None", "App", "Link", "Wifi", "Contact", "Plain"]

FAILURES = []


def to_signed(value):
    value &= 0xFFFFFFFF
    return value - 0x100000000 if value >= 0x80000000 else value


def hex_colour(value):
    return "#%08X" % (value & 0xFFFFFFFF)


def encode(foreground, background, corner_radius, logo):
    return "%s,%s,%s,%s" % (
        hex_colour(foreground),
        hex_colour(background),
        repr_float(corner_radius),
        (logo or NONE).lower(),
    )


def repr_float(value):
    # Kotlin's Float.toString is the shortest form that round-trips.
    for text in ("%.9g" % value, "%.7g" % value, "%.6g" % value,
                 "%.5g" % value, "%.4g" % value, "%.3g" % value):
        if float(text) == value:
            return text
    return repr(value)


def parse_colour(value):
    digits = value[1:] if value.startswith("#") else value
    if len(digits) not in (6, 8):
        return None
    try:
        parsed = int(digits, 16)
    except ValueError:
        return None
    parsed &= 0xFFFFFFFF
    if len(digits) == 6:
        parsed |= 0xFF000000
    return to_signed(parsed)


def decode(value):
    # Kotlin Ints are signed; the expectations have to match what decode
    # actually returns, not what the hex literal looks like.
    default = (to_signed(DEFAULT_FOREGROUND), to_signed(DEFAULT_BACKGROUND), 0.0, None)
    if value is None or not value.strip():
        return default
    parts = value.split(",")
    if len(parts) < 4:
        return default
    foreground = parse_colour(parts[0])
    if foreground is None:
        return default
    background = parse_colour(parts[1])
    if background is None:
        return default
    try:
        radius = float(parts[2])
    except ValueError:
        return default
    if parts[3] == NONE:
        logo = None
    else:
        match = next((n for n in LOGOS if n.lower() == parts[3].lower()), None)
        if match is None:
            return default
        logo = None if match == "None" else match
    return (foreground, background, max(0.0, min(MAX_RADIUS, radius)), logo)


def check(name, condition, detail=""):
    if not condition:
        FAILURES.append(name + ((": " + detail) if detail else ""))


def eq(name, actual, expected):
    check(name, actual == expected, "expected %r, got %r" % (expected, actual))


def run():
    default = (to_signed(DEFAULT_FOREGROUND), to_signed(DEFAULT_BACKGROUND), 0.0, None)
    eq("default round trip", decode(encode(*default)), default)

    styled = (to_signed(0xFF3D8FD1), to_signed(0xFFF4F5F7), 0.3, "Link")
    eq("styled round trip", decode(encode(*styled)), styled)

    for logo in LOGOS:
        got = decode(encode(DEFAULT_FOREGROUND, DEFAULT_BACKGROUND, 0.0, logo))
        want = None if logo == "None" else logo
        eq("logo " + logo, got[3], want)

    eq("null is plain", decode(None), default)
    eq("empty is plain", decode(""), default)
    eq("spaces are plain", decode("   "), default)
    eq("truncated is plain", decode("#FF000000,#FFFFFF"), default)
    eq("nonsense is plain", decode("nonsense"), default)
    eq("bad hex is plain", decode("#ZZZZZZZZ,#FFFFFFFF,0.0,none"), default)
    eq("short colour is plain", decode("#FF00,#FFFFFFFF,0.0,none"), default)
    eq("unknown logo is plain", decode("#FF000000,#FFFFFFFF,0.0,sparkly"), default)
    eq("bad radius is plain", decode("#FF000000,#FFFFFFFF,abc,none"), default)

    eq("six digits are opaque", decode("#111318,#FFFFFF,0.0,none")[0], to_signed(0xFF111318))
    eq("eight digits keep alpha", decode("#80FF0000,#FFFFFFFF,0.0,none")[0],
       to_signed(0x80FF0000))

    clamped = decode("#FF3D8FD1,#FFFFFFFF,9.0,none")
    eq("huge radius keeps colour", clamped[0], to_signed(0xFF3D8FD1))
    eq("huge radius clamped", clamped[2], MAX_RADIUS)
    eq("negative radius clamped", decode("#FF000000,#FFFFFFFF,-2.0,none")[2], 0.0)

    eq("encoded form",
       encode(0xFF3D8FD1, DEFAULT_BACKGROUND, 0.3, "Link"),
       "#FF3D8FD1,#FFFFFFFF,0.3,link")

    eq("logo case insensitive", decode("#FF000000,#FFFFFFFF,0.0,LINK")[3], "Link")
    return FAILURES


if __name__ == "__main__":
    failed = run()
    if failed:
        print("FAILED:")
        for name in failed:
            print("  -", name)
        sys.exit(1)
    print("style mirror: all cases pass")