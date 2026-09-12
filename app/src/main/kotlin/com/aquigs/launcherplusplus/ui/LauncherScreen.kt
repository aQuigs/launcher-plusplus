package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

/**
 * The whole launcher: one horizontal pager over [layout]. Every bump of [homeRequests] scrolls back to the home page,
 * which is how the HOME key reaches the UI without the activity knowing about pages.
 */
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homeRequests: Int,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size }

    LaunchedEffect(homeRequests) { pagerState.animateScrollToPage(layout.homeIndex) }

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
