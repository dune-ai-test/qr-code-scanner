"""Mirror of DeepLinkParser, used to run the Kotlin test cases without a JVM.

Local builds are CI's job, but the deep-link rules are pure string handling
with several interacting branches, so tracing them by eye proved unreliable:
two bugs were found by reading and neither would have been obvious. This
reproduces the same logic in Python and replays every assertion in
DeepLinkParserTest so the branches are actually executed.

Keep this in step with DeepLinkParser.kt. A divergence here is not a build
error, so the file is short on purpose.
"""

import re
import sys

SCHEME = re.compile(r"^([a-zA-Z][a-zA-Z0-9+.-]*):")


def scheme_of(trimmed):
    m = SCHEME.match(trimmed)
    return m.group(1).lower() if m else None


def percent_decode(value):
    if "%" not in value:
        return value
    out = bytearray()
    i = 0
    while i < len(value):
        code = None
        if value[i] == "%" and i + 2 < len(value):
            try:
                code = int(value[i + 1:i + 3], 16)
            except ValueError:
                code = None
        if code is not None:
            out.append(code & 0xFF)
            i += 3
        else:
            out += value[i].encode("utf-8")
            i += 1
    return out.decode("utf-8", errors="replace")


def query_params(query):
    if not query:
        return {}
    out = {}
    for pair in query.split("&"):
        eq = pair.find("=")
        if eq <= 0:
            continue
        key = pair[:eq].lower()
        out[key] = percent_decode(pair[eq + 1:].replace("+", " "))
    return out


def to_float(text):
    try:
        return float(text)
    except ValueError:
        return None


def place(text):
    open_at = text.find("(")
    if open_at >= 0:
        coords = text[:open_at]
        label = text[open_at + 1:].split(")")[0].strip() or None
    else:
        coords = text
        label = None

    parts = [p.strip() for p in coords.strip().split(",")]
    if len(parts) < 2:
        return None
    lat = to_float(parts[0])
    lng = to_float(parts[1])
    if lat is None or lng is None:
        return None
    if not (-90.0 <= lat <= 90.0) or not (-180.0 <= lng <= 180.0):
        return None
    return (lat, lng, label)


def zoom_parameter(text):
    for segment in text.split(";"):
        if segment.lower().startswith("u="):
            try:
                return int(segment[2:])
            except ValueError:
                return None
    return None


def parse_geo(trimmed, raw):
    body = trimmed.split(":", 1)[1].strip()
    q_at = body.find("?")
    head = body[:q_at] if q_at >= 0 else body
    params = query_params(body[q_at + 1:]) if q_at >= 0 else {}

    from_query = place(params["q"]) if "q" in params else None
    from_path = place(head.split(";")[0])

    chosen = from_query or from_path
    if chosen is None:
        return None

    if from_query is not None:
        label = from_query[2]
    elif "q" in params:
        label = params["q"].strip() or None
    else:
        label = chosen[2]

    zoom = None
    if params.get("z", "").isdigit() or _is_int(params.get("z")):
        zoom = int(params["z"])
    else:
        zoom = zoom_parameter(head)

    return {"type": "Location", "lat": chosen[0], "lng": chosen[1],
            "label": label, "zoom": zoom, "raw": raw}


def _is_int(value):
    try:
        int(value)
        return True
    except (TypeError, ValueError):
        return False


def parse_tel(trimmed, raw):
    body = trimmed.split(":", 1)[1]
    number = body.split(";")[0].strip()
    if not number:
        return None
    return {"type": "Phone", "number": number, "raw": raw}


def parse_sms(trimmed, raw):
    scheme = scheme_of(trimmed)
    rest = trimmed.split(":", 1)[1].strip()
    body = None
    q_at = rest.find("?")
    if q_at >= 0:
        number = rest[:q_at]
        body = query_params(rest[q_at + 1:]).get("body")
    elif scheme == "smsto":
        colon_at = rest.find(":")
        if colon_at >= 0:
            number = rest[:colon_at]
            body = rest[colon_at + 1:]
        else:
            number = rest
    else:
        number = rest

    number = number.strip()
    if not number:
        return None
    if body is not None:
        body = body.strip() or None
    return {"type": "Sms", "number": number, "body": body, "raw": raw}


def parse_mailto(trimmed, raw):
    rest = trimmed.split(":", 1)[1]
    q_at = rest.find("?")
    address_part = rest[:q_at] if q_at >= 0 else rest
    params = query_params(rest[q_at + 1:]) if q_at >= 0 else {}

    address = address_part
    if "<" in address:
        address = address[address.rfind("<") + 1:]
    address = address.split(">")[0].strip()
    if not address:
        address = params.get("to", "")
    if not address:
        return None

    subject = (params.get("subject") or "").strip() or None
    body = (params.get("body") or "").strip() or None
    return {"type": "Email", "address": address, "subject": subject,
            "body": body, "raw": raw}


