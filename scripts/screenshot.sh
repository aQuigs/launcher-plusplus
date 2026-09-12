#!/bin/zsh
# shared-source: android/screenshot.sh

# Captures the connected device's screen to screenshots/<name>.png (default name: a timestamp).

set -e

cd "$(dirname "$0")/.."

NAME=${1:-$(date +%Y%m%d-%H%M%S)}
mkdir -p screenshots

adb exec-out screencap -p > "screenshots/$NAME.png"

echo "screenshots/$NAME.png"
