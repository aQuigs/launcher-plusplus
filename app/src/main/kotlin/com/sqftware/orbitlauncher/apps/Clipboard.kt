package com.sqftware.orbitlauncher.apps

import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

// More than any link needs; a clip can hold a whole document.
private const val MAX_TEXT = 2_000

interface Clipboard {
    /** The text last copied, cut short if it is long; null if there is none. Android tells the user it was read. */
    fun text(): String?
}

class SystemClipboard(context: Context) : Clipboard {
    private val manager = context.getSystemService<ClipboardManager>()

    override fun text(): String? =
        manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.run { text ?: uri?.toString() }?.take(MAX_TEXT)?.toString()
}
