package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.sqftware.orbitlauncher.ui.theme.Ground
import com.sqftware.orbitlauncher.ui.theme.LocalGround
import com.sqftware.orbitlauncher.ui.theme.Spacing

// The kinds of text the launcher shows outside its boxes and buttons, each in one style wherever it is, so a screen does
// not pick its own size or colour for a title, a heading, a line that explains, or a note that there is nothing here.

/**
 * The head of a full-screen view: its [title], what it is for in [subtitle], if anything, and a close button calling
 * [onClose], so leaving it does not hang on knowing Back.
 */
@Composable
fun PanelHeader(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().padding(start = Spacing.Edge, top = Spacing.Top, end = Spacing.Tight, bottom = Spacing.Tight)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        subtitle?.let { SupportingText(it, Modifier.padding(end = Spacing.Tight)) }
    }
}

/** The head of a section of a list or a dialog, such as a letter of the drawer's A to Z. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

/** A line that explains what is around it: under a title, in a prompt, beside a choice. */
@Composable
fun SupportingText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/**
 * Says there is nothing here yet, and what to do about it in [action], if anything, centred in its place. Straight on
 * the wallpaper it takes the theme's ink and shade, which read on any wallpaper.
 */
@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val art = LocalThemeArt.current
    val onWallpaper = LocalGround.current == Ground.Wallpaper
    val style = MaterialTheme.typography.bodyLarge.let { if (onWallpaper) it.copy(shadow = Shadow(art.hintShade, blurRadius = 6f)) else it }

    Column(
        modifier.fillMaxWidth().padding(Spacing.Edge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Gap),
    ) {
        Text(
            text,
            style = style,
            color = if (onWallpaper) art.hintInk else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
