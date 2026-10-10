"""Catch resource mistakes aapt would otherwise report.

Aapt is unforgiving about string resources: a bare apostrophe, a bare double
quote, or a format placeholder that does not line up with the arguments all
fail the whole values.xml compile, which is a confusing error to debug from a
Gradle stack trace.
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

APOS = chr(39)
QUOTE = chr(34)

problems = []

for path in glob.glob('app/src/main/res/**/*.xml', recursive=True):
    try:
        ET.parse(path)
    except Exception as exc:
        problems.append("%s: not well-formed: %s" % (path, exc))

strings = open('app/src/main/res/values/strings.xml', encoding='utf-8').read()
ENTITIES = {
    '&apos;': APOS, '&#39;': APOS,
    '&quot;': QUOTE, '&amp;': '&', '&lt;': '<', '&gt;': '>',
    '&nbsp;': ' ',
}


def flatten(body):
    """Resolve entities the way aapt does before it validates the value."""
    out = body
    for entity, ch in ENTITIES.items():
        out = out.replace(entity, ch)
    return out


for m in re.finditer(r'<string name="(\w+)"[^>]*>(.*?)</string>', strings, re.S):
    name, raw = m.group(1), m.group(2)
    body = flatten(raw)

    # aapt flattens the XML first, so an entity like &apos; becomes a bare
    # apostrophe and is then rejected. The only accepted forms are a backslash
    # escape or a fully quoted value.
    bare_apostrophes = [
        i for i, c in enumerate(body)
        if c == APOS and (i == 0 or body[i - 1] != chr(92))
    ]
    quoted = body.startswith(QUOTE) and body.endswith(QUOTE)
    if bare_apostrophes and not quoted:
        problems.append(
            "string %s: bare apostrophe after flattening (%r); "
            "use a backslash escape" % (name, raw)
        )

    if QUOTE in body and not quoted:
        problems.append("string %s: bare double quote" % name)

    # Positional arguments only: aapt will not accept %%1$s without numbering,
    # and a stray % that is not an argument breaks the flatten.
    stripped = body
    for tm in re.finditer(r'%(\d+\$[sd])', stripped):
        pass
    remainder = re.sub(r'%\d+\$[sd]', '', stripped)
    if re.search(r'%[^%]', remainder):
        problems.append("string %s: unnumbered or malformed format specifier" % name)
    for tm in re.finditer(r'%\d+\$([^sd])', stripped):
        problems.append("string %s: unsupported format type %%%s"
                        % (name, tm.group(1)))

for path in glob.glob('app/src/main/res/drawable/*.xml'):
    text = open(path, encoding='utf-8').read()
    for data in re.findall(r'android:pathData="([^"]*)"', text):
        stripped = re.sub(r'[MLHVAZmlhvaz]', '', data)
        if re.search(r'[a-zA-Z]', stripped):
            problems.append("%s: unexpected path token in %r" % (path, data[:80]))

# The resource merger treats every file under res/ as a resource and rejects
# anything that is not xml, ttf, ttc or otf. An attribution note dropped
# beside the fonts is the obvious mistake to make, and it only surfaces on a
# build: res/font/README.md sat here for several commits and the first CI run
# to see it failed in packageDebugResources.
ALLOWED_SUFFIXES = (".xml", ".ttf", ".ttc", ".otf", ".png", ".webp",
                    ".jpg", ".jpeg")
for path in glob.glob("app/src/main/res/**/*", recursive=True):
    if os.path.isdir(path):
        continue
    name = os.path.basename(path)
    if name.startswith("."):
        continue
    if not name.lower().endswith(ALLOWED_SUFFIXES):
        problems.append("%s: not a resource type; res/ accepts only %s"
                        % (path, ", ".join(ALLOWED_SUFFIXES)))

if problems:
    print("\n".join(problems))
    sys.exit(1)

print("resources clean")
