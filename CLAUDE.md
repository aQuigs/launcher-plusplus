# launcher-plusplus

Custom Android home screen (launcher). Native Kotlin + Jetpack Compose, built and tested entirely from the CLI. The shared rules in `.claude/rules/` apply; this file holds what is specific to this repo.

## Stack

- Single `app` module. Every version number lives in `gradle/libs.versions.toml` or the Gradle wrapper; keep them on current stable releases
- Tests: JUnit 4 unit tests on the JVM, Compose UI tests on the emulator

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (pre-commit runs them too)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug                  # Android lint → app/build/reports/lint-results-debug.html (pre-commit runs it too)
scripts/emulator-lock.sh <command>   # device work: boots the emulator under the shared lock, runs <command>, stops it
scripts/emulator-lock.sh ./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the emulator
scripts/emulator.sh [stop]           # boot (or stop) the AVD from the installed Play Store image by hand (headless unless WINDOW=1, IMAGE_TAG=google_apis for adb root)
scripts/run.sh                       # install debug build, make it the home app, go home (VARIANT=Release for the minified build)
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scripts/record.sh [name] [seconds]   # adb screenrecord → screenshots/<name>.mp4 (gitignored)
scripts/pr-media.sh <file> <caption>... # upload shots as GitHub attachments, print the PR body's media table
scrcpy                               # mirror the emulator interactively
```

## Layout

```text
app/src/main/kotlin/com/sqftware/orbitlauncher/
├── MainActivity.kt      # composition root: wires the repository into the UI
├── domain/              # pure Kotlin: types and logic, no Android imports
├── apps/                # Android system adapters (LauncherApps, …) behind interfaces
└── ui/                  # Compose: screens, components, theme
app/src/test/            # JVM unit tests (domain)
app/src/androidTest/     # Compose UI tests and the activity smoke test (emulator)
scripts/                 # emulator, run, screenshot helpers (zsh)
```

Dependencies flow down only: `ui → domain ← apps`, and `MainActivity` is the only place that wires them together. `domain` never imports `android.*`; `ui` reaches the system only through the interfaces in `apps`.

`MainActivity` turns system callbacks (the HOME key, an app's request to pin a shortcut, which the window-less `PinShortcutActivity` hands on so the system's cleared task never clears home's) into one-shot events on flows it owns and knows nothing about pages; a one-shot event is never modeled as state the composition could replay. `LauncherScreen` owns all screen state and decides what an event means. Each page is a `LauncherPage` with an id and a `PageKind`, ordered by `PageLayout` in `domain` and dispatched by an exhaustive `when` over its kind, so a new kind does not compile until it has content. What a page holds is stored by its id, so two pages of a kind keep their own, and `MainActivity` keeps every store to the layout's pages, so a deleted page's data goes with it; ids are never reused. The user edits the layout in the page editor; `LauncherPage.Home` is the ring page with the clock and the dock, which every layout keeps; the launcher starts, and HOME and Back return, on the layout's `home`, that page unless the page editor made another home. Only the ring page, the collections page and the grid page in view take part in menus, drags and open folders; the others are drawn as they are. A grid page keeps its apps and folders as launcher blocks among its widget cells, so apps, widgets and the clock share one layout. Anything dismissable (the app drawer, an open folder, an app's menu) hoists its state to `LauncherScreen`, which has the one `BackHandler` and spells out the order: close what is open, then return home. A popup such as an app's menu is its own focusable window and takes Back before the activity, so it needs no handler. Do not add a second `BackHandler` whose priority depends on composition order. The launcher's own menu is such a popup, opened by a long press on the `EmptySpace` of the ring or grid page in view, which hosts it and is composed behind the page's content so hit testing hands it only the touches nothing else claims; a launcher-wide setting is a row of that menu. A full-screen view over the launcher is a `Panel`, so its veil reaches through the navigation bar as the drawer's does; only a game screen, like Old School's world map, takes the whole window instead, the system bars hidden while it is up. The drawer and every `Panel` draw their ground (the veil and the theme's marks) from the one `PanelGround`, never a color of their own, and every dialog and menu is a `GroundDialog` or `GroundMenu` on the same ground made opaque, so a theme reaches all of them alike; the `panel-ground-only` hook holds it. Data the user keeps, like the apps on the home ring and in the dock, comes through an adapter in `apps` and is loaded and saved in `MainActivity`, the same way as the app list. Trails the user logs are the exception: a foreground service records them while the launcher is out of sight, so the trail recorder's process-wide book loads and saves them and `MainActivity` only collects its flow. It lives in SharedPreferences, and the menu's reset clears every preferences file rather than a list of stores, so a new store needs nothing more to be reset. A shortcut pinned there is an `AppEntry` with a shortcut id, so the ring, the dock and folders take it like an app, while the drawer, search and collections list installed apps only; a pin no ring page or the dock holds any longer is unpinned. `LauncherScreen` holds only what the screen is doing (which page, whether the drawer is open, picking or searching, which folder is open, which app's menu is open). State the system provides, like the time on the home clock, follows the same path: an adapter in `apps` exposes it as a flow, `MainActivity` collects it while the launcher is visible, and `LauncherScreen` gets the current value; only one-shot events cross as flows.

## How we work

- Arc Launcher (`apptech.arc`, sideloaded on the project emulator) is the reference. Where it has a feature, mimic how it behaves and how it is laid out, in our own colors. Unsure how Arc does something? Open it on the emulator and look, do not guess. Where Arc has no such feature, use your judgment or ask.
- Pure logic goes in `domain` with a unit test. `MainActivityTest` is the one end-to-end smoke test against the real system.

## Conventions

- The look follows the wallpaper: a light one gets the day scheme and day ring colors. Colors come from the scheme's roles or `LocalRingColors`, never from an assumption that the launcher is dark.
- What a thing sits on is its `Ground` (`LocalGround`): the wallpaper, a panel (the drawer, every `Panel`) or a pop-up (every dialog and menu, and a `FloatingPane`). Boxes, choices and buttons take their look from it and never pick one: every box is a `Pane`, filled from `BoxColors` for its ground, lit when selected and outlined when marked; a floating bar or switch is a `FloatingPane`; a mark over other content (a drag ghost, the trash can, edit handles, a badge) takes `BoxColors.floating`; a choice is a `ChoiceRow` or `ChoiceChips`; a text button is a `LauncherButton`, with an `Emphasis` for a main or destructive action. Corners come from `MaterialTheme.shapes`. The `launcher-surfaces` and `launcher-buttons` hooks and `LauncherColorsTest` hold it.
- Each kind of thing is built one way, by the shared piece for it, so a fix to the kind reaches every place it is (the `launcher-pop-up-kinds` and `sideways-scroll` hooks hold the first and last):
  - A dialog is an `InfoDialog` (shows or sets things as they change, closed by Done), a `ConfirmDialog` (its verb and Cancel, the verb red when it destroys), a `NameDialog` or a `ChoiceDialog` (closed by Cancel).
  - A menu is a `GroundMenu` of groups of `MenuRow`s: a menu of things to do gives every row an icon, a menu of settings none, and a destructive row is red.
  - A full-screen view is a `Panel`, which carries its title and a close button. Other text outside boxes and buttons is a `SectionLabel`, `SupportingText` or an `EmptyNote`; spaces come from `Spacing`.
  - Text on the wallpaper, which can be light in one patch and dark in the next, takes its style through `legible()`, which gives it a faint halo opposite in color to the text; no text has a shadow of its own (the `legible-text` hook).
  - A note that something happened, or could not, is a short system toast through the `Toasts` adapter.
  - What scrolls fades at an edge with more beyond it (`fadingEdges`); a row that scrolls sideways is a `sidewaysScroll`, whose `bleed` is what rises out of it.
  - A glyph means one thing everywhere: the cross, the trash can and the chevron as `Glyphs.kt` says, and a choice made is a `PickedMark`.
  - UI words are in sentence case; names (of apps, collections, themes) keep their own.
- Colors live in `ui/theme`: components take a `colorScheme` role or a theme token, never a literal or a surface alpha copy, and a look changes by changing its role there (held by the `theme-colours-only` hook and `LauncherColorsTest`).

## Themes

The launcher's look is a theme, picked in the launcher's menu: Space (the ring's constellation, the drawer's stars, the planet folder looks), Clockwork (a watch dial telling the time, a chapter ring, sub-dial and gear folders), Atlas (a compass, a scale of degrees, island, globe and folded-map folders, a topographic drawer), Zen garden (a stone in spreading ripples, a raked ring, stone, koi pond and moss folders, a raked drawer), Ink (a red seal, an ensō brushed around the ring, ensō, seal and ink-drop folders, an ink-wash landscape in the drawer), Germ (a dividing bacillus, a petri dish around the ring, bacillus, virus and cell folders, a microscope slide in the drawer), Paper (a paper crane, washi tape around the ring, folded-square, envelope and pinwheel folders, creased paper in the drawer), Crystal city (glass cubes, a thread of light around the ring, cube, octahedron and geode folders, wireframe cubes in the drawer, and a city seen from above that the menu can set as the wallpaper) Green core (a wire sphere, a wireframe tube around the ring, wire sphere, orb and hex cell folders, waves in the drawer, and a reactor scene for the wallpaper) or Old School (the old game's interface from its own sprites: the minimap in its frame as the emblem, apps as the game's items in its round orbs, a folder as its previews in an orb, opening around the minimap, hitsplats as badges, the dock on its tab stones, the drawer as the stone side panel with the chatbox's search line, collections as the bank, the Choose Option menu, its fonts, its quill clock and, from the globe on the minimap's frame, its world map over the whole screen) or Skyrim (the northern game's interface drawn by hand: its grey local map with the player's arrow as the emblem, its compass along the top of the home page between carved ends, marking the map's places and the user's quest in gold, which a tap opens its world map from, where a tap marks the quest, a place's name as the user first comes to it, snow and mist drifting over the home screen and the drawer, shield folders, menus between hairlines and condensed type). A theme that `keepsJournal`, Skyrim, keeps a `Journal` (`domain/minimap`, saved by `JournalStore` in `apps`): the quest the launcher menu's "Mark quest here" set, which the art reads as `LocalQuest`, and the places discovered, each the nearest settlement in the map's tiles. `LauncherScreen` decides when a place is discovered, only while the home page is in sight, and hands its name to the theme's header, whose banner is the theme's art rather than a note, so it is not a toast. A theme with a `mapStyle`, Old School or Skyrim, shows where the user is unless they turn "Show where I am" off in the launcher's menu: picking the theme asks for their leave, and without it the map turns off and the emblem offers it instead. `MainActivity` paints a minimap of where they are in the theme's style from OpenStreetMap vector tiles (the ground laid out alike for every style, colored by the style) (`domain/minimap`, fetched and cached by `OpenFreeMapTiles` in `apps`) and provides it as `LocalMinimap`, which the emblem draws with the map data's credit for a few seconds; the menu links to the data's license for good. Without where they are, it walks the player through hand-laid made-up places (`MadeUpPlaces` in `domain/minimap`) and provides that map as `LocalMadeUpMap`, which the emblem draws in its place with no credit and no world map orb. A theme with a scene of its own gives it as its `ThemeArt`'s `scene`. A theme is an entry of the `Theme` enum in `domain`, listing its own folder looks and colorways; a `Palette` per colorway in `ui/theme/<Theme>Colors.kt`, from which both schemes and the ring's colors are drawn; and a `ThemeArt` in `ui/<Theme>Art.kt` for the shapes that are its own (the emblem, the ring's marks, what a folder wears, the marks on the panel ground), and, where a theme reshapes a surface, what an icon sits on and the shape it keeps, how its image is drawn, the badge, the clock's time, a header over the home clock, the type, the dock's slots and ground, the drawer's handle and whether it peeks on the home page, the weather over the pages and the drawer, the corners of every surface, the panel ground's veil, the search field, choice buttons, a collection card and its header, a menu's box and a dialog's, each defaulting to the look the other themes share. A theme draws apps' icons in its `IconStyle`: as the app made them, or, for Old School, turned into the game's items by `itemSprite` in `domain`. `MainActivity` hands the theme and its colorway to `Themed`, which provides its colors and its art, and the theme to `LauncherScreen`. A theme owns every surface below, in both the day and night schemes:

- Home ring: its track and marks, the center emblem, the home clock with the theme's header over it (Skyrim's compass), and the theme preview bar in their place, with its theme, folder look and colorway chips
- Folders: each folder look closed on the ring and in the dock, and open in the ring's center
- Dock and the drawer handle
- Weather: over the wallpaper behind the pages and over the drawer's ground, one clock driving both, so a flake is in the same place in each
- Panel ground: the veil and the theme's marks under the drawer, every `Panel` such as Select Collection, and, made opaque, every dialog and menu
- App drawer: search, the list and grid
- Collections page: the cards, their header controls, the app picker
- Grid page: its apps, its folders in each folder look, and an open folder's dialog
- Widget page and the controls on every page (`LauncherButton`, `Pane`, badges, the page dots)
- Popups and panels: the launcher menu, an app's menu, the pin dialog, the place picker, the page editor and its dialogs, the dialog that sets a theme's scene as the wallpaper, and the world map of a theme with one (its `WorldMapArt`: the ground, the user's mark, the trails the user logged, what lies over the map, its buttons and its close)

An icon's square and a folder's previews are laid out by the shared `IconDisc` and `FolderFace`, never a theme: a theme draws under or over them on layers of their own, so a folder's previews are the same size and in the same place in every theme (held by `ThemeSizesTest` and the `folder-previews-shared` hook). Within its square, an icon's image is the theme's to draw: Old School draws an item at the game's own pixel size. A feature that adds one of these surfaces, or changes how one looks, draws it from `ui/theme`, or from `LocalThemeArt` where its shape differs by theme, so each theme can give it its own look, and checks it under every theme. A new surface goes on this list.

## Don't

- Put system calls in `ui`.
