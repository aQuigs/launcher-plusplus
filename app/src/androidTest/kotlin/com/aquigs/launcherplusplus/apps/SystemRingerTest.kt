package com.aquigs.launcherplusplus.apps

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.domain.RingerMode
import com.aquigs.launcherplusplus.shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemRingerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val ringer = SystemRinger(context)
    private val self = context.packageName
    private val wasGranted = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    private val ringerMode = ringer.mode()
    private val zenMode = shell("settings get global zen_mode").trim()

    // Do Not Disturb comes back after the ringer, which may have moved it. The system puts back a zen_mode written
    // straight to the setting, so it goes through the notification service, which names the modes.
    @After
    fun restore() {
        setRinger(ringerMode)
        shell("cmd notification set_dnd ${listOf("off", "priority", "none", "alarms").getOrElse(zenMode.toInt()) { "off" }}")
        shell("cmd notification ${if (wasGranted) "allow_dnd" else "disallow_dnd"} $self")
    }

    private fun setRinger(mode: RingerMode) = shell("cmd audio set-ringer-mode ${mode.name.uppercase()}")

    @Test
    fun theModeFollowsTheSystemsRinger() = runBlocking {
        withTimeout(10_000) {
            val modes = ringer.modes().produceIn(this)
            for (mode in listOf(RingerMode.Vibrate, RingerMode.Silent, RingerMode.Normal)) {
                setRinger(mode)
                modes.receiveAsFlow().first { it == mode }
            }
            modes.cancel()
        }
    }

    @Test
    fun withAccessATapStepsOnFromTheRingersRealMode() {
        shell("cmd notification allow_dnd $self")
        setRinger(RingerMode.Vibrate)

        ringer.cycle()
        assertEquals(RingerMode.Silent, ringer.mode())

        ringer.cycle()
        assertEquals(RingerMode.Normal, ringer.mode())
    }

    @Test
    fun withoutAccessATapChangesNothingAndOpensTheAccessScreen() {
        shell("cmd notification disallow_dnd $self")
        setRinger(RingerMode.Normal)

        ringer.cycle()

        assertEquals(RingerMode.Normal, ringer.mode())
        val deadline = System.currentTimeMillis() + 5_000
        while (shell("dumpsys activity activities").lineSequence().none { "topResumedActivity" in it && "ZenAccess" in it }) {
            assertTrue("the access screen did not open", System.currentTimeMillis() < deadline)
            Thread.sleep(100)
        }
        shell("input keyevent BACK")
    }
}
