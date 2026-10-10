package com.quickscan.feature.history

import com.quickscan.data.barcode.PayloadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The per-type scoping rules. These are the ones that decide what "Delete"
 * actually removes, so they are pinned down here rather than inferred from the
 * toolbar.
 */
class SelectionScopeTest {

    private val links = setOf(PayloadType.Url, PayloadType.Wifi, PayloadType.Text)

    @Test
    fun `tapping a type off narrows to everything else`() {
        val next = nextTypeScope(links, null, PayloadType.Wifi)
        assertEquals(setOf(PayloadType.Url, PayloadType.Text), next)
    }

    @Test
    fun `tapping a type back on widens again`() {
        val narrowed = nextTypeScope(links, null, PayloadType.Wifi)
        val widened = nextTypeScope(links, narrowed, PayloadType.Wifi)
        assertEquals(links, widened)
    }

    @Test
    fun `a second type can be turned off inside a narrowed scope`() {
        val first = nextTypeScope(links, null, PayloadType.Wifi)
        val second = nextTypeScope(links, first, PayloadType.Text)
        assertEquals(setOf(PayloadType.Url), second)
    }

    @Test
    fun `turning the only remaining type off leaves it on rather than empty`() {
        // One type left in scope, switched off, would leave the toolbar acting
        // on nothing. The tap has to leave the single type in play instead.
        val onlyOne = setOf(PayloadType.Url)
        assertEquals(onlyOne, nextTypeScope(onlyOne, onlyOne, PayloadType.Url))
    }

    @Test
    fun `turning a type off from a fully-active scope leaves the rest on`() {
        // The state after tapping a chip twice: everything is back in scope,
        // so a third tap narrows rather than widening again.
        assertEquals(setOf(PayloadType.Wifi, PayloadType.Text), nextTypeScope(links, links, PayloadType.Url))
    }

    @Test
    fun `a type that is not selected leaves the scope alone`() {
        assertNull(nextTypeScope(setOf(PayloadType.Url), null, PayloadType.Wifi))
    }

    @Test
    fun `an unscoped action reaches every ticked row`() {
        val selection = setOf(1L, 2L, 3L)
        val types = mapOf(
            1L to PayloadType.Url,
            2L to PayloadType.Wifi,
            3L to PayloadType.Text,
        )
        assertEquals(selection, idsInScope(selection, types, null))
    }

    @Test
    fun `a scoped action skips the types that were turned off`() {
        val selection = setOf(1L, 2L, 3L)
        val types = mapOf(
            1L to PayloadType.Url,
            2L to PayloadType.Wifi,
            3L to PayloadType.Wifi,
        )
        assertEquals(setOf(1L), idsInScope(selection, types, setOf(PayloadType.Url)))
    }

    @Test
    fun `a scope naming only Wi-Fi reaches exactly the Wi-Fi rows`() {
        val selection = setOf(1L, 2L, 3L, 4L)
        val types = mapOf(
            1L to PayloadType.Url,
            2L to PayloadType.Wifi,
            3L to PayloadType.Wifi,
            4L to PayloadType.Text,
        )
        assertEquals(setOf(2L, 3L), idsInScope(selection, types, setOf(PayloadType.Wifi)))
    }

    @Test
    fun `a scope spanning two types reaches both`() {
        val selection = setOf(1L, 2L, 3L, 4L)
        val types = mapOf(
            1L to PayloadType.Url,
            2L to PayloadType.Wifi,
            3L to PayloadType.Wifi,
            4L to PayloadType.Text,
        )
        val scope = setOf(PayloadType.Url, PayloadType.Wifi)
        assertEquals(setOf(1L, 2L, 3L), idsInScope(selection, types, scope))
    }
}