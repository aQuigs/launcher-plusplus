#!/bin/zsh

# Creates the project AVD from the newest installed arm64 image (Play Store by default,
# IMAGE_TAG=google_apis for the rootable one) and boots it, then blocks until Android is ready.
# Images come from common-configs bootstrap.sh, never from here. HEADLESS=1 runs without a window.

set -e

DEVICE_PROFILE=${DEVICE_PROFILE:-pixel_8}
IMAGE_TAG=${IMAGE_TAG:-google_apis_playstore}

case $IMAGE_TAG in
  google_apis_playstore) toggle=ANDROID_DEV_PLAYSTORE ;;
  google_apis) toggle=ANDROID_DEV_ROOT ;;
  *) echo "Unknown IMAGE_TAG '$IMAGE_TAG': use google_apis_playstore or google_apis" >&2; exit 1 ;;
esac

images=("$ANDROID_HOME"/system-images/android-*/$IMAGE_TAG/arm64-v8a(N))
if (( ! $#images )); then
  echo "No arm64 $IMAGE_TAG system image is installed. Set ANDROID_DEV=1 and $toggle=1 in ~/.zsh_toggles and re-run common-configs bootstrap.sh." >&2
  exit 1
fi

# Sort the API levels on their own: version-sorting whole paths puts android-36.1 before android-36.
api=$(printf '%s\n' ${${images:h:h:t}#android-} | sort -V | tail -1)
system_image="system-images;android-$api;$IMAGE_TAG;arm64-v8a"
AVD_NAME=${AVD_NAME:-launcher_plusplus_${DEVICE_PROFILE}_${api}_$IMAGE_TAG}

if adb get-state >/dev/null 2>&1; then
  running=$(adb emu avd name 2>/dev/null | head -1 | tr -d '\r')
  if [[ $running != "$AVD_NAME" ]]; then
    echo "${running:-Another device} is connected instead of $AVD_NAME; stop it first (adb emu kill)." >&2
    exit 1
  fi
  echo "$AVD_NAME is already running"
  exit 0
fi

if ! avdmanager list avd -c | grep -qxF "$AVD_NAME"; then
  echo no | avdmanager create avd --name "$AVD_NAME" --package "$system_image" --device "$DEVICE_PROFILE"
fi

emulator -avd "$AVD_NAME" -no-boot-anim ${HEADLESS:+-no-window} > "${TMPDIR:-/tmp}/emulator-$AVD_NAME.log" 2>&1 &

adb wait-for-device
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 2
done

echo "Emulator $AVD_NAME ($system_image) is ready"
