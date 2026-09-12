package com.aquigs.launcherplusplus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.sectionsByInitial
import kotlinx.coroutines.launch

object AppDrawerTags {
    const val HANDLE = "drawer_handle"
    const val LIST = "app_list"

    fun section(initial: Char) = "section_$initial"

    fun letter(initial: Char) = "rail_$initial"
}

/**
 * The chevron on the strip that peeks above the pages, pointing the way the drawer will move. It only draws: the sheet's
 * drag-handle slot already makes the strip one clickable control that toggles the drawer.
 */
@Composable
fun DrawerHandle(open: Boolean, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = if (open) "Close the app drawer" else "Open the app drawer",
        modifier = modifier
            .padding(vertical = 8.dp)
            .size(32.dp)
            .graphicsLayer { rotationZ = rotation }
            .testTag(AppDrawerTags.HANDLE),
    )
}

/** Every app in sections headed by their initial, with a rail of those initials down the end edge to jump by. */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val scope = rememberCoroutineScope()
    val sections = remember(apps) { apps.sectionsByInitial() }
    // Each section is one header item followed by its apps, so the rail's targets are the running item counts.
    val headerIndices = remember(sections) {
        sections.runningFold(0) { index, section -> index + 1 + section.apps.size }.dropLast(1)
    }
    var lastSelected by remember { mutableStateOf<Int?>(null) }
    // The chosen letter stays lit while its header is on screen: near the end of the list the scroll stops short, and
    // the section at the top is then an earlier one.
    val highlighted by remember(headerIndices, listState) {
        derivedStateOf {
            val chosenHeader = lastSelected?.let(headerIndices::getOrNull)
            lastSelected.takeIf { chosenHeader != null && listState.layoutInfo.visibleItemsInfo.any { it.index == chosenHeader } }
                ?: headerIndices.indexOfLast { it <= listState.firstVisibleItemIndex }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(end = RAIL_WIDTH),
            modifier = Modifier.fillMaxSize().testTag(AppDrawerTags.LIST),
        ) {
            sections.forEach { section ->
                item(key = section.initial, contentType = "header") { SectionHeader(section.initial) }
                items(section.apps, key = { "${it.packageName}/${it.activityName}" }, contentType = { "app" }) { app ->
                    AppRow(app, onLaunch)
                }
            }
        }
        LetterRail(
            initials = sections.map { it.initial },
            highlighted = highlighted,
            onSelect = { section ->
                lastSelected = section
                scope.launch { listState.scrollToItem(headerIndices[section]) }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private val RAIL_WIDTH = 28.dp

@Composable
private fun SectionHeader(initial: Char) {
    Text(
        text = initial.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag(AppDrawerTags.section(initial)),
    )
}

@Composable
private fun AppRow(app: AppEntry, onLaunch: (AppEntry) -> Unit) {
    Text(
        text = app.label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunch(app) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

/**
 * One touch target for the whole rail rather than a button per letter: a finger sliding down it should keep jumping,
 * and a tap between two letters should still land on one of them.
 */
@Composable
private fun LetterRail(initials: List<Char>, highlighted: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // The gesture loop outlives recompositions, so it reads the latest letters and targets through these.
    val latestInitials by rememberUpdatedState(initials)
    val latestOnSelect by rememberUpdatedState(onSelect)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(RAIL_WIDTH)
            .pointerInput(Unit) {
                awaitEachGesture {
                    var selected = -1
                    fun select(y: Float) {
                        val count = latestInitials.size
                        if (count == 0) return
                        val index = (y / size.height * count).toInt().coerceIn(0, count - 1)
                        if (index != selected) {
                            selected = index
                            latestOnSelect(index)
                        }
                    }

                    val down = awaitFirstDown()
                    select(down.position.y)
                    // Consumed so the sheet underneath does not read a slide down the rail as a drag to close.
                    drag(down.id) { change ->
                        change.consume()
                        select(change.position.y)
                    }
                }
            },
    ) {
        initials.forEachIndexed { index, initial ->
            val isHighlighted = index == highlighted
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .semantics { selected = isHighlighted }
                    .testTag(AppDrawerTags.letter(initial)),
            )
        }
    }
}
