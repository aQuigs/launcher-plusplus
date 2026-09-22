package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.launcherplusplus.R
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.domain.RingerMode

object HomeClockTags {
    const val TIME = "clock_time"
    const val DATE = "clock_date"
    const val RINGER = "clock_ringer"
}

/** The home clock's [face]. Tap the time for the clock app and the date for the calendar. */
@Composable
fun HomeClock(face: ClockFace, onTimeClick: () -> Unit, onDateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val display = MaterialTheme.typography.displayLarge
        Text(
            text = face.time,
            style = display,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            // A large font scale would otherwise wrap the time onto a second line and push the ring down.
            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = display.fontSize),
            modifier = Modifier.target("Open the clock", onTimeClick).testTag(HomeClockTags.TIME),
        )
        Text(
            text = face.date,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.target("Open the calendar", onDateClick).testTag(HomeClockTags.DATE),
        )
    }
}

/** The ringer's [mode], for under the clock's date. A tap calls [onClick]. */
@Composable
fun RingerSwitch(mode: RingerMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.titleSmall
    // In sp, so the glyph grows with the word at a large font scale.
    val glyph = with(LocalDensity.current) { style.lineHeight.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .target("Switch the ringer", onClick)
            .testTag(HomeClockTags.RINGER)
            // The word as a state, not text, so a screen reader says it once, and again when a tap changes it.
            .clearAndSetSemantics {
                contentDescription = "Ringer"
                stateDescription = mode.name
            },
    ) {
        Icon(painterResource(R.drawable.ic_ringer), contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(glyph))
        Text(text = mode.name, style = style)
    }
}

// At least a finger tall and a little wider than the glyphs, so each target is easy to hit and they are easy to tell apart.
private fun Modifier.target(onClickLabel: String, onClick: () -> Unit) =
    clip(RoundedCornerShape(12.dp))
        .clickable(onClickLabel = onClickLabel, onClick = onClick)
        .defaultMinSize(minHeight = 48.dp)
        .wrapContentHeight()
        .padding(horizontal = 12.dp)
