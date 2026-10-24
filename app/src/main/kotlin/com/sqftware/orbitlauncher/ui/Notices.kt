package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

const val NOTICE_MILLIS = 2_000L

/**
 * The launcher's way of saying that something happened or could not: a short note at the foot of the screen for a
 * moment, in its own colors. Only a spin's records and milestones are the system's toasts instead, through Toasts.
 */
@Stable
class Notices {
    private class Shown(val text: String)

    private var shown by mutableStateOf<Shown?>(null)

    /** Puts up [text]; again while it shows starts its moment over. */
    fun show(text: String) {
        shown = Shown(text)
    }

    /** Draws the note showing, if any, until its moment is up. */
    @Composable
    fun Host(modifier: Modifier = Modifier) {
        val note = shown ?: return
        LaunchedEffect(note) {
            delay(NOTICE_MILLIS)
            if (shown === note) shown = null
        }
        NoticeLabel(note.text, modifier)
    }
}

/** The launcher's notices, for any view to [Notices.show] one; one put up outside the launcher shows nowhere. */
val LocalNotices = staticCompositionLocalOf { Notices() }

// A plain box rather than a surface: it must not take the taps meant for what is under it.
@Composable
private fun NoticeLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            // Said aloud, as the system's toast was.
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.inverseOnSurface, textAlign = TextAlign.Center)
    }
}
