package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.net.Uri

internal const val PLAY_STORE = "com.android.vending"

internal fun Context.openStorePage(packageName: String, tag: String) = startOrLog(tag, "the Play Store page of $packageName") {
    val page = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).setPackage(PLAY_STORE)
    // In a task of its own, as when an app is launched, so HOME leaves it rather than clearing it off the launcher's.
    startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
