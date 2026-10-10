"""Mirror of FinderPatternScan, so the run-length state machine can be run locally.

Same reasoning as check_deeplink.py: the geo branches were wrong twice and
tracing them by eye did not catch it. This builds synthetic scanlines with a
known 1:1:3:1:1 signature and asserts the classification, including that the
state machine survives a near miss and a row that begins in light space.

Keep in step with FinderPatternScan.kt. A divergence here is not a build
error, so the file is short on purpose.
"""

import sys

DARK = 128
MIN_WIDTH = 21
FULL_CODE_PATTERNS = 3
CLUSTER_TOLERANCE = 12

NOTHING, PARTLY, FRAMED = "Nothing", "Partly", "Framed"

FAILURES = []


def luminance(value):
    return value & 0xFF


def finder_hits(row, width):
    hits = []
    counts = [0] * 5
    state = 0
    start = 0

    x = 0
    while x < width and luminance(row[x]) >= DARK:
        x += 1

    while x < width:
        if luminance(row[x]) < DARK:
            if (state & 1) == 1:
                state += 1
            counts[state] += 1
        elif (state & 1) == 0:
            if state == 4:
                if is_finder_ratio(counts):
                    hits.append((start + counts[0] + counts[1] + counts[2] // 2,
                                 sum(counts)))
                    state = 0
                    counts = [0] * 5
                else:
                    start += counts[0] + counts[1]
                    state = 3
                    counts = [counts[2], counts[3], counts[4], 1, 0]
            else:
                if state == 0:
                    start = x - counts[0]
                state += 1
                counts[state] += 1
        else:
            counts[state] += 1
        x += 1
    return hits


def is_finder_ratio(counts):
    total = sum(counts)
    if any(c == 0 for c in counts):
        return False
    if total < 7:
        return False
    module = total / 7.0
    tolerance = module / 2.0
    return (
        abs(module - counts[0]) < tolerance
        and abs(module - counts[1]) < tolerance
        and abs(3 * module - counts[2]) < tolerance
        and abs(module - counts[3]) < tolerance
        and abs(module - counts[4]) < tolerance
    )


def cluster(hits):
    if not hits:
        return 0
    ordered = sorted(hits)
    clusters = 1
    previous = ordered[0]
    for hit in ordered[1:]:
        tolerance = max(CLUSTER_TOLERANCE, previous[1])
        if hit[0] - previous[0] > tolerance:
            clusters += 1
        previous = hit
    return clusters


def scan(rows, width):
    if width < MIN_WIDTH or not rows:
        return NOTHING
    hits = []
    for row in rows:
        hits += finder_hits(row, width)
    if not hits:
        return NOTHING
    return FRAMED if cluster(hits) >= FULL_CODE_PATTERNS else PARTLY


# --------------------------------------------------------------- synthetic rows

LIGHT = 230
BLACK = 20


def blank(width, value=LIGHT):
    return [value] * width


def finder_run(module, at, width, value=None):
    """A 1:1:3:1:1 run starting at column `at`."""
    row = blank(width) if value is None else list(value)
    pattern = (
        [BLACK] * module + [LIGHT] * module + [BLACK] * (3 * module) +
        [LIGHT] * module + [BLACK] * module
    )
    for i, v in enumerate(pattern):
        row[at + i] = v
    return row


def check(name, actual, expected):
    if actual != expected:
        FAILURES.append("%s: expected %s, got %s" % (name, expected, actual))


