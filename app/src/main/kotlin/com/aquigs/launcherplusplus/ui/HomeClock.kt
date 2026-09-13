package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.ClockFace

object HomeClockTags {
    const val TIME = "clock_time"
    const val DATE = "clock_date"
}

/** The home clock's [face]. Tap the time for the clock app and the date for the calendar. */
@Composable
fun HomeClock(face: ClockFace, onTimeClick: () -> Unit, onDateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = face.time,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Light,
            modifier = Modifier.opens("the clock", onTimeClick).testTag(HomeClockTags.TIME),
        )
        Text(
            text = face.date,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.opens("the calendar", onDateClick).testTag(HomeClockTags.DATE),
        )
    }
}

// Room round the text, so the touch target and the ripple are larger than the glyphs.
private fun Modifier.opens(what: String, onClick: () -> Unit) =
    clip(RoundedCornerShape(12.dp))
        .clickable(onClickLabel = "Open $what", onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 2.dp)
