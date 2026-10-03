package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.PageLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

const val PAGE_DOTS = "page_dots"

/** How long the dots stay once the pages stop moving. */
private const val LINGER_MILLIS = 800L

/**
 * A dot for each page of [layout], a small house for home, the one [pager] is on brighter than the rest. They show only
 * while the pages move, and a little after.
 */
@Composable
fun PageDots(layout: PageLayout, pager: PagerState, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }.collectLatest { moving ->
            if (!moving) delay(LINGER_MILLIS)
            shown = moving
        }
    }
    val colours = MaterialTheme.colorScheme

    AnimatedVisibility(visible = shown, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(PAGE_DOTS),
        ) {
            layout.pages.indices.forEach { page ->
                val colour = if (page == pager.currentPage) colours.onSurface else colours.outline
                if (page == layout.homeIndex) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = colour, modifier = Modifier.size(10.dp))
                } else {
                    Box(Modifier.size(5.dp).background(colour, CircleShape))
                }
            }
        }
    }
}
