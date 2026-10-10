"""Local sanity checks for the Kotlin sources.

CI is the real compiler, but while changes are only committed locally this
catches the class of mistake the build would otherwise have to find: a name
used with no import and no declaration anywhere.

Checks:
  1. brace / paren / bracket balance
  2. every R.string.* exists in strings.xml
  3. no unused imports
  4. every type-position identifier resolves
"""

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src")

BUILTINS = set("""
String Int Long Float Double Boolean Byte Short Char Unit Any Nothing
List Map Set Array Pair Triple Result Exception Throwable Error
IntArray LongArray FloatArray BooleanArray CharArray ByteArray
ArrayList HashMap LinkedHashMap HashSet MutableList MutableMap MutableSet
Comparable Number Regex Runnable Thread IntRange IntProgression Suppress OptIn
StringBuilder IllegalArgumentException IllegalStateException
""".split())

# `by` delegation has no textual use of these operators.
DELEGATES = set("getValue setValue provideDelegate iterator hasNext next".split())

# Always in scope.
IMPLICIT = set("R Build View Modifier Dp Compose Material3 Android Int Float Boolean String Long Unit System Math Thread".split())

KEYWORDS = set("""
when where try catch finally do val var fun class object interface enum typealias
return break continue throw if else for while is as in out by companion constructor
init this super null true false it data sealed open abstract override private
public protected internal lateinit const inline suspend operator infix external
tailrec vararg crossinline noinline reified expect actual
""".split())


def strip(src):
    """Remove comments and string literals, leaving code."""
    out, i, n = [], 0, len(src)
    B, Q, A = chr(92), chr(34), chr(39)
    while i < n:
        c = src[i]
        if src[i:i + 3] == Q * 3:
            j = src.find(Q * 3, i + 3)
            i = (j + 3) if j != -1 else n
        elif c == Q:
            i += 1
            while i < n and src[i] != Q:
                i += 2 if src[i] == B else 1
            i += 1
        elif c == A:
            i += 1
            while i < n and src[i] != A:
                i += 2 if src[i] == B else 1
            i += 1
        elif src[i:i + 2] == "//":
            j = src.find("\n", i)
            i = j if j != -1 else n
        elif src[i:i + 2] == "/*":
            j = src.find("*/", i + 2)
            i = (j + 2) if j != -1 else n
        else:
            out.append(c)
            i += 1
    return "".join(out)


def kotlin_files():
    for base, _dirs, files in os.walk(APP):
        for f in files:
            if f.endswith(".kt"):
                yield os.path.join(base, f)


def main():
    files = list(kotlin_files())
    sources = {p: io.open(p, encoding="utf-8").read() for p in files}

    strings = set(re.findall(
        r'<string name="(\w+)"',
        io.open(os.path.join(ROOT, "app/src/main/res/values/strings.xml"), encoding="utf-8").read(),
    ))

    project = set()
    for src in sources.values():
        project |= set(re.findall(r'\b(?:class|interface|object|enum class|typealias)\s+(\w+)', src))
        project |= set(re.findall(r'\bfun\s+(?:<[^>]+>\s*)?(\w+)', src))
        project |= set(re.findall(r'\b(?:val|var)\s+(\w+)', src))
        # Enum entries read as constructors, so they need resolving too.
        for block in re.findall(r"enum class\s+\w+[^{]*\{(.*?)\n\}", src, re.S):
            project |= set(re.findall(r"^\s*([A-Z]\w*)\s*[,;(]", block, re.M))

    problems = []

    for path, src in sources.items():
        rel = os.path.relpath(path, ROOT)

        body = strip(src)
        for o, c in (("{", "}"), ("(", ")"), ("[", "]")):
            d = body.count(o) - body.count(c)
            if d:
                problems.append("%s: unbalanced %s%s (%+d)" % (rel, o, c, d))

        if " import import " in src:
            problems.append("%s: malformed import line" % rel)

        for name in re.findall(r'R\.string\.(\w+)', src):
            if name not in strings:
                problems.append("%s: R.string.%s does not exist" % (rel, name))

        imported, rest = {}, []
        for line in src.split("\n"):
            m = re.match(r'import\s+([\w.]+)(?:\s+as\s+(\w+))?$', line.strip())
            if m:
                imported[m.group(2) or m.group(1).split(".")[-1]] = m.group(1)
            elif not line.startswith("import "):
                rest.append(line)
        code = strip("\n".join(rest))
        # Imports are checked against strings kept: a use can live in a template.
        with_strings = "\n".join(rest)

        for name in imported:
            if name in DELEGATES:
                continue
            if not re.search(r'\b' + re.escape(name) + r'\b', with_strings):
                problems.append("%s: unused import %s" % (rel, name))

        known = BUILTINS | KEYWORDS | DELEGATES | IMPLICIT | set(imported) | project

        # Type positions only: an annotation, a generic argument, `is`, or a
        # constructor call. Anything looser drowns in false positives.
        #
        # The trailing \b matters: without it `[A-Za-z0-9]+` stops at an
        # underscore, so `width < MIN_WIDTH` matched as a reference to `MIN`
        # and every other error in the file was reported alongside it.
        candidates = set()
        candidates |= set(re.findall(r':\s*([A-Z][A-Za-z0-9]+)\b', code))
        candidates |= set(re.findall(r'<\s*([A-Z][A-Za-z0-9]+)\b', code))
        candidates |= set(re.findall(r'\bis\s+([A-Z][A-Za-z0-9]+)', code))
        candidates |= set(re.findall(r'(?<![\w.])([A-Z][A-Za-z0-9]+)\s*\(', code))
        candidates -= set(re.findall(r'\.\s*([A-Z][A-Za-z0-9]+)\s*\(', code))
        # A bare capitalised argument: .clip(CircleShape)
        candidates |= set(re.findall(r'\(\s*([A-Z][A-Za-z0-9]+)\s*[),]', code))
        # The right-hand side of an assignment: val x = LocalContext.current
        candidates |= set(re.findall(r'=\s*([A-Z][A-Za-z0-9]+)\s*[.,)(]', code))

        for name in candidates:
            if name not in known:
                problems.append("%s: unresolved reference %s" % (rel, name))

    seen, unique = set(), []
    for p in problems:
        key = re.sub(r'\d+', '', p)
        if key in seen:
            continue
        seen.add(key)
        unique.append(p)

    print("checked %d Kotlin files" % len(files))
    print("\n".join(unique) if unique else "clean")
    return 1 if unique else 0


if __name__ == "__main__":
    sys.exit(main())
