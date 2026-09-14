package com.aquigs.launcherplusplus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.domain.matching
import com.aquigs.launcherplusplus.domain.sectionsByInitial
import kotlinx.coroutines.launch

object AppDrawerTags {
    const val HANDLE = "drawer_handle"
    const val LIST = "app_list"
    const val SEARCH = "app_search"

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

/** How a picked row is marked: a check on a row a tap takes off again, or a dot on one a tap only ever adds. */
enum class PickMark { Check, Dot }

/**
 * Picking apps instead of launching them: the drawer shows [header] above the list, puts a [mark] on the rows [isPicked]
 * says, and a tap calls [onToggle].
 */
class Picking(
    val header: @Composable () -> Unit,
    val isPicked: (AppEntry) -> Boolean,
    val onToggle: (AppEntry) -> Unit,
    val mark: PickMark = PickMark.Check,
)

/**
 * Every app in sections headed by their initial, with a rail of those initials down the end edge to jump by. A tap
 * launches the app and a long press opens its [menu], unless the drawer is [picking]; a long press that moves on
 * becomes a [drag]. A search field heads the list: with a [query] the list holds only the matching apps, without
 * sections or rail, and the keyboard's search key acts on the first of them as a tap would. An app with [unread]
 * notifications shows their number at the end of its row, in full, since a row has the room a badge lacks.
 */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    picking: Picking? = null,
    listState: LazyListState = rememberLazyListState(),
    menu: AppMenu? = null,
    drag: AppDrag? = null,
    query: String,
    onQueryChange: (String) -> Unit,
    unread: UnreadCounts = UnreadCounts(),
) {
    val scope = rememberCoroutineScope()
    val rowMenu = menu.takeIf { picking == null }
    val rowDrag = drag.takeIf { picking == null }
    val searching = query.isNotBlank()
    val matches = remember(apps, query) { if (searching) apps.matching(query) else emptyList() }
    // The matches scroll on their own, so the sections come back where they were and a match does not become the top of
    // the sections' list because it was the first thing on screen when the search ended. Each new query starts at its top.
    val matchesState = rememberLazyListState()
    LaunchedEffect(matches) { matchesState.scrollToItem(0) }
    val sections = remember(apps) { apps.sectionsByInitial() }
    val initials = remember(sections) { sections.map { it.initial } }
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

    Column(modifier.fillMaxSize()) {
        picking?.header?.invoke()
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { matches.firstOrNull()?.let(picking?.onToggle ?: onLaunch) },
        )
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = if (searching) matchesState else listState,
                contentPadding = PaddingValues(end = if (searching) 0.dp else RAIL_WIDTH),
                modifier = Modifier.fillMaxSize().testTag(AppDrawerTags.LIST),
            ) {
                if (!searching) {
                    sections.forEach { section ->
                        item(key = section.initial, contentType = "header") { SectionHeader(section.initial) }
                        items(section.apps, key = { it.key }, contentType = { "app" }) { app ->
                            AppRow(app, onLaunch, picking, rowMenu, rowDrag, unread[app])
                        }
                    }
                } else if (matches.isEmpty()) {
                    item(contentType = "empty") { NoMatches() }
                } else {
                    items(matches, key = { it.key }, contentType = { "app" }) { app ->
                        AppRow(app, onLaunch, picking, rowMenu, rowDrag, unread[app])
                    }
                }
            }
            if (!searching) {
                LetterRail(
                    initials = initials,
                    highlighted = highlighted,
                    onSelect = { section ->
                        lastSelected = section
                        scope.launch { listState.scrollToItem(headerIndices[section]) }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search apps") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear the search") }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            // The placeholder goes once there is text, and a screen reader should still say what the field is for.
            .semantics { contentDescription = "Search apps" }
            .testTag(AppDrawerTags.SEARCH),
    )
}

@Composable
private fun NoMatches() {
    Text(
        text = "No apps match",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
    )
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
private fun AppRow(app: AppEntry, onLaunch: (AppEntry) -> Unit, picking: Picking?, menu: AppMenu?, drag: AppDrag?, unread: Int) {
    val picked = picking?.isPicked(app) == true
    val action = when {
        // The drag comes after the click handling, so it reads each touch first and can keep the moves to itself.
        picking == null -> Modifier.launchable(app, onLaunch, menu).appDrag(app, drag)
        picking.mark == PickMark.Check -> {
            Modifier.toggleable(value = picked, role = Role.Checkbox, onValueChange = { picking.onToggle(app) })
        }
        else -> Modifier.clickable { picking.onToggle(app) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(action)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(text = app.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (unread > 0) {
            // Read as part of the row: a description here would speak for the whole row and silence its name.
            Text(
                text = unread.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp).clearAndSetSemantics { text = AnnotatedString("$unread unread") },
            )
        }
        if (picked) {
            when (picking?.mark) {
                PickMark.Dot -> Dot()
                else -> Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        menu?.content?.invoke(app)
    }
}

/** Marks a row whose app was just added: read as part of the row, like the unread count. */
@Composable
private fun Dot() {
    Box(
        Modifier
            .padding(start = 12.dp)
            .size(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clearAndSetSemantics { text = AnnotatedString("added") },
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
