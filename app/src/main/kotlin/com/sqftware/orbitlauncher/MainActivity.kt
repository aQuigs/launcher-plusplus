package com.sqftware.orbitlauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sqftware.orbitlauncher.apps.LauncherAppsRepository
import com.sqftware.orbitlauncher.apps.NotificationBadges
import com.sqftware.orbitlauncher.apps.RoleManagerHomeRole
import com.sqftware.orbitlauncher.apps.SharedPreferencesCollectionsStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesHomeAppsStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesHourStyleStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesReorderModeStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesWidgetPageStore
import com.sqftware.orbitlauncher.apps.StatusBarNotificationShade
import com.sqftware.orbitlauncher.apps.SystemAppUsage
import com.sqftware.orbitlauncher.apps.SystemRelauncher
import com.sqftware.orbitlauncher.apps.SystemRinger
import com.sqftware.orbitlauncher.apps.SystemWallClock
import com.sqftware.orbitlauncher.apps.SystemWidgetHost
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.ui.AppActions
import com.sqftware.orbitlauncher.ui.HomePress
import com.sqftware.orbitlauncher.ui.LauncherScreen
import com.sqftware.orbitlauncher.ui.PinRequest
import com.sqftware.orbitlauncher.ui.WidgetActions
import com.sqftware.orbitlauncher.ui.theme.LauncherTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)

    // A channel, unlike the presses, keeps a request that starts the activity until the screen is there to collect it.
    private val pinRequestChannel = Channel<PinRequest>(Channel.CONFLATED)
    private val pinRequests = pinRequestChannel.receiveAsFlow()
    private lateinit var repository: LauncherAppsRepository
    private lateinit var widgetHost: SystemWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // LauncherTheme is dark only, so both bars draw light icons whatever the system theme. Dark also drops the backing
        // the system draws behind three-button navigation: LauncherScreen shades the bottom edge itself.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        repository = LauncherAppsRepository(this)
        val homeAppsStore = SharedPreferencesHomeAppsStore(this)
        val wallClock = SystemWallClock(this)
        val hourStyleStore = SharedPreferencesHourStyleStore(this)
        val reorderModeStore = SharedPreferencesReorderModeStore(this)
        val ringer = SystemRinger(this)
        val homeRole = RoleManagerHomeRole(this, activityResultRegistry)
        val badges = NotificationBadges(this)
        val collectionsStore = SharedPreferencesCollectionsStore(this)
        val appUsage = SystemAppUsage(this)
        val shade = StatusBarNotificationShade(this)
        val relauncher = SystemRelauncher(this)
        widgetHost = SystemWidgetHost(this, SharedPreferencesWidgetPageStore(this))
        val widgetActions = WidgetActions(
            view = widgetHost::view,
            add = widgetHost::add,
            remove = widgetHost::remove,
            resize = widgetHost::resize,
            sizing = widgetHost::sizing,
        )
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

        // A rotation must not ask again.
        if (savedInstanceState == null) askToPin(intent)

        setContent {
            LauncherTheme {
                val apps by produceState<List<AppEntry>?>(null) { repository.installedApps().collect { value = it } }
                // Read before the first frame, unlike the app list, so the ring never flashes its empty-ring hint. The
                // file holds a few keys.
                var homeApps by remember { mutableStateOf(homeAppsStore.load()) }
                var twentyFourHour by remember { mutableStateOf(hourStyleStore.load()) }
                var reorderMode by remember { mutableStateOf(reorderModeStore.load()) }
                // The first face is read before the first frame too, so the ring does not move down when the clock arrives.
                // The clock ticks only while the launcher is visible, and each return reads it afresh.
                val clock by produceState(remember { wallClock.face(twentyFourHour) }, twentyFourHour) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { wallClock.faces(twentyFourHour).collect { value = it } }
                }
                // Read before the first frame too, so the switch never shows another mode first. It follows the ringer while
                // the launcher is visible, like the clock.
                val ringerMode by produceState(remember { ringer.mode() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { ringer.modes().collect { value = it } }
                }
                // Read again on each return to the front, since the user may have picked another home app in Settings.
                val isHomeApp by produceState(remember { homeRole.isHeld() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { homeRole.held().collect { value = it } }
                }
                // Only the home app may read its pins, and hears of their changes, so each return to the front and each
                // change of role reads them again.
                val pinnedShortcuts by produceState<List<AppEntry>?>(null, isHomeApp) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { repository.pinnedShortcuts().collect { value = it } }
                }
                LaunchedEffect(homeApps, isHomeApp) { repository.unpinAllBut(homeApps) }
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
                var collections by remember { mutableStateOf(collectionsStore.load()) }
                // Usage access is granted in Settings, so each return to the front reads the grant and the week's usage
                // again. The grant is read before the first frame, so a granted card does not ask for it while the usage
                // loads.
                val foregroundTime by produceState(remember { if (appUsage.isUsageAccessGranted()) ForegroundTime() else null }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { appUsage.foregroundTime().collect { value = it } }
                }
                LauncherScreen(
                    layout = layout,
                    homePresses = homePresses,
                    pinRequests = pinRequests,
                    apps = apps,
                    pinnedShortcuts = pinnedShortcuts,
                    homeApps = homeApps,
                    onHomeAppsChange = {
                        homeApps = it
                        homeAppsStore.save(it)
                    },
                    actions = actions,
                    reorderMode = reorderMode,
                    onReorderModeChange = {
                        reorderMode = it
                        reorderModeStore.save(it)
                    },
                    clock = clock,
                    onTwentyFourHourChange = {
                        twentyFourHour = it
                        hourStyleStore.save(it)
                    },
                    onOpenClock = wallClock::openClock,
                    onOpenCalendar = wallClock::openCalendar,
                    ringerMode = ringerMode,
                    onRingerTap = ringer::cycle,
                    isHomeApp = isHomeApp,
                    onBecomeHomeApp = homeRole::request,
                    widgetPage = widgetPage,
                    widgets = widgetActions,
                    collections = collections,
                    onCollectionsChange = {
                        collections = it
                        collectionsStore.save(it)
                    },
                    foregroundTime = foregroundTime,
                    onOpenUsageSettings = appUsage::openUsageSettings,
                    unread = unread,
                    badgesEnabled = badgesEnabled,
                    onOpenBadgeSettings = badges::openSettings,
                    onOpenNotifications = shade::open,
                    onRestart = relauncher::restart,
                    onReset = relauncher::reset,
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
        } else {
            askToPin(intent)
        }
    }

    // PinShortcutActivity hands on what the system asks it to confirm.
    private fun askToPin(intent: Intent) {
        if (intent.component?.className != PIN_REQUESTS) return
        val pin = repository.pinRequest(intent) ?: return
        pinRequestChannel.trySend(PinRequest(pin.shortcut, pin.appLabel, pin::icon, pin::accept))
    }
}
