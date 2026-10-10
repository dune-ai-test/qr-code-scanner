# Third-party assets

## Geist

`Geist` by Vercel, in the four weights the type scale uses. Licensed under the
SIL Open Font License 1.1: <https://github.com/vercel/geist-font>.

| Weight | File |
|--------|------|
| Regular (400) | `app/src/main/res/font/geist_regular.ttf` |
| Medium (500) | `app/src/main/res/font/geist_medium.ttf` |
| SemiBold (600) | `app/src/main/res/font/geist_semibold.ttf` |
| Bold (700) | `app/src/main/res/font/geist_bold.ttf` |

Only the unmodified binaries are vendored; `AppTypeface.kt` references them by
name and falls back to the platform font when they are absent.

This file lives outside `res/` on purpose: the Android resource merger treats
every file under `res/` as a resource, and rejects anything that is not `.xml`,
`.ttf`, `.ttc` or `.otf`. An attribution note placed next to the fonts is the
obvious place to put it, and it fails the build there.

## Lucide

Icons are vendored as SVG under `tools/lucide/`, from
[lucide-static](https://unpkg.com/lucide-static@1.54.0/) v1.54.0, ISC licensed.
`tools/lucide_to_kt.py` converts them into the stroke-only `ImageVector`s in
`LucideIcons.kt`.

## ZXing

`com.google.zxing:core`, Apache 2.0. Used for both decoding camera frames and
encoding created codes.