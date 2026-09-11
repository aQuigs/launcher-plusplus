#!/bin/zsh

# Creates the project AVD from the newest installed Google APIs arm64 system image and boots it,
# then blocks until Android is ready. System images are installed by common-configs bootstrap.sh
# (ANDROID_DEV), never from here. HEADLESS=1 runs without a window.

set -e

DEVICE_PROFILE=${DEVICE_PROFILE:-pixel_8}

images=("$ANDROID_HOME"/system-images/android-*/google_apis/arm64-v8a(N))
image_dir=$(printf "%s\n" $images | sort -V | tail -1)
if [[ -z $image_dir ]]; then
  echo "No Google APIs arm64 system image is installed. Run common-configs bootstrap.sh with ANDROID_DEV set." >&2
  exit 1
fi

system_image=${${image_dir#$ANDROID_HOME/}//\//;}
api=${${image_dir:h:h:t}#android-}
AVD_NAME=${AVD_NAME:-launcher_plusplus_$api}

if ! avdmanager list avd -c | grep -qx "$AVD_NAME"; then
  echo no | avdmanager create avd --name "$AVD_NAME" --package "$system_image" --device "$DEVICE_PROFILE"
fi

if adb get-state >/dev/null 2>&1; then
  echo "A device is already connected"
  exit 0
fi

emulator -avd "$AVD_NAME" -no-boot-anim ${HEADLESS:+-no-window} > "${TMPDIR:-/tmp}/emulator-$AVD_NAME.log" 2>&1 &

adb wait-for-device
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 2
done

echo "Emulator $AVD_NAME ($system_image) is ready"
