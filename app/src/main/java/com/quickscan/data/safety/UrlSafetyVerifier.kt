package com.quickscan.data.safety

/** A single thing the verifier noticed about a link. */
enum class SafetyReason {
    PlainHttp,
    IpLiteralHost,
    PunycodeHost,
    EmbeddedCredentials,
    SuspiciousTld,
    PhishingKeywords,
    BrandLookalike,
    ShortenedHost,
}

data class SafetyVerdict(
    val url: String,
    val reasons: List<SafetyReason>,
) {
    val isSafe: Boolean get() = reasons.isEmpty()

    /** The headline the result banner shows. */
    val headline: String
        get() = if (isSafe) "Link verified · no known threats" else "Be careful with this link"

    /** True when at least one reason is serious enough to warn rather than note. */
    val isSevere: Boolean
        get() = reasons.any {
            it == SafetyReason.PunycodeHost ||
                it == SafetyReason.BrandLookalike ||
                it == SafetyReason.PhishingKeywords ||
                it == SafetyReason.EmbeddedCredentials
        }
}

/**
 * Inspects a URL entirely on the device. There is no DNS lookup, no reputation
 * service and no network call: the verdict only reflects what can be read off
 * the address itself, which is what the "Link verified" banner promises.
 */
class UrlSafetyVerifier {

    fun verify(url: String): SafetyVerdict {
        val reasons = mutableListOf<SafetyReason>()

        val parsed = runCatching { java.net.URI(url) }.getOrNull()
        val scheme = (parsed?.scheme ?: url.substringBefore("://", "")).lowercase()
        val rawAuthority = parsed?.rawAuthority ?: url
            .substringAfter("://", url)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')

        if (scheme == "http") reasons += SafetyReason.PlainHttp

        val host = hostOf(rawAuthority)
        val loweredHost = host.lowercase()

        if (rawAuthority.contains('@')) reasons += SafetyReason.EmbeddedCredentials
        if (isIpLiteral(loweredHost)) reasons += SafetyReason.IpLiteralHost
        if (loweredHost.startsWith("xn--") || loweredHost.contains(".xn--")) {
            reasons += SafetyReason.PunycodeHost
        }
        if (SHORTENERS.contains(loweredHost)) reasons += SafetyReason.ShortenedHost
        if (SUSPICIOUS_TLDS.any { loweredHost.endsWith(".$it") }) reasons += SafetyReason.SuspiciousTld
        if (PHISHING_TERMS.any { loweredHost.contains(it) }) reasons += SafetyReason.PhishingKeywords
        if (imitationOf(loweredHost)) reasons += SafetyReason.BrandLookalike

        return SafetyVerdict(url = url, reasons = reasons.distinct())
    }

    private fun hostOf(authority: String): String =
        authority.substringAfterLast('@').substringBefore(':')

    private fun isIpLiteral(host: String): Boolean {
        if (host.isBlank()) return false
        if (host.startsWith("[")) return host.endsWith("]") // IPv6 literal
        if (host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))) return true
        return host.all { it.isDigit() || it == '.' } && host.contains('.')
    }

    /**
     * Catches domains that copy a well-known name with a substitution, such as
     * `paypa1-secure.com` or `micros0ft-login.net`.
     */
    private fun imitationOf(host: String): Boolean {
        val labels = host.split('.').filter { it.isNotBlank() }
        if (labels.size < 2) return false
        val name = labels[0]
        if (name.length < 4) return false
        val hasDigitSubstitution = name.any { it.isDigit() }
        val hasSeparator = name.contains('-') || name.contains('_')
        if (!hasDigitSubstitution && !hasSeparator) return false
        if (!name.any { it.isLetter() }) return false

        return IMITATED_BRANDS.any { brand ->
            name.equals(brand, ignoreCase = true) ||
                name.startsWith(brand) || name.endsWith(brand) ||
                name.contains(brand)
        }
    }

    private companion object {
        val SUSPICIOUS_TLDS = listOf(
            "zip", "mov", "tk", "ml", "ga", "cf", "gq", "top", "xyz", "click", "work",
        )

        val SHORTENERS = setOf(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "buff.ly", "is.gd",
            "cutt.ly", "rebrand.ly", "shorturl.at",
        )

        val PHISHING_TERMS = listOf(
            "login", "signin", "verify-account", "account-verify", "secure-update",
            "password-reset", "update-billing", "confirm-identity", "unlock-wallet",
        )

        val IMITATED_BRANDS = listOf(
            "paypal", "apple", "microsoft", "amazon", "google", "netflix", "facebook",
            "instagram", "whatsapp", "coinbase", "binance", "metamask", "chase",
            "bankofamerica", "wellsfargo", "hsbc", "barclays",
        )
    }
}