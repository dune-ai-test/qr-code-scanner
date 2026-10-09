# QuickScan

A local-first QR and barcode scanner for Android, built around one rule: **everything happens on the device.** There is no account, no cloud sync and no network call anywhere in the flow — the app does not even declare the `INTERNET` permission.

The visual language is deliberately iOS-native rather than Material: a floating frosted capsule tab bar, 12–31 px corner radii, hairline strokes and quiet layered shadows, so the product reads as a first-party system utility rather than an Android port. Every colour, spacing step and type size is a token in `core/ui/theme`, so the whole product re-themes from a single change.

## Screens

| # | Screen | Notes |
|---|--------|-------|
| 1 | Onboarding — welcome | Value props, procedural QR hero |
| 2 | Onboarding — name | Live avatar preview that tracks the field |
| 3 | Scan | CameraX preview, reticle, torch, flip, gallery, paste |
| 4 | Result — website | Decoded URL, local safety check, open action |
| 5 | Result — Wi-Fi | SSID, revealable password, join action |
| 6 | History | Stats, filter chips, date grouping |
| 7 | History — empty | First-run state with suggestions |
| 8 | Create | Live preview, four content types |
| 9 | Settings | Scanner, appearance, storage, permissions, about |

The result screen routes by payload type, so text, contact and product codes get their own action set rather than a URL-shaped one.

## Architecture

```
com.quickscan
├── core/qr          QrPlaceholder (seeded 25x25 generator), QrEncoder (ZXing)
├── core/ui          theme tokens, shared components, payload presentation
├── data/barcode     ZxingDecoder, PayloadParser
├── data/safety      UrlSafetyVerifier
├── data/local       Room entities/DAO, DataStore preferences
├── data/repository  ScanRepository, SettingsRepository
├── feature/*        onboarding, scanner, result, history, create, settings
├── nav              NavHost and routes
└── di               Hilt modules
```

Kotlin · Jetpack Compose · Hilt · CameraX · ZXing core · Room · DataStore · Navigation Compose. minSdk 26, targetSdk 35, JDK 17.

## The local-first promise

Two things make it real rather than aspirational:

1. **No `INTERNET` permission** in the manifest. Removing it is the enforcement mechanism, not a promise.
2. **The safety check is pure string inspection.** `UrlSafetyVerifier` reads the address itself — HTTP vs HTTPS, IP-literal hosts, punycode and IDN homographs, embedded credentials, high-abuse TLDs, shortened hosts, phishing keyword patterns, digit-substituted brand lookalikes. There is no DNS lookup and no reputation service, so "Link verified · no known threats" only claims what it actually checked. When it finds something, the banner says so and lists the reasons.

Opening a link, joining a network and sharing are hand-offs to other apps, not network calls made by QuickScan.

## Building

The GitHub Actions workflow in `.github/workflows/android.yml` is the build. Every push to `main` and every pull request runs:

- unit tests (`PayloadParser`, `UrlSafetyVerifier`, `QrPlaceholder`, `ScanRepository`)
- `lintDebug`
- `assembleDebug`

On a push to `main` the debug APK is then published straight to the repository's **releases page as a pre-release** under the rolling `debug` tag — no artifact step, no signing keys required. Download and install it straight from the browser.

Pushing a `v*` tag builds a signed APK and AAB and attaches them to a GitHub release. That job requires four repository secrets and fails fast if any is missing:

| Secret | Meaning |
|--------|---------|
| `KEYSTORE_BASE64` | The keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

### Toolchain

Gradle 8.7, declared in `gradle/wrapper/gradle-wrapper.properties`. The wrapper binary itself is not committed, so run the wrapper task once to materialise `gradlew` locally:

```bash
gradle wrapper          # writes gradlew, gradlew.bat and the wrapper jar
./gradlew testDebugUnitTest lintDebug assembleDebug
```

CI does the same thing without the wrapper: it installs the Gradle version named in the wrapper properties and calls `gradle` directly, so there is exactly one place that decides which Gradle is correct.

To produce a signed release locally, put a `keystore.properties` at the repository root. It is gitignored, and the release build type quietly skips signing when the file is absent:

```properties
storeFile=/path/to/quickscan.jks
storePassword=…
keyAlias=…
keyPassword=…
```

## Typeface

The design calls for **Geist** (Vercel, SIL Open Font License). `AppTypeface.kt` currently maps the family to `FontFamily.Default` so the build stays green with no vendored binaries. To switch, drop `Geist-Regular.ttf`, `Geist-Medium.ttf`, `Geist-SemiBold.ttf` and `Geist-Bold.ttf` into `app/src/main/res/font/` and replace the body of `AppTypeface.family` with the `FontFamily(...)` call shown in that file. The type scale — sizes, leading and tracking — is already correct and is not affected.

## Procedural QR artwork

Screens that show a code without carrying real content (the onboarding hero, the scanner's permission state) use `QrPlaceholder`, a deterministic generator seeded by a long. It emits a true module grid — three 7×7 finder patterns with separators, row/column-6 timing lines, the spec-derived alignment block, the always-dark module and reserved format-information areas — with the data area filled from a seeded LCG. The same seed always paints the same code, and each call site picks its own seed.

Everywhere real data appears, `QrCodeView` renders an actual ZXing `BitMatrix`, so the code on screen is scannable.