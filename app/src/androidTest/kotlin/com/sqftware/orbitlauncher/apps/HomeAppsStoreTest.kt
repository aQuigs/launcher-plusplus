package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.Ring
import com.sqftware.orbitlauncher.domain.RingSlot
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
        val ring = Ring(listOf(RingSlot.App("a/A"), RingSlot.Folder(listOf("b/B", "c/C")), RingSlot.Folder(emptyList())))
        val homeApps = HomeApps(ring = ring, dock = Ring(listOf(RingSlot.App("c/C"), RingSlot.Folder(listOf("a/A", "d/D")))))

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

        assertEquals(HomeApps(ring = Ring(listOf(RingSlot.App("a/A"), RingSlot.App("b/B")))), store.load())
    }
}
