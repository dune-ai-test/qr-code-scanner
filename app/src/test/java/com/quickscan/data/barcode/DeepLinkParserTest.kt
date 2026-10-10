package com.quickscan.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `geo:`, `tel:`, `sms:` and `mailto:` are instructions rather than addresses,
 * so every one of them has to come back as its own type with the right action
 * available. The malformed cases matter as much as the happy path: a broken
 * deep link must stay readable text rather than becoming an intent that opens
 * nowhere.
 */
class DeepLinkParserTest {

    private fun parse(input: String) = DeepLinkParser.parse(input, input)

    // ------------------------------------------------------------------ geo

    @Test
    fun `geo with a plain coordinate pair`() {
        val place = parse("geo:51.5074,-0.1278") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals(-0.1278, place.longitude, 0.0001)
        assertNull(place.label)
        assertNull(place.zoom)
    }

    @Test
    fun `geo with a zoom parameter`() {
        val place = parse("geo:51.5074,-0.1278;u=15") as ScannedPayload.Location
        assertEquals(15, place.zoom)
    }

    @Test
    fun `geo with extra semicolon parameters keeps the zoom`() {
        val place = parse("geo:51.5074,-0.1278;u=12;crs=wgs84") as ScannedPayload.Location
        assertEquals(12, place.zoom)
    }

    @Test
    fun `geo with a query zoom`() {
        val place = parse("geo:51.5074,-0.1278?z=17") as ScannedPayload.Location
        assertEquals(17, place.zoom)
    }

    @Test
    fun `geo with a labelled query`() {
        val place = parse("geo:51.5074,-0.1278?q=Egg%20HQ") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals("Egg HQ", place.label)
    }

    @Test
    fun `geo where the path is null island takes the coordinates from the query`() {
        // The form Google hands out: the path is a placeholder and the real
        // place is in ?q=. Reading the path would open a map in the Atlantic.
        val place = parse("geo:0,0?q=51.5074,-0.1278(Home)") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals(-0.1278, place.longitude, 0.0001)
        assertEquals("Home", place.label)
    }

    @Test
    fun `geo with only a query`() {
        val place = parse("geo:?q=51.5074,-0.1278") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
    }

    @Test
    fun `geo with a label containing an encoded comma keeps it in the label`() {
        val place = parse("geo:51.5074,-0.1278?q=51.5074,-0.1278(Smith%2C%20John)") as ScannedPayload.Location
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals("Smith, John", place.label)
    }

    @Test
    fun `geo with a missing coordinate is not a location`() {
        assertNull(parse("geo:notacoordinate"))
        assertNull(parse("geo:51.5074"))
    }

    @Test
    fun `geo outside the valid range is not a location`() {
        // Null island is in range and stays; somewhere impossible does not.
        assertTrue(parse("geo:0,0") is ScannedPayload.Location)
        assertNull(parse("geo:91,0"))
        assertNull(parse("geo:0,181"))
    }

    // ------------------------------------------------------------------ tel

    @Test
    fun `tel becomes a phone number`() {
        val phone = parse("tel:+15551234567") as ScannedPayload.Phone
        assertEquals("+15551234567", phone.number)
        assertEquals(PayloadType.Phone, phone.type)
    }

    @Test
    fun `tel with parameters keeps only the number`() {
        val phone = parse("tel:+15551234567;ext=42") as ScannedPayload.Phone
        assertEquals("+15551234567", phone.number)
    }

    @Test
    fun `tel with no number is not a phone number`() {
        assertNull(parse("tel:"))
        assertNull(parse("tel:;phone-context=mobile"))
    }

    @Test
    fun `tel is case insensitive`() {
        assertEquals("+15551234567", (parse("TEL:+15551234567") as ScannedPayload.Phone).number)
    }

    // ------------------------------------------------------------------ sms

    @Test
    fun `sms with a body in the query`() {
        val sms = parse("sms:+15551234567?body=Table%20four") as ScannedPayload.Sms
        assertEquals("+15551234567", sms.number)
        assertEquals("Table four", sms.body)
    }

    @Test
    fun `sms with no body`() {
        val sms = parse("sms:+15551234567") as ScannedPayload.Sms
        assertEquals("+15551234567", sms.number)
        assertNull(sms.body)
    }

    @Test
    fun `smsto uses a colon instead of a query`() {
        // iOS writes the body after a colon; both separators are in the wild.
        val sms = parse("smsto:+15551234567:Table four") as ScannedPayload.Sms
        assertEquals("+15551234567", sms.number)
        assertEquals("Table four", sms.body)
    }

    @Test
    fun `smsto with no body`() {
        val sms = parse("smsto:+15551234567") as ScannedPayload.Sms
        assertEquals("+15551234567", sms.number)
        assertNull(sms.body)
    }

    @Test
    fun `an empty sms body reads as no body`() {
        val sms = parse("sms:+15551234567?body=") as ScannedPayload.Sms
        assertNull(sms.body)
    }

    @Test
    fun `sms with no number is not a message`() {
        assertNull(parse("sms:"))
    }

    // -------------------------------------------------------------- mailto

    @Test
    fun `mailto becomes an email`() {
        val email = parse("mailto:hi@example.com") as ScannedPayload.Email
        assertEquals("hi@example.com", email.address)
        assertNull(email.subject)
        assertNull(email.body)
    }

    @Test
    fun `mailto with a subject and body`() {
        val email = parse("mailto:hi@example.com?subject=Booking&body=Table%20for%204") as ScannedPayload.Email
        assertEquals("hi@example.com", email.address)
        assertEquals("Booking", email.subject)
        assertEquals("Table for 4", email.body)
    }

    @Test
    fun `mailto with a display name keeps only the address`() {
        val email = parse("mailto:Marco%20Rossi<a@b.com>") as ScannedPayload.Email
        assertEquals("a@b.com", email.address)
    }

    @Test
    fun `mailto with the address in the query`() {
        val email = parse("mailto:?to=a@b.com&subject=Hi") as ScannedPayload.Email
        assertEquals("a@b.com", email.address)
        assertEquals("Hi", email.subject)
    }

    @Test
    fun `mailto with several recipients keeps them all`() {
        val email = parse("mailto:a@b.com,c@d.com") as ScannedPayload.Email
        assertEquals("a@b.com,c@d.com", email.address)
    }

    @Test
    fun `mailto with no address is not an email`() {
        assertNull(parse("mailto:"))
        assertNull(parse("mailto:?subject=nobody"))
    }

    @Test
    fun `mailto decodes an accent over UTF-8`() {
        val email = parse("mailto:hi@example.com?subject=caf%C3%A9") as ScannedPayload.Email
        assertEquals("café", email.subject)
    }

    // ------------------------------------------------------------ fallbacks

    @Test
    fun `an ordinary web link is not a deep link`() {
        assertNull(parse("https://example.com"))
    }

    @Test
    fun `a WIFI code is not a deep link`() {
        assertNull(parse("WIFI:T:WPA;S:Home;P:secret;;"))
    }

    @Test
    fun `plain text with no scheme is not a deep link`() {
        assertNull(parse("just some text"))
        assertNull(parse(""))
    }

    @Test
    fun `schemeOf reads the scheme only`() {
        assertEquals("geo", DeepLinkParser.schemeOf("geo:51.5,-0.12"))
        assertEquals("mailto", DeepLinkParser.schemeOf("MAILTO:a@b.com"))
        assertNull(DeepLinkParser.schemeOf("example.com"))
    }
}