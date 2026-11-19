package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface Storage {
    /** How much of the phone's storage is free, out of 100, now and every minute after, for as long as it is collected. */
    fun free(): Flow<Int>

    /** Opens the system's storage settings. */
    fun open()
}

private const val STORAGE_TAG = "SystemStorage"

// Storage fills slowly, and the system has no broadcast for it short of running low.
private const val STORAGE_POLL_MILLIS = 60_000L

/** The data partition, where apps and their files live. */
class SystemStorage(private val context: Context) : Storage {
    override fun free(): Flow<Int> = flow {
        while (true) {
            val stat = StatFs(Environment.getDataDirectory().path)
            emit((stat.availableBytes * 100 / stat.totalBytes).toInt())
            delay(STORAGE_POLL_MILLIS)
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    override fun open() = startOrLog(STORAGE_TAG, "storage settings") {
        context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
