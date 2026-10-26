package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.sqftware.orbitlauncher.domain.Vital
import com.sqftware.orbitlauncher.domain.Vitals

/**
 * The phone's [Vitals] for a theme that shows them by its emblem, read where they are drawn as they change while the
 * launcher is visible; [onOpen] opens what a touch on one opens, and [onSpinStats] the spin stats while the ring spins,
 * null while it does not.
 */
class VitalsShown(val read: () -> Vitals, val onOpen: (Vital) -> Unit, val onSpinStats: (() -> Unit)?)

/** What the emblem shows of the phone, or null where the theme shows nothing of it. */
val LocalVitals = staticCompositionLocalOf<VitalsShown?> { null }
