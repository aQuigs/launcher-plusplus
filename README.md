# launcher-plusplus

A custom Android launcher (home screen). Native Kotlin + Jetpack Compose, CLI-only workflow.

## Local dev

Requires JDK 21, the Android command-line tools, gradle, and a Play Store emulator image, all installed by common-configs `bootstrap.sh` with `ANDROID_DEV=1` and `ANDROID_DEV_PLAYSTORE=1`.

```bash
./gradlew testDebugUnitTest          # unit tests
scripts/emulator.sh                  # boot the emulator (creates the AVD on first run)
./gradlew connectedDebugAndroidTest  # UI tests on the emulator
scripts/run.sh                       # install and set as the home app
```
