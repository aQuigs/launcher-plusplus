package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.Favourites
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesCollectionsStore(context)

    @Before
    @After
    fun forgetThePage() = context.getSharedPreferences("collections", Context.MODE_PRIVATE).edit(commit = true) { clear() }

    @Test
    fun nothingStoredIsTheDefaultPage() {
        assertEquals(CollectionsPage(), store.load())
    }

    @Test
    fun thePageComesBackAsItWent() {
        val page = CollectionsPage()
            .toggleExpanded(CollectionKind.NewApps)
            .add(CollectionKind.Category(AppCategory.Tools), Favourites(listOf("a/A", "b/B")))

        store.save(page)

        assertEquals(page, SharedPreferencesCollectionsStore(context).load())
    }

    @Test
    fun anEmptiedPageStaysEmpty() {
        store.save(CollectionsPage(emptyList()))

        assertEquals(CollectionsPage(emptyList()), SharedPreferencesCollectionsStore(context).load())
    }
}
