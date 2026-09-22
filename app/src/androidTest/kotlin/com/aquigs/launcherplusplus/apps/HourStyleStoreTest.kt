package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HourStyleStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesHourStyleStore(context)

    @Before
    @After
    fun forgetTheChoice() = context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit(commit = true) { clear() }

    // A clock that has never been flipped must keep following the system, not fall to 12 hours.
    @Test
    fun nothingIsChosenUntilSavedAndTwelveHoursComeBackAsChosen() {
        assertNull(store.load())

        store.save(twentyFourHour = false)
        assertEquals(false, SharedPreferencesHourStyleStore(context).load())
    }
}
