package com.quickscan.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps copied scan text from sitting on the clipboard indefinitely.
 *
 * A Wi-Fi password is the case that matters: with "Copy automatically" on it
 * would otherwise stay readable by any other app until something else copied
 * over it. A sensitive copy is cleared after a minute, but only if it is
 * still the value we put there, so a later copy by the user is never wiped.
 */
object ClipboardGuard {

    /** Long enough to paste anywhere, short enough not to linger. */
    const val DEFAULT_LIFETIME_MILLIS = 60_000L

    private var instance: Handler? = null

    private fun handlerFor(context: Context): Handler =
        instance ?: synchronized(this) {
            instance ?: Handler(context.applicationContext).also { instance = it }
        }

    /**
     * Puts [text] on the clipboard.
     *
     * @param sensitive true for a password or anything else that should not
     *   outlive the moment it was copied.
     */
    fun copy(
        context: Context,
        label: String,
        text: String,
        sensitive: Boolean = false,
    ) {
        val handler = handlerFor(context)
        if (sensitive) handler.copyThenExpire(label, text) else handler.copyPersistent(label, text)
    }

    private class Handler(context: Context) {
        private val appContext = context.applicationContext

        // Lives as long as the process, so a scheduled clear is not lost.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var pending: Job? = null

        private val clipboard: ClipboardManager?
            get() = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        /**
         * A second copy before the first expires cancels the earlier timer, so
         * the newest value is the one that gets cleared.
         */
        fun copyThenExpire(label: String, text: String) {
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            pending?.cancel()
            pending = scope.launch {
                delay(DEFAULT_LIFETIME_MILLIS)
                clearIfUnchanged(text)
            }
        }

        fun copyPersistent(label: String, text: String) {
            pending?.cancel()
            pending = null
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        }

        /**
         * Clears [text] if the clipboard still holds it. Returns true when it did.
         */
        fun clearIfUnchanged(text: String): Boolean {
            val manager = clipboard ?: return false
            val current = manager.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(appContext)
                ?.toString()
            if (current != text) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.clearPrimaryClip()
            } else {
                // Before Android 13 there is no clear; an empty clip stands in.
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            return true
        }
    }
}
