package com.aquigs.launcherplusplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.ui.HomePress
import com.aquigs.launcherplusplus.ui.LauncherScreen
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = LauncherAppsRepository(this)
        val layout = PageLayout()

        setContent {
            LauncherTheme {
                val apps by produceState(emptyList<AppEntry>()) {
                    value = withContext(Dispatchers.IO) { repository.installedApps() }
                }
                LauncherScreen(
                    layout = layout,
                    homePresses = homePresses,
                    apps = apps,
                    onLaunch = repository::launch,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    // A HOME press relaunches the home activity, which singleTask delivers here. The lifecycle state cannot say where the
    // press came from: the framework pauses a resumed launcher and starts a stopped one before delivering, so both arrive
    // STARTED. The system does flag the press that brought the launcher's task to the front. getIntent() deliberately
    // stays the launch intent: nothing reads it later, and ActivityScenario identifies the activity by it.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            val broughtToFront = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0
            homePresses.tryEmit(HomePress(launcherInFront = !broughtToFront))
        }
    }
}
