package com.quickscan.core.qr

import com.quickscan.core.ui.headline
import com.quickscan.data.barcode.PayloadParser

/**
 * Turns a pasted list into the codes a sheet should carry.
 *
 * One line is one code. That is the whole contract, and it is the one people
 * already have in their clipboard — a column of links copied from a page, or
 * typed out one per line.
 */
object BatchPayloads {

    const val MAX_ITEMS = QrContactSheet.MAX_ITEMS

    data class Item(
        /** What the code will encode. */
        val content: String,
        /** What is printed under it, so the sheet is readable without decoding. */
        val label: String,
    )

    fun parse(text: String): List<Item> {
        val seen = mutableSetOf<String>()
        val items = mutableListOf<Item>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val content = normalise(trimmed)
            // Two identical codes on one poster is a mistake, not a request.
            if (!seen.add(content)) continue
            items += Item(content = content, label = PayloadParser.parse(content).headline)
            if (items.size == MAX_ITEMS) break
        }
        return items
    }

    /**
     * `example.com` becomes `https://example.com`, matching what the single
     * Create screen already does with a bare address.
     *
     * Anything that already carries a scheme is left exactly as typed. Testing
     * for a scheme rather than for a leading `http` is the point: a pasted
     * `geo:` or `mailto:` must survive verbatim, or the code it produces
     * means something different from what was copied.
     */
    private fun normalise(line: String): String {
        if (line.contains("://")) return line
        val colon = line.indexOf(':')
        if (colon > 0 && SCHEME.matches(line.substring(0, colon))) return line
        return if (looksLikeDomain(line)) "https://$line" else line
    }

    private val SCHEME = Regex("[a-zA-Z][a-zA-Z0-9+.\\-]*")

    /** A dot, no spaces, and something after it — "not.a.domain" qualifies. */
    private fun looksLikeDomain(line: String): Boolean {
        if (line.any { it.isWhitespace() }) return false
        val host = line.substringBefore('/').substringBefore('?')
        val dot = host.lastIndexOf('.')
        return dot > 0 && dot < host.length - 1
    }
}