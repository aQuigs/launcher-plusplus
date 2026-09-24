package com.sqftware.orbitlauncher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.sqftware.orbitlauncher.apps.LauncherAppsRepository

/** [MainActivity] under the name only this app may start, so a pin request that arrives through it came from the system. */
const val PIN_REQUESTS = "com.sqftware.orbitlauncher.PinRequests"

/**
 * Where the system asks the home app to confirm a shortcut another app wants on the home screen. The system starts it in
 * a new, cleared task, which would clear the home screen's own task if [MainActivity] took the request itself, so it only
 * hands the request on to the home screen, and goes without ever drawing. A request the launcher cannot take goes no
 * further, so the app that asked stays in front.
 */
class PinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LauncherAppsRepository(this).pinRequest(intent) != null) {
            startActivity(Intent(intent.action).setComponent(ComponentName(this, PIN_REQUESTS)).putExtras(intent))
        }
        finish()
    }
}
