package com.sqftware.orbitlauncher.apps

import android.content.ActivityNotFoundException
import android.util.Log

// What the user tapped may have gone since it was listed: an uninstall the package callback has not reported yet, a
// shortcut the app disabled, a locked user, a phone without a clock or calendar app. None of that may crash the launcher.
internal inline fun startOrLog(tag: String, what: String, start: () -> Unit) {
    started(tag, what, start)
}

/** Whether [start] started [what], as [startOrLog] does, for a caller that goes on only if it did. */
internal inline fun started(tag: String, what: String, start: () -> Unit): Boolean = try {
    start()
    true
} catch (e: RuntimeException) {
    when (e) {
        is ActivityNotFoundException, is SecurityException, is IllegalStateException -> {
            Log.w(tag, "Cannot start $what", e)
            false
        }
        else -> throw e
    }
}
