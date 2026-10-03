package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.CardSetting
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionPages
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.Favourites
import com.sqftware.orbitlauncher.domain.LauncherPage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesCollectionsStore(context)
    private val first = LauncherPage.Collections.id

    @Before
    @After
    fun forgetThePages() = context.getSharedPreferences("collections", Context.MODE_PRIVATE).edit(commit = true) { clear() }

    @Test
    fun nothingStoredIsTheDefaultPage() {
        assertEquals(CollectionsPage(), store.load().on(first))
    }

    @Test
    fun eachPageComesBackAsItWent() {
        val page = CollectionsPage()
            .toggleExpanded(CollectionKind.NewApps)
            .setDefault(CardSetting.Rows, 3)
            .set(CollectionKind.NewApps, CardSetting.Limit, 20)
            .add(CollectionKind.Category(AppCategory.Tools), Favourites(listOf("a/A", "b/B")))
        val pages = CollectionPages().with(first, page).with("collections-2", CollectionsPage(emptyList()).copy(defaults = page.defaults))

        store.save(pages)

        val loaded = SharedPreferencesCollectionsStore(context).load()
        assertEquals(page, loaded.on(first))
        assertEquals(CollectionsPage(emptyList(), page.defaults), loaded.on("collections-2"))
    }

    @Test
    fun anEmptiedPageStaysEmpty() {
        store.save(CollectionPages().with(first, CollectionsPage(emptyList())))

        assertEquals(CollectionsPage(emptyList()), SharedPreferencesCollectionsStore(context).load().on(first))
    }
}
