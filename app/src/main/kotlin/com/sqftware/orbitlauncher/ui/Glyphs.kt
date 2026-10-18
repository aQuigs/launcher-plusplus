package com.sqftware.orbitlauncher.ui

import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import com.sqftware.orbitlauncher.ui.theme.GlyphFill

// A glyph means one thing wherever it shows: a cross takes something off where it is (an app off the ring, a widget
// off its page, a page out of the layout), a bin uninstalls or is where a lifted app is dropped to take it off its card,
// and a chevron opens or closes what it sits on, never a menu. Material's core set lacks those below, drawn here from
// Material's own paths.

/** A glyph from Material's [pathData], filled as the core set's are so it takes its tint. */
internal fun materialGlyph(name: String, pathData: String): ImageVector =
    materialIcon(name) { addPath(addPathNodes(pathData), fill = SolidColor(GlyphFill)) }

/** The order of a list. */
internal val SortGlyph = materialGlyph("Sort", "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z")

/** A folder's own planet, of the folder looks. */
internal val PlanetGlyph = materialGlyph(
    "Public",
    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79" +
        "L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 " +
        "2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z",
)

/** Shown where it was hidden. */
internal val ShownGlyph = materialGlyph(
    "Visibility",
    "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5" +
        "s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
)

/** Hidden where it showed. */
internal val HiddenGlyph = materialGlyph(
    "VisibilityOff",
    "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25" +
        "-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 " +
        "1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 " +
        "3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15" +
        ".02-.16c0-1.66-1.34-3-3-3l-.17.01z",
)
