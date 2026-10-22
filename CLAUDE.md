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

`MainActivity` turns system callbacks (the HOME key, an app's request to pin a shortcut, which the window-less `PinShortcutActivity` hands on so the system's cleared task never clears home's) into one-shot events on flows it owns and knows nothing about pages; a one-shot event is never modelled as state the composition could replay. `LauncherScreen` owns all screen state and decides what an event means. Each page is a `LauncherPage` with an id and a `PageKind`, ordered by `PageLayout` in `domain` and dispatched by an exhaustive `when` over its kind, so a new kind does not compile until it has content. What a page holds is stored by its id, so two pages of a kind keep their own, and `MainActivity` keeps every store to the layout's pages, so a deleted page's data goes with it; ids are never reused. The user edits the layout in the page editor; `LauncherPage.Home` is the ring page with the clock and the dock, which every layout keeps; the launcher starts, and HOME and Back return, on the layout's `home`, that page unless the page editor made another home. Only the ring page, the collections page and the grid page in view take part in menus, drags and open folders; the others are drawn as they are. A grid page keeps its apps and folders as launcher blocks among its widget cells, so apps, widgets and the clock share one layout. Anything dismissable (the app drawer, an open folder, an app's menu) hoists its state to `LauncherScreen`, which has the one `BackHandler` and spells out the order: close what is open, then return home. A popup such as an app's menu is its own focusable window and takes Back before the activity, so it needs no handler. Do not add a second `BackHandler` whose priority depends on composition order. The launcher's own menu is such a popup, opened by a long press on the `EmptySpace` of the ring or grid page in view, which hosts it and is composed behind the page's content so hit testing hands it only the touches nothing else claims; a launcher-wide setting is a row of that menu. A full-screen view over the launcher is a `Panel`, so its veil reaches through the navigation bar as the drawer's does. The drawer and every `Panel` draw their ground (the veil and the theme's marks) from the one `PanelGround`, never a colour of their own, and every dialog and menu is a `GroundDialog` or `GroundMenu` on the same ground made opaque, so a theme reaches all of them alike; the `panel-ground-only` hook holds it. Data the user keeps, like the apps on the home ring and in the dock, comes through an adapter in `apps` and is loaded and saved in `MainActivity`, the same way as the app list. It lives in SharedPreferences, and the menu's reset clears every preferences file rather than a list of stores, so a new store needs nothing more to be reset. A shortcut pinned there is an `AppEntry` with a shortcut id, so the ring, the dock and folders take it like an app, while the drawer, search and collections list installed apps only; a pin no ring page or the dock holds any longer is unpinned. `LauncherScreen` holds only what the screen is doing (which page, whether the drawer is open, picking or searching, which folder is open, which app's menu is open). State the system provides, like the time on the home clock, follows the same path: an adapter in `apps` exposes it as a flow, `MainActivity` collects it while the launcher is visible, and `LauncherScreen` gets the current value; only one-shot events cross as flows.

## How we work

- Arc Launcher (`apptech.arc`, sideloaded on the project emulator) is the reference. Where it has a feature, mimic how it behaves and how it is laid out, in our own colours. Unsure how Arc does something? Open it on the emulator and look, do not guess. Where Arc has no such feature, use your judgement or ask.
- Pure logic goes in `domain` with a unit test. `MainActivityTest` is the one end-to-end smoke test against the real system.

## Conventions

- The look follows the wallpaper: a light one gets the day scheme and day ring colours. Colours come from the scheme's roles or `LocalRingColors`, never from an assumption that the launcher is dark.
- What a thing sits on is its `Ground` (`LocalGround`): the wallpaper, a panel (the drawer, every `Panel`) or a pop-up (every dialog and menu, and a `FloatingPane`). Boxes, choices and buttons take their look from it and never pick one: every box is a `Pane`, filled from `BoxColors` for its ground, lit when selected and outlined when marked; a floating bar or switch is a `FloatingPane`; a mark over other content (a drag ghost, the bin, edit handles, a badge) takes `BoxColors.floating`; a choice is a `ChoiceRow` or `ChoiceChips`; a text button is a `LauncherButton`, with an `Emphasis` for a main or destructive action. Corners come from `MaterialTheme.shapes`. The `launcher-surfaces` and `launcher-buttons` hooks and `LauncherColorsTest` hold it.
- Each kind of thing is built one way, by the shared piece for it, so a fix to the kind reaches every place it is (the `launcher-pop-up-kinds` and `sideways-scroll` hooks hold the first and last):
  - A dialog is an `InfoDialog` (shows or sets things as they change, closed by Done), a `ConfirmDialog` (its verb and Cancel, the verb red when it destroys), a `NameDialog` or a `ChoiceDialog` (closed by Cancel).
  - A menu is a `GroundMenu` of groups of `MenuRow`s: a menu of things to do gives every row an icon, a menu of settings none, and a destructive row is red.
  - A full-screen view is a `Panel`, which carries its title and a close button. Other text outside boxes and buttons is a `SectionLabel`, `SupportingText` or an `EmptyNote`; spaces come from `Spacing`.
  - Text on the wallpaper, which can be light in one patch and dark in the next, takes its style through `legible()`, which gives it a faint halo opposite in colour to the text; no text has a shadow of its own (the `legible-text` hook).
  - A note that something happened, or could not, is a notice through `LocalNotices`, never a system toast.
  - What scrolls fades at an edge with more beyond it (`fadingEdges`); a row that scrolls sideways is a `sidewaysScroll`, whose `bleed` is what rises out of it.
  - A glyph means one thing everywhere: the cross, the bin and the chevron as `Glyphs.kt` says, and a choice made is a `PickedMark`.
  - UI words are in sentence case; names (of apps, collections, themes) keep their own.
