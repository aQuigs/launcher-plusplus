#!/bin/zsh
# shared-source: scripts/github/pr-media.sh

# Publishes screenshots or recordings for the current branch's PR and prints markdown for the PR body.
# Files are committed to the orphan pr-media branch under <branch>/, so binaries never enter main's history.
# Progress goes to stderr, the markdown to stdout.
# Usage: scripts/pr-media.sh <file>...   (IMG_WIDTH sets the image width, default 300)

set -e

cd "$(dirname "$0")/.."

if (( $# == 0 )); then
  echo "Usage: scripts/pr-media.sh <file>..."
  exit 1
fi
FILES=("$@")
MEDIA_BRANCH=pr-media
IMG_WIDTH=${IMG_WIDTH:-300}

for FILE in "${FILES[@]}"; do
  if [[ ! -f $FILE ]]; then
    echo "No such file: $FILE"
    exit 1
  fi
done

REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
BRANCH=$(git branch --show-current)
if [[ -z $BRANCH ]]; then
  echo "Not on a branch, check one out first"
  exit 1
fi
MEDIA_DIR=${BRANCH//\//-}

echo "Building a $MEDIA_BRANCH commit with ${#FILES[@]} file(s) under $MEDIA_DIR/" >&2
# A throwaway index keeps the working tree and the real index untouched
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT
export GIT_INDEX_FILE="$TEMP_DIR/index"

if git fetch -q origin "$MEDIA_BRANCH" 2>/dev/null; then
  PARENT_ARGS=(-p FETCH_HEAD)
  git read-tree FETCH_HEAD
else
  echo "$MEDIA_BRANCH does not exist on origin yet, creating it" >&2
  PARENT_ARGS=()
  git read-tree --empty
fi

for FILE in "${FILES[@]}"; do
  BLOB=$(git hash-object -w "$FILE")
  git update-index --add --cacheinfo "100644,$BLOB,$MEDIA_DIR/$(basename "$FILE")"
done

TREE=$(git write-tree)
COMMIT=$(git commit-tree "${PARENT_ARGS[@]}" -m "Media for $MEDIA_DIR" "$TREE")
echo "Pushing $COMMIT to origin/$MEDIA_BRANCH" >&2
git push -q origin "$COMMIT:refs/heads/$MEDIA_BRANCH"

for FILE in "${FILES[@]}"; do
  BASENAME=$(basename "$FILE")
  URL="https://raw.githubusercontent.com/$REPO/$MEDIA_BRANCH/$MEDIA_DIR/$BASENAME"
  case $BASENAME in
    *.png|*.jpg|*.jpeg|*.gif) echo "<img src=\"$URL\" alt=\"${BASENAME%.*}\" width=\"$IMG_WIDTH\">" ;;
    *) echo "[$BASENAME]($URL)" ;;
  esac
done
