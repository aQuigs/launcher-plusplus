package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em

/**
 * Material's type set wholly in [family], each group of roles spaced out by its own share of an em: the [display] roles,
 * the [heading] roles (headlines and titles), the [body] and the [label]s.
 */
internal fun typographyIn(family: FontFamily, display: Float, heading: Float, body: Float, label: Float): Typography {
    val type = Typography()
    fun TextStyle.spaced(spacing: Float) = copy(fontFamily = family, letterSpacing = spacing.em)
    return type.copy(
        displayLarge = type.displayLarge.spaced(display),
        displayMedium = type.displayMedium.spaced(display),
        displaySmall = type.displaySmall.spaced(display),
        headlineLarge = type.headlineLarge.spaced(heading),
        headlineMedium = type.headlineMedium.spaced(heading),
        headlineSmall = type.headlineSmall.spaced(heading),
        titleLarge = type.titleLarge.spaced(heading),
        titleMedium = type.titleMedium.spaced(heading),
        titleSmall = type.titleSmall.spaced(heading),
        bodyLarge = type.bodyLarge.spaced(body),
        bodyMedium = type.bodyMedium.spaced(body),
        bodySmall = type.bodySmall.spaced(body),
        labelLarge = type.labelLarge.spaced(label),
        labelMedium = type.labelMedium.spaced(label),
        labelSmall = type.labelSmall.spaced(label),
    )
}