- Colours live in `ui/theme`: components take a `colorScheme` role or a theme token, never a literal or a surface alpha copy, and a look changes by changing its role there (held by the `theme-colours-only` hook and `LauncherColorsTest`).

## Themes

The launcher's look is a theme, picked in the launcher's menu: Space (the ring's constellation, the drawer's stars, the planet folder looks), Clockwork (a watch dial telling the time, a chapter ring, sub-dial and gear folders), Atlas (a compass, a scale of degrees, island, globe and folded-map folders, a topographic drawer), Zen garden (a stone in spreading ripples, a raked ring, stone, koi pond and moss folders, a raked drawer), Ink (a red seal, an ensō brushed round the ring, ensō, seal and ink-drop folders, an ink-wash landscape in the drawer), Germ (a dividing bacillus, a petri dish round the ring, bacillus, virus and cell folders, a microscope slide in the drawer), Paper (a paper crane, washi tape round the ring, folded-square, envelope and pinwheel folders, creased paper in the drawer), Crystal city (glass cubes, a thread of light round the ring, cube, octahedron and geode folders, wireframe cubes in the drawer, and a city seen from above that the menu can set as the wallpaper) Green core (a wire sphere, a wireframe tube round the ring, wire sphere, orb and hex cell folders, waves in the drawer, and a reactor scene for the wallpaper) or Old School (the old game's interface from its own sprites: the minimap in its frame as the emblem, apps as the game's items in its round orbs, a pile of two items as a folder, stack counts as badges, the dock on its tab stones, the inventory tab as the drawer's handle off the home page, the drawer as the stone side panel with the chatbox's search line, collections as the bank, the Choose Option menu, its fonts and its quill clock). A theme that `mapsWhereIAm`, Old School alone so far, offers "Show where I am" in the launcher's menu: with the user's leave, `MainActivity` paints a game-style minimap of where they are from OpenStreetMap vector tiles (`domain/minimap`, fetched and cached by `OpenFreeMapTiles` in `apps`) and provides it as `LocalMinimap`, which the emblem draws with the map data's credit. A theme with a scene of its own gives it as its `ThemeArt`'s `scene`. A theme is an entry of the `Theme` enum in `domain`, listing its own folder looks and colourways; a `Palette` per colourway in `ui/theme/<Theme>Colors.kt`, from which both schemes and the ring's colours are drawn; and a `ThemeArt` in `ui/<Theme>Art.kt` for the shapes that are its own (the emblem, the ring's marks, folders, the marks on the panel ground), and, where a theme reshapes a surface, what an icon sits on and how its image is drawn, the badge, the clock's time, the type, the dock's slots and ground, the drawer's handle and whether it peeks on the home page, the corners of every surface, the panel ground's veil, the search field, choice buttons, a collection card and its header, a menu's box and a dialog's, each defaulting to the look the other themes share. A theme draws apps' icons in its `IconStyle`: as the app made them, or, for Old School, turned into the game's items by `itemSprite` in `domain`. `MainActivity` hands the theme and its colourway to `Themed`, which provides its colours and its art, and the theme to `LauncherScreen`. A theme owns every surface below, in both the day and night schemes:

- Home ring: its track and marks, the centre emblem, the home clock and the theme preview bar in its place, with its theme, folder look and colourway chips
- Folders: each folder look closed on the ring and in the dock, and open in the ring's centre
- Dock and the drawer handle
- Panel ground: the veil and the theme's marks under the drawer, every `Panel` such as Select Collection, and, made opaque, every dialog and menu
- App drawer: search, the list and grid
- Collections page: the cards, their header controls, the app picker
- Grid page: its apps, its folders in each folder look, and an open folder's dialog
- Widget page and the controls on every page (`LauncherButton`, `Pane`, badges, the page dots)
- Popups and panels: the launcher menu, an app's menu, the pin dialog, the place picker, the page editor and its dialogs, the dialog that sets a theme's scene as the wallpaper

A feature that adds one of these surfaces, or changes how one looks, draws it from `ui/theme`, or from `LocalThemeArt` where its shape differs by theme, so each theme can give it its own look, and checks it under every theme. A new surface goes on this list.

## Don't

- Put system calls in `ui`.
