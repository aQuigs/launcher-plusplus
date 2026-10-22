package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.sqftware.orbitlauncher.domain.CardColour
import com.sqftware.orbitlauncher.domain.minimap.GRASS_DARK

// The game's interface: its chatbox parchment by day, and by night a shade under its bank brown, with cream text so the
// scheme's dimmed text still reads on the glass, its yellow for what is lit and the orange of a menu's target. What is
// drawn from the game's own sprites keeps their colours, and its orange text is a token of its own.
private val Bank = Color(0xFF201B14)
private val Parchment = Color(0xFFE6DABB)
private val Cream = Color(0xFFF2E8CC)
private val Yellow = Color(0xFFFFFF00)
private val Target = Color(0xFFFF9040)

internal val OldSchoolPalette = Palette(
    deep = Bank,
    accent = Yellow,
    pale = Cream,
    spark = Yellow,
    warm = Target,
    alarm = Color(0xFFFF8C8C),
    frost = Parchment,
    ringMark = Color(0xFFEEEEEE),
)

/**
 * The game's text: the shadow every line casts, the yellow of a stack's count, which its clock and hints take too, white
 * for a hitsplat's count and a menu's verb, and orange for a menu's target.
 */
val GameShadow = Color.Black
val StackCount = Yellow
val HitsplatCount = Color.White
val MenuVerb = Color.White

/** The game's right-click menu: its brown box, and the black of its "Choose Option" bar and the line round its options. */
val MenuGround = Color(0xFF5D5447)
val MenuLine = Color.Black
val MenuTarget = Target

/** The orange of the game's interface text, such as a window's title. */
val GameOrange = Color(0xFFFF981F)

/**
 * The line typed into the chatbox, with the asterisk the game shows for a caret: the light blue it takes over the
 * see-through chatbox, on the night's stone, and the blue over the opaque one's parchment, the day's ground.
 */
val ChatTypedNight = Color(0xFF9090FF)
val ChatTypedDay = Color(0xFF0000FF)

/**
 * The bank's brown ground, the panel texture's own colour, which the card draws under the texture. The bank's text is in
 * the night scheme whatever the wallpaper, its [BankDimText] the parchment rather than the night's mist, which a card
 * colour's wash would leave too dim.
 */
val BankGround = Color(0xFF3E3429)
val BankDimText = Parchment

/** What a card [colour] washes the bank's ground in, over the texture: its hue, faint enough that the bank's text reads on it. */
fun bankTint(colour: CardColour): Color = colour.hue?.copy(alpha = BANK_TINT) ?: Color.Transparent

/** [colour] as the bank shows it, its wash over the bank's ground, for a swatch that looks as the card will. */
fun bankSwatch(colour: CardColour): Color = bankTint(colour).compositeOver(BankGround)

private const val BANK_TINT = 0.25f

/** What shows through the minimap's frame before the map has loaded: the game's grass. */
val MinimapGrass = Color(0xFF000000.toInt() or GRASS_DARK)

/** The player's own square at the minimap's middle, outlined so it shows on a white wall. */
val MinimapSelf = Color.White
val MinimapSelfEdge = Color.Black
