# launcher-plusplus

Custom Android home screen (launcher). Native Kotlin + Jetpack Compose, built and tested entirely from the CLI.

## Stack

- Kotlin + Jetpack Compose (Material 3), single `app` module
- Android Gradle Plugin 9 with built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android` to the module, only the Compose compiler plugin
- Every version number lives in `gradle/libs.versions.toml` or the Gradle wrapper; keep them on current stable releases
- Tests: JUnit 4 unit tests on the JVM, Compose UI tests on the emulator
- No Android Studio. Tooling (JDK 21, cmdline-tools, gradle, scrcpy) is installed by the machine setup, never by this repo

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (pre-commit runs them too)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug                  # Android lint → app/build/reports/lint-results-debug.html (pre-commit runs it too)
scripts/emulator.sh                  # AVD from the installed Play Store image, boot, wait (IMAGE_TAG=google_apis for adb root, HEADLESS=1 for no window)
./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the running emulator
scripts/run.sh                       # install debug build, make it the home app, go home (VARIANT=Release for the minified build)
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scripts/record.sh [name] [seconds]   # adb screenrecord → screenshots/<name>.mp4 (gitignored)
scripts/pr-media.sh <files>          # upload shots as GitHub attachments, print markdown for the PR body
scrcpy                               # mirror the emulator interactively
```

Files headed `Shared script:`, `Shared workflow:` or `Shared config:` are copies of files in a separate tooling checkout. When its `sync-common` is on PATH, every build overwrites them, matched by name. Edit them at the source, never here, and do not name a repo-owned file after a shared one. To adopt another shared file, create it once under the same name and let the build fill it. Build config every app needs goes in the shared `scripts/android-app.gradle`.

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

`MainActivity` turns system callbacks (the HOME key, an app's request to pin a shortcut, which the window-less `PinShortcutActivity` hands on so the system's cleared task never clears home's) into one-shot events on flows it owns and knows nothing about pages; a one-shot event is never modelled as state the composition could replay. `LauncherScreen` owns all screen state and decides what an event means. Pages are the `LauncherPage` enum, ordered by `PageLayout` in `domain` and dispatched by an exhaustive `when`, so a new page does not compile until it has content. Anything dismissable (the app drawer, an open folder, an app's menu) hoists its state to `LauncherScreen`, which has the one `BackHandler` and spells out the order: close what is open, then return home. A popup such as an app's menu is its own focusable window and takes Back before the activity, so it needs no handler. Do not add a second `BackHandler` whose priority depends on composition order. The launcher's own menu is such a popup, opened by a long press on the home page's `EmptySpace`, which is composed behind the page's content so hit testing hands it only the touches nothing else claims; a launcher-wide setting is a row of that menu. Data the user keeps, like the apps on the home ring and in the dock, comes through an adapter in `apps` and is loaded and saved in `MainActivity`, the same way as the app list. It lives in SharedPreferences, and the menu's reset clears every preferences file rather than a list of stores, so a new store needs nothing more to be reset. A shortcut pinned there is an `AppEntry` with a shortcut id, so the ring, the dock and folders take it like an app, while the drawer, search and collections list installed apps only; a pin the home screen no longer holds is unpinned. `LauncherScreen` holds only what the screen is doing (which page, whether the drawer is open, picking or searching, which folder is open, which app's menu is open). State the system provides, like the time on the home clock, follows the same path: an adapter in `apps` exposes it as a flow, `MainActivity` collects it while the launcher is visible, and `LauncherScreen` gets the current value; only one-shot events cross as flows.

## How we work

- Arc Launcher (`apptech.arc`, sideloaded on the project emulator) is the reference. Where it has a feature, mimic how it behaves and how it is laid out, in our own colours. Unsure how Arc does something? Open it on the emulator and look, do not guess. Where Arc has no such feature, use your judgement or ask.
- Every change after the initial scaffold ships as a PR against `main`, using the PR template. Code changes get an adversarial-review pass and `/simplify` on the branch before handover; docs-only PRs skip those.
- User-visible changes carry before/after screenshots (or a recording) in the PR's "Screenshots / recording" section: capture the before shot on `main` and the after shot on the branch, publish both with `scripts/pr-media.sh` and paste its markdown. Media is uploaded as GitHub attachments, never committed.
- The emulator is the test target. Gradle auto-downloads the platform and build-tools for `compileSdk` on first build; system images come from the machine setup (toggles in `~/.zsh_toggles`); `scripts/emulator.sh` only creates an AVD from the installed Play Store image and names the toggle to set if it is missing. Never run `sdkmanager` installs from this repo.
- Pure logic goes in `domain` with a unit test. UI behaviour gets a Compose test in `androidTest` that renders the composable with fake data. `MainActivityTest` is the one end-to-end smoke test against the real system.
- A passing test is not a passing feature: for UI changes, install on the emulator, screenshot, and look at the PNG before calling it done. That after shot is the one that goes in the PR.
- Pre-commit runs hygiene checks, markdownlint, lint and the unit tests. Install with `pre-commit install`.
- GitHub Actions run on every push: `android-ci` (build, lint, unit tests) and the pre-commit hooks. The emulator tests run locally only.

## Conventions

- Kotlin official code style, 4-space indent (`.editorconfig`). Terse over verbose.
- Comments explain *why*, never *what*. Self-evident code gets no comment.
- Commit messages describe the change and the reason. No `Co-Authored-By` trailers.
- PR template: check or uncheck items, never delete them.

## Don't

- Add Android Studio-only files or workflows (`.idea/`, run configurations).
- Put Android imports in `domain`, or system calls in `ui`.
- Add libraries (DI, navigation, Hilt) before a feature needs them.
