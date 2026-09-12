#!/bin/zsh
# shared-source: android/record.sh

# Records the connected device's screen for N seconds (default 10) to screenshots/<name>.mp4.
# Usage: scripts/record.sh [name] [seconds]

set -e

cd "$(dirname "$0")/.."

NAME=${1:-$(date +%Y%m%d-%H%M%S)}
TIME_LIMIT=${2:-10}
mkdir -p screenshots

adb shell screenrecord --time-limit "$TIME_LIMIT" "/sdcard/$NAME.mp4"
adb pull -q "/sdcard/$NAME.mp4" "screenshots/$NAME.mp4"
adb shell rm "/sdcard/$NAME.mp4"

echo "screenshots/$NAME.mp4"
