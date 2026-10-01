package com.sqftware.orbitlauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sqftware.orbitlauncher.apps.LauncherAppsRepository
import com.sqftware.orbitlauncher.apps.NotificationBadges
import com.sqftware.orbitlauncher.apps.PlayAppUpdates
import com.sqftware.orbitlauncher.apps.RoleManagerHomeRole
import com.sqftware.orbitlauncher.apps.SharedPreferencesAmbientMotionStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesAppSettingsStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesCollectionsStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesDrawerStyleStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesFolderLookStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesHomeAppsStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesHourStyleStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesReorderModeStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesThemeStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesUpdateCheckStore
import com.sqftware.orbitlauncher.apps.SharedPreferencesWidgetPageStore
import com.sqftware.orbitlauncher.apps.StatusBarNotificationShade
import com.sqftware.orbitlauncher.apps.SystemAppUsage
import com.sqftware.orbitlauncher.apps.SystemDefaultAppFinder
import com.sqftware.orbitlauncher.apps.SystemRelauncher
import com.sqftware.orbitlauncher.apps.SystemRinger
import com.sqftware.orbitlauncher.apps.SystemWallClock
import com.sqftware.orbitlauncher.apps.SystemWallpaper
import com.sqftware.orbitlauncher.apps.SystemWidgetHost
import com.sqftware.orbitlauncher.apps.WindowSystemBars
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.suggestSetup
import com.sqftware.orbitlauncher.ui.AppActions
import com.sqftware.orbitlauncher.ui.HomePress
import com.sqftware.orbitlauncher.ui.LauncherScreen
import com.sqftware.orbitlauncher.ui.PinRequest
import com.sqftware.orbitlauncher.ui.Themed
import com.sqftware.orbitlauncher.ui.WidgetActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val LEAVE_FOR_SETTINGS_MILLIS = 2_000L

