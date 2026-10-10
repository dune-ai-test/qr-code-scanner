package com.quickscan.core.qr

/**
 * Stores a [QrStyle] in one text column.
 *
 * A saved code has to render the way it was made, or "styled preview" means
 * nothing the moment the code is reopened. That needs the style to travel with
 * the row, and one nullable column does it more cheaply than four — and
 * `null` then answers a question the four-column version could not: *was this
 * code styled at all?* A scanned code was not, and should render plain rather
 * than as whatever the creator's last palette choice happened to be.
 *
 * Format: `#AARRGGBB,#AARRGGBB,<radius>,<logo>`, e.g.
 * `#FF3D8FD1,#FFFFFFFF,0.30,link`. Decoding is forgiving because a style is
 * decoration: anything unreadable falls back to the default rather than
 * failing to open a code the user can still see.
 */
object QrStyleCodec {

    fun encode(style: QrStyle): String {
        val logo = style.logo?.name?.lowercase() ?: NONE
        return "${hex(style.foreground)},${hex(style.background)}," +
            "${style.cornerRadius},$logo"
    }

    /** Null, blank or malformed all mean "this code was not styled". */
    fun decode(value: String?): QrStyle {
        if (value.isNullOrBlank()) return QrStyle.DEFAULT
        val parts = value.split(',')
        if (parts.size < 4) return QrStyle.DEFAULT
        val foreground = parseColour(parts[0]) ?: return QrStyle.DEFAULT
        val background = parseColour(parts[1]) ?: return QrStyle.DEFAULT
        val radius = parts[2].toFloatOrNull() ?: return QrStyle.DEFAULT
        val logo = if (parts[3] == NONE) {
            null
        } else {
            QrLogo.entries.firstOrNull { it.name.equals(parts[3], ignoreCase = true) }
                ?: return QrStyle.DEFAULT
        }
        return QrStyle(
            foreground = foreground,
            background = background,
            // Out-of-range rounding is not worth discarding the colour for.
            cornerRadius = radius.coerceIn(0f, MAX_RADIUS),
            logo = logo,
        )
    }

    private fun hex(value: Int): String = "#%08X".format(value)

    private fun parseColour(value: String): Int? {
        val digits = value.removePrefix("#")
        // A 6-digit value is RGB; alpha defaults to opaque.
        val parsed = when (digits.length) {
            6, 8 -> digits.toLongOrNull(16) ?: return null
            else -> return null
        }
        return if (digits.length == 6) 0xFF000000.toInt() or parsed.toInt() else parsed.toInt()
    }

    private const val NONE = "none"

    /** Matches what QrStyle's own documentation calls the edge of usefulness. */
    private const val MAX_RADIUS = 0.5f
}