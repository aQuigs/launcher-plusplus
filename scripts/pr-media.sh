#!/bin/zsh
# shared-source: scripts/github/pr-media.sh

# Publishes screenshots or recordings for the current branch's PR and prints markdown for the PR body.
# Files land on the orphan `pr-media` branch under <branch>/, so binaries never enter main's history.
# Usage: scripts/pr-media.sh <file>...

set -e

cd "$(dirname "$0")/.."

(( $# )) || { echo "Usage: scripts/pr-media.sh <file>..." >&2; exit 1; }

MEDIA_BRANCH=pr-media
IMG_WIDTH=${IMG_WIDTH:-300}
repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
dir=${$(git branch --show-current)//\//-}

if git fetch -q origin "$MEDIA_BRANCH" 2>/dev/null; then
  parent=(-p FETCH_HEAD)
else
  parent=()
fi

export GIT_INDEX_FILE=$(mktemp -u)
if (( $#parent )); then
  git read-tree FETCH_HEAD
else
  git read-tree --empty
fi

for file in "$@"; do
  git update-index --add --cacheinfo "100644,$(git hash-object -w "$file"),$dir/${file:t}"
done

commit=$(git commit-tree $parent -m "Media for $dir" "$(git write-tree)")
git push -q origin "${commit}:refs/heads/$MEDIA_BRANCH"
rm -f "$GIT_INDEX_FILE"

for file in "$@"; do
  url="https://raw.githubusercontent.com/$repo/$MEDIA_BRANCH/$dir/${file:t}"
  case ${file:e} in
    png|jpg|jpeg|gif) echo "<img src=\"$url\" alt=\"${file:t:r}\" width=\"$IMG_WIDTH\">" ;;
    *) echo "[${file:t}]($url)" ;;
  esac
done
