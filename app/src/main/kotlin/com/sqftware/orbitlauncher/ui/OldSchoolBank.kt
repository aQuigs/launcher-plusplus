package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.CardColour
import com.sqftware.orbitlauncher.ui.theme.BankGround
import com.sqftware.orbitlauncher.ui.theme.GameOrange
import com.sqftware.orbitlauncher.ui.theme.BankDimText
import com.sqftware.orbitlauncher.ui.theme.StackCount
import com.sqftware.orbitlauncher.ui.theme.bankTint
import kotlin.math.min

/** How many of the game's pixels thick its interface frame is, and how far a card's content keeps inside it. */
private const val FRAME = 3
private const val FRAME_GAP = 3

/** The frame's corner sprites are this many of the game's pixels square. */
private const val CORNER = 32

/**
 * Where the frame's straight pieces lie in their sprites: the top and bottom edges along the top of theirs, the left and
 * right edges down the middle of theirs, the divider across the middle of its, and the stub of each of the divider's
 * joins with the frame this far down its sprite.
 */
private val TOP_EDGE = IntRect(0, 0, CORNER, FRAME)
private val LEFT_EDGE = IntRect(13, 0, 13 + FRAME, CORNER)
private val RIGHT_EDGE = IntRect(17, 0, 17 + FRAME, CORNER)
private val DIVIDER = IntRect(0, 13, CORNER, 13 + FRAME)
private const val JOIN_STUB = 15

/** The game's pixels round a bank tab's face that are its own: its rim and highlight, and its bevelled top right. */
private const val TAB_LEFT = 3
private const val TAB_RIGHT = 8
private const val TAB_TOP = 7
private const val TAB_MIDDLE = 20

/** How tall a card's bank tab is, in the game's pixels: a little under the game's own, so the header stays a button high. */
private const val TAB_HEIGHT = 32

/** The game's small stone button, in its pixels. */
private const val STONE_BUTTON = 25

/** The bank's sprites a card is drawn from, loaded once for every card. */
private class BankSprites(
    val texture: ImageBitmap,
    val corners: List<ImageBitmap>,
    val top: ImageBitmap,
    val bottom: ImageBitmap,
    val left: ImageBitmap,
    val right: ImageBitmap,
    val divider: ImageBitmap,
    val joinLeft: ImageBitmap,
    val joinRight: ImageBitmap,
)

@Composable
private fun bankSprites() = BankSprites(
    texture = sprite(R.drawable.osrs_897_0),
    corners = listOf(sprite(R.drawable.osrs_991_0), sprite(R.drawable.osrs_992_0), sprite(R.drawable.osrs_993_0), sprite(R.drawable.osrs_994_0)),
    top = sprite(R.drawable.osrs_989_0),
    bottom = sprite(R.drawable.osrs_995_0),
    left = sprite(R.drawable.osrs_988_0),
    right = sprite(R.drawable.osrs_990_0),
    divider = sprite(R.drawable.osrs_987_0),
    joinLeft = sprite(R.drawable.osrs_996_0),
    joinRight = sprite(R.drawable.osrs_997_0),
)

/**
 * A collection as the game's bank: its brown panel texture washed in the card's [colour], in the interface's stone frame
 * with the frame's divider under the [header], as the bank has under its tabs, and a yellow rim inside the frame while
 * it is a [dropTarget]. Inside, the bank's own colours, dark whatever the wallpaper, as the game's bank is.
 */
@Composable
internal fun BankCard(
    colour: CardColour,
    dropTarget: Boolean,
    modifier: Modifier,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    val sprites = bankSprites()
    val px = gamePixel
    val tint = bankTint(colour)
    val inset = ((FRAME + FRAME_GAP) * px).asDp()
    val frame = (FRAME * px).asDp()

    GameNight {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onSurfaceVariant = BankDimText)) {
            Column(
                modifier
                    .drawBehind {
                        clipRect {
                            drawRect(BankGround)
                            drawTiled(sprites.texture, IntRect(IntOffset.Zero, IntSize(sprites.texture.width, sprites.texture.height)), px, mirror = false)
                            drawRect(tint)
                        }
                        drawFrame(sprites, px)
                        if (dropTarget) {
                            val rim = (FRAME * px).toFloat()
                            drawRect(StackCount, Offset(rim + px / 2f, rim + px / 2f), Size(size.width - 2 * rim - px, size.height - 2 * rim - px), style = Stroke(px.toFloat()))
                        }
                    },
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    // The buttons' own padding keeps them off the frame's right edge.
                    Box(Modifier.padding(start = inset, end = frame, top = inset)) { header() }
                    Spacer(Modifier.fillMaxWidth().height(inset).drawBehind { drawDivider(sprites, px) })
                    Column(Modifier.padding(start = inset, end = frame, bottom = frame)) { body() }
                }
            }
        }
    }
}

