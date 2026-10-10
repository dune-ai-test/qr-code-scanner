import io
import os


def edit(path, pairs, required=True):
    s = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old not in s:
            if required:
                raise SystemExit('not found in %s: %r' % (path, old[:100]))
            continue
        s = s.replace(old, new)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(s)


base = 'app/src/main/java/com/quickscan/'

# --- The verifier and its tests go entirely.
os.remove(base + 'data/safety/UrlSafetyVerifier.kt')
os.rmdir(base + 'data/safety')
os.remove('app/src/test/java/com/quickscan/data/safety/UrlSafetyVerifierTest.kt')
os.rmdir('app/src/test/java/com/quickscan/data/safety')

# --- DI no longer provides it.
edit(base + 'di/Modules.kt', [
    ('\n    @Provides\n    @Singleton\n    fun provideUrlSafetyVerifier(): UrlSafetyVerifier = UrlSafetyVerifier()\n', '\n'),
    ('import com.quickscan.data.safety.UrlSafetyVerifier\n', ''),
])

# --- PayloadPresentation loses the reason labels.
edit(base + 'core/ui/PayloadPresentation.kt', [
    ("""@get:StringRes
val SafetyReason.labelRes: Int
    get() = when (this) {
        SafetyReason.PlainHttp -> R.string.safety_reason_plain_http
        SafetyReason.IpLiteralHost -> R.string.safety_reason_ip_host
        SafetyReason.PunycodeHost -> R.string.safety_reason_punycode
        SafetyReason.EmbeddedCredentials -> R.string.safety_reason_credentials
        SafetyReason.SuspiciousTld -> R.string.safety_reason_tld
        SafetyReason.PhishingKeywords -> R.string.safety_reason_keywords
        SafetyReason.BrandLookalike -> R.string.safety_reason_lookalike
        SafetyReason.ShortenedHost -> R.string.safety_reason_shortened
    }

""", ''),
    ('import com.quickscan.data.safety.SafetyReason\n', ''),
    ('import androidx.annotation.StringRes\n', ''),
])

# --- The banner component goes with it.
p = base + 'core/ui/component/QSRows.kt'
s = io.open(p, encoding='utf-8').read()
start = s.index('/** Status or safety banner')
end = s.index('enum class BannerTone')
end = s.index('\n', s.index('\n', end) + 1) + 1
s = s[:start] + s[end:]
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)

# --- ResultViewModel drops the verdict.
edit(base + 'feature/result/ResultViewModel.kt', [
    ('import com.quickscan.data.safety.SafetyVerdict\nimport com.quickscan.data.safety.UrlSafetyVerifier\n', ''),
    ('    val safety: SafetyVerdict? = null,\n', ''),
    ('    urlSafetyVerifier: UrlSafetyVerifier,\n', ''),
    ('                safety = (payload as? ScannedPayload.Url)?.let { urlSafetyVerifier.verify(it.url) },\n', ''),
    ('            row?.let { emit(it.toState(payload, urlSafetyVerifier)) }',
     '            row?.let { emit(it.toState(payload)) }'),
    ("""    private fun ScanEntity.toState(
        payload: ScannedPayload,
        verifier: UrlSafetyVerifier,
    ) = ResultUiState(
        entity = this,
        payload = payload,
        safety = (payload as? ScannedPayload.Url)?.let { verifier.verify(it.url) },
        pinned = isPinned,
        loading = false,
    )""",
     """    private fun ScanEntity.toState(payload: ScannedPayload) = ResultUiState(
        entity = this,
        payload = payload,
        pinned = isPinned,
        loading = false,
    )"""),
])

# --- ResultScreen drops the banner and the URL hero's verdict slot.
edit(base + 'feature/result/ResultScreen.kt', [
    ("""        safety?.let { verdict ->
            QSBanner(
                icon = LucideShieldCheck,
                message = verdict.headline,
                tone = if (verdict.isSafe) BannerTone.Success else BannerTone.Warning,
            )
            if (!verdict.isSafe) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    verdict.reasons.forEach { reason ->
                        Text(
                            text = stringResource(reason.labelRes),
                            style = QsTheme.text.rowSub12,
                            color = palette.warnInk,
                            modifier = Modifier.padding(start = Space.xl),
                        )
                    }
                }
            }
        }
""", ''),
    ("import com.quickscan.core.ui.component.BannerTone\n", ''),
    ('import com.quickscan.core.ui.component.LucideShieldCheck\n', ''),
    ('import com.quickscan.core.ui.component.QSBanner\n', ''),
    ('import com.quickscan.data.safety.SafetyReason\n', ''),
    ("""private fun UrlHero(
    raw: String,
    safety: SafetyVerdict?,
    scannedAt: Long,
) {""",
     """private fun UrlHero(
    raw: String,
    scannedAt: Long,
) {"""),
    ("""                is ScannedPayload.Url -> UrlHero(
                    raw = payload.raw,
                    safety = state.safety,
                    scannedAt = entity.createdAt,
                )""",
     """                is ScannedPayload.Url -> UrlHero(
                    raw = payload.raw,
                    scannedAt = entity.createdAt,
                )"""),
])

print('ok')