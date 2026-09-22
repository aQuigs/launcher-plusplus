package com.aquigs.launcherplusplus.apps

import android.text.format.DateFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.RestoreHourStyle
import com.aquigs.launcherplusplus.setSystemHourStyle
import com.aquigs.launcherplusplus.domain.ClockFace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SystemWallClockTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val wallClock = SystemWallClock(context)

    @get:Rule
    val restoreHourStyle = RestoreHourStyle()

    // What the platform's own formatters make of this moment in the user's style and locale. ICU keeps the narrow no-break
    // space before AM and PM that they swap for a plain space, for apps that parse the text.
    private fun platformFace(): ClockFace {
        val now = Date()
        val locale = context.resources.configuration.locales[0]
        return ClockFace(
            time = DateFormat.getTimeFormat(context).format(now),
            date = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), now).toString(),
            twentyFourHour = DateFormat.is24HourFormat(context),
        )
    }

    private fun ClockFace.withPlainSpaces() = copy(time = time.replace('\u202F', ' '))

    private fun minuteNow() = System.currentTimeMillis() / 60_000

    @Test
    fun theFaceMatchesThePlatformsOwnFormatting() {
        var expected: ClockFace
        var face: ClockFace
        // Read again if the minute turned over between the reads.
        do {
            expected = platformFace()
            face = wallClock.face(twentyFourHour = null).withPlainSpaces()
        } while (expected != platformFace())

        assertEquals(expected, face)
    }

    @Test
    fun aNewHourStyleShowsWithoutWaitingForTheMinute() = runBlocking {
        withTimeout(10_000) {
            val faces = wallClock.faces(twentyFourHour = null).produceIn(this)
            faces.receive()

            // The other style first, or the first change would be no change.
            val styles = if (DateFormat.is24HourFormat(context)) listOf(false, true) else listOf(true, false)
            for (twentyFourHour in styles) {
                setSystemHourStyle(twentyFourHour)
                faces.receiveAsFlow().first { it.withPlainSpaces() == platformFace() }
            }
            faces.cancel()
        }
    }

    // What the clock shows in the style chosen is what it would show following a system set to that style.
    @Test
    fun aChosenHourStyleOverridesTheSystems() {
        for (chosen in listOf(false, true)) {
            var following: ClockFace
            var overriding: ClockFace
            do {
                val minute = minuteNow()
                setSystemHourStyle(chosen)
                following = wallClock.face(twentyFourHour = null)
                setSystemHourStyle(!chosen)
                overriding = wallClock.face(chosen)
            } while (minuteNow() != minute)

            assertEquals(following, overriding)
        }
    }
}
