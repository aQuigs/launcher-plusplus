package com.sqftware.orbitlauncher.apps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Kept
import com.sqftware.orbitlauncher.domain.KeptNotification
import com.sqftware.orbitlauncher.shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val CHANNEL = "badges_test"
private const val NOTIFICATION_ID = 7

@RunWith(AndroidJUnit4::class)
class NotificationBadgesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val badges = NotificationBadges(context)
    private val listener = ComponentName(context, UnreadListener::class.java).flattenToString()
    private val self = AppEntry("Orbit", context.packageName, "irrelevant")
    private val kept = KeptUnread(context)
    private val wasEnabled = badges.isEnabled()

    // The listener changes the kept notifications on the main thread, so the test does too rather than race it.
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    // The launcher posts the notifications itself, under the permission its debug manifest declares for this test: a
    // test package may not post as another app, and the shell's notifications cannot be cancelled afterwards.
    @Before
    fun allowPostingAsTheLauncher() {
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        notificationManager.createNotificationChannel(NotificationChannel(CHANNEL, "Badge test", NotificationManager.IMPORTANCE_LOW))
        onMain { kept.update { Kept() } }
    }

    // The permission stays granted: revoking a runtime permission kills the process, this test runner included, and the
    // uninstall after the run takes it away regardless.
    @After
    fun cleanUp() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.deleteNotificationChannel(CHANNEL)
        onMain { kept.update { Kept() } }
        shell("cmd notification ${if (wasEnabled) "allow_listener" else "disallow_listener"} $listener")
    }

    private fun post(number: Int = 0) {
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Badge test")
            .setNumber(number)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @Test
    fun accessFollowsTheSystemsGrant() {
        shell("cmd notification disallow_listener $listener")
        assertFalse(badges.isEnabled())
        assertFalse(runBlocking { badges.enabled().first() })

        shell("cmd notification allow_listener $listener")
        assertTrue(badges.isEnabled())
        assertTrue(runBlocking { badges.enabled().first() })
    }

    @Test
    fun countsFollowTheNotificationsWhileAccessIsGrantedAndEmptyWhenItGoes() = runBlocking {
        withTimeout(20_000) {
            shell("cmd notification disallow_listener $listener")
            post()
            assertEquals(0, badges.counts().first()[self])

            shell("cmd notification allow_listener $listener")
            assertEquals(1, badges.counts().first { it[self] > 0 }[self])

            post(number = 5)
            badges.counts().first { it[self] == 5 }

            notificationManager.cancel(NOTIFICATION_ID)
            badges.counts().first { it[self] == 0 }

            post()
            badges.counts().first { it[self] == 1 }
            shell("cmd notification disallow_listener $listener")
            badges.counts().first { it[self] == 0 }
            assertFalse(badges.isEnabled())
        }
    }

    @Test
    fun keptCountsAddToTheLiveOnesUntilTheAppIsOpened() = runBlocking<Unit> {
        withTimeout(20_000) {
            shell("cmd notification allow_listener $listener")
            onMain { kept.update { Kept(mapOf("dismissed" to KeptNotification(context.packageName, 2))) } }
            badges.counts().first { it[self] == 2 }

            post()
            badges.counts().first { it[self] == 3 }

            onMain { badges.opened(context.packageName) }
            badges.counts().first { it[self] == 1 }
        }
    }
}