def parse(trimmed, raw):
    scheme = scheme_of(trimmed)
    if scheme == "geo":
        return parse_geo(trimmed, raw)
    if scheme == "tel":
        return parse_tel(trimmed, raw)
    if scheme in ("sms", "smsto", "mms"):
        return parse_sms(trimmed, raw)
    if scheme == "mailto":
        return parse_mailto(trimmed, raw)
    return None


# --------------------------------------------------------------------- cases

FAILURES = []


def check(name, condition):
    if not condition:
        FAILURES.append(name)


def eq(name, actual, expected):
    check(name, actual == expected or (
        isinstance(actual, float) and isinstance(expected, float)
        and abs(actual - expected) < 1e-4))


def run():
    p = lambda s: parse(s, s)

    for name, text, lat, lng, label, zoom in [
        ("geo plain", "geo:51.5074,-0.1278", 51.5074, -0.1278, None, None),
        ("geo ;u=", "geo:51.5074,-0.1278;u=15", 51.5074, -0.1278, None, 15),
        ("geo params", "geo:51.5074,-0.1278;u=12;crs=wgs84", 51.5074, -0.1278, None, 12),
        ("geo ?z=", "geo:51.5074,-0.1278?z=17", 51.5074, -0.1278, None, 17),
        ("geo ?q= label", "geo:51.5074,-0.1278?q=Egg%20HQ", 51.5074, -0.1278, "Egg HQ", None),
        ("geo null island", "geo:0,0?q=51.5074,-0.1278(Home)", 51.5074, -0.1278, "Home", None),
        ("geo query only", "geo:?q=51.5074,-0.1278", 51.5074, -0.1278, None, None),
        ("geo comma label", "geo:51.5074,-0.1278?q=51.5074,-0.1278(Smith%2C%20John)",
         51.5074, -0.1278, "Smith, John", None),
    ]:
        got = p(text)
        check(name + " parsed", got is not None and got["type"] == "Location")
        if got and got["type"] == "Location":
            eq(name + " lat", got["lat"], lat)
            eq(name + " lng", got["lng"], lng)
            eq(name + " label", got["label"], label)
            eq(name + " zoom", got["zoom"], zoom)

    for name, text in [("geo garbage", "geo:notacoordinate"),
                       ("geo half", "geo:51.5074"),
                       ("geo lat range", "geo:91,0"),
                       ("geo lng range", "geo:0,181"),
                       ("tel empty", "tel:"),
                       ("tel params only", "tel:;phone-context=mobile"),
                       ("sms empty", "sms:"),
                       ("mailto empty", "mailto:"),
                       ("mailto no address", "mailto:?subject=nobody"),
                       ("https", "https://example.com"),
                       ("wifi", "WIFI:T:WPA;S:Home;P:secret;;"),
                       ("text", "just some text"),
                       ("empty", "")]:
        check(name + " rejected", p(text) is None)

    check("null island kept", p("geo:0,0") is not None)

    for name, text, number in [
        ("tel", "tel:+15551234567", "+15551234567"),
        ("tel params", "tel:+15551234567;ext=42", "+15551234567"),
        ("tel upper", "TEL:+15551234567", "+15551234567"),
        ("sms bare", "sms:+15551234567", "+15551234567"),
        ("smsto bare", "smsto:+15551234567", "+15551234567"),
    ]:
        got = p(text)
        check(name + " parsed", got is not None)
        if got:
            eq(name + " number", got["number"], number)

    eq("sms body", p("sms:+15551234567?body=Table%20four")["body"], "Table four")
    eq("sms no body", p("sms:+15551234567")["body"], None)
    eq("smsto colon body", p("smsto:+15551234567:Table four")["body"], "Table four")
    eq("smsto no body", p("smsto:+15551234567")["body"], None)
    eq("sms empty body", p("sms:+15551234567?body=")["body"], None)

    eq("mailto address", p("mailto:hi@example.com")["address"], "hi@example.com")
    eq("mailto no subject", p("mailto:hi@example.com")["subject"], None)
    both = p("mailto:hi@example.com?subject=Booking&body=Table%20for%204")
    eq("mailto subject", both["subject"], "Booking")
    eq("mailto body", both["body"], "Table for 4")
    eq("mailto display name", p("mailto:Marco%20Rossi<a@b.com>")["address"], "a@b.com")
    in_query = p("mailto:?to=a@b.com&subject=Hi")
    eq("mailto query address", in_query["address"], "a@b.com")
    eq("mailto query subject", in_query["subject"], "Hi")
    eq("mailto many", p("mailto:a@b.com,c@d.com")["address"], "a@b.com,c@d.com")
    eq("mailto utf8", p("mailto:hi@example.com?subject=caf%C3%A9")["subject"], "café")

    eq("scheme geo", scheme_of("geo:51.5,-0.12"), "geo")
    eq("scheme mailto", scheme_of("MAILTO:a@b.com"), "mailto")
    eq("scheme none", scheme_of("example.com"), None)

    return FAILURES


if __name__ == "__main__":
    failed = run()
    if failed:
        print("FAILED:")
        for name in failed:
            print("  -", name)
        sys.exit(1)
    print("deep-link mirror: all cases pass")