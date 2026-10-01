package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.Colourways
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.FolderLooks
import com.sqftware.orbitlauncher.domain.Theme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePicksStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)
    private val colourwayPrefs = context.getSharedPreferences("colourways", Context.MODE_PRIVATE)
    private val store = folderLookStore(context)

    @Before
    @After
    fun forgetTheChoice() {
        prefs.edit(commit = true) { clear() }
        colourwayPrefs.edit(commit = true) { clear() }
    }

    @Test
    fun colourwaysAreKeptPerThemeApartFromFolderLooks() {
        colourwayStore(context).save(Colourways().with(Theme.Atlas, Colourway.Nautical))

        val loaded = colourwayStore(context).load()
        assertEquals(Colourway.Nautical, loaded.of(Theme.Atlas))
        assertEquals(Colourway.Brass, loaded.of(Theme.Clockwork))
        assertEquals(FolderLooks(), store.load())
    }

    @Test
    fun eachThemeKeepsItsOwnPickAndAnUnknownLookFallsBackToTheThemesFirst() {
        assertEquals(FolderLooks(), store.load())

        store.save(FolderLooks().with(Theme.Space, FolderLook.Ringed).with(Theme.Clockwork, FolderLook.Gear))
        val loaded = folderLookStore(context).load()
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
