# QuickScan

A local-first QR and barcode scanner for Android, built around one rule: **everything happens on the device.** There is no account, no cloud sync and no network call anywhere in the flow — the app does not even declare the `INTERNET` permission.

The visual language is deliberately iOS-native rather than Material: a floating frosted capsule tab bar, 12–31 px corner radii, hairline strokes and quiet layered shadows, so the product reads as a first-party system utility rather than an Android port. Every colour, spacing step and type size is a token in `core/ui/theme`, so the whole product re-themes from a single change.

## Features

Everything the build contains today.

### Onboarding

- **Welcome screen** — hero with the procedural QR, corner brackets and
  accent glow; three value props (instant decoding, works offline, private
  by default); "Get started"
- **Name screen** — "What should we call you?", an avatar that recomputes
  its initial as you type, a field with a focus ring and clear button, a
  green privacy hint, and both Continue and "Skip for now"
- Persisted, so it runs once

### Scanning

- **CameraX** preview with live analysis
- **Reticle** with four corner brackets, and a sweep line that holds still
  when the system animation scale is off
- **Auto-detect pill** — green while codes are being read, red when the
  shutter is the only way in
- **Torch** toggle and **front/back camera** flip
- **Zoom** — pinch anywhere on the viewfinder for continuous zoom, or tap
  the pill under the torch to step through the stops the lens actually has
  (0.5x, 1x, 2x and up). The pill is hidden on a fixed-focal lens
- **Shutter button** — grabs a single frame and decodes it, in either
  auto-detect mode
- **Scan image** from the photo library, and **Paste link** for a typed or
  shared address
