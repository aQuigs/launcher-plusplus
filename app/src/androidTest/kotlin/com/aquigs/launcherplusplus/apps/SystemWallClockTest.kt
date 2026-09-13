package com.aquigs.launcherplusplus.apps

import android.icu.text.DateFormatSymbols
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import java.time.format.TextStyle

@RunWith(AndroidJUnit4::class)
class SystemWallClockTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val wallClock = SystemWallClock(context)
    private val locale = context.resources.configuration.locales[0]
    private val hourStyle = shell("settings get system time_12_24").trim()

    // The user's hour style comes back afterwards; an unset one reads as "null".
    @After
    fun restoreHourStyle() {
        shell(if (hourStyle == "null") "settings delete system time_12_24" else "settings put system time_12_24 $hourStyle")
    }

    // Read again if the minute turned over between the two reads.
    private fun nowAndFace(): Pair<ZonedDateTime, ClockFace> {
        while (true) {
            val now = ZonedDateTime.now()
            val face = wallClock.face()
            if (now.minute == ZonedDateTime.now().minute) return now to face
        }
    }

    private fun expectedTime(now: ZonedDateTime, twentyFourHour: Boolean) =
        if (twentyFourHour) "%02d:%02d".format(now.hour, now.minute) else "${(now.hour + 11) % 12 + 1}:%02d".format(now.minute)

    @Test
    fun theFaceSpellsTodayInTheUsersStyle() {
        val (now, face) = nowAndFace()
        val twentyFourHour = DateFormat.is24HourFormat(context)

        assertTrue(face.time, face.time.startsWith(expectedTime(now, twentyFourHour)))
        val period = DateFormatSymbols.getInstance(locale).amPmStrings[if (now.hour < 12) 0 else 1]
        assertEquals(face.time, !twentyFourHour, face.time.endsWith(period))
        val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val month = now.month.getDisplayName(TextStyle.FULL, locale)
        for (part in listOf(dayOfWeek, month, now.dayOfMonth.toString())) assertTrue(face.date, part in face.date)
    }

    @Test
    fun facesBeginWithTheCurrentFace() {
        var face: ClockFace
        var first: ClockFace
        do {
            face = wallClock.face()
            first = runBlocking { withTimeout(1_000) { wallClock.faces().first() } }
        } while (face != wallClock.face())

        assertEquals(face, first)
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
                faces.receiveAsFlow().first { face -> (style == "24") == face.time.none(Char::isLetter) }
            }
            faces.cancel()
        }
    }
}
