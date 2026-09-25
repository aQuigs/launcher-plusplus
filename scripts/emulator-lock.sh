#!/bin/zsh
# Shared script: sync-common keeps every repo's copy identical to the original in the tooling checkout; edit the original only.

# Runs a command holding this repo's emulator lock, so sessions that share the emulator take turns.
# The command inherits the lock and the OS drops it when the command ends, however it ends, so it can't leak.
# Usage: scripts/emulator-lock.sh <command> [args...]
#   e.g. scripts/emulator-lock.sh ./gradlew connectedDebugAndroidTest
#   LOCK_WAIT_MINUTES (default 20) is how long to wait for another session's lock before failing; EMULATOR_LOCK overrides the lock path

set -e

zmodload zsh/system

if (( $# == 0 )); then
  echo "Usage: $0 <command> [args...]"
  exit 2
fi

# Worktrees of a repo share its emulator, so the lock is named after the main checkout, not the worktree
SCRIPT_DIR=${0:A:h}
COMMON_DIR=$(git -C "$SCRIPT_DIR" rev-parse --path-format=absolute --git-common-dir 2>/dev/null || true)
REPO_DIR=${COMMON_DIR:+${COMMON_DIR:h}}
REPO_NAME=$(basename "${REPO_DIR:-$SCRIPT_DIR:h}")

LOCK=${EMULATOR_LOCK:-/tmp/$REPO_NAME-emulator.flock}
# Separate from the lock file: closing any descriptor on that file, even one opened only to write it, drops the lock
HOLDER=$LOCK.holder
WAIT_MINUTES=${LOCK_WAIT_MINUTES:-20}

touch "$LOCK"

# -e keeps the lock across the exec below
if ! zsystem flock -e -t 0 "$LOCK" 2>/dev/null; then
  echo "Waiting up to $WAIT_MINUTES min for the emulator lock, held by $(cat "$HOLDER" 2>/dev/null)"
  if ! zsystem flock -e -t $(( WAIT_MINUTES * 60 )) "$LOCK"; then
    echo "Gave up after $WAIT_MINUTES min waiting for the emulator lock $LOCK, held by $(cat "$HOLDER" 2>/dev/null)"
    exit 1
  fi
fi

# exec keeps this PID, so the holder recorded is the command itself
print -r -- "pid $$ since $(date '+%H:%M:%S'): $*" > "$HOLDER"
exec "$@"
