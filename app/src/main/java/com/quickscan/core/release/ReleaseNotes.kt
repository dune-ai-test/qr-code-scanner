package com.quickscan.core.release

/**
 * What's new, newest first.
 *
 * The list is the release history: every version that shipped something worth
 * saying, newest at the top. Adding an entry here and bumping the version is
 * the whole workflow, and the screen marks the newest entry seen so the badge
 * only lights up for something you have not read yet.
 */
object ReleaseNotes {

    data class Entry(
        val version: String,
        val date: String,
        val highlights: List<Highlight>,
    )

    data class Highlight(val title: String, val detail: String)

    /** Newest first. */
    val entries: List<Entry> = listOf(
        Entry(
            version = "1.2.0",
            date = "October 2026",
            highlights = listOf(
                Highlight(
                    title = "Keep scanning without leaving",
                    detail = "Continuous mode saves each code, says what it read, " +
                        "and leaves the camera pointed at the next one. For a table " +
                        "of codes rather than one at a time.",
                ),
                Highlight(
                    title = "It tells you when to hold still",
                    detail = "A scan that keeps failing now says \"Hold steady\" " +
                        "instead of looking identical to pointing at nothing. It " +
                        "knows the difference from the finder patterns in frame.",
                ),
                Highlight(
                    title = "Links that do things, not just links",
                    detail = "geo:, tel:, sms: and mailto: codes get their own " +
                        "result screen and the matching action — open in Maps, " +
                        "call, send a message, write an email.",
                ),
                Highlight(
                    title = "Many codes on one sheet",
                    detail = "Paste a list, one per line, and get every code on a " +
                        "single image you can print, share or pin up.",
                ),
                Highlight(
                    title = "Bulk actions, by type",
                    detail = "Select several scans and act on all of them, or " +
                        "switch a type off to delete only the ones you meant.",
                ),
                Highlight(
                    title = "Zoom, and codes that look the same everywhere",
                    detail = "Pinch the viewfinder to get closer, and a code now " +
                        "renders the way it was made wherever you open it.",
                ),
            ),
        ),
        Entry(
            version = "1.1.0",
            date = "October 2026",
            highlights = listOf(
                Highlight(
                    title = "Export a code as an image",
                    detail = "Save a created QR to your Photos, or share the image " +
                        "itself instead of a wall of encoded text.",
                ),
                Highlight(
                    title = "Style & colours",
                    detail = "Pick the module colour, round the corners, and drop " +
                        "a logo in the middle. A code with a logo switches to high " +
                        "error correction so it still scans.",
                ),
                Highlight(
                    title = "Favourites",
                    detail = "Pin the scans you keep coming back for. Pinned items " +
                        "sort to the top of History.",
                ),
            ),
        ),
        Entry(
            version = "1.0.0",
            date = "September 2026",
            highlights = listOf(
                Highlight(
                    title = "Scan QR, Data Matrix, EAN, UPC and more",
                    detail = "Continuous scanning from the camera, or a still from " +
                        "your photo library. Everything is decoded on the device.",
                ),
                Highlight(
                    title = "A result screen per payload",
                    detail = "Links open straight away, Wi-Fi codes reveal a " +
                        "password and offer to join, contacts offer to save.",
                ),
                Highlight(
                    title = "History that groups itself",
                    detail = "Scans grouped by day with stats and filters for links, " +
                        "Wi-Fi and text.",
                ),
                Highlight(
                    title = "Nothing leaves the phone",
                    detail = "No account, no cloud, no network calls. The app does " +
                        "not even ask for internet permission.",
                ),
            ),
        ),
    )

    val current: Entry get() = entries.first()

    /** True when there is an entry the reader has not seen yet. */
    fun isUnseen(lastSeenVersion: String?): Boolean =
        lastSeenVersion != current.version
}
