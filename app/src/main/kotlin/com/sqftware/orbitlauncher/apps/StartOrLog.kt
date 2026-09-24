package com.sqftware.orbitlauncher.apps

import android.content.ActivityNotFoundException
import android.util.Log

// What the user tapped may have gone since it was listed: an uninstall the package callback has not reported yet, a
// shortcut the app disabled, a locked user, a phone without a clock or calendar app. None of that may crash the launcher.
internal inline fun startOrLog(tag: String, what: String, start: () -> Unit) {
    try {
        start()
    } catch (e: RuntimeException) {
        when (e) {
            is ActivityNotFoundException, is SecurityException, is IllegalStateException -> Log.w(tag, "Cannot start $what", e)
            else -> throw e
        }
    }
}
