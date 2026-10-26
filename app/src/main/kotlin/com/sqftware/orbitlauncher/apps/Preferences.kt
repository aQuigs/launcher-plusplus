package com.sqftware.orbitlauncher.apps

import android.content.SharedPreferences

// Preferences hold no doubles, and a float rounds off what a double keeps: a spin's fraction of a turn in a growing
// total, a spot on Earth to the meter.
internal fun SharedPreferences.getDouble(key: String) = Double.fromBits(getLong(key, 0.0.toRawBits()))

internal fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor = putLong(key, value.toRawBits())
