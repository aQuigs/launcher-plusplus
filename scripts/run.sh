#!/bin/zsh

# Installs the debug build (VARIANT=Release for the minified one), makes it the default home app, and shows it.

set -e

cd "$(dirname "$0")/.."

APP_ID=com.aquigs.launcherplusplus

./gradlew "install${VARIANT:-Debug}" -q
adb shell cmd package set-home-activity "$APP_ID/.MainActivity"
adb shell input keyevent KEYCODE_HOME

until adb shell dumpsys activity activities | grep -q "ResumedActivity.*$APP_ID"; do
  sleep 1
done

echo "Launcher++ is now the home app"
