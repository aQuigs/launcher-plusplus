package com.aquigs.launcherplusplus.apps

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.aquigs.launcherplusplus.domain.HostedWidget
import com.aquigs.launcherplusplus.domain.WidgetPage
import com.aquigs.launcherplusplus.domain.WidgetSizing
import com.aquigs.launcherplusplus.domain.widgetRows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlin.math.ceil

interface WidgetHost {
    /** The widget page as stored, less the widgets whose providers have gone. */
    fun page(): WidgetPage

    /**
     * [page] now, then again each time a widget is added, removed, resized or lost with its provider. The widgets draw their
     * updates only while this is collected, so collect it while the launcher is visible.
     */
    fun updates(): Flow<WidgetPage>

    /** Lets the user pick a widget for the page, where it takes the rows its provider asks for, up to [pageRows]. */
    fun add(pageRows: Int)

    /** Takes the widget [id] off the page and gives its id back to the system. */
    fun remove(id: Int)

    /** Makes the widget [id] [rows] tall. */
    fun resize(id: Int, rows: Int)

    /** How the provider of the widget [id] lets it be resized. */
    fun sizing(id: Int): WidgetSizing

    /** The system's view of the widget [id], which draws the widget and tells it the size it is laid out at. */
    fun view(context: Context, id: Int): View
}

private const val TAG = "SystemWidgetHost"
private const val HOST_ID = 0x4C50
private const val PICK_KEY = "widget_pick"

// Below 0x10000, where the result registry's own request codes start.
private const val CONFIGURE_REQUEST = 0x5749

/**
 * Widgets hosted by the system's app widget service under one host id, so the ids it hands out come back to this app
 * after a restart. The system picker binds the picked provider to the id the launcher allocated. A provider that wants
 * configuring is started through the host, which answers through the activity itself rather than the result registry,
 * so the activity forwards that answer to [onActivityResult]. An id whose pick or configuration is cancelled goes back.
 * The pick in progress is stored, since a recreated activity gets the answer, and an old one still there when the user
 * asks again was lost with its process.
 */
class SystemWidgetHost(private val activity: ComponentActivity, private val store: WidgetPageStore) : WidgetHost {
    private val manager = AppWidgetManager.getInstance(activity)
    private val host = object : AppWidgetHost(activity, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?) = SizedWidgetView(context)

        // The system reports a widget whose provider was uninstalled, at once or when listening starts again.
        override fun onAppWidgetRemoved(appWidgetId: Int) = remove(appWidgetId)
    }
    private val page = MutableStateFlow(store.load())
    private val restored = store.loadPick()
    private var pending: WidgetPick? = restored
        set(value) {
            field = value
            store.savePick(value)
        }
    private val picker = activity.activityResultRegistry.register(PICK_KEY, StartActivityForResult()) { result ->
        val pick = pending ?: return@register
        val info = manager.getAppWidgetInfo(pick.id).takeIf { result.resultCode == Activity.RESULT_OK }
        when {
            info == null -> discard(pick)
            info.configure == null -> place(pick, info)
            else -> configure(pick)
        }
    }

    init {
        // The system drops a widget with its provider, so a stored one it no longer holds has gone; an id it holds that is
        // neither stored nor being picked was a pick lost with its process.
        val held = host.appWidgetIds.toSet()
        val kept = page.value.widgets.filter { it.id in held }
        val keptIds = kept.mapTo(mutableSetOf()) { it.id } + listOfNotNull(pending?.id)
        held.filter { it !in keptIds }.forEach(host::deleteAppWidgetId)
        if (kept.size != page.value.widgets.size) set(WidgetPage(kept))
    }

    override fun page(): WidgetPage = page.value

    override fun updates(): Flow<WidgetPage> = page.onStart { host.startListening() }.onCompletion { host.stopListening() }

    override fun add(pageRows: Int) {
        val inProgress = pending
        if (inProgress != null) {
            // Only a pick from before the process started can still be waiting once the user is back to ask again.
            if (inProgress != restored) return
            discard(inProgress)
        }
        val pick = WidgetPick(host.allocateAppWidgetId(), pageRows)
        pending = pick
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pick.id)
        if (!start("the widget picker") { picker.launch(intent) }) discard(pick)
    }

    override fun remove(id: Int) {
        host.deleteAppWidgetId(id)
        if (pending?.id == id) pending = null
        set(page.value.remove(id))
    }

    override fun resize(id: Int, rows: Int) = set(page.value.resize(id, rows))

    override fun sizing(id: Int): WidgetSizing {
        val info = manager.getAppWidgetInfo(id) ?: return WidgetSizing(vertical = false)
        val maxResizeHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.maxResizeHeight.takeIf { it > 0 } else null
        return WidgetSizing(
            vertical = info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0,
            minHeightDp = info.minHeight.toDp(),
            minResizeHeightDp = info.minResizeHeight.toDp(),
            // Rounded down, so the rows it allows fit under it.
            maxResizeHeightDp = maxResizeHeight?.let { (it / activity.resources.displayMetrics.density).toInt() },
        )
    }

    override fun view(context: Context, id: Int): View = host.createView(context, id, manager.getAppWidgetInfo(id))

    /** The answer of a configuration the host started; other requests are not the host's. */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode != CONFIGURE_REQUEST) return
        val pick = pending ?: return
        val info = manager.getAppWidgetInfo(pick.id).takeIf { resultCode == Activity.RESULT_OK }
        if (info == null) discard(pick) else place(pick, info)
    }

    private fun configure(pick: WidgetPick) {
        val started = start("the widget's configuration") {
            host.startAppWidgetConfigureActivityForResult(activity, pick.id, 0, CONFIGURE_REQUEST, null)
        }
        if (!started) discard(pick)
    }

    private fun place(pick: WidgetPick, info: AppWidgetProviderInfo) {
        pending = null
        set(page.value.add(HostedWidget(pick.id, widgetRows(info.minHeight.toDp(), pick.pageRows))))
    }

    // Rounded up, so the rows made of it hold the widget.
    private fun Int.toDp() = ceil(this / activity.resources.displayMetrics.density).toInt()

    private fun discard(pick: WidgetPick) {
        pending = null
        host.deleteAppWidgetId(pick.id)
    }

    private fun set(value: WidgetPage) {
        page.value = value
        store.save(value)
    }

    private inline fun start(what: String, start: () -> Unit): Boolean {
        var started = false
        startOrLog(TAG, what) {
            start()
            started = true
        }
        return started
    }
}

/** A widget's view that tells its provider the size it is laid out at, so the provider draws for that space. */
private class SizedWidgetView(context: Context) : AppWidgetHostView(context) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val density = resources.displayMetrics.density
        val widthDp = w / density
        val heightDp = h / density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
        }
    }
}
