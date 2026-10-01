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

`MainActivity` turns system callbacks (the HOME key, an app's request to pin a shortcut, which the window-less `PinShortcutActivity` hands on so the system's cleared task never clears home's) into one-shot events on flows it owns and knows nothing about pages; a one-shot event is never modelled as state the composition could replay. `LauncherScreen` owns all screen state and decides what an event means. Pages are the `LauncherPage` enum, ordered by `PageLayout` in `domain` and dispatched by an exhaustive `when`, so a new page does not compile until it has content. Anything dismissable (the app drawer, an open folder, an app's menu) hoists its state to `LauncherScreen`, which has the one `BackHandler` and spells out the order: close what is open, then return home. A popup such as an app's menu is its own focusable window and takes Back before the activity, so it needs no handler. Do not add a second `BackHandler` whose priority depends on composition order. The launcher's own menu is such a popup, opened by a long press on the home page's `EmptySpace`, which is composed behind the page's content so hit testing hands it only the touches nothing else claims; a launcher-wide setting is a row of that menu. A full-screen view over the launcher is a `Panel`, so its veil reaches through the navigation bar as the drawer's does. Data the user keeps, like the apps on the home ring and in the dock, comes through an adapter in `apps` and is loaded and saved in `MainActivity`, the same way as the app list. It lives in SharedPreferences, and the menu's reset clears every preferences file rather than a list of stores, so a new store needs nothing more to be reset. A shortcut pinned there is an `AppEntry` with a shortcut id, so the ring, the dock and folders take it like an app, while the drawer, search and collections list installed apps only; a pin the home screen no longer holds is unpinned. `LauncherScreen` holds only what the screen is doing (which page, whether the drawer is open, picking or searching, which folder is open, which app's menu is open). State the system provides, like the time on the home clock, follows the same path: an adapter in `apps` exposes it as a flow, `MainActivity` collects it while the launcher is visible, and `LauncherScreen` gets the current value; only one-shot events cross as flows.

## How we work

- Arc Launcher (`apptech.arc`, sideloaded on the project emulator) is the reference. Where it has a feature, mimic how it behaves and how it is laid out, in our own colours. Unsure how Arc does something? Open it on the emulator and look, do not guess. Where Arc has no such feature, use your judgement or ask.
- Pure logic goes in `domain` with a unit test. `MainActivityTest` is the one end-to-end smoke test against the real system.

## Conventions

- The look follows the wallpaper: a light one gets the day scheme and day ring colours. Colours come from the scheme's roles or `LocalRingColors`, never from an assumption that the launcher is dark.
- Colours live in `ui/theme`: components take a `colorScheme` role or a theme token, never a literal or a surface alpha copy, and a look changes by changing its role there (held by the `theme-colours-only` hook and `LauncherColorsTest`).

## Themes

The launcher's look is a theme, picked in the launcher's menu: Space (the ring's constellation, the drawer's stars, the planet folder looks), Clockwork (a watch dial telling the time, a chapter ring, sub-dial and gear folders), Atlas (a compass, a scale of degrees, island, globe and folded-map folders, a topographic drawer), Zen garden (a stone in spreading ripples, a raked ring, stone, koi pond and moss folders, a raked drawer) or Ink (a red seal, an ensō brushed round the ring, ensō, seal and ink-drop folders, an ink-wash landscape in the drawer). A theme is an entry of the `Theme` enum in `domain`, listing its own folder looks; a `Palette` in `ui/theme/<Theme>Colors.kt`, from which both schemes and the ring's colours are drawn; and a `ThemeArt` in `ui/<Theme>Art.kt` for the shapes that are its own (the emblem, the ring's marks, folders, the drawer's backdrop). `MainActivity` hands the theme to `LauncherTheme` and `LauncherScreen`, which provides its art. A theme owns every surface below, in both the day and night schemes:

- Home ring: its track and marks, the centre emblem, the home clock and the theme preview bar in its place
- Folders: each folder look closed on the ring and in the dock, and open in the ring's centre
- Dock and the drawer handle
- App drawer: the veil and what is drawn on it, search, the list and grid
- Collections page: the cards, their header controls, the app picker
- Widget page and the controls on every page (`TonalButton`, badges)
- Popups and panels: the launcher menu, an app's menu, the pin dialog, the place picker

A feature that adds one of these surfaces, or changes how one looks, draws it from `ui/theme`, or from `LocalThemeArt` where its shape differs by theme, so each theme can give it its own look, and checks it under every theme. A new surface goes on this list.

## Don't

- Put system calls in `ui`.
