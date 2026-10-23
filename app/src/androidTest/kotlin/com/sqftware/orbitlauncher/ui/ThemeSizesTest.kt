package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.IconPlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A theme only draws round an icon's square and a folder's previews, so every theme lays them out at one size, in one place. */
@RunWith(AndroidJUnit4::class)
class ThemeSizesTest {
    @get:Rule
    val compose = createComposeRule()

    private var theme by mutableStateOf(Theme.Space)
    private var look by mutableStateOf(FolderLook.Rim)
    private var place by mutableStateOf(IconPlace.Elsewhere)

    private fun show(item: @Composable () -> Unit) = compose.setContent {
        Themed(theme, theme.colourways.first(), lightWallpaper = false) {
            CompositionLocalProvider(LocalFolderStyle provides FolderStyle(look), LocalIconPlace provides place) {
                Box(Modifier.size(80.dp)) { item() }
            }
        }
    }

    private fun everyThemeAndPlace(check: (Theme, IconPlace) -> Unit) = Theme.entries.forEach { shown ->
        IconPlace.entries.forEach { shownPlace ->
            theme = shown
            place = shownPlace
            check(shown, shownPlace)
        }
    }

    @Test
    fun everyThemeLaysAnIconOutTheSame() {
        show { IconDisc(modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().testTag(ICON)) } }
        val reference = compose.onNodeWithTag(ICON).getUnclippedBoundsInRoot()

        everyThemeAndPlace { shown, shownPlace ->
            assertEquals("$shown, $shownPlace", reference, compose.onNodeWithTag(ICON).getUnclippedBoundsInRoot())
        }
    }

    @Test
    fun everyThemeAndLookLaysOutTheSamePreviews() {
        val folder = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 0), listOf(mail, clock) + alphabet.take(2))
        show { FolderFace(folder, { _, _ -> null }, Modifier.fillMaxSize()) }
        val reference = previews()

        everyThemeAndPlace { shown, shownPlace ->
            // Orbit has no previews: every app circles the planet as a moon.
            shown.folderLooks.filter { it != FolderLook.Orbit }.forEach { shownLook ->
                look = shownLook
                assertEquals("$shown, $shownLook, $shownPlace", reference, previews())
            }
        }
    }

    private fun previews() = (0 until PREVIEWS).map { compose.onNodeWithTag(FolderTags.preview(it)).getUnclippedBoundsInRoot() }

    private companion object {
        const val ICON = "icon"
    }
}
