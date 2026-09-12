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
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.domain.sectionsByInitial
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

/**
 * The whole launcher: a horizontal pager over [layout] with the app drawer peeking below it as a chevron. Each element
 * of [homeRequests] (a HOME press while the launcher is in front) closes the drawer and scrolls to the home page; Back
 * closes the drawer if it is open, otherwise it returns to the home page.
 */
@ExperimentalMaterial3Api
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homeRequests: Flow<Unit>,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
    drawerState: SheetState = rememberStandardBottomSheetState(skipHiddenState = true),
) {
    val scope = rememberCoroutineScope()
    val drawerOpen = drawerState.currentValue == SheetValue.Expanded
    val sections = remember(apps) { apps.sectionsByInitial() }

    // Each scroll gets its own job: a drag in progress cancels the animation, and that must not stop the collector.
    fun closeDrawer() = scope.launch { drawerState.partialExpand() }
    fun goHome() = scope.launch { pagerState.animateScrollToPage(layout.homeIndex) }

    LaunchedEffect(homeRequests, pagerState, drawerState, layout) {
        homeRequests.collect {
            closeDrawer()
            goHome()
        }
    }
    // One handler with the order spelled out, instead of one per dismissable relying on composition order.
    BackHandler(enabled = drawerOpen || pagerState.currentPage != layout.homeIndex) {
        if (drawerOpen) closeDrawer() else goHome()
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(drawerState),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = { DrawerHandle(drawerState) },
        sheetContainerColor = drawerContainerColor,
        containerColor = Color.Transparent,
        sheetContent = { AppDrawer(sections = sections, onLaunch = onLaunch) },
        modifier = modifier,
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            key = { layout.pages[it].name },
            modifier = Modifier.fillMaxSize().padding(padding).testTag(LauncherTags.PAGER),
        ) { index ->
            val page = layout.pages[index]
            Box(Modifier.fillMaxSize().testTag(LauncherTags.page(page))) {
                when (page) {
                    // Empty until the home layout PR: the drawer handle below is its only control for now.
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
