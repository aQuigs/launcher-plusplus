package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.CREATE_YOUR_OWN
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.title
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionPickerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(fontScale: Float, displayScale: Float) = compose.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density * displayScale, fontScale)) {
            CollectionPicker(page = CollectionsPage(), customs = emptyList(), onToggle = {}, onCreate = {})
        }
    }

    private fun assertEveryNameWhole() {
        val tiles = CollectionKind.all.map { CollectionTags.tile(it) to it.title } + (CollectionTags.CREATE to CREATE_YOUR_OWN)
        tiles.forEach { (tag, title) ->
            compose.pickerTile(tag)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(title) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()

            assertTrue("$title is cut short or split mid-word", layout.breaksBetweenWords(title))
        }
    }

    @Test
    fun everyNameShowsWholeAtTheDefaultSizes() {
        show(fontScale = 1f, displayScale = 1f)
        assertEveryNameWhole()
    }

    @Test
    fun everyNameShowsWholeAtTheLargestFontAndDisplaySizes() {
        show(fontScale = 2f, displayScale = 4f / 3)
        assertEveryNameWhole()
    }
}
