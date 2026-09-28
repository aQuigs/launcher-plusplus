package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmbientMotionStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesAmbientMotionStore(context)

    @Before
    @After
    fun forgetTheChoice() = context.getSharedPreferences("motion", Context.MODE_PRIVATE).edit(commit = true) { clear() }

    @Test
    fun theSkyTurnsUntilStopped() {
        assertTrue(store.load())

        store.save(on = false)
        assertFalse(SharedPreferencesAmbientMotionStore(context).load())
    }
}
