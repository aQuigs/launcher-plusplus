package com.aquigs.launcherplusplus.apps

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.shell
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusBarNotificationShadeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // The expanded shade is the window with the focus.
    private fun focus() = shell("dumpsys window").lineSequence().first { "mCurrentFocus" in it }

    @After
    fun collapse() {
        shell("cmd statusbar collapse")
    }

    // The call is hidden from the SDK, so a system image that blocks it is caught here rather than by a swipe that does
    // nothing.
    @Test
    fun opensTheShade() {
        // The lock screen is drawn in the shade's window too, and would pass for the shade opened.
        focus().let { assertFalse(it, "NotificationShade" in it) }

        StatusBarNotificationShade(context).open()

        val deadline = SystemClock.uptimeMillis() + 5_000
        var focus = focus()
        while ("NotificationShade" !in focus && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
            focus = focus()
        }
        assertTrue(focus, "NotificationShade" in focus)
    }
}
