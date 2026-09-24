package com.sqftware.orbitlauncher.domain

internal fun app(label: String, packageName: String = "pkg.$label") = AppEntry(label, packageName, "$packageName.Main")

internal fun ringOf(vararg apps: AppEntry) = Ring(apps.map { RingSlot.App(it.key) })

internal fun folder(vararg apps: AppEntry) = RingSlot.Folder(apps.map { it.key })
