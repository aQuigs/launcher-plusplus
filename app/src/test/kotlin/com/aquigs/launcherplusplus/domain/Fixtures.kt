package com.aquigs.launcherplusplus.domain

internal fun app(label: String, packageName: String = "pkg.$label") = AppEntry(label, packageName, "$packageName.Main")

internal fun ringOf(vararg apps: AppEntry) = Ring(apps.map { RingSlot.App(it.key) })

internal fun folder(name: String, vararg apps: AppEntry) = RingSlot.Folder(name, apps.map { it.key })
