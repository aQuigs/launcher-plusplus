package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.CapCrimpLit
import com.sqftware.orbitlauncher.ui.theme.CapCrimpShade
import com.sqftware.orbitlauncher.ui.theme.CapFace
import com.sqftware.orbitlauncher.ui.theme.CapPrint
import com.sqftware.orbitlauncher.ui.theme.PipBoyHudColors
import com.sqftware.orbitlauncher.ui.theme.PipBoyScanline
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.ScanlineTile
import com.sqftware.orbitlauncher.ui.theme.VaultBand
import com.sqftware.orbitlauncher.ui.theme.VaultSteel
import com.sqftware.orbitlauncher.ui.theme.VaultSteelDark
import com.sqftware.orbitlauncher.ui.theme.legible
import com.sqftware.orbitlauncher.ui.theme.typographyIn
import kotlin.math.roundToInt

/** How far apart the screen's scanlines are, and how thick each is. */
private val SCANLINE_PITCH = 3.dp
private val SCANLINE = 1.dp

/** How many marks the ring's gauge has, and how many steps apart its long ones are. */
private const val GAUGE_TICKS = 60
private const val GAUGE_MAJOR = 5

/** How many marks round the map's rim, and how many steps apart those at the quarters are. */
private const val RIM_TICKS = 36
private const val RIM_QUARTER = 9

/** How wide each of the compass's bracket ends is, as a share of the bar's height. */
private const val COMPASS_BRACKET = 0.35f

/** A vault door's previews take its middle, inside its ring, and so many cogs stand round its edge. */
private const val VAULT_DISC = 0.6f
private const val VAULT_COGS = 12

/** A bottle cap's previews take its face, and so many crimps go round its skirt. */
private const val CAP_DISC = 0.62f
private const val CAP_CRIMPS = 21

/**
 * Fallout: an homage to the wasteland games' wrist computer, drawn by hand rather than from their files. The emblem is
 * its green map of where the user is, scanlined like its screen, turned with the phone, with the player's arrow at its
 * middle; the compass runs along the top of the home page, ticked every 15°, with the map's places and the user's quest
 * marked on it, and names each place the user discovers below it; dust blows over the launcher; the ring is a gauge's
 * scale; folders are vault doors or bottle caps; menus are bracketed in phosphor, in a monospaced type.
 */
object FalloutArt : HudArt(PipBoyHud, Dust) {
    /** A gauge's scale: short marks round the ring and long ones every fifth, standing out from the track. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit = tickedTrack(
        centre, slots, radius, iconSize, colours,
        minor = ticks(centre, GAUGE_TICKS, radius, radius + 4.dp.toPx()) { it % GAUGE_MAJOR != 0 },
        major = ticks(centre, GAUGE_TICKS, radius - 3.dp.toPx(), radius + 9.dp.toPx()) { it % GAUGE_MAJOR == 0 },
        majorWidth = 1.5.dp,
    )

    @Composable
    override fun folderWear(folder: RingItem.Folder): FolderWear = when (LocalFolderStyle.current.look) {
        FolderLook.BottleCap -> FolderWear(CAP_DISC, Modifier.bottleCap())
        else -> FolderWear(VAULT_DISC, Modifier.vaultDoor())
    }

    /** The screen's scanlines, faint, fixed to the screen as the list scrolls under them. */
    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha > 0f) drawScanlines(colour.copy(alpha = colour.alpha * 0.12f))
    }

    @Composable
    override fun menuBox(ground: PanelGround): Modifier = bracketedBox(ground)

    override val menuRim: BorderStroke?
        @Composable get() = null

    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = bracketedBox(ground)

    @Composable
    override fun typography(day: Boolean): Typography = FalloutTypography
}

// The wrist computer sets everything in one monospaced face, its small type spaced out a little.
private val FalloutTypography = typographyIn(FontFamily.Monospace, display = 0f, heading = 0f, body = 0f, label = 0.06f)

/**
 * The wrist computer's view of where the user is: its green map scanlined in a phosphor-rimmed disc, ticked round like a
 * dial, the compass a fine scale between brackets, the quest an amber chevron, and a place discovered named in a box of
 * the screen's dark.
 */
private object PipBoyHud : HudLook {
    override val colors = PipBoyHudColors
    override val compassTick = 15f

    // A chevron pointing down at the spot, as the games mark a quest's target.
    override fun questShape(half: Float) = Path().apply {
        moveTo(-half * 0.8f, -half)
        lineTo(0f, -half * 0.45f)
        lineTo(half * 0.8f, -half)
        lineTo(0f, half)
        close()
    }

    // A dial's marks round the rim, longer at the quarters and a small wedge at north.
    override fun CacheDrawScope.rimMarks(center: Offset, radius: Float): DrawScope.() -> Unit {
        val short = 3.dp.toPx()
        val marks = ticks(center, RIM_TICKS, radius - short, radius) { it % RIM_QUARTER != 0 }
        val quarters = ticks(center, RIM_TICKS, radius - 2 * short, radius) { it % RIM_QUARTER == 0 }
        val north = Path().apply {
            val wedge = 2.5f * short
            moveTo(center.x, center.y - radius + wedge)
            lineTo(center.x - wedge / 2, center.y - radius)
            lineTo(center.x + wedge / 2, center.y - radius)
            close()
        }
        val ink = colors.rim.inner
        val thin = Stroke(1.dp.toPx())
        val thick = Stroke(1.5.dp.toPx())
        return {
            drawPath(marks, ink.copy(alpha = ink.alpha * 0.6f), style = thin)
            drawPath(quarters, ink, style = thick)
            drawPath(north, ink)
        }
    }

