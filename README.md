# launcher-plusplus

A custom Android launcher (home screen). Native Kotlin + Jetpack Compose, CLI-only workflow.

## Local dev

Requires JDK 21, Gradle, the Android command-line tools with `platform-tools` and `emulator`, and an arm64 `google_apis_playstore` system image. `scripts/emulator.sh` never installs anything and says what is missing.

```bash
./gradlew testDebugUnitTest          # unit tests
scripts/emulator.sh                  # boot the emulator (creates the AVD on first run)
./gradlew connectedDebugAndroidTest  # UI tests on the emulator
scripts/run.sh                       # install and set as the home app
```

The generic scripts (`emulator.sh`, `screenshot.sh`, `record.sh`, `pr-media.sh`, `sync-common.sh`) are copies of common-configs `repo-scripts/`; the pre-commit hook keeps them identical when that checkout is present, and they work as-is without it.
