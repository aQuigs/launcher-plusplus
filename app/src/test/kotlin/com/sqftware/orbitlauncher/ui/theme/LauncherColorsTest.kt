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

    // Every colour a box's text comes in, on a panel's box over the worst patch of wallpaper as above, and every box and
    // floating mark against the wallpaper its scheme is for. A box on the wallpaper is the card glass, whose showing hangs on
    // the wallpaper, and one on a pop-up has no fill, so only a panel's is held to show. A floating Pane is on the pop-up
    // ground.
    // Body text is held to WCAG's 4.5:1; the accents, which mark large figures and glyphs, and the lit choice to its 3:1.
    @Test
    fun `boxes on a panel and floating marks show, and text on a box reads on every ground`() {
        val failures = mutableListOf<String>()
        listOf(false, true).flatMap { light -> Colourway.entries.map { Triple(it, light, lookOf(it, light)) } }.forEach { (way, light, look) ->
            val wallpapers = if (light) listOf(Color.White, Color.Black) else listOf(Color.Black, Color.White)
            with(look.colors) {
                val texts = listOf(onSurface to 4.5f, onSurfaceVariant to 4.5f, primary to 3f, tertiary to 3f)
                Ground.entries.forEach { ground ->
                    val (usual, worst) = wallpapers.map { wallpaper ->
                        when (ground) {
                            Ground.Wallpaper -> wallpaper
                            Ground.Panel -> surfaceDim.compositeOver(wallpaper)
                            Ground.Popup -> surfaceDim.copy(alpha = 1f)
                        }
                    }
                    val box = look.boxes.on(ground)
                    val under = box.compositeOver(if (ground == Ground.Wallpaper) usual else worst)
                    texts.forEach { (text, least) ->
                        val reads = contrast(text, under)
                        if (reads < least) failures += "$text on a box on $ground, $way, light $light: $reads"
                    }
                    if (ground != Ground.Wallpaper) {
                        val shows = contrast(box.compositeOver(usual), usual)
                        val floats = contrast(look.boxes.floating, usual)
                        if (ground == Ground.Panel && shows < 1.15f) failures += "a box on $ground, $way, light $light: $shows"
                        if (floats < 1.15f) failures += "a floating mark on $ground, $way, light $light: $floats"
                    }
                }
                val lit = contrast(look.boxes.onLit, look.boxes.lit)
                if (lit < 3f) failures += "a selected box, $way, light $light: $lit"
                assertEquals(1f, look.boxes.floating.alpha)
            }
        }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `the bank's text reads on its ground in every card colour`() {
        CardColour.entries.forEach { colour ->
            val ground = bankTint(colour).compositeOver(BankGround)
            listOf(BankColors.onSurface, BankColors.onSurfaceVariant).forEach { text ->
                assertTrue("$text on $colour", contrast(text, ground) >= 4.5f)
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
