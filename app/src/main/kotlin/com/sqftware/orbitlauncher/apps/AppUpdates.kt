package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.hours

interface AppUpdates {
    /** Whether the store has a newer launcher for this user, then again each hour. Collect while the launcher is visible. */
    fun available(): Flow<Boolean>

    /** The launcher's own store page, where the update is. */
    fun openStorePage()
}

private const val TAG = "PlayAppUpdates"
private val CHECK_EVERY = 1.hours.inWholeMilliseconds

/**
 * Updates as Play offers them to this user, so a tester sees the release on the testing track they joined. A build Play
 * did not install has none.
 */
class PlayAppUpdates(private val context: Context) : AppUpdates {
    private val manager = AppUpdateManagerFactory.create(context)

    // Kept across collections, so each return to the launcher does not ask Play again within the hour.
    private var available = false
    private var nextCheck = 0L

    override fun available(): Flow<Boolean> = flow {
        while (true) {
            if (SystemClock.elapsedRealtime() >= nextCheck) {
                available = check() ?: available
                nextCheck = SystemClock.elapsedRealtime() + CHECK_EVERY
            }
            emit(available)
            delay(nextCheck - SystemClock.elapsedRealtime())
        }
    }

    override fun openStorePage() = context.openStorePage(context.packageName, TAG)

    // Null when Play cannot say, as for a build it did not install or before it has loaded what it offers, so such a check
    // keeps what the last one found.
    private suspend fun check(): Boolean? = suspendCancellableCoroutine { done ->
        manager.appUpdateInfo.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                done.resume(
                    when (task.result.updateAvailability()) {
                        UpdateAvailability.UPDATE_AVAILABLE -> true
                        UpdateAvailability.UPDATE_NOT_AVAILABLE -> false
                        else -> null
                    },
                )
            } else {
                Log.w(TAG, "Cannot check for an update", task.exception)
                done.resume(null)
            }
        }
    }
}
