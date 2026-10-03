package com.sqftware.orbitlauncher.domain

import java.io.Serializable

/** One of an app's shortcuts, declared in its manifest or published while it runs. */
data class AppShortcut(val packageName: String, val id: String, val label: String)

/** Where an app was long-pressed. Serializable so the screen can save a pick that started there. */
sealed interface AppSpot : Serializable {
    data object Drawer : AppSpot

    /** The drawer's row of the most used apps, whose apps are in the drawer's list too and offer the same. */
    data object MostUsedRow : AppSpot

    data class Home(val place: HomePlace) : AppSpot

    data class Card(val kind: CollectionKind) : AppSpot
}

/** Something an app's long-press menu offers besides its shortcuts. */
sealed interface AppOption {
    data class Remove(val place: HomePlace) : AppOption

    data object NewFolder : AppOption

    data class RemoveFromCard(val kind: CollectionKind.HandPicked) : AppOption

    data class AddTo(val place: HomePlace.Slots) : AppOption

    /** Picks a second app to pair the app with in split screen. */
    data object SplitWith : AppOption

    /** Swaps which of a pair's apps opens on top. */
    data object Flip : AppOption

    /** Turns the app's unread badge off, or back on when it [isOff]; on a pair, its [member]'s, wherever that app is. */
    data class Badge(val isOff: Boolean, val member: AppEntry? = null) : AppOption

    /** Leaves the app off the New Apps and Most Used cards, or puts it back when it [isOff]. */
    data class BuiltInCards(val isOff: Boolean) : AppOption

    data object PlayStore : AppOption

    data object AppInfo : AppOption

    data object Uninstall : AppOption
}

/**
 * The options for [app] long-pressed at [spot], in menu order, given the [home] apps, the [settings] made for apps,
 * whether the badges are enabled and whether the app [hasStorePage]. A home place takes the app off it, and the ring and
 * the dock start a folder in its slot; a hand-picked card takes it off the card. Anywhere but home, the menu adds the app
 * to the ring or the dock where it has no slot of its own yet. A plain app can be split with another. A pinned shortcut
 * wears no badge and is on no card, so offers neither setting. A pair belongs to no one app, so offers only where it is,
 * flipping its order there, and each of its apps' badge, which the pair's own adds up.
 */
fun appOptions(
    app: AppEntry,
    spot: AppSpot,
    home: HomeApps,
    settings: AppSettings,
    badgesEnabled: Boolean,
    hasStorePage: Boolean,
): List<AppOption> = buildList {
    val place = (spot as? AppSpot.Home)?.place
    val card = (spot as? AppSpot.Card)?.kind
    if (place != null) add(AppOption.Remove(place))
    if (place is HomePlace.Slots) add(AppOption.NewFolder)
    if (card is CollectionKind.HandPicked) add(AppOption.RemoveFromCard(card))
    if (place == null) listOf(HomePlace.Ring, HomePlace.Dock).filter { app !in home[it] }.forEach { add(AppOption.AddTo(it)) }
    if (app.isApp) {
        add(AppOption.SplitWith)
        if (badgesEnabled) add(AppOption.Badge(settings.isBadgeOff(app)))
        add(AppOption.BuiltInCards(settings.isOffBuiltInCards(app)))
    }
    if (app.kind is EntryKind.AppPair) {
        if (place != null || card is CollectionKind.HandPicked) add(AppOption.Flip)
        if (badgesEnabled) app.opens.forEach { add(AppOption.Badge(settings.isBadgeOff(it), it)) }
    } else {
        if (hasStorePage) add(AppOption.PlayStore)
        add(AppOption.AppInfo)
    }
    if (app.canUninstall) add(AppOption.Uninstall)
}