def run():
    W = 300

    check("empty frame", scan([blank(W)] * 4, W), NOTHING)
    check("all dark", scan([blank(W, BLACK)] * 4, W), NOTHING)
    check("too narrow", scan([blank(10)], 10), NOTHING)
    check("no rows", scan([], W), NOTHING)

    one = [finder_run(4, 40, W)] * 4
    check("one pattern", scan(one, W), PARTLY)

    # A run of the right shape but the wrong ratio must not register. Without
    # this the ratio check could be loosened to anything and the rest would
    # still pass.
    wrong = []
    for _ in range(4):
        row = blank(W)
        row[40:44] = [BLACK] * 4
        row[44:48] = [LIGHT] * 4
        row[48:56] = [BLACK] * 8      # centre should be three modules
        row[56:60] = [LIGHT] * 4
        row[60:64] = [BLACK] * 4
        wrong.append(row)
    check("wrong ratio", scan(wrong, W), NOTHING)

    # Half the required contrast: drift of more than half a module.
    drifted = []
    for _ in range(4):
        row = blank(W)
        row[40:52] = [BLACK] * 12
        row[52:56] = [LIGHT] * 4
        row[56:76] = [BLACK] * 20
        row[76:80] = [LIGHT] * 4
        row[80:92] = [BLACK] * 12
        drifted.append(row)
    check("too much drift", scan(drifted, W), NOTHING)

    two = [finder_run(4, 40, W) + finder_run(4, 200, W)[40:]] * 4
    two = []
    for _ in range(4):
        row = finder_run(4, 40, W)
        other = finder_run(4, 200, W)
        for i in range(200, 200 + 28):
            row[i] = other[i]
        two.append(row)
    check("two patterns", scan(two, W), PARTLY)

    three = []
    for _ in range(4):
        row = finder_run(4, 30, W)
        for start in (140, 240):
            other = finder_run(4, start, W)
            for i in range(start, start + 28):
                row[i] = other[i]
        three.append(row)
    check("three patterns", scan(three, W), FRAMED)

    # A code tilted across scanlines drifts; the same pattern must still
    # count once, not once per line.
    drifting = []
    for offset in (0, 9, 18):
        row = finder_run(4, 100 + offset, W)
        row2 = finder_run(4, 220 + offset, W)
        for i in range(220 + offset, 220 + offset + 28):
            row[i] = row2[i]
        drifting.append(row)
    drifting.append(finder_run(4, 130, W))
    for start in (30,):
        other = finder_run(4, start, W)
        for i in range(start, start + 28):
            drifting[-1][i] = other[i]
    check("tilted code", scan(drifting, W), FRAMED)

    # A near miss: a run whose ratio is wrong by more than half a module. The
    # state machine must shift left and carry on rather than abandoning the
    # row, so the valid patterns after it are still found.
    noisy = []
    for _ in range(4):
        row = blank(W)
        row[100:120] = [BLACK] * 20         # one long dark run: not a pattern
        for start in (40, 200):
            other = finder_run(4, start, W)
            for i in range(start, start + 28):
                row[i] = other[i]
        noisy.append(row)
    check("recovers past a bad run", scan(noisy, W), PARTLY)

    # Three valid patterns with the bad run between them. If the bad run
    # derailed the state machine this would fall back to Partly.
    three_around_noise = []
    for _ in range(4):
        row = blank(W)
        row[100:120] = [BLACK] * 20
        for start in (30, 160, 250):
            other = finder_run(4, start, W)
            for i in range(start, start + 28):
                row[i] = other[i]
        three_around_noise.append(row)
    check("three patterns around a bad run", scan(three_around_noise, W), FRAMED)

    # A large module size: the pattern is 56px wide and has to cluster
    # without merging with its neighbour.
    big = []
    for _ in range(4):
        row = blank(W)
        for start in (20, 110, 200):
            other = finder_run(8, start, W)
            for i in range(start, start + 56):
                row[i] = other[i]
        big.append(row)
    check("large module size", scan(big, W), FRAMED)

    # Row beginning in light space must still find the pattern.
    lead = [LIGHT] * 25 + finder_run(4, 40, W)[25:]
    check("leading light", scan([lead] * 4, W), PARTLY)

    # Module size that is not a round number of pixels.
    odd = [finder_run(3, 50, W)] * 4
    check("odd module size", scan(odd, W), PARTLY)

    # Noise that is not a pattern at all.
    noise = []
    for line in range(4):
        row = []
        for x in range(W):
            row.append(BLACK if (x * 7 + line * 13) % 11 < 3 else LIGHT)
        noise.append(row)
    # Noise may or may not trip the ratio; it must never claim Framed.
    if scan(noise, W) == FRAMED:
        FAILURES.append("noise reported Framed")

    return FAILURES


if __name__ == "__main__":
    failed = run()
    if failed:
        print("FAILED:")
        for name in failed:
            print("  -", name)
        sys.exit(1)
    print("confidence mirror: all cases pass")