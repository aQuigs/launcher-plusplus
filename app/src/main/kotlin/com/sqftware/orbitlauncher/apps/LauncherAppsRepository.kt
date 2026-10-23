package com.sqftware.orbitlauncher.apps

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.EntryKind
import com.sqftware.orbitlauncher.domain.IconStyle
import com.sqftware.orbitlauncher.domain.ITEM_LAYER_SIZE
import com.sqftware.orbitlauncher.domain.itemMark
import com.sqftware.orbitlauncher.domain.itemSprite
import com.sqftware.orbitlauncher.domain.RingPages
import com.sqftware.orbitlauncher.domain.isStorable
import com.sqftware.orbitlauncher.domain.sortedByLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private const val TAG = "LauncherAppsRepository"
private const val MAX_SHORTCUTS = 4
// Room for a few hundred icons at the largest launcher size: every drawer row asks for its app's, and a cache the drawer
// churns through would evict the ring's and the dock's, which would then come back blank on the way home.
private const val ICON_CACHE_BYTES = 32 shl 20
// Only an app drawn at two sizes, as one on the ring and in the dock or drawer is, asks for its mark again; a mark is
// some 20 kB.
private const val MARK_CACHE_APPS = 64
private const val SHOWN_SHORTCUTS = ShortcutQuery.FLAG_MATCH_MANIFEST or ShortcutQuery.FLAG_MATCH_DYNAMIC

// What a square leaves free on each side when its corners touch a circle as wide: (1 - 1/√2) / 2.
private val SQUARE_IN_CIRCLE = ((1 - 1 / sqrt(2.0)) / 2).toFloat()

private val PROFILE_CHANGES = IntentFilter().apply {
    addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
    addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
}

class LauncherAppsRepository(private val context: Context) : AppRepository {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val ownUser = Process.myUserHandle()

    // An icon is asked for again each time its app comes back into view, and a collection card brings dozens back at a
    // tap; every icon shown asks again after a package change, which redraws only that package's.
    private val icons = IconCache(ICON_CACHE_BYTES)
    private val marks = ItemMarks(MARK_CACHE_APPS) { app ->
        rendered(app.key, { density -> entryIcon(app, density) }) { itemMark(it.layers(ITEM_LAYER_SIZE)) }
    }

    // One at a time and in order, so unpinning for an older home screen never lands after a newer one's.
    private val pinWork = Dispatchers.IO.limitedParallelism(1)

    override fun installedApps(): Flow<List<AppEntry>> =
        changes(withShortcuts = false) {
            icons.forget(it)
            marks.forget(it)
        }
            .map { loadApps() }
            .flowOn(Dispatchers.IO)

    override fun pinnedShortcuts(): Flow<List<AppEntry>> =
        changes(withShortcuts = true)
            .map { pinned().filter { it.isEnabled }.mapNotNull { it.toEntry() } }
            .flowOn(Dispatchers.IO)

