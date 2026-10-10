package com.quickscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickscan.core.qr.DecodeImages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Decoding pieces that need a real Android runtime.
 *
 * The pure logic is covered on the JVM. What is left here needs the actual
 * framework: bitmap creation, the sampling budget, and the claim the whole app
 * rests on that it never asks for the network.
 */
@RunWith(AndroidJUnit4::class)
class DecodeInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theEdgeBudgetIsTheOneTheDesignExpects() {
        assertEquals(1600, DecodeImages.MAX_DECODE_EDGE)
    }

    @Test
    fun aSmallBitmapIsLeftAlone() {
        val small = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

        val result = with(DecodeImages) { small.sampled() }

        assertSame(small, result)
    }

    @Test
    fun aLargeBitmapIsScaledToTheBudgetAndKeepsItsShape() {
        val wide = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)

        val result = with(DecodeImages) { wide.sampled() }

        assertEquals(DecodeImages.MAX_DECODE_EDGE, maxOf(result.width, result.height))
        // 4:3 stays 4:3.
        assertEquals(result.height * 4, result.width * 3)
    }

    @Test
    fun aTwelveMegapixelPhotoWouldOtherwiseStayHuge() {
        val huge = Bitmap.createBitmap(4128, 3096, Bitmap.Config.ARGB_8888)

        val result = with(DecodeImages) { huge.sampled() }

        // 4128 * 3096 * 4 bytes is about 51MB; sampled it is under 4MB.
        val beforeBytes = 4128L * 3096L * 4L
        val afterBytes = result.width.toLong() * result.height.toLong() * 4L
        org.junit.Assert.assertTrue(
            "sampling should shed most of the memory",
            afterBytes < beforeBytes / 8,
        )
    }

    @Test
    fun jpegRoundTripsForTheCapturePath() {
        val source = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)

        val bytes = java.io.ByteArrayOutputStream().use { out ->
            source.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        assertNotNull(decoded)
        assertEquals(200, decoded!!.width)
        assertEquals(200, decoded.height)
    }

    @Test
    fun theAppNeverAsksForTheNetwork() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val requested = info.requestedPermissions ?: emptyArray()

        org.junit.Assert.assertFalse(
            "INTERNET must not be declared; it is the whole local-first promise",
            requested.contains("android.permission.INTERNET"),
        )
    }

    @Test
    fun theAppAsksForTheCamera() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val requested = info.requestedPermissions ?: emptyArray()

        assertEquals(
            true,
            requested.contains("android.permission.CAMERA"),
        )
    }
}