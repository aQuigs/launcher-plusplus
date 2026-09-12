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
./gradlew testDebugUnitTest          # JVM unit tests (also runs in pre-commit)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
scripts/emulator.sh                  # AVD from the installed Play Store image, boot, wait (IMAGE_TAG=google_apis for adb root, HEADLESS=1 for no window)
./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the running emulator
scripts/run.sh                       # install debug build, make it the home app, go home
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scripts/record.sh [name] [seconds]   # adb screenrecord → screenshots/<name>.mp4 (gitignored)
scripts/pr-media.sh <files>          # upload shots as GitHub attachments, print markdown for the PR body
scrcpy                               # mirror the emulator interactively
```

A script whose second line starts with `# Shared script:` is a verbatim copy of a file kept in a separate tooling checkout (`grep -l '^# Shared script:' scripts/*` lists them). When that checkout's `sync-common` is on PATH, every build refreshes the copies (`syncSharedScripts`), so change a shared script at its source, never here; without the command the task is skipped and the copies work as-is. Scripts without the header belong to this repo. Adopt another shared script by copying it once under its own name.

## Layout

```text
app/src/main/kotlin/com/aquigs/launcherplusplus/
├── MainActivity.kt      # composition root: wires the repository into the UI
├── domain/              # pure Kotlin: types and logic, no Android imports
├── apps/                # Android system adapters (LauncherApps, …) behind interfaces
└── ui/                  # Compose: screens, components, theme
app/src/test/            # JVM unit tests (domain)
app/src/androidTest/     # Compose UI tests and the activity smoke test (emulator)
scripts/                 # emulator, run, screenshot helpers (zsh)
```

Dependencies flow down only: `ui → domain ← apps`, and `MainActivity` is the only place that wires them together. `domain` never imports `android.*`; `ui` reaches the system only through the interfaces in `apps`.

The screen is `LauncherScreen`: a pager over the `PageLayout` in `domain`, with one `when` branch per `LauncherPage` (the compiler flags a page without content). System events such as the HOME key reach the UI as state owned by `MainActivity` (`homeRequests`), never as calls into composables.

## How we work

- Every change after the initial scaffold ships as a PR against `main`, using the PR template. Code changes get an adversarial-review pass and `/simplify` on the branch before handover; docs-only PRs skip those.
- User-visible changes carry before/after screenshots (or a recording) in the PR's "Screenshots / recording" section: capture the before shot on `main` and the after shot on the branch, publish both with `scripts/pr-media.sh` and paste its markdown. Media is uploaded as GitHub attachments, never committed.
- The emulator is the test target. Gradle auto-downloads the platform and build-tools for `compileSdk` on first build; system images come from the machine setup (toggles in `~/.zsh_toggles`); `scripts/emulator.sh` only creates an AVD from the installed Play Store image and names the toggle to set if it is missing. Never run `sdkmanager` installs from this repo.
- Pure logic goes in `domain` with a unit test. UI behaviour gets a Compose test in `androidTest` that renders the composable with fake data. `MainActivityTest` is the one end-to-end smoke test against the real system.
- A passing test is not a passing feature: for UI changes, install on the emulator, screenshot, and look at the PNG before calling it done. That after shot is the one that goes in the PR.
- Pre-commit runs hygiene checks, markdownlint, and the unit tests. Install with `pre-commit install`.

## Conventions

- Kotlin official code style, 4-space indent (`.editorconfig`). Terse over verbose.
- Comments explain *why*, never *what*. Self-evident code gets no comment.
- Commit messages describe the change and the reason. No `Co-Authored-By` trailers.
- PR template: check or uncheck items, never delete them.

## Don't

- Add Android Studio-only files or workflows (`.idea/`, run configurations).
- Put Android imports in `domain`, or system calls in `ui`.
- Add libraries (DI, navigation, Hilt) before a feature needs them.
