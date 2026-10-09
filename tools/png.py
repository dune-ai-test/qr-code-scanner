"""Minimal PNG reader plus helpers for measuring layout on a device screenshot."""

import io
import struct
import zlib


def read_png(path):
    data = io.open(path, "rb").read()
    pos, idat = 8, b""
    w = h = ctype = None
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        if typ == b"IHDR":
            w, h, _bd, ctype = struct.unpack(">IIBB", data[pos + 8:pos + 18])
        elif typ == b"IDAT":
            idat += data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
    raw = zlib.decompress(idat)
    bpp = {0: 1, 2: 3, 4: 2, 6: 4}[ctype]
    stride = w * bpp
    out, prev, p = bytearray(), bytearray(stride), 0
    for _ in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if f == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                c = prev[i - bpp] if i >= bpp else 0
                b = prev[i]
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out += line
        prev = line
    return w, h, bpp, bytes(out)


def luma(px, bpp, w, x, y):
    i = (y * w + x) * bpp
    return 0.299 * px[i] + 0.587 * px[i + 1] + 0.114 * px[i + 2]


def row_mean(px, bpp, w, y, step=4):
    total = n = 0
    for x in range(0, w, step):
        i = (y * w + x) * bpp
        total += 0.299 * px[i] + 0.587 * px[i + 1] + 0.114 * px[i + 2]
        n += 1
    return total / n


def row_min(px, bpp, w, y, step=2):
    return min(luma(px, bpp, w, x, y) for x in range(0, w, step))


def first_dark_below(px, bpp, w, y0, y1, x0, x1, threshold=140, step=2):
    """First row in [y0,y1) with dark pixels between x0 and x1."""
    for y in range(y0, y1):
        for x in range(x0, x1, step):
            if luma(px, bpp, w, x, y) < threshold:
                return y
    return None