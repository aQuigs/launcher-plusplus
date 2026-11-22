package com.sqftware.orbitlauncher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

/** [MainActivity] under the name only this app may start, so a place that arrives through it was shared to the launcher. */
const val SHARED_PLACES = "com.sqftware.orbitlauncher.SharedPlaces"

/**
 * Where another app, such as a maps app, shares a place with the launcher, for the prompt that asks for one. Like
 * [PinShortcutActivity], it only hands the text on to the home screen, whose task it must not clear, and goes without
 * ever drawing.
 */
class SharePlaceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
            startActivity(
                Intent(Intent.ACTION_SEND)
                    .setComponent(ComponentName(this, SHARED_PLACES))
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finish()
    }
}
