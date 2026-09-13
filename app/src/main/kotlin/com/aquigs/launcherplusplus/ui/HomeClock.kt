package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.launcherplusplus.domain.ClockFace

object HomeClockTags {
    const val TIME = "clock_time"
    const val DATE = "clock_date"
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
            modifier = Modifier.opens("the clock", onTimeClick).testTag(HomeClockTags.TIME),
        )
        Text(
            text = face.date,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.opens("the calendar", onDateClick).testTag(HomeClockTags.DATE),
        )
    }
}

// At least a finger tall and a little wider than the glyphs, so each target is easy to hit and the two are easy to tell apart.
private fun Modifier.opens(what: String, onClick: () -> Unit) =
    clip(RoundedCornerShape(12.dp))
        .clickable(onClickLabel = "Open $what", onClick = onClick)
        .defaultMinSize(minHeight = 48.dp)
        .wrapContentHeight()
        .padding(horizontal = 12.dp)
