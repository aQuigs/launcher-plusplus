package com.aquigs.launcherplusplus.apps

import android.text.format.DateFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SystemWallClockTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val wallClock = SystemWallClock(context)
    private val hourStyle = shell("settings get system time_12_24").trim()

    // The user's hour style comes back afterwards; an unset one reads as "null".
    @After
    fun restoreHourStyle() {
        shell(if (hourStyle == "null") "settings delete system time_12_24" else "settings put system time_12_24 $hourStyle")
    }

    // What the platform's own formatters make of this moment in the user's style and locale. ICU keeps the narrow no-break
    // space before AM and PM that they swap for a plain space, for apps that parse the text.
    private fun platformFace(): ClockFace {
        val now = Date()
        val locale = context.resources.configuration.locales[0]
        return ClockFace(
            time = DateFormat.getTimeFormat(context).format(now),
            date = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), now).toString(),
        )
    }

    private fun ClockFace.withPlainSpaces() = copy(time = time.replace('\u202F', ' '))

    @Test
    fun theFaceMatchesThePlatformsOwnFormatting() {
        var expected: ClockFace
        var face: ClockFace
        // Read again if the minute turned over between the reads.
        do {
            expected = platformFace()
            face = wallClock.face().withPlainSpaces()
        } while (expected != platformFace())

        assertEquals(expected, face)
    }

    @Test
    fun aNewHourStyleShowsWithoutWaitingForTheMinute() = runBlocking {
        withTimeout(10_000) {
            val faces = wallClock.faces().produceIn(this)
            faces.receive()

            // The other style first, or the first change would be no change.
            val styles = if (DateFormat.is24HourFormat(context)) listOf("12", "24") else listOf("24", "12")
            for (style in styles) {
                shell("settings put system time_12_24 $style")
                faces.receiveAsFlow().first { it.withPlainSpaces() == platformFace() }
            }
            faces.cancel()
        }
    }
}
