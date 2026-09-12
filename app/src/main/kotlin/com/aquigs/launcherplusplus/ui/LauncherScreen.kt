package com.aquigs.launcherplusplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

/**
 * The whole launcher: one horizontal pager over [layout]. Each element of [homeRequests] (a HOME press while the
 * launcher is in front) and Back on a side page scroll to the home page.
 */
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homeRequests: Flow<Unit>,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
) {
    val scope = rememberCoroutineScope()
    // Each scroll gets its own job: a drag in progress cancels the animation, and that must not stop the collector.
    fun goHome() = scope.launch { pagerState.animateScrollToPage(layout.homeIndex) }

    LaunchedEffect(homeRequests, pagerState, layout) { homeRequests.collect { goHome() } }
    BackHandler(enabled = pagerState.currentPage != layout.homeIndex) { goHome() }

    HorizontalPager(
        state = pagerState,
        key = { layout.pages[it].name },
        modifier = modifier.fillMaxSize().testTag(LauncherTags.PAGER),
    ) { index ->
        val page = layout.pages[index]
        Box(Modifier.fillMaxSize().testTag(LauncherTags.page(page))) {
            when (page) {
                LauncherPage.Home -> AppList(apps = apps, onLaunch = onLaunch)
                LauncherPage.Widgets, LauncherPage.Collections -> PlaceholderPage(page)
            }
        }
    }
}

@Composable
private fun PlaceholderPage(page: LauncherPage) {
    Text(
        text = page.name,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize().wrapContentSize(),
    )
}
