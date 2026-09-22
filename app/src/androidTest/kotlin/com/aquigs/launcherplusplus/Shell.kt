package com.aquigs.launcherplusplus

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/** Runs [command] as the shell user, which may change settings and roles the test app may not, and returns its output. */
fun shell(command: String): String {
    val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }
}

private const val HOUR_STYLE = "time_12_24"

fun setSystemHourStyle(twentyFourHour: Boolean) = shell("settings put system $HOUR_STYLE ${if (twentyFourHour) 24 else 12}")

/** Puts the system's hour style back as it was once a test that changes it is done; an unset one reads as "null". */
class RestoreHourStyle : ExternalResource() {
    private lateinit var style: String

    override fun before() {
        style = shell("settings get system $HOUR_STYLE").trim()
    }

    override fun after() {
        shell(if (style == "null") "settings delete system $HOUR_STYLE" else "settings put system $HOUR_STYLE $style")
    }
}