    override fun CacheDrawScope.screenOver(): DrawScope.() -> Unit = { drawScanlines(PipBoyScanline) }

    // A fine scale on a shade of the screen's dark, a baseline under it, square brackets at its ends and a notch above
    // its middle where the user faces.
    override fun CacheDrawScope.compassBar(mid: Float, band: Float): CompassBar {
        val top = mid - band / 2
        val bottom = mid + band / 2
        val bracket = band * COMPASS_BRACKET
        val line = Stroke(1.dp.toPx())
        val thick = Stroke(2.dp.toPx())
        val shade = fadedAtEnds(colors.shade, 0.06f)
        val end = Path().apply {
            moveTo(bracket, top)
            lineTo(0f, top)
            lineTo(0f, bottom)
            lineTo(bracket, bottom)
        }
        val notch = Path().apply {
            val half = 4.dp.toPx()
            moveTo(size.width / 2 - half, top - half)
            lineTo(size.width / 2 + half, top - half)
            lineTo(size.width / 2, top + half * 0.4f)
            close()
        }
        return CompassBar(size.width / 2 - 2 * bracket) {
            drawRect(shade, topLeft = Offset(0f, top), size = Size(size.width, band))
            drawLine(colors.line, Offset(bracket, bottom), Offset(size.width - bracket, bottom), line.width)
            drawPath(end, colors.line, style = thick)
            scale(-1f, 1f) { drawPath(end, colors.line, style = thick) }
            drawPath(notch, colors.line)
        }
    }

    // What happened, small, over the place's name.
    @Composable
    override fun DiscoveryCard(name: String) {
        val type = MaterialTheme.typography
        Text("LOCATION DISCOVERED", style = type.labelLarge.copy(color = colors.line, letterSpacing = 0.2.em).legible(colors.line))
        Text(
            name.uppercase(),
            Modifier.fillMaxWidth(0.85f).padding(top = 4.dp),
            style = type.headlineSmall.copy(color = colors.ink).legible(colors.ink),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

// The scanlines' tile for each screen density drawn at: built once, as the drawer's ground redraws on every frame of a drag.
private val scanlineTiles = mutableMapOf<Float, ShaderBrush>()

/** Faint horizontal lines across the whole of what is drawn, in [colour], as a tube's screen shows: one tiled line. */
private fun DrawScope.drawScanlines(colour: Color) {
    val tiles = scanlineTiles.getOrPut(density) {
        val pitch = SCANLINE_PITCH.roundToPx().coerceAtLeast(2)
        val line = SCANLINE.toPx().roundToInt().coerceIn(1, pitch - 1)
        val tile = ImageBitmap(1, pitch)
        Canvas(tile).drawRect(0f, 0f, 1f, line.toFloat(), Paint().apply { color = ScanlineTile })
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
    drawRect(tiles, alpha = colour.alpha, colorFilter = ColorFilter.tint(colour.copy(alpha = 1f), BlendMode.SrcIn))
}

/**
 * A menu's or dialog's box, as the wrist computer frames its panels: the solid [ground] squared off, a faint phosphor
 * line round it and brackets at its corners.
 */
@Composable
private fun bracketedBox(ground: PanelGround): Modifier {
    val ink = MaterialTheme.colorScheme.primary
    return Modifier.solidGround(ground, RectangleShape).drawWithCache {
        val line = Stroke(1.dp.toPx())
        val thick = Stroke(2.dp.toPx())
        val arm = 10.dp.toPx()
        val inset = thick.width / 2
        val corners = Path().apply {
            for ((x, y) in listOf(inset to inset, size.width - inset to inset, inset to size.height - inset, size.width - inset to size.height - inset)) {
                val alongX = if (x < size.width / 2) arm else -arm
                val alongY = if (y < size.height / 2) arm else -arm
                moveTo(x + alongX, y)
                lineTo(x, y)
                lineTo(x, y + alongY)
            }
        }
        onDrawWithContent {
            drawContent()
            drawRect(ink.copy(alpha = 0.35f), topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset), style = line)
            drawPath(corners, ink, style = thick)
        }
    }
}

/** A vault's round door: its steel cogged at the edge, a ring rolled round its middle, and a band of paint across its top. */
private fun Modifier.vaultDoor() = drawWithCache {
    val radius = size.minDimension / 2
    val cogs = cogPath(size.center, radius, radius * 0.86f, VAULT_COGS)
    val ring = Stroke(radius * 0.1f)
    val inner = radius * 0.78f
    val band = Stroke(radius * 0.09f)
    onDrawBehind {
        drawPath(cogs, VaultSteel)
        drawCircle(VaultSteelDark, inner + ring.width / 2)
        drawCircle(VaultSteel, inner - ring.width / 2, style = ring)
        drawArc(
            VaultBand,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = center - Offset(inner, inner),
            size = Size(2 * inner, 2 * inner),
            style = band,
        )
    }
}

/** A bottle cap seen from above: its crimped skirt lit from the upper left, its face, and a ring printed on it. */
private fun Modifier.bottleCap() = drawWithCache {
    val radius = size.minDimension / 2
    val skirt = cogPath(size.center, radius, radius * 0.88f, CAP_CRIMPS)
    val light = Brush.linearGradient(listOf(CapCrimpLit, CapCrimpShade), start = Offset.Zero, end = Offset(size.width, size.height))
    val print = Stroke(radius * 0.06f)
    onDrawBehind {
        drawPath(skirt, light)
        drawCircle(CapFace, radius * 0.8f)
        drawCircle(CapPrint, radius * 0.72f, style = print)
    }
}
