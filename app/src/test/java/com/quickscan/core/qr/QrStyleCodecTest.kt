package com.quickscan.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The style that travels with a saved code.
 *
 * This is what makes "a saved code looks the same everywhere" true rather than
 * aspirational: the round trip has to be exact, and a code that cannot be read
 * back has to fall back to plain rather than fail.
 */
class QrStyleCodecTest {

    @Test
    fun `the default style survives a round trip`() {
        val encoded = QrStyleCodec.encode(QrStyle.DEFAULT)
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode(encoded))
    }

    @Test
    fun `a styled code survives a round trip`() {
        val style = QrStyle(
            foreground = 0xFF3D8FD1.toInt(),
            background = 0xFFF4F5F7.toInt(),
            cornerRadius = 0.3f,
            logo = QrLogo.Link,
        )
        assertEquals(style, QrStyleCodec.decode(QrStyleCodec.encode(style)))
    }

    @Test
    fun `every logo survives`() {
        for (logo in QrLogo.entries) {
            val style = QrStyle(logo = logo)
            assertEquals(logo, QrStyleCodec.decode(QrStyleCodec.encode(style)).logo)
        }
    }

    @Test
    fun `a null logo is none and decodes back to null`() {
        val decoded = QrStyleCodec.decode(QrStyleCodec.encode(QrStyle(logo = null)))
        assertNull(decoded.logo)
    }

    @Test
    fun `nothing stored means plain`() {
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode(null))
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode(""))
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode("   "))
    }

    @Test
    fun `a truncated value is plain rather than a crash`() {
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode("#FF000000,#FFFFFF"))
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode("nonsense"))
    }

    @Test
    fun `an unreadable colour is plain`() {
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode("#ZZZZZZZZ,#FFFFFFFF,0.0,none"))
        assertEquals(QrStyle.DEFAULT, QrStyleCodec.decode("#FF00,#FFFFFFFF,0.0,none"))
    }

    @Test
    fun `an unknown logo is plain`() {
        assertEquals(
            QrStyle.DEFAULT,
            QrStyleCodec.decode("#FF000000,#FFFFFFFF,0.0,sparkly"),
        )
    }

    @Test
    fun `a six digit colour is read as opaque`() {
        val decoded = QrStyleCodec.decode("#111318,#FFFFFF,0.0,none")
        assertEquals(0xFF111318.toInt(), decoded.foreground)
    }

    @Test
    fun `an eight digit colour keeps its alpha`() {
        val decoded = QrStyleCodec.decode("#80FF0000,#FFFFFFFF,0.0,none")
        assertEquals(0x80FF0000.toInt(), decoded.foreground)
    }

    @Test
    fun `an absurd radius is clamped rather than refused`() {
        // The colour is worth keeping; a rounding value outside the useful
        // range is decoration, not a reason to lose the whole style.
        val decoded = QrStyleCodec.decode("#FF3D8FD1,#FFFFFFFF,9.0,none")
        assertEquals(0xFF3D8FD1.toInt(), decoded.foreground)
        assertEquals(0.5f, decoded.cornerRadius, 0.0001f)
    }

    @Test
    fun `a negative radius is clamped`() {
        val decoded = QrStyleCodec.decode("#FF000000,#FFFFFFFF,-2.0,none")
        assertEquals(0f, decoded.cornerRadius, 0.0001f)
    }

    @Test
    fun `the encoded form is compact enough for one column`() {
        val encoded = QrStyleCodec.encode(
            QrStyle(foreground = 0xFF3D8FD1.toInt(), cornerRadius = 0.3f, logo = QrLogo.Link),
        )
        assertEquals("#FF3D8FD1,#FFFFFFFF,0.3,link", encoded)
    }

    @Test
    fun `the logo name is matched case-insensitively`() {
        val decoded = QrStyleCodec.decode("#FF000000,#FFFFFFFF,0.0,LINK")
        assertEquals(QrLogo.Link, decoded.logo)
    }
}