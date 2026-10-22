package com.sqftware.orbitlauncher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.ClockFace
import com.sqftware.orbitlauncher.domain.RingerMode
import com.sqftware.orbitlauncher.ui.theme.legible

object HomeClockTags {
    const val TIME = "clock_time"
    const val DATE = "clock_date"
    const val RINGER = "clock_ringer"
    const val UPDATE = "clock_update"
}

/** The home clock's [face]. Tap the time for the clock app and the date for the calendar. */
@Composable
fun HomeClock(face: ClockFace, onTimeClick: () -> Unit, onDateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        LocalThemeArt.current.ClockTime(face.time, Modifier.target("Open the clock", onTimeClick, MaterialTheme.shapes.medium).testTag(HomeClockTags.TIME))
        Text(
            text = face.date,
            style = MaterialTheme.typography.titleMedium.legible(),
            modifier = Modifier.target("Open the calendar", onDateClick, MaterialTheme.shapes.medium).testTag(HomeClockTags.DATE),
        )
    }
}

/** The clock's time as most themes show it: the scheme's largest type, light. */
@Composable
internal fun PlainClockTime(time: String, modifier: Modifier) {
    val display = MaterialTheme.typography.displayLarge
    Text(
        text = time,
        style = display.legible(),
        fontWeight = FontWeight.Light,
        maxLines = 1,
        // A large font scale would otherwise wrap the time onto a second line and push the ring down.
        autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = display.fontSize),
        modifier = modifier,
    )
}

/** The ringer's [mode], for under the clock's date. A tap calls [onClick]. */
@Composable
fun RingerSwitch(mode: RingerMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlyphLabel(
        glyph = R.drawable.ic_ringer,
        label = mode.name,
        modifier = modifier
            .target("Switch the ringer", onClick, MaterialTheme.shapes.medium)
            .testTag(HomeClockTags.RINGER)
            // The word as a state, not text, so a screen reader says it once, and again when a tap changes it.
            .clearAndSetSemantics {
                contentDescription = "Ringer"
                stateDescription = mode.name
            },
    )
}

/** Says a newer launcher is out, beside the ringer. A tap calls [onClick]. */
@Composable
fun UpdateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlyphLabel(
        glyph = R.drawable.ic_update,
        label = "Update",
        modifier = modifier.target("Open the update", onClick, MaterialTheme.shapes.medium).testTag(HomeClockTags.UPDATE),
    )
}

@Composable
private fun GlyphLabel(@DrawableRes glyph: Int, label: String, modifier: Modifier) {
    val style = MaterialTheme.typography.titleSmall
    // In sp, so the glyph grows with the word at a large font scale.
    val size = with(LocalDensity.current) { style.lineHeight.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(painterResource(glyph), contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(size))
        Text(text = label, style = style.legible())
    }
}

// At least a finger tall and a little wider than the glyphs, so each target is easy to hit and they are easy to tell apart.
private fun Modifier.target(onClickLabel: String, onClick: () -> Unit, shape: Shape) =
    clip(shape)
        .clickable(onClickLabel = onClickLabel, onClick = onClick)
        .defaultMinSize(minHeight = 48.dp)
        .wrapContentHeight()
        .padding(horizontal = 12.dp)
