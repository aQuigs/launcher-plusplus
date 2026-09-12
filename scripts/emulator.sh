#!/bin/zsh
# Shared script: sync-common keeps every repo's copy identical to the original in the tooling checkout; edit the original only.

# Boots this repo's emulator, creating its AVD on first use, and waits until Android is ready.
# Uses the system image bootstrap.sh installed; this script never installs anything.
# Usage: scripts/emulator.sh   (IMAGE_TAG=google_apis for the rootable image, HEADLESS=1 for no window)

set -e

# A missing image must expand to nothing, not to a "no matches" error
setopt null_glob

cd "$(dirname "$0")/.."

IMAGE_TAG=${IMAGE_TAG:-google_apis_playstore}
IMAGE_ROOT=$ANDROID_HOME/system-images

# bootstrap.sh keeps exactly one image per variant, so any other count means it needs re-running
IMAGE_DIRS=("$IMAGE_ROOT"/android-*/"$IMAGE_TAG"/arm64-v8a)
if (( ${#IMAGE_DIRS[@]} != 1 )); then
  echo "Expected one arm64 $IMAGE_TAG system image under $IMAGE_ROOT, found ${#IMAGE_DIRS[@]}"
  echo "Set ANDROID_DEV=1 plus ANDROID_DEV_PLAYSTORE=1 or ANDROID_DEV_ROOT=1 in ~/.zsh_toggles and re-run bootstrap.sh"
  exit 1
fi

RELATIVE_DIR=${IMAGE_DIRS[1]#$IMAGE_ROOT/}
API_DIR=${RELATIVE_DIR%%/*}

# One AVD per repo, API level, and variant, so a newer image from bootstrap gets a fresh AVD
REPO_NAME=$(basename "$PWD" | tr '-' '_')
AVD_NAME=${REPO_NAME}_${API_DIR#android-}_$IMAGE_TAG
AVD_DIR=$HOME/.android/avd/$AVD_NAME.avd
LOG_FILE=${TMPDIR:-/tmp}/emulator-$AVD_NAME.log

# adb -e ignores plugged-in phones; a different emulator must be stopped because the boot wait cannot tell them apart
RUNNING_AVD=$(adb -e emu avd name 2>/dev/null | head -1 | tr -d '\r')
if [[ $RUNNING_AVD == "$AVD_NAME" ]]; then
  echo "$AVD_NAME is already running"
  exit 0
elif [[ -n $RUNNING_AVD ]]; then
  echo "Emulator $RUNNING_AVD is running instead of $AVD_NAME, stop it first: adb -e emu kill"
  exit 1
fi

if [[ ! -d $AVD_DIR ]]; then
  echo "Creating AVD $AVD_NAME"
  # no declines the custom hardware profile prompt; pixel_8 is a device profile that supports Play Store images
  echo no | avdmanager create avd --name "$AVD_NAME" --package "system-images;$API_DIR;$IMAGE_TAG;arm64-v8a" --device pixel_8

  # avdmanager writes hw.keyboard=no, which blocks typing from the host keyboard
  sed -i '' '/^hw.keyboard=/d' "$AVD_DIR/config.ini"
  echo 'hw.keyboard=yes' >> "$AVD_DIR/config.ini"
fi

echo "Booting $AVD_NAME, log at $LOG_FILE"
# WebView, and so Google sign-in, aborts on the host GPU translator with an empty GL version; SwiftShader is the only renderer where it survives
emulator -avd "$AVD_NAME" -gpu swiftshader_indirect -no-boot-anim ${HEADLESS:+-no-window} > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!

until [[ $(adb -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') == 1 ]]; do
  if ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Emulator exited before boot completed, see $LOG_FILE"
    exit 1
  fi
  sleep 2
done

echo "Emulator $AVD_NAME is ready"
