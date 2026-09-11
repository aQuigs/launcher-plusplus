package com.aquigs.launcherplusplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.aquigs.launcherplusplus.apps.LauncherAppsRepository
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.ui.AppList
import com.aquigs.launcherplusplus.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = LauncherAppsRepository(this)

        setContent {
            LauncherTheme {
                val apps by produceState(emptyList<AppEntry>()) {
                    value = withContext(Dispatchers.IO) { repository.installedApps() }
                }
                AppList(apps = apps, onLaunch = repository::launch, modifier = Modifier.safeDrawingPadding())
            }
        }
    }
}
