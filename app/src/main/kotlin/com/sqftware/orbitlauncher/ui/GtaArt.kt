package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.em
import com.sqftware.orbitlauncher.domain.IconPlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.Condensed
import com.sqftware.orbitlauncher.ui.theme.GtaHudColors
import com.sqftware.orbitlauncher.ui.theme.HeadingDisc
import com.sqftware.orbitlauncher.ui.theme.HeadingRing
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.WantedLit
import com.sqftware.orbitlauncher.ui.theme.WantedUnlit
import com.sqftware.orbitlauncher.ui.theme.WheelChip
import com.sqftware.orbitlauncher.ui.theme.WheelChipEdge
import com.sqftware.orbitlauncher.ui.theme.WheelPool
import com.sqftware.orbitlauncher.ui.theme.WheelTile
import com.sqftware.orbitlauncher.ui.theme.WheelTileEdge
import com.sqftware.orbitlauncher.ui.theme.WheelVeil
import com.sqftware.orbitlauncher.ui.theme.legible
import com.sqftware.orbitlauncher.ui.theme.typographyIn
import kotlin.math.cos
import kotlin.math.sin

/** The minimap's screen: how much wider than tall it is, and how round its corners are, as a share of its height. */
private const val MAP_ASPECT = 1.8f
private const val MAP_CORNER = 0.16f

/** A weapon wheel's tile round an app's disc: how much wider and taller than the disc it reaches. */
private const val TILE_WIDE = 1.62f
private const val TILE_TALL = 1.2f

/** The most stars a wanted level has, as in the game. */
private const val MAX_WANTED = 6

/** How tall a wanted star is, and the room between two. */
private val STAR = 18.dp
private val STAR_GAP = 5.dp

/**
 * GTA: an homage to the coast game's heads-up display, drawn by hand rather than from its files. The emblem is its
 * minimap of where the user is, a wide pastel screen turned with the phone that darkens after dark as the game's does,
 * with the player's arrow at its middle; the compass runs along the top of the home page, a thin line with the heading
 * in a dark disc and the user's destination marked in teal, and over the clock the wanted level is a star for each app
 * with something unread; the ring is the apps alone on the wallpaper; a folder opens into a weapon wheel of tiles with
 * its name in the middle; menus and the drawer are the pause menu's charcoal, in a condensed type; and the sunset over
 * the coast is a scene for the wallpaper.
 */
object GtaArt : HudArt(GtaHud, drift = null) {
    override val scene: ThemeScene = Sunset

    /** The game blurs and darkens the world behind its weapon wheel. */
    override val folderBackdrop = FolderBackdrop(blur = 10.dp, veil = WheelVeil)

    /** No track: the game leaves its world clear, so the ring is drawn only as the place an app being dragged would land. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val line = Stroke(1.dp.toPx())
        return { glow, alpha -> if (glow > 0f) drawCircle(colours.lit.copy(alpha = colours.lit.alpha * glow * alpha), radius, centre, style = line) }
    }

    /** A closed folder is a dark chip with a hairline round it, its previews on it. */
    @Composable
    // The wheel's middle is the game's world under its name, not a chip.
    override fun folderWear(folder: RingItem.Folder): FolderWear = FolderWear(behind = Modifier.wheelChip(), fadesOpen = true)

    /** In an open folder an app stands on its tile of the weapon wheel, its disc over it as anywhere. */
    @Composable
    override fun iconGround(): Modifier {
        val disc = super.iconGround()
        return if (LocalIconPlace.current == IconPlace.OpenFolder) Modifier.wheelTile().then(disc) else disc
    }

