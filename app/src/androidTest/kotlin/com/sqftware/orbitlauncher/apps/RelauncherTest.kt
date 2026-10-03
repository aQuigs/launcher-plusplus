package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.CollectionPages
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.Ring
import com.sqftware.orbitlauncher.domain.RingPages
import com.sqftware.orbitlauncher.domain.RingSlot
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RelauncherTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // The stores apply their writes, which reach the disk a moment later, where the reset finds them; the user's data is
    // there long before they get to the Reset button. A commit waits for the writes before it.
    private fun waitForTheStoresToWrite() = listOf("home", "collections", "widgets", "clock").forEach {
        context.getSharedPreferences(it, Context.MODE_PRIVATE).edit(commit = true) {}
    }

    @Test
    fun erasingForgetsEveryStore() {
        SharedPreferencesRingPagesStore(context).save(
            RingPages(rings = mapOf(LauncherPage.Home.id to Ring(listOf(RingSlot.App("a/A"), RingSlot.Folder(listOf("b/B"))))), dock = Ring(listOf(RingSlot.App("c/C")))),
        )
        // Emptied, which is stored, unlike the default page.
        SharedPreferencesCollectionsStore(context).save(CollectionPages().with(LauncherPage.Collections.id, CollectionsPage(emptyList())))
        val widgetStore = SharedPreferencesWidgetPageStore(context)
        widgetStore.save(WidgetPages(mapOf(LauncherPage.Widgets.id to WidgetPage(listOf(HostedWidget(12, row = 0, column = 0, rows = 2, columns = 4))))))
        widgetStore.savePick(WidgetPick(14, LauncherPage.Widgets.id, pageRows = 9, columnWidthDp = 90.5f, rowHeightDp = 84.25f))
        SharedPreferencesHourStyleStore(context).save(twentyFourHour = true)
        // A store added later, which no one told the reset about.
        context.getSharedPreferences("later", Context.MODE_PRIVATE).edit(commit = true) { putString("key", "value") }
        waitForTheStoresToWrite()

        SystemRelauncher(context).erase()

        assertEquals(RingPages(), SharedPreferencesRingPagesStore(context).load())
        assertEquals(CollectionPages(), SharedPreferencesCollectionsStore(context).load())
        assertEquals(WidgetPages(), SharedPreferencesWidgetPageStore(context).load())
        assertNull(SharedPreferencesWidgetPageStore(context).loadPick())
        assertNull(SharedPreferencesHourStyleStore(context).load())
        // The stores read this process's cache, which a clear only applied would empty too, and the restart would then end
        // the process before the disk caught up, bringing it all back. Every preference in a file is a named element.
        for (file in File(context.dataDir, "shared_prefs").listFiles().orEmpty()) {
            assertFalse("${file.name} still holds data", "name=" in file.readText())
        }
    }
}