- **Continuous mode** — off by default. When on, a detection is saved,
  announced in a toast naming what was read, and the camera keeps scanning
  rather than opening the result. For a table of codes rather than one at a
  time. A pill under Auto-detect says so, because not navigating would
  otherwise look like the app hanging. See
  [The repeat gate](#the-repeat-gate) for why holding one code still in frame
  does not announce it twice.
- **Formats**: QR, Data Matrix, Aztec, PDF417, EAN-13, EAN-8, UPC-A, UPC-E,
  Code 128, Code 39, Code 93, ITF, Codabar
- **Centre-crop decoding** with a full-frame fallback, plus a retry on
  inverted frames for light-on-dark codes
- **Decode confidence** — a "Hold steady" hint that appears while a decode
  keeps failing, driven by how many QR finder patterns are visible. Turns a
  scan that is not working into feedback instead of silence; see
  [Decode confidence](#decode-confidence)
- **FrameGate** and **full-resolution escalation** — see [Scanning](#scanning)
- **Deep links** — `geo:`, `tel:`, `sms:` and `mailto:` are instructions
  rather than addresses, so each gets its own result type and its own action
  instead of falling through to plain text. See
  [Deep links](#deep-links)

### Results — one layout per payload type

- **URL** — real QR, type badge, decoded address, timestamp, copy, open and
  share, a details card (Type, Length, Scanned, Source), and delete
- **Wi-Fi** — wifi chip, SSID, security, a revealable password, join, copy,
  details, and "Save for later"
- **Contact** — avatar, name, phone and email, call or share
- **Text and product** — type icon, badge, raw content, share
- **Pin and unpin** from the nav action

### Create

- **Four content types** — Link, Text, Wi-Fi, Contact, each with its own
  field set
- **Live preview** that updates as you type
- **Style & colours** — five module colours, three corner shapes (square,
  soft, round) and five logo overlays (none, app, link, Wi-Fi, contact). A
  logo switches the code to high error correction so it survives the hole
- **Save to history**
- **Export** — renders a 1024px PNG, saves it to Photos, or shares the
  image itself through a FileProvider

### History

- **Stats** — total scans, this week, links
- **Filter chips** — All, Links, Wi-Fi, Text
- **Search** across the list
- **Date grouping** — Today, Yesterday, weekday, then date
- **Favourites** — pinned scans in their own section, sorted to the top
- **Bulk actions** — long-press to select, select-all, share as text, pin,
  and delete with confirmation and a count
- **Per-type bulk actions** — when a selection spans more than one type, a
  chip row appears under the toolbar with each type and its ticked count.
  Switching a chip off takes that type out of *every* bulk action, so
  "delete only the Wi-Fi ones" is two taps: turn Links and Text off, delete.
  The count in the toolbar and in the confirmation both narrow with the
  chips, so the number being deleted is always the number on screen.
  The browse filters hide while selecting, so there is only ever one row of
  chips on screen and its meaning is unambiguous.
- **Empty state** with a scan call to action and three suggestions
- **Clear scan history**, with confirmation

Bulk pin writes the flag outright rather than flipping each row. Pinning a
selection where some rows are already pinned means pinning all of them;
toggling would scatter the ones that happened to be unpinned. When every row
in scope is already pinned the same button becomes an unpin.

### Settings

- **Profile** card
- **Scanner** — auto-detect, continuous scanning, default camera, copy
  automatically, scan sound, vibrate
- **Appearance** — dark mode, four accent colours, larger text
- **History & storage** — a retention picker offering 7 days, 30 days,
  90 days, 1 year and **Always**; storage used; clear history
- **Permissions** — camera, photo library and notifications, each showing
  live grant state
- **About** — version, and What's new
- **What's new screen** — the release history, with an unread dot on the
  Settings row until it has been opened

### Navigation and system

- A capsule tab bar across Scan, History, Create and Settings that
  **floats over** the content instead of reserving a band for it
- Nav host with typed routes
- **Launcher shortcuts** for Scan, History and Create

### Underneath

- **Design tokens** — colour, type, radii and spacing as one system, with
  four accents each carrying a light and a dark tint
- **Geist** vendored in four weights
- **ZXing** for both decode and encode, behind a stateless decoder
- **Room** for history, **DataStore** for preferences, **Hilt** for wiring
- **Procedural QR generator** — seeded 25x25 grids with finder patterns,
  separators, timing lines, an alignment block and the dark module
- **Clipboard expiry** — see [Privacy](#privacy)

## Screens

| # | Screen | Notes |
|---|--------|-------|
| 1 | Onboarding — welcome | Value props, procedural QR hero |
| 2 | Onboarding — name | Live avatar preview that tracks the field |
| 3 | Scan | CameraX preview, reticle, torch, flip, gallery, paste |
| 4 | Result — website | Decoded URL with copy, open, share and pin |
| 5 | Result — Wi-Fi | SSID, revealable password, join action |
| 6 | History | Stats, filters, search, favourites, bulk and per-type bulk actions |
| 7 | History — empty | First-run state with suggestions |
| 8 | Create | Live preview, four content types, styling, PNG export |
| 9 | Settings | Scanner, appearance, storage, permissions, about |
| 10 | What's new | Release history, read from `ReleaseNotes` |

The result screen routes by payload type, so text, contact, product and the
deep-link types each get their own action set rather than a URL-shaped one.

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

Kotlin · Jetpack Compose · Hilt · CameraX · ZXing core · Room · DataStore · Navigation Compose. minSdk 27, targetSdk 35, JDK 17.

## The local-first promise

**No `INTERNET` permission** in the manifest. Removing it is the enforcement mechanism, not a promise: there is no code path that could open a socket even by accident. Opening a link, joining a network and sharing are hand-offs to other apps.

## Building

The GitHub Actions workflow in `.github/workflows/android.yml` is the build. Every push to `main` and every pull request runs:

- unit tests (`PayloadParser`, `QrPlaceholder`, `ScanRepository`, `LumaRotation`, `FrameGate`, `Zoom`, `SelectionScope`, `RepeatGate`, `DeepLinkParser`,
  `FinderPatternScan`)
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

### The repeat gate

Continuous mode leaves the camera pointed at whatever it just read, so
the analyser decodes that same code again every time the frame gate lets
a still scene through — about every 750ms. Without a guard, holding a
code in frame would announce it several times a second.

`RepeatGate` answers one question: is this the code I just read, still
sitting there? A payload is refused while it is the same one as the last
accepted, and becomes eligible again the moment a *different* payload is
read. So each code announces once per approach: holding it still says
nothing more, and returning to it after dealing with the next one says it
again.

Two details follow from that shape rather than from the constant. Only the
last payload is kept, so the gate cannot grow during a long session and
does not accumulate a list of what everyone at a table scanned. And the
window is a floor rather than a delay — about 2s, comfortably above the
750ms gap between decodes of a still scene — so it only ever suppresses a
genuine repeat.

An earlier version keyed on a map of everything seen with a five-second
window. It passed the same tests it was written against and was still
wrong: five seconds outlives the repository's three-second dedupe window,
so holding one code for six seconds wrote a second history row for it.
That is the kind of bug that only shows up in a feature nobody has used
yet, which is why the window is now shorter than the dedupe it sits on.

## Decode confidence

A failed decode says nothing about *why* it failed. The viewfinder looks the
same whether the camera has found a code and cannot quite read it, or has
found nothing at all — which is what makes a scan that is not working feel
like a dead end rather than something you can act on.

`FinderPatternScan` looks at what a QR code is made of. A finder pattern is a
7×7 concentric square whose dark/light runs measure 1:1:3:1:1, so scanning a
handful of scanlines for that ratio says how much of a code is in frame. One
or two patterns means something is arriving; three means a whole code is
framed and a decode is close behind. The viewfinder says "Hold steady" for
either, and nothing when there is genuinely nothing there.

ZXing runs this same state machine while trying to read a code, but only
surfaces the result once a decode has already succeeded — the opposite of
when it is useful. Doing it here means the answer arrives while the decode is
still failing, which is the only time it is worth anything.

Three things are deliberately not done:

- **No cross-checks.** ZXing verifies that three candidate patterns really do
  form a QR code before believing them. Only the count matters here, not the
  position, so that work is skipped — which is why the centre run is held to
  half a module rather than ZXing's three. At three, a 1:1:2:1:1 run passes
  as a pattern and the ratio stops carrying any weight on its own.
- **No perspective transform.** "How many" is a much easier question than
  "where and at what angle", and costs about a tenth of the work.
- **No 1D barcodes.** A finder pattern is a QR idea. An EAN-13 in frame gets
  no hint, which is honest: the signal does not exist for that symbology, and
  inventing one would be a guess dressed as feedback.

The scan runs on the same geometry the decoder reads — same width, height,
row stride and origin — because a confidence measured against different pixels
from the ones the decoder sees would be describing a different frame. It
reads fourteen scanlines out of the luma plane by absolute index, so nothing
is copied but those lines.

`tools/check_confidence.py` mirrors the state machine and replays every case
in `FinderPatternScanTest` against synthetic scanlines. A finder pattern has a
fixed shape, so a row containing one can be written down exactly rather than
photographed. What no test reaches is how often this agrees with a real code
at a real angle, which is the part only a device can answer.

## Deep links

`geo:`, `tel:`, `sms:` and `mailto:` are instructions, not addresses. A scanner
that only knows `http` shows all four as plain text, which throws away the
intent: the user is left copying a URI by hand instead of being offered a
place to open, a number to dial or a message to write.

`DeepLinkParser` turns each into its own `PayloadType`, so the result screen
gives it the action that matches — Open in Maps, Call number, Send message,
Send email — instead of a Share button.

| Scheme | Carries | Result |
|--------|---------|--------|
| `geo:` | coordinates, optional `?q=` label, `?z=` or `;u=` zoom | Location |
| `tel:` | a number, `;ext=` dropped | Phone |
| `sms:` / `smsto:` | a number, body from `?body=` or a trailing colon | Sms |
| `mailto:` | address, `?subject=`, `?body=` | Email |

Three decisions worth naming:

- **A malformed deep link stays text.** `place()` rejects coordinates outside
  the valid range and `?q=` values with no comma, so a nonsense `geo:` comes
  back null and falls through to the plain-text result. Opening a map in the
  ocean is worse than showing the string that produced it.
- **`geo:0,0?q=51.5,-0.12(Home)` prefers the query.** That is the shape Google
  hands out: the path is null island and the real place is in `?q=`. Reading
  the path would open the Atlantic.
- **`+` is a space only inside a query.** `URLDecoder` is not used, because it
  would turn the `+` in a `+44` number into a space the moment the number
  passed through a query string.

`mailto:` and `tel:` used to produce a Contact payload. Contact is a vCard, and
has no "send email" action — a `mailto:` with no phone number landed on a
screen offering nothing but Share.

The parser is pure string handling with several interacting branches, so
`tools/check_deeplink.py` mirrors it and replays every case in
`DeepLinkParserTest` locally. Two bugs in the geo branches — `;u=15` breaking
the coordinate split, and a bare `?q=Egg HQ` losing its label — were found by
running it, after tracing by eye had missed both.

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

## Status

Worth knowing before trusting any of this on a device.

- **CI is not the same as a device.** Everything compiles, lints and passes
  unit tests, but the most recent features — PNG export, the style panel,
  What's new, favourites, bulk actions, per-type bulk actions, launcher
  shortcuts, Geist, camera zoom, continuous mode, deep links, the clipboard
  policy, decode confidence and the scanning budget — have not been seen on a
  screen, because no phone was connected while they were written.
- **Live auto-detection is unconfirmed** on real hardware. The shutter
  path, which uses the same decoder, is proven: it reads a photographed
  QR end to end. The live path additionally goes through `FrameGate`,
  which makes it stricter rather than looser, so it is the first thing
  to check on a device.
- **The signed release job has never run.** It needs the four keystore
  secrets above and a `v*` tag; it is the only path in CI with no
  execution history.
- **Release builds are unverified with R8.** The ProGuard rules keep all
  of ZXing, which may be broader than needed.
- **Room schema export is off**, so the first migration will be blind.
