package com.aquigs.launcherplusplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.ui.LauncherScreen
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalMaterial3Api::class)
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
                    homeRequests = homeRequests,
                    apps = apps,
                    onLaunch = repository::launch,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    // A HOME press relaunches the home activity, which singleTask delivers here. It only counts while the launcher is
    // already in front; coming back from an app keeps the page you left, like the stock launcher. The framework pauses
    // a resumed activity around onNewIntent, so "in front" arrives as STARTED, while a launcher stopped behind another
    // app arrives as CREATED. getIntent() deliberately stays the launch intent: nothing reads it later, and
    // ActivityScenario identifies the activity by it.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            homeRequests.tryEmit(Unit)
        }
    }
}
