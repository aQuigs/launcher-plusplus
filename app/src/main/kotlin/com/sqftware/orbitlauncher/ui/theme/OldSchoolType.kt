package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.sqftware.orbitlauncher.R
import kotlin.math.max

/** The game's own type: its plain, bold and small fonts, drawn whole game pixels high. */
private val GameFont = FontFamily(Font(R.font.osrs_plain), Font(R.font.osrs_bold, FontWeight.Bold))
private val GameSmallFont = FontFamily(Font(R.font.osrs_small))

/** The size the game's fonts are drawn at, in its pixels. */
private const val GAME_FONT_SIZE = 16

/**
 * Every style in the game's fonts, each a whole number of screen pixels to one of the font's, with the font's one pixel
 * shadow if [shadowed]: the game draws it under light text on its stone, never under dark text on its chatbox's
 * parchment, where it smears. Half the game's scale for most text, as a phone's type is smaller than the game's, and up
 * to twice it for display type.
 */
internal fun gameTypography(px: Int, density: Density, shadowed: Boolean): Typography {
    fun style(fontPixel: Int, family: FontFamily = GameFont, weight: FontWeight = FontWeight.Normal): TextStyle {
        val size = with(density) { (GAME_FONT_SIZE * fontPixel).toSp() }
        val shadow = Shadow(GameShadow, Offset(fontPixel.toFloat(), fontPixel.toFloat()), 0f).takeIf { shadowed }
        return TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, lineHeight = size * 1.15f, shadow = shadow)
    }
    val small = max(1, px / 2)
    val mid = max(small + 1, px * 3 / 4)
    return Typography(
        displayLarge = style(px * 2, weight = FontWeight.Bold),
        displayMedium = style(px * 2, weight = FontWeight.Bold),
        displaySmall = style(px * 3 / 2, weight = FontWeight.Bold),
        headlineLarge = style(px * 3 / 2, weight = FontWeight.Bold),
        headlineMedium = style(px, weight = FontWeight.Bold),
        headlineSmall = style(px, weight = FontWeight.Bold),
        titleLarge = style(px, weight = FontWeight.Bold),
        titleMedium = style(mid, weight = FontWeight.Bold),
        titleSmall = style(mid),
        bodyLarge = style(mid),
        bodyMedium = style(mid),
        bodySmall = style(small),
        labelLarge = style(mid, weight = FontWeight.Bold),
        labelMedium = style(small),
        labelSmall = style(small, GameSmallFont),
    )
}