    /** The wheel's middle: the folder's name, as the game names the weapon picked, coming in as its apps go out to their tiles. */
    @Composable
    override fun OpenFolderOver(folder: RingItem.Folder, shown: () -> Float, modifier: Modifier) {
        val ink = GtaHudColors.ink
        // The shade deepens toward the wheel's middle, as the game's does, so the name reads on a pale wallpaper and an
        // unnamed folder's middle still shows where a tap closes it.
        Box(
            modifier.graphicsLayer { alpha = shown() }.drawBehind { drawCircle(WheelPool) },
            contentAlignment = Alignment.Center,
        ) {
            // A folder the user never named has nothing to call it: the game's middle shows a name only when it has one.
            if (folder.name.isNotBlank()) Text(
                folder.name.uppercase(),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold).legible(ink),
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    /** The compass along the top, and under it, at its right, the wanted level. */
    @Composable
    override fun HomeHeader(discovery: String?, modifier: Modifier) {
        Column(modifier.zIndex(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            HudHeader(GtaHud, discovery, Modifier)
            WantedLevel(LocalUnread.current.appsInShade.coerceAtMost(MAX_WANTED), Modifier.fillMaxWidth(COMPASS_SPAN).padding(top = 4.dp))
        }
    }

    /** The pause menu's panels are plain charcoal. */
    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    @Composable
    override fun typography(day: Boolean): Typography = GtaTypography
}

// The game sets its headings, hints and labels in a condensed type and its body and clock in a plain geometric one.
private val GtaTypography = typographyIn(Condensed, display = 0f, heading = 0.02f, body = 0f, label = 0.06f).copy(
    displayLarge = MaterialTypography.displayLarge,
    displayMedium = MaterialTypography.displayMedium,
    displaySmall = MaterialTypography.displaySmall,
    bodyLarge = MaterialTypography.bodyLarge,
    bodyMedium = MaterialTypography.bodyMedium,
    bodySmall = MaterialTypography.bodySmall,
)

/**
 * The game's view of where the user is: its pastel minimap on a wide screen with a hairline round it, the compass a thin
 * gray line with the heading in a dark disc, the destination a teal pin, and a place discovered named in condensed caps.
 */
private object GtaHud : HudLook {
    override val colors = GtaHudColors
    override val window = MapWindow(MAP_ASPECT, MAP_CORNER)

    // Only the quarters, lettered, hung under the line with the places, as the game's compass shows them.
    override val compassTick = 90f
    override val compassDrop = 0.7f

    // A map pin: a round head over a point at the spot.
    override fun questShape(half: Float) = Path().apply {
        val head = half * 0.62f
        val middle = -half + head
        moveTo(0f, half)
        cubicTo(-head * 0.5f, half * 0.45f, -head, middle + head * 0.75f, -head, middle)
        arcTo(Rect(Offset(0f, middle), head), 180f, 180f, false)
        cubicTo(head, middle + head * 0.75f, head * 0.5f, half * 0.45f, 0f, half)
        close()
    }

    // A thin gray line across, and where the user faces the player's dark disc, ringed in white with its white arrow.
    override fun CacheDrawScope.compassBar(mid: Float, band: Float): CompassBar {
        val line = Stroke(2.dp.toPx())
        val disc = band * 0.42f
        val ring = Stroke(1.dp.toPx())
        val heading = Offset(size.width / 2, mid)
        val arrow = playerArrow(heading, disc * 0.55f)
        return CompassBar(size.width / 2 - disc) {
            drawLine(colors.line, Offset(disc, mid), Offset(size.width - disc, mid), line.width)
            drawCircle(HeadingDisc, disc, heading)
            drawCircle(HeadingRing, disc - ring.width / 2, heading, style = ring)
            drawPath(arrow, colors.ink)
        }
    }

    @Composable
    override fun DiscoveryCard(name: String) {
        Text(
            name.uppercase(),
            Modifier.fillMaxWidth(0.85f),
            style = MaterialTheme.typography.headlineSmall.copy(color = colors.ink, letterSpacing = 0.04.em).legible(colors.ink),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * The wanted level, [stars] of the game's six lit from the right, the rest a faint dark, as the game shows how wanted
 * the player is; none at all while nothing is unread. Its room stays, so the clock does not move as it comes and goes.
 */
@Composable
private fun WantedLevel(stars: Int, modifier: Modifier) {
    val description = if (stars == 0) "" else "Wanted level $stars: ${counted(stars, "app")} with unread notifications"
    Spacer(
        modifier
            .height(STAR)
            .clearAndSetSemantics { if (stars > 0) contentDescription = description }
            .drawWithCache {
                val side = STAR.toPx()
                val gap = STAR_GAP.toPx()
                val star = starPath(side / 2)
                val shadow = Stroke(1.5.dp.toPx())
                onDrawBehind {
                    if (stars == 0) return@onDrawBehind
                    for (index in 0 until MAX_WANTED) {
                        val lit = index >= MAX_WANTED - stars
                        val x = size.width - (MAX_WANTED - index) * (side + gap) + gap + side / 2
                        translate(x, size.height / 2) {
                            if (lit) drawPath(star, WantedUnlit, style = shadow)
                            drawPath(star, if (lit) WantedLit else WantedUnlit)
                        }
                    }
                }
            },
    )
}

/** A five-pointed star of [radius] round the origin, a point up. */
private fun starPath(radius: Float) = Path().apply {
    val inner = radius * 0.45f
    for (point in 0 until 10) {
        val r = if (point % 2 == 0) radius else inner
        val angle = Math.toRadians(-90.0 + point * 36.0)
        val at = Offset((r * cos(angle)).toFloat(), (r * sin(angle)).toFloat())
        if (point == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
    }
    close()
}

/** A closed folder's dark chip and its hairline, drawn on the whole item. */
private fun Modifier.wheelChip() = drawWithCache {
    val radius = size.minDimension / 2
    val edge = Stroke(1.dp.toPx())
    onDrawBehind {
        drawCircle(WheelChip, radius)
        drawCircle(WheelChipEdge, radius - edge.width / 2, style = edge)
    }
}

/** A weapon wheel's tile, wider and a little taller than the disc it lies under. */
private fun Modifier.wheelTile() = drawWithCache {
    val tile = Size(size.width * TILE_WIDE, size.height * TILE_TALL)
    val edge = Stroke(1.dp.toPx())
    val topLeft = size.center - Offset(tile.width / 2, tile.height / 2)
    onDrawBehind {
        drawRect(WheelTile, topLeft, tile)
        drawRect(WheelTileEdge, topLeft + Offset(edge.width / 2, edge.width / 2), Size(tile.width - edge.width, tile.height - edge.width), style = edge)
    }
}
