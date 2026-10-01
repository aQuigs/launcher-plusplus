package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.FolderLooks
import com.sqftware.orbitlauncher.domain.Theme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderLookStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)
    private val store = SharedPreferencesFolderLookStore(context)

    @Before
    @After
    fun forgetTheChoice() = prefs.edit(commit = true) { clear() }

    @Test
    fun eachThemeKeepsItsOwnPickAndAnUnknownLookFallsBackToTheThemesFirst() {
        assertEquals(FolderLooks(), store.load())

        store.save(FolderLooks().with(Theme.Space, FolderLook.Ringed).with(Theme.Clockwork, FolderLook.Gear))
        val loaded = SharedPreferencesFolderLookStore(context).load()
        assertEquals(FolderLook.Ringed, loaded.of(Theme.Space))
        assertEquals(FolderLook.Gear, loaded.of(Theme.Clockwork))

        prefs.edit(commit = true) { putString("look", "Nebula") }
        assertEquals(FolderLook.Rim, store.load().of(Theme.Space))
    }

    @Test
    fun aThemeWithNoPickIsNotStoredAsIfOneWasMade() {
        store.save(FolderLooks().with(Theme.Space, FolderLook.Ringed))

        assertEquals(null, store.load().picked(Theme.Clockwork))
    }

    @Test
    fun spacesPickFromBeforeThemesIsKept() {
        prefs.edit(commit = true) { putString("look", "SolarSystem") }

        assertEquals(FolderLook.SolarSystem, store.load().of(Theme.Space))
    }
}
