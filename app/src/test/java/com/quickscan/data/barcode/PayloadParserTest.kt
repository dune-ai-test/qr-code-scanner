package com.quickscan.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadParserTest {

    @Test
    fun `https url becomes a Url payload with host and scheme`() {
        val payload = PayloadParser.parse("https://studioblume.co/autumn-lookbook")

        val url = payload as ScannedPayload.Url
        assertEquals(PayloadType.Url, payload.type)
        assertEquals("studioblume.co", url.host)
        assertEquals("https", url.scheme)
    }

    @Test
    fun `plain http url keeps its scheme`() {
        val payload = PayloadParser.parse("http://example.com") as ScannedPayload.Url
        assertEquals("http", payload.scheme)
        assertEquals("example.com", payload.host)
    }

    @Test
    fun `url with credentials reports the host after the at sign`() {
        val payload = PayloadParser.parse("https://user:pw@example.com/x") as ScannedPayload.Url
        assertEquals("example.com", payload.host)
    }

    @Test
    fun `wifi payload reads ssid password encryption and hidden flag`() {
        val payload = PayloadParser.parse("WIFI:T:WPA;S:Brew & Co Guest;P:brewco2024;H:true;;")

        val wifi = payload as ScannedPayload.Wifi
        assertEquals(PayloadType.Wifi, payload.type)
        assertEquals("Brew & Co Guest", wifi.ssid)
        assertEquals("brewco2024", wifi.password)
        assertEquals("WPA", wifi.encryption)
        assertTrue(wifi.hidden)
    }

    @Test
    fun `wifi payload unescapes escaped separators`() {
        val payload = PayloadParser.parse("""WIFI:T:WPA;S:Caf\;e\; Bar;P:pa\\ss;;""")

        val wifi = payload as ScannedPayload.Wifi
        assertEquals("Caf;e; Bar", wifi.ssid)
        assertEquals("pa\\ss", wifi.password)
    }

    @Test
    fun `wifi payload defaults a missing ssid`() {
        val wifi = PayloadParser.parse("WIFI:T:nopass;;") as ScannedPayload.Wifi
        assertEquals("Wi-Fi network", wifi.ssid)
        assertEquals("nopass", wifi.encryption)
    }

    @Test
    fun `vcard becomes a Contact payload`() {
        val raw = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Marco Rossi
            TEL:+441632960001
            EMAIL:marco@example.com
            END:VCARD
        """.trimIndent()

        val contact = PayloadParser.parse(raw) as ScannedPayload.Contact
        assertEquals(PayloadType.Contact, contact.type)
        assertEquals("Marco Rossi", contact.name)
        assertEquals("+441632960001", contact.phone)
        assertEquals("marco@example.com", contact.email)
    }

    @Test
    fun `mailto becomes an Email payload rather than a contact`() {
        // It used to land in Contact, which has no "send email" action: with
        // no phone number that screen offered nothing but Share.
        val email = PayloadParser.parse("mailto:hi@example.com?subject=hi") as ScannedPayload.Email
        assertEquals("hi@example.com", email.address)
        assertEquals("hi", email.subject)
    }

    @Test
    fun `tel becomes a Phone payload rather than a contact`() {
        val phone = PayloadParser.parse("tel:+15551234567") as ScannedPayload.Phone
        assertEquals("+15551234567", phone.number)
    }

    @Test
    fun `geo becomes a Location`() {
        val place = PayloadParser.parse("geo:51.5074,-0.1278?q=Egg") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals("Egg", place.label)
    }

    @Test
    fun `a malformed geo stays plain text`() {
        assertEquals(PayloadType.Text, PayloadParser.parse("geo:nonsense").type)
    }

    @Test
    fun `a vCard is still a Contact, not an Email`() {
        val contact = PayloadParser.parse(
            "BEGIN:VCARD\nFN:Marco Rossi\nTEL:+441632960001\nEND:VCARD",
        )
        assertTrue(contact is ScannedPayload.Contact)
    }

    @Test
    fun `numeric barcode becomes a Product payload`() {
        val payload = PayloadParser.parse("5901234123457", "EAN_13")
        assertEquals(PayloadType.Product, payload.type)
        assertEquals("5901234123457", (payload as ScannedPayload.Product).value)
    }

    @Test
    fun `long numeric text without a retail symbology stays Text`() {
        assertEquals(PayloadType.Text, PayloadParser.parse("123456789012345", null).type)
    }

    @Test
    fun `plain text falls through to a Text payload`() {
        val payload = PayloadParser.parse("Invoice #4821")
        assertEquals(PayloadType.Text, payload.type)
        assertEquals("Invoice #4821", payload.raw)
    }

    @Test
    fun `whitespace around a url is trimmed`() {
        val payload = PayloadParser.parse("   https://example.com   ") as ScannedPayload.Url
        assertEquals("https://example.com", payload.url)
    }

    @Test
    fun `vcard with no fields still produces a contact`() {
        val contact = PayloadParser.parse("BEGIN:VCARD\nEND:VCARD") as ScannedPayload.Contact
        assertFalse(contact.name.isBlank())
    }
}