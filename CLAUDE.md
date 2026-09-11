# launcher-plusplus

Custom Android home screen (launcher). Native Kotlin + Jetpack Compose, built and tested entirely from the CLI.

## Stack

- Kotlin + Jetpack Compose (Material 3), single `app` module
- Android Gradle Plugin 9 with built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android` to the module, only the Compose compiler plugin
- Every version number lives in `gradle/libs.versions.toml` or the Gradle wrapper; keep them on current stable releases
- Tests: JUnit 4 unit tests on the JVM, Compose UI tests on the emulator
- No Android Studio. Tooling (JDK 21, cmdline-tools, gradle, scrcpy) comes from common-configs `bootstrap.sh`

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (also runs in pre-commit)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
scripts/emulator.sh                  # AVD from the newest installed arm64 image, boot, wait (HEADLESS=1 for no window)
./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the running emulator
scripts/run.sh                       # install debug build, make it the home app, go home
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scrcpy                               # mirror the emulator interactively
```

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

## How we work

- Every change after the initial scaffold ships as a PR against `main`, using the PR template. Run an adversarial-review pass and `/simplify` on the branch before handing it over.
- The emulator is the test target. Gradle auto-downloads the platform and build-tools for `compileSdk` on first build; common-configs `bootstrap.sh` installs the newest stable Google APIs arm64 system image; `scripts/emulator.sh` only creates an AVD from whatever image is installed. Never run `sdkmanager` installs from this repo.
- Pure logic goes in `domain` with a unit test. UI behaviour gets a Compose test in `androidTest` that renders the composable with fake data. `MainActivityTest` is the one end-to-end smoke test against the real system.
- A passing test is not a passing feature: for UI changes, install on the emulator, screenshot, and look at the PNG before calling it done.
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
