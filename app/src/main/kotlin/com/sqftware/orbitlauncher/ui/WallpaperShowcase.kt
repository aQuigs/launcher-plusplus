package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private const val FADE_MILLIS = 400

/**
 * [content], faded away while [showing] so the wallpaper shows alone, until a tap anywhere calls [onDone]. It stays
 * composed meanwhile, so it comes back as it was, and takes no touches until it is whole again: a tap on the way back
 * must not land on an app not yet seen.
 */
@Composable
fun WallpaperShowcase(showing: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shown = animateFloatAsState(if (showing) 0f else 1f, tween(FADE_MILLIS), label = "launcher")
    val fading by remember { derivedStateOf { shown.value < 1f } }
    val away = showing || fading

    Box(modifier) {
        // The layer spans the whole window, so what the content draws through the system bars fades with it.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown.value }
                .then(if (away) Modifier.clearAndSetSemantics {} else Modifier),
        ) { content() }
        if (away) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null, onClickLabel = "Show launcher", onClick = onDone)
                    .semantics { contentDescription = "Wallpaper" }
                    .testTag(LauncherTags.WALLPAPER),
            )
        }
    }
}
