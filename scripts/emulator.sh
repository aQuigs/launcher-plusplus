#!/bin/zsh
# shared-source: scripts/android/emulator.sh

# Boots this repo's emulator, creating its AVD first when needed, and waits until Android is ready.
# The system image comes from the machine setup (bootstrap.sh); this script never installs anything.
# Usage: scripts/emulator.sh
#   IMAGE_TAG=google_apis    use the rootable image instead of the Play Store one
#   HEADLESS=1               run without a window
#   DEVICE_PROFILE=<device>  avdmanager device profile, default pixel_8 (Play Store images need one that supports it)
#   AVD_NAME=<name>          override the derived AVD name

set -e

IMAGE_TAG=${IMAGE_TAG:-google_apis_playstore}
DEVICE_PROFILE=${DEVICE_PROFILE:-pixel_8}

case $IMAGE_TAG in
  google_apis_playstore) TOGGLE=ANDROID_DEV_PLAYSTORE ;;
  google_apis) TOGGLE=ANDROID_DEV_ROOT ;;
  *)
    echo "Unknown IMAGE_TAG '$IMAGE_TAG', use google_apis_playstore or google_apis"
    exit 1
    ;;
esac

if [[ -z $ANDROID_HOME ]]; then
  echo "ANDROID_HOME is not set, set ANDROID_DEV=1 in ~/.zsh_toggles and re-run bootstrap.sh"
  exit 1
fi
IMAGE_ROOT="$ANDROID_HOME/system-images"

echo "Finding the newest installed arm64 $IMAGE_TAG system image"
IMAGE_DIRS=$(find "$IMAGE_ROOT" -mindepth 3 -maxdepth 3 -type d -name arm64-v8a -path "*/$IMAGE_TAG/*" 2>/dev/null || true)
if [[ -z $IMAGE_DIRS ]]; then
  echo "No arm64 $IMAGE_TAG system image under $IMAGE_ROOT"
  echo "Set ANDROID_DEV=1 and $TOGGLE=1 in ~/.zsh_toggles and re-run bootstrap.sh"
  exit 1
fi
echo "$IMAGE_DIRS"

# Directories are named android-<api>; sort the api alone so android-36.1 sorts after android-36
API_LEVELS=$(echo "$IMAGE_DIRS" | awk -F/ '{print $(NF-2)}' | sed 's/^android-//' | sort -V)
API_LEVEL=$(echo "$API_LEVELS" | tail -1)
SYSTEM_IMAGE="system-images;android-$API_LEVEL;$IMAGE_TAG;arm64-v8a"
echo "Using $SYSTEM_IMAGE"

# One AVD per repo, device, api, and image, so repos sharing this script never collide
REPO_DIR=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
REPO_NAME=$(basename "$REPO_DIR" | tr '-' '_')
AVD_NAME=${AVD_NAME:-${REPO_NAME}_${DEVICE_PROFILE}_${API_LEVEL}_${IMAGE_TAG}}
LOG_FILE="${TMPDIR:-/tmp}/emulator-$AVD_NAME.log"

# adb -e talks only to emulators, so a plugged-in phone does not get in the way
RUNNING_AVD=$(adb -e emu avd name 2>/dev/null | head -1 | tr -d '\r' || true)
if [[ $RUNNING_AVD == "$AVD_NAME" ]]; then
  echo "$AVD_NAME is already running"
  exit 0
fi
if [[ -n $RUNNING_AVD ]]; then
  echo "Emulator $RUNNING_AVD is running instead of $AVD_NAME, stop it first: adb -e emu kill"
  exit 1
fi

if avdmanager list avd -c | grep -qxF "$AVD_NAME"; then
  echo "AVD $AVD_NAME exists"
else
  echo "Creating AVD $AVD_NAME"
  # avdmanager asks whether to build a custom hardware profile; no keeps the device profile's defaults
  echo no | avdmanager create avd --name "$AVD_NAME" --package "$SYSTEM_IMAGE" --device "$DEVICE_PROFILE"
  CONFIG_FILE="${ANDROID_AVD_HOME:-$HOME/.android/avd}/$AVD_NAME.avd/config.ini"
  # avdmanager writes hw.keyboard=no, which blocks typing from the host keyboard
  sed -i '' '/^hw.keyboard=/d' "$CONFIG_FILE"
  echo 'hw.keyboard=yes' >> "$CONFIG_FILE"
fi

echo "Booting $AVD_NAME, log at $LOG_FILE"
# WebView, and so Google sign-in, aborts on the host GPU translator with an empty GL version; SwiftShader is the only renderer where it survives
emulator -avd "$AVD_NAME" -gpu swiftshader_indirect -no-boot-anim ${HEADLESS:+-no-window} > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!

echo "Waiting for Android to finish booting"
until [[ $(adb -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]]; do
  if ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Emulator exited before boot completed, see $LOG_FILE"
    exit 1
  fi
  sleep 2
done

echo "Emulator $AVD_NAME ($SYSTEM_IMAGE) is ready"
