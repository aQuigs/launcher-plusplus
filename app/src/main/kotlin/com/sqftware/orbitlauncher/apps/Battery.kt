package com.sqftware.orbitlauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface Battery {
    /** How charged the battery is, out of 100, now and as it changes, for as long as it is collected. */
    fun charge(): Flow<Int>

    /** Opens the system's battery usage. */
    fun open()
}

private const val BATTERY_TAG = "SystemBattery"

/** The battery as the system broadcasts it, which it does at once to a new receiver and again on every change. */
class SystemBattery(private val context: Context) : Battery {
    override fun charge(): Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) trySend(level * 100 / scale)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

    override fun open() = startOrLog(BATTERY_TAG, "battery usage") {
        context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