    // Once at first, then again after each change the callback reports; changes while a load runs make one more load.
    private fun changes(withShortcuts: Boolean, onChange: (packageNames: Collection<String>) -> Unit = {}): Flow<Unit> =
        callbackFlow {
            val callback = PackageChanges(withShortcuts) {
                onChange(it)
                trySend(Unit)
            }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            // The callback hears of a work profile's packages, but not of the profile coming, going or pausing, which
            // changes the apps listed but no icon. Pins are the user's own only, so they never hear of it.
            val profileChanges = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    trySend(Unit)
                }
            }.takeUnless { withShortcuts }
            profileChanges?.let { ContextCompat.registerReceiver(context, it, PROFILE_CHANGES, ContextCompat.RECEIVER_NOT_EXPORTED) }
            send(Unit)
            awaitClose {
                launcherApps.unregisterCallback(callback)
                profileChanges?.let(context::unregisterReceiver)
            }
        }.conflate()

    private fun loadApps(): List<AppEntry> =
        profiles().flatMap { profile ->
            val workProfile = userManager.workProfileOf(profile)
            launcherApps.getActivityList(null, profile).map { info ->
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    // An app built into the system can only lose its updates, which its App info page offers.
                    canUninstall = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0,
                    installedAt = info.firstInstallTime,
                    category = categoryHint(info.applicationInfo.category),
                    workProfile = workProfile,
                )
            }
        }.sortedByLabel()

    // The user's own apps and those of their work profile. A private space is listed only to launchers allowed to see
    // hidden profiles, which this one is not; a clone profile would pass for work, so from Android 15, which names a
    // profile's kind, only a work profile does.
    private fun profiles(): List<UserHandle> = launcherApps.profiles.filter { profile ->
        profile == ownUser || Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM ||
            launcherApps.getLauncherUserInfo(profile)?.userType == UserManager.USER_TYPE_PROFILE_MANAGED
    }

    // Null once the work profile has gone, so nothing of it starts in the user's own instead.
    private fun userOf(workProfile: Long?): UserHandle? =
        if (workProfile == null) ownUser else userManager.getUserForSerialNumber(workProfile)

    private val AppEntry.user: UserHandle? get() = userOf(workProfile)

    override suspend fun icon(app: AppEntry, style: IconStyle): ImageBitmap? {
        val key = if (style == IconStyle.Plain) app.key else "$style:${app.key}"
        return icons[key] ?: icons.keep(app, key) {
            when (style) {
                IconStyle.Plain -> draw(app.key) { density -> entryIcon(app, density) }
                is IconStyle.Item -> marks[app]?.let { withContext(Dispatchers.IO) { itemSprite(it, style.size, cut = style.cut).toImageBitmap() } }
            }
        }
    }

    // A pair has no icon of its own: the screen draws its apps' together.
    private fun entryIcon(app: AppEntry, density: Int): Drawable? = when (val kind = app.kind) {
        EntryKind.App -> activityIcon(app, density)
        is EntryKind.Shortcut -> pinnedIcon(shortcutInfo(app.packageName, kind.id, ShortcutQuery.FLAG_MATCH_PINNED, ownUser), app, density)
        is EntryKind.AppPair -> null
    }

    private fun activityIcon(app: AppEntry, density: Int): Drawable? = app.user?.let { user ->
        launcherApps.resolveActivity(Intent().setComponent(app.component), user)?.getIcon(density)
    }

    // The ring and the dialog that asks share it through the cache. A square icon, as a web page's is, would lose its
    // corners to the disc, so it shrinks to fit whole on the disc's tint; an adaptive one is made for the cut. A shortcut
    // without an icon of its own wears its app's, as on other launchers.
    private fun pinnedIcon(info: ShortcutInfo?, shortcut: AppEntry, density: Int): Drawable? =
        info?.let { launcherApps.getShortcutIconDrawable(it, density) }
            ?.let { if (it is AdaptiveIconDrawable) it else InsetDrawable(it, SQUARE_IN_CIRCLE) }
            ?: activityIcon(shortcut, density)

    override fun launch(app: AppEntry) = startOrLog(TAG, app.key) {
        when (val kind = app.kind) {
            EntryKind.App -> app.user?.let { user ->
                // A paused work profile starts nothing; asking to unpause it shows the system's own prompt, as a tap on a
                // paused app does on other launchers.
                if (userManager.isQuietModeEnabled(user)) {
                    userManager.requestQuietModeEnabled(false, user)
                } else {
                    launcherApps.startMainActivity(app.component, user, null, null)
                }
            }
            is EntryKind.Shortcut -> launcherApps.startShortcut(app.packageName, kind.id, null, null, ownUser)
            is EntryKind.AppPair -> context.startActivity(SplitScreenActivity.intent(context, kind.first.component, kind.second.component))
        }
    }

    override suspend fun shortcuts(app: AppEntry): List<AppShortcut> = withContext(Dispatchers.IO) {
        val user = app.user
        if (!app.isApp || user == null) return@withContext emptyList()
        query(shortcutQuery(app.packageName).setActivity(app.component), app.key, user)
            .filter { it.isEnabled }
            .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
            .take(MAX_SHORTCUTS)
            .map { AppShortcut(packageName = it.`package`, id = it.id, label = it.label, workProfile = app.workProfile) }
    }

    override suspend fun shortcutIcon(shortcut: AppShortcut): ImageBitmap? = draw(shortcut.logName) { density ->
        userOf(shortcut.workProfile)?.let { user ->
            shortcutInfo(shortcut.packageName, shortcut.id, SHOWN_SHORTCUTS, user)?.let { launcherApps.getShortcutIconDrawable(it, density) }
        }
    }

    private fun shortcutInfo(packageName: String, id: String, flags: Int, user: UserHandle): ShortcutInfo? =
        launcherApps.getShortcuts(shortcutQuery(packageName, flags).setShortcutIds(listOf(id)), user)?.firstOrNull()

    override fun startShortcut(shortcut: AppShortcut) = startOrLog(TAG, shortcut.logName) {
        userOf(shortcut.workProfile)?.let { launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, it) }
    }

    override fun openAppInfo(app: AppEntry) = startOrLog(TAG, "app info for ${app.key}") {
        app.user?.let { launcherApps.startAppDetailsActivity(app.component, it, null, null) }
    }

    // An app built into the system counts once the Play Store has updated it, as it then has a page there. A work app's
    // store is the one its organisation manages, which the user's own Play Store does not speak for.
    override suspend fun hasStorePage(app: AppEntry): Boolean = withContext(Dispatchers.IO) {
        if (app.isWork) return@withContext false
        try {
            context.packageManager.getInstallSourceInfo(app.packageName).installingPackageName == PLAY_STORE
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun openStorePage(app: AppEntry) = context.openStorePage(app.packageName, TAG)

    override fun uninstall(app: AppEntry) = startOrLog(TAG, "uninstall for ${app.key}") {
        // In a task of its own, left out of recents: in the launcher's task, HOME would clear the confirmation and count as
        // a press inside the launcher.
        val user = app.user ?: return@startOrLog
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)).putExtra(Intent.EXTRA_USER, user)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
    }

    override fun pinRequest(intent: Intent): PinItem? {
        val request = launcherApps.getPinItemRequest(intent) ?: return null
        val info = request.shortcutInfo
        if (request.requestType != PinItemRequest.REQUEST_TYPE_SHORTCUT || info?.userHandle != ownUser || !request.isValid) return null
        val shortcut = info.toEntry() ?: return null
        return object : PinItem {
            override val shortcut = shortcut
            override val appLabel by lazy { labelOf(info.`package`) }

            // Until it is pinned, a lookup by id finds nothing, since the app may only have made it for the request. Kept,
            // so the ring shows it at once if the user adds it.
            override suspend fun icon() = icons.keep(shortcut) {
                draw(shortcut.key) { density -> pinnedIcon(info, shortcut, density) }
            }

            override fun accept() = try {
                request.accept()
            } catch (e: RuntimeException) {
                // Accepted already, or the launcher is no longer the home app the request was made to.
                Log.w(TAG, "Cannot pin ${shortcut.key}", e)
                false
            }
        }
    }

    override suspend fun unpinAllBut(pages: RingPages) = withContext(pinWork) {
        val pinned = pinned(ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY).groupBy({ it.`package` }, { it.id })
        pages.keptPins(pinned).forEach { (packageName, ids) ->
            try {
                launcherApps.pinShortcuts(packageName, ids, ownUser)
            } catch (e: RuntimeException) {
                // The app went meanwhile, or the launcher stopped being the home app.
                Log.w(TAG, "Cannot unpin the shortcuts of $packageName", e)
            }
        }
    }

    // The launcher's own pins, disabled ones too.
    private fun pinned(extraFlags: Int = 0) = query(ShortcutQuery().setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED or extraFlags), "pins", ownUser)

    // Only the default home app may read other apps' shortcuts; until then there are none, and a menu offers the options
    // alone.
    private fun query(query: ShortcutQuery, what: String, user: UserHandle): List<ShortcutInfo> = try {
        if (launcherApps.hasShortcutHostPermission()) launcherApps.getShortcuts(query, user).orEmpty() else emptyList()
    } catch (e: RuntimeException) {
        // A locked user, or an app removed since the list loaded.
        Log.w(TAG, "No shortcuts for $what", e)
        emptyList()
    }

    private val ShortcutInfo.label get() = (shortLabel ?: longLabel ?: id).toString()

    private fun ShortcutInfo.toEntry(): AppEntry? = AppEntry(
        label = label,
        packageName = `package`,
        activityName = activity?.className.orEmpty(),
        canUninstall = false,
        kind = EntryKind.Shortcut(id),
    ).takeIf { isStorable(it.key) }

    private fun labelOf(packageName: String): String = try {
        context.packageManager.run { getApplicationInfo(packageName, 0).loadLabel(this).toString() }
    } catch (_: PackageManager.NameNotFoundException) {
        // An app the launcher cannot see, having no launchable activity.
        packageName
    }

    private suspend fun draw(what: String, drawable: (density: Int) -> Drawable?): ImageBitmap? = withContext(Dispatchers.IO) {
        val size = activityManager.launcherLargeIconSize
        rendered(what, drawable) { it.toBitmap(size, size).asImageBitmap() }
    }

    private fun <T> rendered(what: String, drawable: (density: Int) -> Drawable?, render: (Drawable) -> T): T? = try {
        drawable(activityManager.launcherLargeIconDensity)?.let(render)
    } catch (e: RuntimeException) {
        // An app on the home screen whose icon cannot be drawn would otherwise crash the launcher on every start.
        Log.w(TAG, "No icon for $what", e)
        null
    }

    private fun shortcutQuery(packageName: String, flags: Int = SHOWN_SHORTCUTS) =
        ShortcutQuery().setPackage(packageName).setQueryFlags(flags)

    private val AppEntry.component get() = ComponentName(packageName, activityName)

    // Only the system categories with a card of the same meaning; the rest, undefined included, say nothing.
    private fun categoryHint(category: Int): AppCategory? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Music
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.Video
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Photos
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.Media
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.Transport
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.Tools
        else -> null
    }

    private val AppShortcut.logName get() = "$packageName shortcut $id"

    // Any of these can add, remove or relabel launchable activities, so each reloads the whole list. Apps change their
    // shortcuts often, so only a list of shortcuts hears of that too.
    private class PackageChanges(
        private val withShortcuts: Boolean,
        private val onChange: (packageNames: Collection<String>) -> Unit,
    ) : LauncherApps.Callback() {
        override fun onShortcutsChanged(packageName: String, shortcuts: List<ShortcutInfo>, user: UserHandle) {
            if (withShortcuts) changed(packageName)
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)

        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)

        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)

        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = changed(*packageNames)

        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = changed(*packageNames)

        private fun changed(vararg packageNames: String) = onChange(packageNames.asList())
    }
}
