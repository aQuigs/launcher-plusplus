package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.LinkApp
import com.sqftware.orbitlauncher.domain.LinkValue
import com.sqftware.orbitlauncher.domain.KeptInputs
import com.sqftware.orbitlauncher.domain.PlaceValue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeptInputsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)
    private val home = PlaceValue("Home, sweet", "2 Elm St\nSpringfield", 37.0, -122.0)
    private val work = PlaceValue("Work", "1 Main St")

    @Before
    @After
    fun forget() = listOf("places", "chats").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit(commit = true) { clear() } }

    @Test
    fun keepsFavoritesAndRecentApart() {
        assertEquals(KeptInputs<PlaceValue>(), placesStore(context).load())

        placesStore(context).save(KeptInputs(favorites = listOf(home), recent = listOf(work)))
        assertEquals(KeptInputs(favorites = listOf(home), recent = listOf(work)), placesStore(context).load())
    }

    @Test
    fun keepsEachAppsChatsApart() {
        val general = KeptInputs(favorites = listOf(LinkValue(LinkApp.Slack, "general, or so", "slack://channel?team=T1&id=C1")))
        linksStore(context, LinkApp.Slack).save(general)

        assertEquals(general, linksStore(context, LinkApp.Slack).load())
        assertEquals(KeptInputs<LinkValue>(), linksStore(context, LinkApp.Discord).load())
    }

    @Test
    fun readsTheRecentOnesKeptBeforeThereWereFavorites() {
        prefs.edit(commit = true) { putString("recent", listOf(home, work).joinToString("\n") { it.encode() }) }

        assertEquals(KeptInputs(recent = listOf(home, work)), placesStore(context).load())
    }
}
