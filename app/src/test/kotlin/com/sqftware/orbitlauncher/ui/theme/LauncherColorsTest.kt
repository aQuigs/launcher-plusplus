package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherColorsTest {
    @Test
    fun `no role is left on a stock Material colour`() {
        listOf(LauncherColors to darkColorScheme(), LauncherDayColors to lightColorScheme()).forEach { (scheme, stock) ->
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
        listOf(Triple(LauncherColors, Color.Black, Color.White), Triple(LauncherDayColors, Color.White, Color.Black)).forEach { (scheme, wallpaper, patch) ->
            with(scheme) {
                val opaque = listOf(surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest)
                val veil = surfaceDim.compositeOver(patch)
                val pairs = listOf(
                    onBackground to wallpaper,
                    onSurface to surface.compositeOver(wallpaper),
                    onSecondaryContainer to secondaryContainer.compositeOver(wallpaper),
                    onError to error,
                ) + listOf(onSurface, onSurfaceVariant, primary).map { it to veil } + opaque.map { onSurface to it }
                pairs.forEach { (text, fill) -> assertTrue("$text on $fill", contrast(text, fill) >= 4.5f) }
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
