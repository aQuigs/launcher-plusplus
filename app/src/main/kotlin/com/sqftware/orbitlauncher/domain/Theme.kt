package com.sqftware.orbitlauncher.domain

/**
 * How the whole launcher looks, a launcher-wide setting: every surface a theme owns (CLAUDE.md lists them) drawn its way,
 * with its own [folderLooks] to pick from, the first until the user picks.
 */
enum class Theme(val label: String, val folderLooks: List<FolderLook>) {
    Space("Space", listOf(FolderLook.Rim, FolderLook.Ringed, FolderLook.Orbit, FolderLook.Plain, FolderLook.SolarSystem)),
    Clockwork("Clockwork", listOf(FolderLook.SubDial, FolderLook.Gear)),
    Atlas("Atlas", listOf(FolderLook.Island, FolderLook.Globe, FolderLook.FoldedMap)),
    ZenGarden("Zen garden", listOf(FolderLook.Stone, FolderLook.KoiPond, FolderLook.Moss)),
    Ink("Ink", listOf(FolderLook.Enso, FolderLook.Seal, FolderLook.InkDrop)),
    Germ("Germ", listOf(FolderLook.Bacillus, FolderLook.Virus, FolderLook.Cell)),
    Paper("Paper", listOf(FolderLook.FoldedSquare, FolderLook.Envelope, FolderLook.Pinwheel)),
}

/**
 * The folder look picked in each theme, kept apart so coming back to a theme finds its own pick. A theme with none
 * picked, or with a pick it does not offer, shows its first.
 */
data class FolderLooks(private val picks: Map<Theme, FolderLook> = emptyMap()) {
    fun of(theme: Theme): FolderLook = picked(theme) ?: theme.folderLooks.first()

    /** The look picked in [theme], if one of its own was. */
    fun picked(theme: Theme): FolderLook? = picks[theme]?.takeIf { it in theme.folderLooks }

    fun with(theme: Theme, look: FolderLook): FolderLooks = copy(picks = picks + (theme to look))
}
