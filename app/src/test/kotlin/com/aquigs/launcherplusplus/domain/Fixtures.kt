package com.aquigs.launcherplusplus.domain

internal fun app(label: String, packageName: String = "pkg.$label") = AppEntry(label, packageName, "$packageName.Main")