class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)

    // A channel, unlike the presses, keeps a request that starts the activity until the screen is there to collect it.
    private val pinRequestChannel = Channel<PinRequest>(Channel.CONFLATED)
    private val pinRequests = pinRequestChannel.receiveAsFlow()
    private lateinit var repository: LauncherAppsRepository
    private lateinit var widgetHost: SystemWidgetHost
    private var barsLight: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wallpaper = SystemWallpaper(this)
        val lightAtStart = wallpaper.isLight()
        showBarsFor(lightAtStart)
        repository = LauncherAppsRepository(this)
        val homeAppsStore = SharedPreferencesHomeAppsStore(this)
        val wallClock = SystemWallClock(this)
        val hourStyleStore = SharedPreferencesHourStyleStore(this)
        val themeStore = SharedPreferencesThemeStore(this)
        val folderLookStore = SharedPreferencesFolderLookStore(this)
        val reorderModeStore = SharedPreferencesReorderModeStore(this)
        val drawerStyleStore = SharedPreferencesDrawerStyleStore(this)
        val ambientMotionStore = SharedPreferencesAmbientMotionStore(this)
        val ringer = SystemRinger(this)
        val appUpdates = PlayAppUpdates(this)
        val updateCheckStore = SharedPreferencesUpdateCheckStore(this)
        val homeRole = RoleManagerHomeRole(this, activityResultRegistry)
        val badges = NotificationBadges(this)
        val collectionsStore = SharedPreferencesCollectionsStore(this)
        val appSettingsStore = SharedPreferencesAppSettingsStore(this)
        val appUsage = SystemAppUsage(this)
        val defaultApps = SystemDefaultAppFinder(this)
        val shade = StatusBarNotificationShade(this)
        val systemBars = WindowSystemBars(window)
        val relauncher = SystemRelauncher(this)
        widgetHost = SystemWidgetHost(this, SharedPreferencesWidgetPageStore(this))
        val widgetActions = WidgetActions(
            view = widgetHost::view,
            add = widgetHost::add,
            remove = widgetHost::remove,
            resize = widgetHost::resize,
            move = widgetHost::move,
            sizing = widgetHost::sizing,
        )
        val layout = PageLayout()
        val clearBadge = { app: AppEntry -> app.opens.forEach { badges.opened(it.packageName) } }
        val actions = AppActions(
            icon = repository::icon,
            launch = {
                clearBadge(it)
                repository.launch(it)
            },
            clearBadge = clearBadge,
            shortcuts = repository::shortcuts,
            shortcutIcon = repository::shortcutIcon,
            startShortcut = {
                badges.opened(it.packageName)
                repository.startShortcut(it)
            },
            openAppInfo = repository::openAppInfo,
            hasStorePage = repository::hasStorePage,
            openStorePage = repository::openStorePage,
            uninstall = repository::uninstall,
        )

        // A rotation must not ask again.
        if (savedInstanceState == null) askToPin(intent)

        setContent {
            // Read before the first frame, so the launcher never flashes the other look. A new wallpaper is set from
            // elsewhere, so it is followed while the launcher is visible and read again on each return.
            val lightWallpaper by produceState(lightAtStart) {
                repeatOnLifecycle(Lifecycle.State.STARTED) { wallpaper.lightness().collect { value = it } }
            }
            LaunchedEffect(lightWallpaper) { showBarsFor(lightWallpaper) }
            var theme by remember { mutableStateOf(themeStore.load()) }
            Themed(theme, lightWallpaper) {
                val apps by produceState<List<AppEntry>?>(null) { repository.installedApps().collect { value = it } }
                // Read before the first frame, unlike the app list, so the ring never flashes its empty-ring hint. The
                // file holds a few keys. Each folder keeps the planet it shows, loaded or changed, so none takes another's
                // as folders come and go.
                var homeApps by remember { mutableStateOf(homeAppsStore.load().withPlanetsKept()) }
                var twentyFourHour by remember { mutableStateOf(hourStyleStore.load()) }
                var folderLooks by remember { mutableStateOf(folderLookStore.load()) }
                var reorderMode by remember { mutableStateOf(reorderModeStore.load()) }
                var drawerStyle by remember { mutableStateOf(drawerStyleStore.load()) }
                var ambientMotion by remember { mutableStateOf(ambientMotionStore.load()) }
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
                var checkForUpdates by remember { mutableStateOf(updateCheckStore.load()) }
                // Play tells no one of a new release, so it is asked, only while the launcher is visible, like the clock.
                val updateAvailable by produceState(false, checkForUpdates) {
                    if (checkForUpdates) repeatOnLifecycle(Lifecycle.State.STARTED) { appUpdates.available().collect { value = it } }
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
                // The widgets only draw their updates while the launcher is visible, like the clock. A change reaches the
                // page at once rather than a dispatch later, so a widget let go is drawn where it landed in that frame.
                val widgetPage by produceState(remember { widgetHost.page() }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        withContext(Dispatchers.Main.immediate) { widgetHost.updates().collect { value = it } }
                    }
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
                var appSettings by remember { mutableStateOf(appSettingsStore.load()) }
                // Usage access is granted in Settings, so each return to the front reads the grant and the week's usage
                // again. The grant is read before the first frame, so a granted card does not ask for it while the usage
                // loads.
                val foregroundTime by produceState(remember { if (appUsage.isUsageAccessGranted()) ForegroundTime() else null }) {
                    repeatOnLifecycle(Lifecycle.State.STARTED) { appUsage.foregroundTime().collect { value = it } }
                }
                fun changeHomeApps(changed: HomeApps) {
                    homeApps = changed.withPlanetsKept()
                    homeAppsStore.save(homeApps)
                }
                fun changeCollections(changed: CollectionsPage) {
                    collections = changed
                    collectionsStore.save(changed)
                }

                val scope = rememberCoroutineScope()
                // Usage is read afresh rather than from foregroundTime, which may not have caught up yet with access just
                // granted in Settings.
                fun setUpHome() = scope.launch {
                    val defaults = async { defaultApps.find() }
                    val time = async { appUsage.foregroundTime().first() }
                    val (found, used) = defaults.await() to time.await()
                    // Read after the wait, so apps picked by hand meanwhile, or a second tap, keep what is there.
                    val setup = suggestSetup(homeApps, collections, apps ?: return@launch, found, used) ?: return@launch
                    changeHomeApps(setup.home)
                    changeCollections(setup.collections)
                }
                // Asks for usage access first, which the guess leans on, then sets up once back from Settings, granted or
                // not, or at once should Settings never open.
                var askingUsage by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(askingUsage) {
                    if (!askingUsage) return@LaunchedEffect
                    withTimeoutOrNull(LEAVE_FOR_SETTINGS_MILLIS) { lifecycle.currentStateFlow.first { !it.isAtLeast(Lifecycle.State.RESUMED) } }
                    lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
                    askingUsage = false
                    setUpHome()
                }

                LauncherScreen(
                    layout = layout,
                    homePresses = homePresses,
                    pinRequests = pinRequests,
                    apps = apps,
                    pinnedShortcuts = pinnedShortcuts,
                    homeApps = homeApps,
                    onHomeAppsChange = ::changeHomeApps,
                    onSetUpHome = {
                        if (appUsage.isUsageAccessGranted()) {
                            setUpHome()
                        } else {
                            askingUsage = true
                            appUsage.openUsageSettings()
                        }
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
                    theme = theme,
                    onThemeChange = {
                        theme = it
                        themeStore.save(it)
                    },
                    folderLook = folderLooks.of(theme),
                    onFolderLookChange = {
                        folderLooks = folderLooks.with(theme, it)
                        folderLookStore.save(folderLooks)
                    },
                    ambientMotion = ambientMotion,
                    onAmbientMotionChange = {
                        ambientMotion = it
                        ambientMotionStore.save(it)
                    },
                    drawerStyle = drawerStyle,
                    onDrawerStyleChange = {
                        drawerStyle = it
                        drawerStyleStore.save(it)
                    },
                    onOpenClock = wallClock::openClock,
                    onOpenCalendar = wallClock::openCalendar,
                    ringerMode = ringerMode,
                    onRingerTap = ringer::cycle,
                    updateAvailable = updateAvailable,
                    onOpenUpdate = appUpdates::openStorePage,
                    checkForUpdates = checkForUpdates,
                    onCheckForUpdatesChange = {
                        checkForUpdates = it
                        updateCheckStore.save(it)
                    },
                    isHomeApp = isHomeApp,
                    onBecomeHomeApp = homeRole::request,
                    widgetPage = widgetPage,
                    widgets = widgetActions,
                    collections = collections,
                    onCollectionsChange = ::changeCollections,
                    foregroundTime = foregroundTime,
                    onOpenUsageSettings = appUsage::openUsageSettings,
                    unread = appSettings.badges(unread),
                    badgesEnabled = badgesEnabled,
                    onOpenBadgeSettings = badges::openSettings,
                    appSettings = appSettings,
                    onAppSettingsChange = {
                        appSettings = it
                        appSettingsStore.save(it)
                    },
                    onOpenNotifications = shade::open,
                    onSystemBarsShownChange = systemBars::setShown,
                    onRestart = relauncher::restart,
                    onReset = relauncher::reset,
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

    // The bars draw their icons in the same ink as LauncherTheme for the wallpaper, whatever the system theme. A fixed
    // style also drops the backing the system draws behind three-button navigation: LauncherScreen shades the bottom edge
    // itself.
    private fun showBarsFor(lightWallpaper: Boolean) {
        if (lightWallpaper == barsLight) return
        barsLight = lightWallpaper
        val style = if (lightWallpaper) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
