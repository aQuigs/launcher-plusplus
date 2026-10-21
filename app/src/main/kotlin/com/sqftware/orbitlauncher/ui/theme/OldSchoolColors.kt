package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.sqftware.orbitlauncher.domain.CardColour
import com.sqftware.orbitlauncher.domain.Colourway

// The game's interface: its chatbox parchment by day, and by night a shade under its bank brown, with cream text so the
// scheme's dimmed text still reads on the glass, its yellow for what is lit and the orange of a menu's target. What is
// drawn from the game's own sprites keeps their colours, and its orange text is a token of its own.
private val Bank = Color(0xFF201B14)
private val Parchment = Color(0xFFE6DABB)
private val Cream = Color(0xFFF2E8CC)
private val Yellow = Color(0xFFFFFF00)

internal val OldSchoolPalette = Palette(
    deep = Bank,
    accent = Yellow,
    pale = Cream,
    spark = Yellow,
    warm = Color(0xFFFF9040),
    alarm = Color(0xFFFF8C8C),
    frost = Parchment,
    ringMark = Color(0xFFEEEEEE),
)

/** The game's text: the shadow every line casts, the yellow of a stack's count, white for a menu's verb, orange for its target. */
val GameShadow = Color.Black
val StackCount = Yellow
val MenuVerb = Color.White

/** The game's right-click menu: its black box and the brown of its rim and its "Choose Option" header. */
val MenuGround = Color.Black
val MenuRim = Color(0xFF5D5447)
val MenuTarget = Color(0xFFFF9040)

/** The orange of the game's interface text, such as a window's title. */
val GameOrange = Color(0xFFFF981F)

/**
 * The line typed into the chatbox, with the asterisk the game shows for a caret: the light blue it takes over the
 * see-through chatbox, on the night's stone, and the blue over the opaque one's parchment, the day's ground.
 */
val ChatTypedNight = Color(0xFF9090FF)
val ChatTypedDay = Color(0xFF0000FF)

/**
 * The bank: its brown ground, the panel texture's own colour, which the card draws under the texture; and its colours,
 * the night scheme's whatever the wallpaper, as the bank is dark in the game by day as well. Its dimmer text is the
 * parchment rather than the night's mist, which a card colour's wash would leave too dim.
 */
val BankGround = Color(0xFF3E3429)
val BankColors: ColorScheme = lookOf(Colourway.Classic, lightWallpaper = false).colors.copy(onSurfaceVariant = Parchment)

/** What a card [colour] washes the bank's ground in, over the texture: its hue, faint enough that the bank's text reads on it. */
fun bankTint(colour: CardColour): Color = colour.hue?.copy(alpha = BANK_TINT) ?: Color.Transparent

private const val BANK_TINT = 0.25f

/** What shows through the minimap's frame before the map has loaded: the game's grass. */
val MinimapGrass = Color(0xFF698F24)

/** The player's own square at the minimap's middle, outlined so it shows on a white wall. */
val MinimapSelf = Color.White
val MinimapSelfEdge = Color.Black
