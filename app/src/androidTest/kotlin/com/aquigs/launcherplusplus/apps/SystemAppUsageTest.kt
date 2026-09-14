package com.aquigs.launcherplusplus.apps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.shell
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemAppUsageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val usage = SystemAppUsage(context)

    // The app op is what the usage-access settings screen sets; the shell may set it without that screen.
    private fun setUsageAccess(mode: String) = shell("appops set ${context.packageName} GET_USAGE_STATS $mode")

    @After
    fun revoke() {
        setUsageAccess("default")
    }

    @Test
    fun accessFollowsTheAppOpAndTheWeeksUsageComesOnlyWithIt() = runBlocking {
        setUsageAccess("default")
        assertFalse(usage.isUsageAccessGranted())
        assertNull(usage.foregroundTime().first())

        setUsageAccess("allow")
        assertTrue(usage.isUsageAccessGranted())
        val time = usage.foregroundTime().first()

        assertNotNull(time)
        assertTrue("something has been in front this week", time!!.byPackage.isNotEmpty())
        assertTrue("only packages that were in front: ${time.byPackage}", time.byPackage.values.all { it > 0L })
    }
}
