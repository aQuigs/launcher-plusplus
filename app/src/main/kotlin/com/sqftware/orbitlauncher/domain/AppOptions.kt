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
 * apps, the [settings] made for apps, whether the badges are enabled and whether the app [hasStorePage]. The drawer adds
 * to the ring or the dock where the app has no slot of its own yet. A pinned shortcut wears no badge and is on no card,
 * so offers neither setting.
 */
fun appOptions(
    app: AppEntry,
    place: HomePlace?,
    home: HomeApps,
    settings: AppSettings,
    badgesEnabled: Boolean,
    hasStorePage: Boolean,
): List<AppOption> = buildList {
    val isApp = app.shortcutId == null
    if (place != null) add(AppOption.Remove(place))
    if (place is HomePlace.Slots) add(AppOption.NewFolder)
    if (place == null) listOf(HomePlace.Ring, HomePlace.Dock).filter { app !in home[it] }.forEach { add(AppOption.AddTo(it)) }
    if (isApp && badgesEnabled) add(AppOption.Badge(settings.isBadgeOff(app)))
    if (isApp) add(AppOption.BuiltInCards(settings.isOffBuiltInCards(app)))
    if (hasStorePage) add(AppOption.PlayStore)
    add(AppOption.AppInfo)
    if (app.canUninstall) add(AppOption.Uninstall)
}
