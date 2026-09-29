package com.sqftware.orbitlauncher.apps

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.IntentCompat

private const val TAG = "SplitScreenActivity"
private const val FIRST = "first"
private const val SECOND = "second"

// Long enough for the first app's task to be in front, which the second one's must land beside, cold as well as warm.
private const val SECOND_APP_DELAY_MILLIS = 300L

/**
 * Opens a pair of apps in split screen, the first on top. The system has no call for a launcher to do that, but an app
 * started with [Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT] beside another's task enters split screen with it, only if an
 * activity starts it: from a service or the application context the flag is ignored and the app opens full screen. So
 * this starts the first app, then the second beside it once the first is in front, and goes. It draws nothing, but
 * cannot have no display at all, as that must finish before it resumes, and it must wait for the second start.
 */
class SplitScreenActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val first = IntentCompat.getParcelableExtra(intent, FIRST, ComponentName::class.java)
        val second = IntentCompat.getParcelableExtra(intent, SECOND, ComponentName::class.java)
        if (first == null || second == null) {
            finish()
            return
        }

        // Alone, the second app would open full screen: nothing to split with.
        if (!started(TAG, first.flattenToShortString()) { startActivity(launcher(first).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
            finish()
            return
        }
        handler.postDelayed({
            startOrLog(TAG, second.flattenToShortString()) {
                startActivity(launcher(second).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT))
            }
            finish()
        }, SECOND_APP_DELAY_MILLIS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun launcher(component: ComponentName) =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)

    companion object {
        /**
         * What opens [first] and [second] in split screen, started from [context]. Each start gets a task of its own, so a
         * second pair tapped while the first one's is still waiting is not handed to that one and lost.
         */
        fun intent(context: Context, first: ComponentName, second: ComponentName): Intent =
            Intent(context, SplitScreenActivity::class.java)
                .putExtra(FIRST, first)
                .putExtra(SECOND, second)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
}
