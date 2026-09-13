package com.aquigs.launcherplusplus.apps

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

interface HomeRole {
    /** Whether the launcher is the device's home app right now. */
    fun isHeld(): Boolean

    /** [isHeld] now, then again each time the user has answered a [request]. */
    fun held(): Flow<Boolean>

    /** Asks the user to make the launcher the home app. */
    fun request()
}

private const val TAG = "RoleManagerHomeRole"
private const val REQUEST_KEY = "home_role"

/**
 * The home role as the system's role manager sees it. The role dialog learns which app is asking from the calling
 * package, which only a start for a result sets, so requests go through the activity's [registry]. The adapter lives as
 * long as the activity and never unregisters.
 */
class RoleManagerHomeRole(private val context: Context, registry: ActivityResultRegistry) : HomeRole {
    private val roleManager = context.getSystemService(RoleManager::class.java)
    private val answers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val roleDialog = registry.register(REQUEST_KEY, StartActivityForResult()) { answers.tryEmit(Unit) }

    override fun isHeld(): Boolean = roleManager.isRoleHeld(RoleManager.ROLE_HOME)

    // The dialog only pauses the launcher, so its answer is what prompts the next read. A change made in Settings shows
    // when the launcher comes back to the front and collects afresh.
    override fun held(): Flow<Boolean> = answers.onStart { emit(Unit) }.map { isHeld() }

    override fun request() {
        var asked = false
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            startOrLog(TAG, "the home role dialog") {
                roleDialog.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                asked = true
            }
        }
        // Without the dialog, the settings page for the default home app does the same job.
        if (!asked) {
            startOrLog(TAG, "the home app settings") {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
