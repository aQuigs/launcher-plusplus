package com.aquigs.launcherplusplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

/** A HOME press. [launcherInFront] is false when the press brought the launcher back from another app. */
data class HomePress(val launcherInFront: Boolean)

/**
 * The whole launcher: a horizontal pager over [layout] with the app drawer peeking below it as a chevron. Every
 * [HomePress] closes the drawer; one made while the launcher was in front also scrolls to the home page. Back closes the
 * drawer if it is open, otherwise it returns to the home page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homePresses: Flow<HomePress>,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberStandardBottomSheetState(skipHiddenState = true)
    val drawerOpen = drawerState.targetValue == SheetValue.Expanded

    // Each animation gets its own job: a drag in progress cancels it, and that must not stop the collector.
    fun closeDrawer() = scope.launch { drawerState.partialExpand() }
    fun goHome() = scope.launch { pagerState.animateScrollToPage(layout.homeIndex) }

    LaunchedEffect(homePresses, pagerState, drawerState, layout) {
        homePresses.collect { press ->
            closeDrawer()
            // Coming back from an app keeps the page you left, like the stock launcher.
            if (press.launcherInFront) goHome()
        }
    }
    // One handler with the order spelled out, instead of one per dismissable relying on composition order.
    BackHandler(enabled = drawerOpen || pagerState.currentPage != layout.homeIndex) {
        if (drawerOpen) closeDrawer() else goHome()
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(drawerState),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = { DrawerHandle(open = drawerOpen) },
        // Translucent so the wallpaper still shows through the open drawer.
        sheetContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        containerColor = Color.Transparent,
        sheetContent = { AppDrawer(apps = apps, onLaunch = onLaunch) },
        // The collapsed sheet is full height and continues below the scaffold, where the list would show through the
        // navigation-bar inset.
        modifier = modifier.clipToBounds(),
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            key = { layout.pages[it].name },
            modifier = Modifier.fillMaxSize().padding(padding).testTag(LauncherTags.PAGER),
        ) { index ->
            val page = layout.pages[index]
            Box(Modifier.fillMaxSize().testTag(LauncherTags.page(page))) {
                when (page) {
                    // Empty until the home layout lands: the drawer strip below is its only control for now.
                    LauncherPage.Home -> Unit
                    LauncherPage.Widgets, LauncherPage.Collections -> PlaceholderPage(page)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPage(page: LauncherPage) {
    Text(
        text = page.name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxSize().wrapContentSize(),
    )
}
