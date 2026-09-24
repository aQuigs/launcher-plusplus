package com.sqftware.orbitlauncher.apps

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.makeHomeApp
import com.sqftware.orbitlauncher.makeLauncherHome
import com.sqftware.orbitlauncher.otherHomeApp
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleManagerHomeRoleTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val requested = mutableListOf<Intent>()

    // Stands in for the activity's registry: keeps the dialog's intent and answers at once, as a dismissed dialog would.
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            requested += input as Intent
            dispatchResult(requestCode, Activity.RESULT_CANCELED, null)
        }
    }
    private val homeRole = RoleManagerHomeRole(context, registry)

    @After
    fun restoreTheLauncherAsHome() = makeLauncherHome()

    @Test
    fun theRoleFollowsTheSystemsHomeApp() {
        makeHomeApp(otherHomeApp())
        assertFalse(homeRole.isHeld())

        makeLauncherHome()
        assertTrue(homeRole.isHeld())
    }

    @Test
    fun aRequestOpensTheRoleDialogAndReadsTheRoleAgainOnItsAnswer() = runBlocking {
        withTimeout(10_000) {
            makeHomeApp(otherHomeApp())
            val held = homeRole.held().produceIn(this)
            assertFalse(held.receive())

            // What the dialog does when the user says yes, before it answers.
            makeLauncherHome()
            homeRole.request()

            // As a URI, so the extras count too: the intent names the role and the package the dialog belongs to.
            val roleDialog = context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)
            assertEquals(roleDialog.toUri(Intent.URI_INTENT_SCHEME), requested.single().toUri(Intent.URI_INTENT_SCHEME))
            assertTrue(held.receive())
            held.cancel()
        }
    }
}
