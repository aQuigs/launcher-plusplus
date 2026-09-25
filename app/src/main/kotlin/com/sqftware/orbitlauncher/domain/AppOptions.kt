package com.sqftware.orbitlauncher.domain

/** One of an app's shortcuts, declared in its manifest or published while it runs. */
data class AppShortcut(val packageName: String, val id: String, val label: String)

/** Something an app's long-press menu offers besides its shortcuts. */
sealed interface AppOption {
    data class Remove(val place: HomePlace) : AppOption

    data object NewFolder : AppOption

    data object AppInfo : AppOption

    data object Uninstall : AppOption
}

/** The options for [app] long-pressed in [place], or in the drawer when [place] is null, in menu order. */
fun appOptions(app: AppEntry, place: HomePlace?): List<AppOption> = listOfNotNull(
    place?.let(AppOption::Remove),
    AppOption.NewFolder.takeIf { place is HomePlace.Slots },
    AppOption.AppInfo,
    AppOption.Uninstall.takeIf { app.canUninstall },
)
