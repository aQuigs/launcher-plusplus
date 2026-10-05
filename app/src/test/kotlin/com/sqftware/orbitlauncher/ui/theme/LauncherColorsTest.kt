package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.sqftware.orbitlauncher.domain.CardColour
import com.sqftware.orbitlauncher.domain.Colourway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherColorsTest {
    private val nights = Colourway.entries.map { lookOf(it, lightWallpaper = false).colors }
    private val days = Colourway.entries.map { lookOf(it, lightWallpaper = true).colors }

    @Test
    fun `no role is left on a stock Material colour`() {
        (nights.map { it to darkColorScheme() } + days.map { it to lightColorScheme() }).forEach { (scheme, stock) ->
            val ours = scheme.roles()
            val stockRoles = stock.roles()

            assertTrue("found only ${ours.size} roles", ours.size > 30)
            assertEquals(emptyList<String>(), ours.filter { (role, colour) -> colour == stockRoles[role] || colour == Color.Unspecified }.keys.toList())
        }
    }

    // Each scheme over the wallpaper it is picked for (night on black, day on white), and the drawer's veil over the
    // opposite, as the worst patch a wallpaper may have.
    @Test
    fun `text reads over the wallpaper, the glass, the veil, a badge and every opaque container`() {
        (nights.map { Triple(it, Color.Black, Color.White) } + days.map { Triple(it, Color.White, Color.Black) }).forEach { (scheme, wallpaper, patch) ->
            with(scheme) {
                val opaque = listOf(surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
                val veil = surfaceDim.compositeOver(patch)
                val pairs = listOf(
                    onBackground to wallpaper,
                    onSurface to surface.compositeOver(wallpaper),
                    onSecondaryContainer to secondaryContainer.compositeOver(wallpaper),
                    onError to error,
                    error to surfaceContainerHigh,
                ) + listOf(onSurface, onSurfaceVariant, primary).map { it to veil } + opaque.map { onSurface to it }
                pairs.forEach { (text, fill) -> assertTrue("$text on $fill", contrast(text, fill) >= 4.5f) }
            }
        }
    }

    // On the theme's own ground (its deep by night, frost by day) rather than pure black and white, which flatter the tints.
    @Test
    fun `a card's text reads on every card colour over the wallpaper its scheme is for`() {
        listOf(false, true).flatMap { light -> Colourway.entries.map { lookOf(it, light) } }.forEach { look ->
            CardColour.entries.forEach { colour ->
                listOf(look.colors.onSurface, look.colors.onSurfaceVariant).forEach { text ->
                    assertTrue("$text on $colour", contrast(text, look.cardTints.swatch(colour)) >= 4.5f)
                }
            }
        }
    }

    @Test
    fun `what floats over the wallpaper is opaque`() {
        (nights + days).forEach { scheme ->
            with(scheme) {
                listOf(surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
                    .forEach { assertEquals("$it", 1f, it.alpha) }
            }
        }
    }

    // 3:1 is the WCAG floor for a control's edge; either scheme may be showing, so it cannot lean on the scheme's colours.
    @Test
    fun `a folder's edge stands out on a white and on a black wallpaper`() {
        listOf(FolderEdge, DialEdge, GearCog, ChartEdge, GardenEdge, InkEdge, Membrane, PaperEdge, CrystalEdge, CoreEdge).forEach { edge ->
            listOf(Color.White, Color.Black).forEach { wallpaper ->
                val best = listOf(edge.outer, edge.inner).maxOf { contrast(it.compositeOver(wallpaper), wallpaper) }
                assertTrue("edge on $wallpaper: $best", best >= 3f)
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    // Material adds roles between releases; reflection picks them up, so a new one fails here until it is given a value.
    private fun ColorScheme.roles(): Map<String, Color> = ColorScheme::class.java.methods
        .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType }
        .associate { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(this) as Long).toULong()) }
}
