package com.aquigs.launcherplusplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.ui.LauncherScreen
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var homeRequests by mutableIntStateOf(0)

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

    // The HOME key relaunches the home activity, which singleTask delivers here. Only a press while the launcher is
    // already in front goes back to the home page; returning from an app keeps the page you left, like the stock launcher.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (hasWindowFocus()) homeRequests++
    }
}
