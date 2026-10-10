package com.quickscan.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a pasted list becomes. The rule that matters most is the one about
 * schemes: a pasted `geo:` must survive verbatim, because prefixing it turns
 * a map location into a broken URL.
 */
class BatchPayloadsTest {

    private fun parse(text: String) = BatchPayloads.parse(text)

    @Test
    fun `one link per line`() {
        val items = parse("https://a.example\nhttps://b.example")
        assertEquals(2, items.size)
        assertEquals("https://a.example", items[0].content)
        assertEquals("https://b.example", items[1].content)
    }

    @Test
    fun `blank lines and surrounding space are dropped`() {
        val items = parse("\n  https://a.example  \n\n\n  https://b.example \n")
        assertEquals(2, items.size)
        assertEquals("https://a.example", items[0].content)
    }

    @Test
    fun `an empty paste makes nothing`() {
        assertTrue(parse("").isEmpty())
        assertTrue(parse("   \n  \n").isEmpty())
    }

    @Test
    fun `a bare domain gets https`() {
        assertEquals("https://example.com", parse("example.com").single().content)
    }

    @Test
    fun `a domain with a path or query gets https`() {
        assertEquals("https://example.com/a", parse("example.com/a").single().content)
        assertEquals("https://example.com/a?b=c", parse("example.com/a?b=c").single().content)
    }

    @Test
    fun `an existing scheme is never touched`() {
        // The bug this guards: prefixing these would change what the code means.
        assertEquals("geo:51.5,-0.12", parse("geo:51.5,-0.12").single().content)
        assertEquals("mailto:a@b.com", parse("mailto:a@b.com").single().content)
        assertEquals("tel:+15551234567", parse("tel:+15551234567").single().content)
        assertEquals("sms:+15551234567?body=hi", parse("sms:+15551234567?body=hi").single().content)
        assertEquals("http://a.example", parse("http://a.example").single().content)
        assertEquals("https://a.example", parse("https://a.example").single().content)
    }

    @Test
    fun `a WIFI payload survives intact`() {
        val raw = "WIFI:T:WPA;S:Home;P:secret;;"
        assertEquals(raw, parse(raw).single().content)
    }

    @Test
    fun `plain text with a colon is not mistaken for a scheme`() {
        // "Hello: world" has a scheme-shaped prefix, and adding https:// to it
        // would encode something nobody asked for.
        val item = parse("Hello: world").single()
        assertEquals("Hello: world", item.content)
    }

    @Test
    fun `a sentence is not treated as a domain`() {
        assertEquals("Buy milk today", parse("Buy milk today").single().content)
    }

    @Test
    fun `a dotted token is treated as a domain`() {
        // There is no way to tell "notes.txt" from "example.com" without a
        // public-suffix list, and the sheet prints what was encoded under each
        // code, so a wrong guess is visible rather than silent. Preferring the
        // link reading matches the single Create screen.
        assertEquals("https://notes.txt", parse("notes.txt").single().content)
    }

    @Test
    fun `duplicates become one code`() {
        val items = parse("https://a.example\nhttps://b.example\nhttps://a.example")
        assertEquals(2, items.size)
    }

    @Test
    fun `a duplicate after normalisation is still a duplicate`() {
        // example.com and https://example.com are the same code.
        val items = parse("example.com\nhttps://example.com")
        assertEquals(1, items.size)
    }

    @Test
    fun `the list is capped`() {
        val many = (1..80).joinToString("\n") { "https://site$it.example" }
        assertEquals(BatchPayloads.MAX_ITEMS, parse(many).size)
    }

    @Test
    fun `a label names the code so the sheet is readable`() {
        // headline, not toastLabel: a sheet has room to wrap and ellipsis what
        // a toast cannot, and the code underneath is what actually encodes the
        // address. A toast wants the host, a printed sheet wants what was
        // pasted.
        assertEquals("https://example.com", parse("https://example.com").single().label)
        assertEquals("Home", parse("WIFI:T:WPA;S:Home;P:secret;;").single().label)
    }

    @Test
    fun `a long label is kept whole for the renderer to wrap`() {
        val item = parse("https://example.com/a/very/long/path/that/keeps/going").single()
        assertTrue(item.label.isNotBlank())
        assertTrue(item.label.contains("example.com"))
    }
}