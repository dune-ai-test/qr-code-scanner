package com.quickscan.data.barcode

/**
 * Parses the URI schemes that are instructions rather than addresses.
 *
 * `geo:`, `tel:`, `sms:` and `mailto:` all look like text to a scanner that
 * only knows about `http`. A code carrying one of them is a request to do
 * something — show a place, dial a number, start a message, write an email —
 * and falling through to a plain-text result throws that away and leaves the
 * user copying a URI by hand.
 *
 * Returns null for anything unrecognised or malformed, so a broken `geo:`
 * stays visible as the text it is instead of becoming a map intent pointing
 * at null island.
 *
 * Like everything else here, this is string inspection on the device. Nothing
 * is resolved, and no address is looked up until the user asks for it.
 */
object DeepLinkParser {

    private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")

    /** The scheme of [trimmed], lowercased, or null when it carries none. */
    fun schemeOf(trimmed: String): String? =
        SCHEME.find(trimmed)?.groupValues?.get(1)?.lowercase()

    fun parse(trimmed: String, raw: String): ScannedPayload? =
        when (schemeOf(trimmed)) {
            "geo" -> parseGeo(trimmed, raw)
            "tel" -> parseTel(trimmed, raw)
            "sms", "smsto", "mms" -> parseSms(trimmed, raw)
            "mailto" -> parseMailto(trimmed, raw)
            else -> null
        }

    /**
     * `geo:51.5,-0.12`, with `?z=15`, `;u=15;crs=wgs84`, or a `?q=` carrying
     * the coordinates and a label: `geo:0,0?q=51.5,-0.12(Home)`.
     */
    private fun parseGeo(trimmed: String, raw: String): ScannedPayload.Location? {
        val body = trimmed.substringAfter(':').trim()
        val queryAt = body.indexOf('?')
        val head = if (queryAt >= 0) body.substring(0, queryAt) else body
        val params = if (queryAt >= 0) queryParams(body.substring(queryAt + 1)) else emptyMap()

        val fromQuery = params["q"]?.let(::place)
        // `;u=15;crs=wgs84` are parameters, not part of the coordinate pair.
        val fromPath = place(head.substringBefore(';'))

        // The coordinate pair can sit in the path, in ?q=, or both — and when
        // both are present the path is the placeholder `0,0`, so the query is
        // the one carrying the actual place.
        val chosen = fromQuery ?: fromPath ?: return null

        // ?q= is either the coordinates plus a label, as in
        // `geo:0,0?q=51.5,-0.12(Home)`, or the label on its own, as in
        // `geo:51.5,-0.12?q=Egg HQ`. A value with no comma parses as no
        // coordinates, which is what distinguishes the two.
        val label = when {
            fromQuery != null -> fromQuery.third
            params["q"] != null -> params["q"]?.trim()?.ifBlank { null }
            else -> chosen.third
        }
        val zoom = params["z"]?.toIntOrNull() ?: head.zoomParameter()

        return ScannedPayload.Location(
            raw = raw,
            latitude = chosen.first,
            longitude = chosen.second,
            label = label,
            zoom = zoom,
        )
    }

    /** `;u=15` among the semicolon-separated geo parameters. */
    private fun String.zoomParameter(): Int? = split(';')
        .firstOrNull { it.startsWith("u=", ignoreCase = true) }
        ?.substringAfter('=')
        ?.toIntOrNull()

    /** `tel:+15551234567`, with the optional `;ext=42` parameters dropped. */
    private fun parseTel(trimmed: String, raw: String): ScannedPayload.Phone? {
        val number = trimmed.substringAfter(':').substringBefore(';').trim()
        if (number.isEmpty()) return null
        return ScannedPayload.Phone(raw = raw, number = number)
    }

    /**
     * `sms:+15551234567?body=Hello` and the iOS-shaped `smsto:+15551234567:Hi`
     * both turn up, so both separators are accepted.
     */
    private fun parseSms(trimmed: String, raw: String): ScannedPayload.Sms? {
        val rest = trimmed.substringAfter(':').trim()
        var number: String
        var body: String? = null

        val queryAt = rest.indexOf('?')
        when {
            queryAt >= 0 -> {
                number = rest.substring(0, queryAt)
                body = queryParams(rest.substring(queryAt + 1))["body"]
            }

            schemeOf(trimmed) == "smsto" -> {
                val colonAt = rest.indexOf(':')
                if (colonAt >= 0) {
                    number = rest.substring(0, colonAt)
                    body = rest.substring(colonAt + 1)
                } else {
                    number = rest
                }
            }

            else -> number = rest
        }

        number = number.trim()
        if (number.isEmpty()) return null
        return ScannedPayload.Sms(raw = raw, number = number, body = body?.trim()?.ifBlank { null })
    }

    private fun parseMailto(trimmed: String, raw: String): ScannedPayload.Email? {
        val rest = trimmed.substringAfter(':')
        val queryAt = rest.indexOf('?')
        val addressPart = if (queryAt >= 0) rest.substring(0, queryAt) else rest
        val params = if (queryAt >= 0) queryParams(rest.substring(queryAt + 1)) else emptyMap()

        // `mailto:Marco%20Rossi<a@b.com>` carries a display name the mail app
        // does not need, and `mailto:?to=a@b.com` puts the address in the
        // query. Neither is worth losing the message over.
        val address = addressPart
            .substringAfterLast('<')
            .substringBefore('>')
            .trim()
            .ifBlank { params["to"].orEmpty() }
        if (address.isEmpty()) return null

        return ScannedPayload.Email(
            raw = raw,
            address = address,
            subject = params["subject"]?.trim()?.ifBlank { null },
            body = params["body"]?.trim()?.ifBlank { null },
        )
    }

    /**
     * `51.5,-0.12` optionally followed by `(Home)`. Ranges are checked so a
     * nonsense coordinate comes back as null and stays readable text, rather
     * than opening a map somewhere in the ocean.
     */
    private fun place(text: String): Triple<Double, Double, String?>? {
        val open = text.indexOf('(')
        val coords = if (open >= 0) text.substring(0, open) else text
        val label = if (open >= 0) {
            text.substring(open + 1).substringBefore(')').trim().ifBlank { null }
        } else {
            null
        }

        val parts = coords.trim().split(',')
        if (parts.size < 2) return null
        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return Triple(latitude, longitude, label)
    }

    /** Splits `a=1&b=2`, lowercasing keys and decoding values. */
    private fun queryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val key = pair.substring(0, eq).lowercase()
            // `+` is a space in a query string, but is part of a phone number
            // everywhere else — so it is only decoded here, after the split.
            val value = percentDecode(pair.substring(eq + 1).replace('+', ' '))
            key to value
        }.toMap()
    }

    /**
     * Percent decoding over UTF-8 bytes. `URLDecoder` is not used because it
     * also turns `+` into a space, which would corrupt a `+44` number the
     * moment it passed through a query.
     */
    private fun percentDecode(value: String): String {
        if ('%' !in value) return value
        val bytes = java.io.ByteArrayOutputStream(value.length)
        var index = 0
        while (index < value.length) {
            val code = if (value[index] == '%' && index + 2 < value.length) {
                value.substring(index + 1, index + 3).toIntOrNull(16)
            } else {
                null
            }
            if (code != null) {
                bytes.write(code)
                index += 3
            } else {
                bytes.write(value[index].toString().toByteArray(Charsets.UTF_8))
                index++
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }
}