#!/bin/zsh

# Installs a build on the connected device, makes it the default home app, and shows it: the debug build, or with
# VARIANT=Release the store-speed one, installed through Gradle so its startup profile goes on too.

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