/** The interface's stone frame round the draw scope: its corners, and its straight edges repeated between them. */
private fun DrawScope.drawFrame(sprites: BankSprites, scale: Int) {
    val width = size.width.toInt()
    val height = size.height.toInt()
    val corner = CORNER * scale
    val thick = FRAME * scale

    repeatAlong(sprites.top, TOP_EDGE, IntOffset(corner, 0), width - corner, scale, across = true)
    repeatAlong(sprites.bottom, TOP_EDGE, IntOffset(corner, height - thick), width - corner, scale, across = true)
    repeatAlong(sprites.left, LEFT_EDGE, IntOffset(0, corner), height - corner, scale, across = false)
    repeatAlong(sprites.right, RIGHT_EDGE, IntOffset(width - thick, corner), height - corner, scale, across = false)
    listOf(IntOffset(0, 0), IntOffset(width - corner, 0), IntOffset(0, height - corner), IntOffset(width - corner, height - corner))
        .zip(sprites.corners)
        .forEach { (at, image) -> drawSprite(image, Offset(at.x.toFloat(), at.y.toFloat()), scale) }
}

/** The frame's divider across the top of the draw scope, joined to the frame's edges at both ends as the game joins it. */
private fun DrawScope.drawDivider(sprites: BankSprites, scale: Int) {
    val width = size.width.toInt()
    repeatAlong(sprites.divider, DIVIDER, IntOffset(0, 0), width, scale, across = true)
    val joinTop = (-JOIN_STUB * scale).toFloat()
    drawSprite(sprites.joinLeft, Offset(0f, joinTop), scale)
    drawSprite(sprites.joinRight, Offset((width - CORNER * scale).toFloat(), joinTop), scale)
}

/**
 * The part [from] of [image], repeated rightwards from [start] up to [end] if [across], or else downwards, each pixel
 * [scale] square; the last cut short.
 */
private fun DrawScope.repeatAlong(image: ImageBitmap, from: IntRect, start: IntOffset, end: Int, scale: Int, across: Boolean) {
    var at = if (across) start.x else start.y
    while (at < end) {
        val run = min(if (across) from.width else from.height, (end - at + scale - 1) / scale)
        val part = IntRect(from.topLeft, if (across) IntSize(run, from.height) else IntSize(from.width, run))
        drawSpritePart(image, part, if (across) Offset(at.toFloat(), start.y.toFloat()) else Offset(start.x.toFloat(), at.toFloat()), scale)
        at += run * scale
    }
}

/**
 * The game's selected bank tab with the collection's [glyph] and [title] on it, set as every theme sets them, in the
 * interface's orange, as wide as they need up to the room [modifier] gives: its rim and bevelled corner as the game draws
 * them, its face stretched from one of its own pixels, so every pixel stays whole.
 */
@Composable
internal fun BankTab(glyph: ImageVector, title: String, modifier: Modifier) {
    val tab = sprite(R.drawable.osrs_1079_0)
    val px = gamePixel
    val height = (TAB_HEIGHT * px).asDp()

    Box(modifier) {
        CompositionLocalProvider(LocalContentColor provides GameOrange) {
            PlainCardTitle(
                glyph,
                title,
                Modifier
                    .heightIn(min = height)
                    .drawBehind { drawStretched(tab, px) }
                    .padding(start = (TAB_LEFT * px).asDp() + 4.dp, end = (TAB_RIGHT * px).asDp()),
            )
        }
    }
}

/**
 * [image] filling the draw scope as a nine-patch at [scale]: its corners whole, and between them the one row or column
 * of it at [TAB_MIDDLE] stretched, which a flat face keeps crisp at any size.
 */
private fun DrawScope.drawStretched(image: ImageBitmap, scale: Int) {
    val width = size.width.toInt()
    val height = size.height.toInt()
    val columns = listOf(0 to TAB_LEFT, TAB_MIDDLE to 1, image.width - TAB_RIGHT to TAB_RIGHT)
    val rows = listOf(0 to TAB_TOP, TAB_MIDDLE to 1)
    val xs = listOf(0 to TAB_LEFT * scale, TAB_LEFT * scale to width - (TAB_LEFT + TAB_RIGHT) * scale, width - TAB_RIGHT * scale to TAB_RIGHT * scale)
    val ys = listOf(0 to TAB_TOP * scale, TAB_TOP * scale to height - TAB_TOP * scale)
    rows.zip(ys).forEach { (row, y) ->
        columns.zip(xs).forEach { (column, x) ->
            if (x.second > 0 && y.second > 0) {
                drawImage(
                    image,
                    IntOffset(column.first, row.first),
                    IntSize(column.second, row.second),
                    IntOffset(x.first, y.first),
                    IntSize(x.second, y.second),
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}

/** A card header's button as the game's small stone button, its [glyph] on it in the interface's orange. */
@Composable
internal fun StoneButtonFace(glyph: ImageVector, contentDescription: String?, glyphModifier: Modifier) {
    val stone = sprite(R.drawable.osrs_195_0)
    val px = gamePixel
    Box(contentAlignment = Alignment.Center, modifier = Modifier.spriteSized(stone, px)) {
        Icon(glyph, contentDescription, glyphModifier.size(20.dp), tint = GameOrange)
    }
}

/** The bin as a stone button with a bin on it, red as the game's selected one while an app is over it. */
@Composable
internal fun StoneBin(highlighted: Boolean, modifier: Modifier) {
    val stone = sprite(if (highlighted) R.drawable.osrs_196_0 else R.drawable.osrs_195_0)
    val px = gamePixel

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.drawBehind {
            // Twice the game's pixels, as its largest type is drawn, unless the bin is smaller.
            drawSpriteCentred(stone, center, fitScale(size.minDimension, STONE_BUTTON.toFloat(), 2 * px))
        },
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = if (highlighted) StackCount else GameOrange)
    }
}
