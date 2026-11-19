package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

interface Battery {
    /** How charged the battery is, out of 100, now and as it changes, for as long as it is collected. */
    fun charge(): Flow<Int>

    /** Opens the system's battery usage. */
    fun open()
}

private const val BATTERY_TAG = "SystemBattery"

/** The battery's charge, read again on every change the system broadcasts. */
class SystemBattery(private val context: Context) : Battery {
    private val battery = context.getSystemService(BatteryManager::class.java)

    override fun charge(): Flow<Int> = context.broadcasts(IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        .map { battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }
        .filter { it in 0..100 }
        .distinctUntilChanged()

    override fun open() = startOrLog(BATTERY_TAG, "battery usage") {
        context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
