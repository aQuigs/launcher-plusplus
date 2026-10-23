package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BroadcastsTest {
    // Home can destroy a launcher before the flows it collects are cancelled: one that Play's Open button started after
    // an update, say.
    @Test
    fun aCollectionOutlivingItsActivityEndsCleanly() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        val collecting = Job()
        try {
            lateinit var activity: Context
            scenario.onActivity { activity = it }
            activity.broadcasts(IntentFilter(Intent.ACTION_TIME_TICK)).launchIn(CoroutineScope(Dispatchers.Main + collecting))
            instrumentation.waitForIdleSync()

            scenario.moveToState(Lifecycle.State.DESTROYED)
            // The system drops the activity's receivers in a cleanup it posts as the activity is destroyed.
            instrumentation.waitForIdleSync()

            var ending: Throwable? = null
            collecting.invokeOnCompletion { ending = it }
            collecting.cancel()
            withTimeout(5_000) { collecting.join() }
            assertTrue("The flow ended with $ending", ending is CancellationException)
        } finally {
            collecting.cancel()
            scenario.close()
        }
    }
}
