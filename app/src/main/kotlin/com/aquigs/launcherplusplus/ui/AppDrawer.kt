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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppSection
import kotlinx.coroutines.launch

object AppDrawerTags {
    const val HANDLE = "drawer_handle"
    const val LIST = "app_list"
    const val RAIL = "letter_rail"

    fun section(initial: Char) = "section_$initial"

    fun letter(initial: Char) = "rail_$initial"
}

/** The chevron that peeks above the home page: tap or drag it to open the drawer, and again to close it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerHandle(state: SheetState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val open = state.targetValue == SheetValue.Expanded
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = if (open) "Close the app drawer" else "Open the app drawer",
        modifier = modifier
            .clip(CircleShape)
            .clickable { scope.launch { if (open) state.partialExpand() else state.expand() } }
            .padding(vertical = 8.dp)
            .size(32.dp)
            .rotate(rotation)
            .testTag(AppDrawerTags.HANDLE),
    )
}

/** Every app in sections headed by their initial, with a rail of those initials down the end edge to jump by. */
@Composable
fun AppDrawer(
    sections: List<AppSection>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val scope = rememberCoroutineScope()
    // Each section is one header item followed by its apps, so the rail's targets are the running item counts.
    val headerIndices = remember(sections) { sections.runningFold(0) { index, section -> index + 1 + section.apps.size } }
    // Remembered so the rail's pointer handler, keyed on it, is not reset mid-slide by a recomposition.
    val initials = remember(sections) { sections.map { it.initial } }
    val currentSection by remember(headerIndices) {
        derivedStateOf { headerIndices.indexOfLast { it <= listState.firstVisibleItemIndex }.coerceAtMost(sections.lastIndex) }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(end = RAIL_WIDTH),
            modifier = Modifier.fillMaxSize().testTag(AppDrawerTags.LIST),
        ) {
            sections.forEach { section ->
                item(key = "section_${section.initial}") { SectionHeader(section.initial) }
                items(section.apps, key = { "${it.packageName}/${it.activityName}" }) { app -> AppRow(app, onLaunch) }
            }
        }
        LetterRail(
            initials = initials,
            current = currentSection,
            onSelect = { scope.launch { listState.scrollToItem(headerIndices[it]) } },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private val RAIL_WIDTH = 28.dp

/** Translucent so the wallpaper still shows through the open drawer. */
val drawerContainerColor: Color
    @Composable get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)

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
private fun LetterRail(initials: List<Char>, current: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // While a finger is on the rail its letter is the one to show, whatever the list managed to scroll to.
    var pressed by remember { mutableStateOf<Int?>(null) }
    val highlighted = pressed ?: current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(RAIL_WIDTH)
            .pointerInput(initials) {
                fun select(y: Float) {
                    if (initials.isEmpty()) return
                    val index = (y / size.height * initials.size).toInt().coerceIn(0, initials.lastIndex)
                    if (index != pressed) {
                        pressed = index
                        onSelect(index)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    select(down.position.y)
                    // Consumed so the sheet underneath does not read a slide down the rail as a drag to close.
                    drag(down.id) { change ->
                        change.consume()
                        select(change.position.y)
                    }
                    pressed = null
                }
            }
            .testTag(AppDrawerTags.RAIL),
    ) {
        initials.forEachIndexed { index, initial ->
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (index == highlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (index == highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).wrapContentHeight().testTag(AppDrawerTags.letter(initial)),
            )
        }
    }
}
