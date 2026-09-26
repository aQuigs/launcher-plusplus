package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.FolderLook
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
    fun theSolarSystemUntilAnotherIsChosenAndAnUnknownLookFallsBackToIt() {
        assertEquals(FolderLook.SolarSystem, store.load())

        store.save(FolderLook.Ringed)
        assertEquals(FolderLook.Ringed, SharedPreferencesFolderLookStore(context).load())

        prefs.edit(commit = true) { putString("look", "Nebula") }
        assertEquals(FolderLook.SolarSystem, store.load())
    }
}
