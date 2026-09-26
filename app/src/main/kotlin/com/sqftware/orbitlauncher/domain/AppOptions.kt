package com.sqftware.orbitlauncher.domain

/** One of an app's shortcuts, declared in its manifest or published while it runs. */
data class AppShortcut(val packageName: String, val id: String, val label: String)

/** Something an app's long-press menu offers besides its shortcuts. */
sealed interface AppOption {
    data class Remove(val place: HomePlace) : AppOption

    data object NewFolder : AppOption

    data class AddTo(val place: HomePlace.Slots) : AppOption

    /** Turns the app's unread badge off, or back on when it [isOff]. */
    data class Badge(val isOff: Boolean) : AppOption

    /** Leaves the app off the New Apps and Most Used cards, or puts it back when it [isOff]. */
    data class BuiltInCards(val isOff: Boolean) : AppOption

    data object PlayStore : AppOption

    data object AppInfo : AppOption

    data object Uninstall : AppOption
}

/**
 * The options for [app] long-pressed in [place], or in the drawer when [place] is null, in menu order, given the [home]
 * apps, the [settings] made for apps, and whether the badges are enabled. The drawer adds to the ring or the dock where
 * the app has no slot of its own yet. A pinned shortcut wears no badge and is on no card, so offers neither setting.
 */
fun appOptions(
    app: AppEntry,
    place: HomePlace?,
    home: HomeApps = HomeApps(),
    settings: AppSettings = AppSettings(),
    badgesEnabled: Boolean = false,
): List<AppOption> {
    val isApp = app.shortcutId == null
    return listOfNotNull(place?.let(AppOption::Remove), AppOption.NewFolder.takeIf { place is HomePlace.Slots }) +
        listOf(HomePlace.Ring, HomePlace.Dock).filter { place == null && app !in home[it] }.map(AppOption::AddTo) +
        listOfNotNull(
            AppOption.Badge(isOff = !settings.showsBadge(app)).takeIf { isApp && badgesEnabled },
            AppOption.BuiltInCards(isOff = !settings.onBuiltInCards(app)).takeIf { isApp },
            AppOption.PlayStore.takeIf { app.fromPlayStore },
            AppOption.AppInfo,
            AppOption.Uninstall.takeIf { app.canUninstall },
        )
}
