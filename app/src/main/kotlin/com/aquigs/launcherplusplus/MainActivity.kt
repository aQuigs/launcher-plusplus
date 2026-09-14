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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.apps.NotificationBadges
import com.aquigs.launcherplusplus.apps.RoleManagerHomeRole
import com.aquigs.launcherplusplus.apps.SharedPreferencesHomeAppsStore
import com.aquigs.launcherplusplus.apps.SharedPreferencesWidgetPageStore
import com.aquigs.launcherplusplus.apps.SystemWallClock
import com.aquigs.launcherplusplus.apps.SystemWidgetHost
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.ui.AppActions
import com.aquigs.launcherplusplus.ui.HomePress
import com.aquigs.launcherplusplus.ui.LauncherScreen
import com.aquigs.launcherplusplus.ui.WidgetActions
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)
    private lateinit var widgetHost: SystemWidgetHost

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
        val wallClock = SystemWallClock(this)
        val homeRole = RoleManagerHomeRole(this, activityResultRegistry)
        val badges = NotificationBadges(this)
        widgetHost = SystemWidgetHost(this, SharedPreferencesWidgetPageStore(this))
        val widgetActions = WidgetActions(view = widgetHost::view, add = widgetHost::add, remove = widgetHost::remove)
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
                // The first face is read before the first frame too, so the ring does not move down when the clock arrives.
                // The clock ticks only while the launcher is visible, and each return reads it afresh.
                val clock by produceState(remember { wallClock.face() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { wallClock.faces().collect { value = it } }
                }
                // Read again on each return to the front, since the user may have picked another home app in Settings.
                val isHomeApp by produceState(remember { homeRole.isHeld() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { homeRole.held().collect { value = it } }
                }
                // The widgets only draw their updates while the launcher is visible, like the clock.
                val widgetPage by produceState(remember { widgetHost.page() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { widgetHost.updates().collect { value = it } }
                }
                // Notification access is granted and revoked in Settings, so each return reads it again; the counts follow
                // the notifications only while the launcher is visible, like the clock.
                val badgesEnabled by produceState(remember { badges.isEnabled() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { badges.enabled().collect { value = it } }
                }
                val unread by produceState(UnreadCounts()) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { badges.counts().collect { value = it } }
                }
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
                    clock = clock,
                    onOpenClock = wallClock::openClock,
                    onOpenCalendar = wallClock::openCalendar,
                    isHomeApp = isHomeApp,
                    onBecomeHomeApp = homeRole::request,
                    widgetPage = widgetPage,
                    widgets = widgetActions,
                    unread = unread,
                    badgesEnabled = badgesEnabled,
                    onOpenBadgeSettings = badges::openSettings,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    // The widget host's configuration step is an activity API that answers here, not through the result registry, which
    // dispatches the registry's own requests first.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgetHost.onActivityResult(requestCode, resultCode)
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
