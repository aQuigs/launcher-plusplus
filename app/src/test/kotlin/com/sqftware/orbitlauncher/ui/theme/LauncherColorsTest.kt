package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherColorsTest {
    @Test
    fun `no role is left on a stock Material colour`() {
        val stock = darkColorScheme().roles()
        val ours = LauncherColors.roles()

        assertTrue("found only ${ours.size} roles", ours.size > 30)
        assertEquals(emptyList<String>(), ours.filter { (role, colour) -> colour == stock[role] || colour == Color.Unspecified }.keys.toList())
    }

    // Material adds roles between releases; reflection picks them up, so a new one fails here until it is given a value.
    private fun ColorScheme.roles(): Map<String, Color> = ColorScheme::class.java.methods
        .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == Long::class.javaPrimitiveType }
        .associate { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(this) as Long).toULong()) }
}
