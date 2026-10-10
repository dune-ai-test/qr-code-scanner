# QuickScan

A local-first QR and barcode scanner for Android, built around one rule: **everything happens on the device.** There is no account, no cloud sync and no network call anywhere in the flow — the app does not even declare the `INTERNET` permission.

The visual language is deliberately iOS-native rather than Material: a floating frosted capsule tab bar, 12–31 px corner radii, hairline strokes and quiet layered shadows, so the product reads as a first-party system utility rather than an Android port. Every colour, spacing step and type size is a token in `core/ui/theme`, so the whole product re-themes from a single change.

## Screens

| # | Screen | Notes |
|---|--------|-------|
| 1 | Onboarding — welcome | Value props, procedural QR hero |
| 2 | Onboarding — name | Live avatar preview that tracks the field |
| 3 | Scan | CameraX preview, reticle, torch, flip, gallery, paste |
| 4 | Result — website | Decoded URL with copy, open and share |
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
├── data/barcode     ZxingDecoder, LumaRotation, FrameGate, PayloadParser
├── data/local       Room entities/DAO, DataStore preferences
├── data/repository  ScanRepository, SettingsRepository
├── feature/*        onboarding, scanner, result, history, create, settings
├── nav              NavHost and routes
└── di               Hilt modules
```

Kotlin · Jetpack Compose · Hilt · CameraX · ZXing core · Room · DataStore · Navigation Compose. minSdk 26, targetSdk 35, JDK 17.

## The local-first promise

**No `INTERNET` permission** in the manifest. Removing it is the enforcement mechanism, not a promise: there is no code path that could open a socket even by accident. Opening a link, joining a network and sharing are hand-offs to other apps.

## Building

The GitHub Actions workflow in `.github/workflows/android.yml` is the build. Every push to `main` and every pull request runs:

- unit tests (`PayloadParser`, `QrPlaceholder`, `ScanRepository`, `LumaRotation`, `FrameGate`)
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

## Icons

Icons come from [Lucide](https://lucide.dev) v1.54.0 (ISC). The published SVGs
are vendored under `tools/lucide/`, and `tools/lucide_to_kt.py` turns them into
the stroke-only `ImageVector`s in `LucideIcons.kt` — rewriting `circle`,
`ellipse`, `rect`, `line` and `polyline` as path data first, then parsing the
result into Compose calls. Regenerate after swapping an SVG:

```bash
python tools/lucide_to_kt.py tools/lucide     app/src/main/java/com/quickscan/core/ui/component/LucideIcons.kt
```

Keeping the icons generated rather than hand-written matters: an approximate
path looks plausible in code review and renders as an unrecognisable shape on
the device.

## Scanning

Decoding is budgeted, because analysing every frame of a 720p stream is
about 0.9 megapixels of luminance work thirty times a second for a
viewfinder that has usually not moved.

- `FrameGate` rate-limits decoding to ten passes a second and skips any
  frame whose sparse signature is unchanged, while still letting a
  still scene through once every 750ms so a slow change is not missed.
- When the viewfinder keeps changing but the live stream reads nothing,
  one **full-resolution still** is captured and decoded instead, which
  is what finds a small or distant code. That is budgeted too: at most
  one every 2.5 seconds, and only after three scene changes.
- Photos and captures are sampled to a 1600px long edge before they
  reach ZXing. A 12MP photo is a 51MB int array; no code needs that
  much resolution.
- The rotation and row packing live in `LumaRotation`, separate from
  the decoder, because that is where a camera bug once left half the
  frame black and nothing could ever decode.

## Typeface

**Geist** (Vercel, SIL Open Font License 1.1). The four weights the
type scale uses are vendored under `app/src/main/res/font/`; see the
README there for attribution.

## Privacy

Copies pass through a single `ClipboardGuard`. A Wi-Fi password, or a
decoded result read from a code, is cleared after a minute. Everything
else persists, because a URL you deliberately copied should still be
there when you paste it. The clear only fires if the clipboard still
holds what we put there, so a later copy is never wiped.

Looping animations read the system animation scale and hold still when
animations are switched off.

## Procedural QR artwork

Screens that show a code without carrying real content (the onboarding hero, the scanner's permission state) use `QrPlaceholder`, a deterministic generator seeded by a long. It emits a true module grid — three 7×7 finder patterns with separators, row/column-6 timing lines, the spec-derived alignment block, the always-dark module and reserved format-information areas — with the data area filled from a seeded LCG. The same seed always paints the same code, and each call site picks its own seed.

Everywhere real data appears, `QrCodeView` renders an actual ZXing `BitMatrix`, so the code on screen is scannable.