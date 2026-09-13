package com.aquigs.launcherplusplus

import android.app.role.RoleManager
import androidx.test.platform.app.InstrumentationRegistry

private val launcherPackage get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

/** Makes [packageName] the home app, as the user would in Settings; the role is exclusive, so the holder before loses it. */
fun makeHomeApp(packageName: String) {
    val output = shell("cmd role add-role-holder --user 0 ${RoleManager.ROLE_HOME} $packageName")
    check(output.isBlank()) { "Cannot make $packageName the home app: $output" }
}

/** Makes the launcher the home app again, as `scripts/run.sh` does. */
fun makeLauncherHome() = makeHomeApp(launcherPackage)

/** Another home app to hand the role to: the first the system lists that is not the launcher. */
fun otherHomeApp(): String =
    shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
        .lines()
        .map { it.trim().substringBefore('/', missingDelimiterValue = "") }
        .first { it.isNotEmpty() && it != launcherPackage }
