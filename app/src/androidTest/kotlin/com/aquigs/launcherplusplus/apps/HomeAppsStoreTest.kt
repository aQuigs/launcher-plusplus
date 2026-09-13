package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.HomeApps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAppsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesHomeAppsStore(context)

    @After
    fun emptyHome() = store.save(HomeApps())

    @Test
    fun theRingAndTheDockComeBackApart() {
        val homeApps = HomeApps(ring = Favourites(listOf("a/A", "b/B")), dock = Favourites(listOf("c/C")))

        store.save(homeApps)

        assertEquals(homeApps, SharedPreferencesHomeAppsStore(context).load())
    }

    @Test
    fun aRingStoredBeforeTheDockLoadsAsTheRing() {
        // The format the ring shipped with: one "favourites" string of keys, one per line.
        context.getSharedPreferences("home", Context.MODE_PRIVATE).edit(commit = true) {
            clear()
            putString("favourites", "a/A\nb/B")
        }

        assertEquals(HomeApps(ring = Favourites(listOf("a/A", "b/B"))), store.load())
    }
}
