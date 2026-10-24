package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.widget.Toast

interface Toasts {
    /** Shows [text] in a short toast, after any already showing or waiting. */
    fun show(text: String)
}

class SystemToasts(private val context: Context) : Toasts {
    override fun show(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
