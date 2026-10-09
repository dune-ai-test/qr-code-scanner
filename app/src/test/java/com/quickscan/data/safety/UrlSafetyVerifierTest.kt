package com.quickscan.data.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSafetyVerifierTest {

    private val verifier = UrlSafetyVerifier()

    @Test
    fun `ordinary https link reports no reasons`() {
        val verdict = verifier.verify("https://studioblume.co/autumn-lookbook")

        assertTrue(verdict.isSafe)
        assertTrue(verdict.reasons.isEmpty())
        assertFalse(verdict.isSevere)
    }

    @Test
    fun `plain http is flagged`() {
        val verdict = verifier.verify("http://example.com")
        assertTrue(SafetyReason.PlainHttp in verdict.reasons)
    }

    @Test
    fun `ipv4 literal host is flagged`() {
        val verdict = verifier.verify("https://192.168.1.10/admin")
        assertTrue(SafetyReason.IpLiteralHost in verdict.reasons)
    }

    @Test
    fun `ipv6 literal host is flagged`() {
        val verdict = verifier.verify("https://[2001:db8::1]/admin")
        assertTrue(SafetyReason.IpLiteralHost in verdict.reasons)
    }

    @Test
    fun `punycode host is flagged as severe`() {
        val verdict = verifier.verify("https://xn--pypal-4ve.com/account")
        assertTrue(SafetyReason.PunycodeHost in verdict.reasons)
        assertTrue(verdict.isSevere)
    }

    @Test
    fun `credentials in the address are flagged`() {
        val verdict = verifier.verify("https://user:pass@example.com/")
        assertTrue(SafetyReason.EmbeddedCredentials in verdict.reasons)
        assertTrue(verdict.isSevere)
    }

    @Test
    fun `suspicious tld is flagged`() {
        assertTrue(SafetyReason.SuspiciousTld in verifier.verify("https://login-update.zip").reasons)
    }

    @Test
    fun `shortener host is flagged`() {
        val verdict = verifier.verify("https://bit.ly/3xample")
        assertTrue(SafetyReason.ShortenedHost in verdict.reasons)
    }

    @Test
    fun `phishing keyword in the host is flagged`() {
        val verdict = verifier.verify("https://account-verify.example.org/reset")
        assertTrue(SafetyReason.PhishingKeywords in verdict.reasons)
    }

    @Test
    fun `digit-substituted brand lookalike is flagged`() {
        val verdict = verifier.verify("https://paypa1-secure.example.com/login")
        assertTrue(SafetyReason.BrandLookalike in verdict.reasons)
        assertTrue(verdict.isSevere)
    }

    @Test
    fun `real brand domain is not flagged as a lookalike`() {
        val verdict = verifier.verify("https://www.paypal.com/us/signin")
        // "paypal" appears in the second label, but the registrable name is
        // paypal.com with no substitution, so nothing should be reported.
        assertTrue(verdict.isSafe)
    }

    @Test
    fun `several problems are reported at once`() {
        val verdict = verifier.verify("http://paypa1.xyz/login")
        assertTrue(verdict.reasons.size >= 3)
        assertFalse(verdict.isSafe)
        assertTrue(verdict.reasons.distinct().size == verdict.reasons.size)
    }

    @Test
    fun `warnings change the headline away from verified`() {
        assertEquals("Link verified · no known threats", verifier.verify("https://example.com").headline)
        assertEquals(
            "Be careful with this link",
            verifier.verify("http://paypa1.xyz/login").headline,
        )
    }

    @Test
    fun `a malformed address does not throw`() {
        val verdict = verifier.verify("not a url at all")
        assertTrue(verdict.isSafe)
    }
}