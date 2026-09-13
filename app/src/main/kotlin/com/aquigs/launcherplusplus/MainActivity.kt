package com.aquigs.launcherplusplus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.apps.SharedPreferencesHomeAppsStore
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.ui.AppActions
import com.aquigs.launcherplusplus.ui.HomePress
import com.aquigs.launcherplusplus.ui.LauncherScreen
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // LauncherTheme is dark only, so both bars draw light icons whatever the system theme. The navigation bar says so
        // through auto, because dark would also take away the backing the system draws behind three-button navigation.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { true },
        )
        val repository = LauncherAppsRepository(this)
        val homeAppsStore = SharedPreferencesHomeAppsStore(this)
        val layout = PageLayout()
        val actions = AppActions(
            icon = repository::icon,
            launch = repository::launch,
            shortcuts = repository::shortcuts,
            shortcutIcon = repository::shortcutIcon,
            startShortcut = repository::startShortcut,
            openAppInfo = repository::openAppInfo,
            uninstall = repository::uninstall,
        )

        setContent {
            LauncherTheme {
                val apps by produceState<List<AppEntry>?>(null) { repository.installedApps().collect { value = it } }
                // Read before the first frame, unlike the app list, so the ring never flashes its empty-ring hint. The
                // file holds a few keys.
                var homeApps by remember { mutableStateOf(homeAppsStore.load()) }
                LauncherScreen(
                    layout = layout,
                    homePresses = homePresses,
                    apps = apps,
                    homeApps = homeApps,
                    onHomeAppsChange = {
                        homeApps = it
                        homeAppsStore.save(it)
                    },
                    actions = actions,
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
