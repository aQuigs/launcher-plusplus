package com.aquigs.launcherplusplus.apps

import com.aquigs.launcherplusplus.domain.AppEntry

interface AppRepository {
    fun installedApps(): List<AppEntry>

    fun launch(app: AppEntry)
}
