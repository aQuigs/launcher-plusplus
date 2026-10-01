package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// Under the microscope: a lime stain and a virus's pink on a dark culture by night, and the same on a lit slide by day.
private val Culture = Color(0xFF0F1F1C)
private val Stain = Color(0xFFB5F25A)
private val Slide = Color(0xFFE6F5E0)
private val Pink = Color(0xFFE8609A)

internal val GermPalette = Palette(
    deep = Culture,
    accent = Stain,
    pale = Slide,
    spark = Color(0xFFF4FFE8),
    warm = Pink,
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFCFE3D6),
)

/** A germ's membrane, which like a planet's edge has to show on any patch of the wallpaper: lime, outlined in the culture. */
val Membrane = DiscEdge(outer = Culture.copy(alpha = 0.7f), inner = Stain)

/** A bacillus's body, and the cytoplasm that fills a cell. */
val GermBody = Color(0xFF12302A).copy(alpha = 0.92f)
val Cytoplasm = Stain.copy(alpha = 0.16f)

/** A virus, the light catching it, and the knobs on its spikes. */
val Virus = Pink
val VirusLight = Color(0xFFF08DB8)

/** A cell's nucleus and the specks round it. */
val Nucleus = Color(0xFF9B87D9).copy(alpha = 0.8f)
val Organelle = Pink.copy(alpha = 0.7f)

/** The cocci scattered in the petri dish round the ring, in the virus's pink; its rods are in the ring's own colours. */
val DishCoccus = Pink.copy(alpha = 0.6f)
