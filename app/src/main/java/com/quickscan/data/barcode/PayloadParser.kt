package com.quickscan.data.barcode

/** The kinds of payload QuickScan knows how to act on. */
enum class PayloadType {
    Url,
    Wifi,
    Contact,
    Product,
    Text,
}

sealed interface ScannedPayload {
    val type: PayloadType
    val raw: String

    data class Url(
        override val raw: String,
        val url: String,
        val host: String,
        val scheme: String,
    ) : ScannedPayload {
        override val type = PayloadType.Url
    }

    data class Wifi(
        override val raw: String,
        val ssid: String,
        val password: String,
        val encryption: String,
        val hidden: Boolean,
    ) : ScannedPayload {
        override val type = PayloadType.Wifi

        /** Human label for the result hero, e.g. "WPA2 · Guest network". */
        val securityLabel: String get() = encryption.ifBlank { "None" }

        val displayMeta: String
            get() = buildString {
                append(securityLabel)
                if (hidden) append(" · Hidden network")
            }
    }

    data class Contact(
        override val raw: String,
        val name: String,
        val phone: String?,
        val email: String?,
        val organisation: String?,
    ) : ScannedPayload {
        override val type = PayloadType.Contact
    }

    data class Product(
        override val raw: String,
        val value: String,
        val symbology: String,
    ) : ScannedPayload {
        override val type = PayloadType.Product
    }

    data class Text(
        override val raw: String,
    ) : ScannedPayload {
        override val type = PayloadType.Text
    }
}

/**
 * Turns a decoded string into a typed payload. Everything here is string
 * inspection on the device; nothing is looked up or resolved.
 */
object PayloadParser {

    private val VCARD_LINE = Regex("^([A-Za-z0-9-]+):(.*)$")
    private val NUMERIC_ONLY = Regex("^\\d{6,14}$")

    fun parse(raw: String, symbology: String? = null): ScannedPayload {
        val trimmed = raw.trim()

        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed, raw)
        }
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            return parseVCard(trimmed, raw)
        }
        val lower = trimmed.lowercase()
        if (lower.startsWith("mailto:")) {
            return ScannedPayload.Contact(
                raw = raw,
                name = "",
                phone = null,
                email = trimmed.removePrefix("mailto:").substringBefore('?'),
                organisation = null,
            )
        }
        if (lower.startsWith("tel:")) {
            return ScannedPayload.Contact(
                raw = raw,
                name = "",
                phone = trimmed.substringAfter(':'),
                email = null,
                organisation = null,
            )
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            val withoutScheme = trimmed.substringAfter("://")
            val host = withoutScheme
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .substringAfterLast('@')
        return ScannedPayload.Url(
                raw = raw,
                url = trimmed,
                host = host.ifBlank { withoutScheme },
                scheme = lower.substringBefore("://"),
            )
        }
        if (isRetailSymbology(symbology) && NUMERIC_ONLY.matches(trimmed)) {
            return ScannedPayload.Product(
                raw = raw,
                value = trimmed,
                symbology = symbology.orEmpty(),
            )
        }
        return ScannedPayload.Text(raw)
    }

    /**
     * `WIFI:T:WPA;S:MyNetwork;P:secret;H:true;;`
     *
     * Field values may be backslash-escaped (`\;`, `\,`, `\\`, `\:`, `\"`),
     * so parsing splits on unescaped semicolons only.
     */
    private fun parseWifi(trimmed: String, raw: String): ScannedPayload.Wifi {
        val body = trimmed.substringAfter(':').removeSuffix(";;")
        val fields = splitUnescaped(body, ';')

        var ssid = ""
        var password = ""
        var encryption = ""
        var hidden = false

        for (field in fields) {
            if (field.length < 2 || field[1] != ':') continue
            val key = field[0].uppercase()
            val value = field.substring(2).unescapeWifi()
            when (key) {
                "S" -> ssid = value
                "P" -> password = value
                "T" -> encryption = value
                "H" -> hidden = value.equals("true", ignoreCase = true)
            }
        }

        return ScannedPayload.Wifi(
            raw = raw,
            ssid = ssid.ifBlank { "Wi-Fi network" },
            password = password,
            encryption = encryption.ifBlank { "nopass" },
            hidden = hidden,
        )
    }

    private fun parseVCard(trimmed: String, raw: String): ScannedPayload.Contact {
        var name = ""
        var phone: String? = null
        var email: String? = null
        var organisation: String? = null

        for (line in trimmed.lineSequence()) {
            val match = VCARD_LINE.find(line.trim()) ?: continue
            val key = match.groupValues[1].uppercase()
            val value = match.groupValues[2].trim()
            when {
                (key == "FN" || key == "N") && name.isBlank() ->
                    name = if (key == "N") value.replace(";", " ").trim() else value

                key == "TEL" && phone == null -> phone = value
                key == "EMAIL" && email == null -> email = value
                key == "ORG" && organisation == null ->
                    organisation = value.removePrefix("ORG:").trim().ifBlank { null }
            }
        }

        return ScannedPayload.Contact(
            raw = raw,
            name = name.ifBlank { "Contact" },
            phone = phone,
            email = email,
            organisation = organisation,
        )
    }

    private fun isRetailSymbology(symbology: String?) = when (symbology?.uppercase()) {
        "EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128" -> true
        else -> false
    }

    private fun splitUnescaped(input: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            when {
                escaped -> {
                    current.append(ch)
                    escaped = false
                }

                ch == '\\' -> escaped = true
                ch == delimiter -> {
                    parts += current.toString()
                    current.clear()
                }

                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) parts += current.toString()
        return parts
    }

    private fun String.unescapeWifi(): String = buildString {
        var escaped = false
        for (ch in this@unescapeWifi) {
            when {
                escaped -> {
                    append(ch)
                    escaped = false
                }

                ch == '\\' -> escaped = true
                else -> append(ch)
            }
        }
    }
}