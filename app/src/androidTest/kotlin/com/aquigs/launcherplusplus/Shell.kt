package com.aquigs.launcherplusplus

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/** Runs [command] as the shell user, which may change settings and roles the test app may not, and returns its output. */
fun shell(command: String): String {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }
}
