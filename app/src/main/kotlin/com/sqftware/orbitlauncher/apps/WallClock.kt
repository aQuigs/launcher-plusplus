package com.sqftware.orbitlauncher.apps

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.icu.text.DateFormat
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import androidx.core.content.ContextCompat
import com.sqftware.orbitlauncher.domain.ClockFace
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import java.util.Date

interface WallClock {
    /** The time and date to show now, the time in 24 hours if [twentyFourHour], in 12 if not, and in the system's style if null. */
    fun face(twentyFourHour: Boolean?): ClockFace

    /** [face] now, then again each minute and whenever the time, the time zone or the system's hour style changes. */
    fun faces(twentyFourHour: Boolean?): Flow<ClockFace>

    /** Opens the clock app, on its alarms. */
    fun openClock()

    /** Opens the calendar on today. */
    fun openCalendar()
}

private const val TAG = "SystemWallClock"

/** The system clock, with the date in the order the locale puts it and the hours in the 12- or 24-hour style asked for. */
class SystemWallClock(private val context: Context) : WallClock {
    override fun face(twentyFourHour: Boolean?): ClockFace {
        val locale = context.resources.configuration.locales[0]
        val now = Date()
        val inTwentyFour = twentyFourHour ?: is24HourFormat(context)
        return ClockFace(
            time = DateFormat.getInstanceForSkeleton(if (inTwentyFour) "Hm" else "hm", locale).format(now),
            date = DateFormat.getInstanceForSkeleton("EEEEMMMMd", locale).format(now),
            twentyFourHour = inTwentyFour,
        )
    }

    override fun faces(twentyFourHour: Boolean?): Flow<ClockFace> =
        callbackFlow {
            val ticks = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    trySend(Unit)
                }
            }
            // The hour style is a setting, not a broadcast; TextClock watches it the same way.
            val hourStyle = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(context, ticks, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.TIME_12_24), false, hourStyle)
            send(Unit)
            awaitClose {
                context.unregisterReceiver(ticks)
                context.contentResolver.unregisterContentObserver(hourStyle)
            }
        }
            .conflate()
            .map { face(twentyFourHour) }

    override fun openClock() = startOrLog(TAG, "the clock") {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openCalendar() = startOrLog(TAG, "the calendar") {
        val today = ContentUris.appendId(CalendarContract.CONTENT_URI.buildUpon().appendPath("time"), System.currentTimeMillis()).build()
        context.startActivity(Intent(Intent.ACTION_VIEW, today).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
