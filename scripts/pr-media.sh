#!/bin/zsh

# Uploads screenshots or recordings as GitHub attachments and prints markdown for the PR body.
# This is the same upload the web editor does on paste; the endpoint is undocumented but accepts the gh token.
# Progress goes to stderr, the markdown to stdout.
# Usage: scripts/pr-media.sh <file>...   (IMG_WIDTH sets the image width, default 300)

set -e

if (( $# == 0 )); then
  echo "Usage: scripts/pr-media.sh <file>..."
  exit 1
fi
FILES=("$@")
IMG_WIDTH=${IMG_WIDTH:-300}
UPLOAD_URL=https://uploads.github.com/user-attachments/assets

for FILE in "${FILES[@]}"; do
  if [[ ! -f $FILE ]]; then
    echo "No such file: $FILE"
    exit 1
  fi
  NAME=$(basename "$FILE")
  if [[ $NAME == *[^A-Za-z0-9._-]* ]]; then
    echo "File name '$NAME' may only contain letters, digits, dots, underscores, and dashes"
    exit 1
  fi
done

TOKEN=$(gh auth token)
REPO_ID=$(gh api 'repos/{owner}/{repo}' --jq .id)

for FILE in "${FILES[@]}"; do
  NAME=$(basename "$FILE")
  MIME_TYPE=$(file -b --mime-type "$FILE")
  echo "Uploading $NAME ($MIME_TYPE)" >&2

  # Temporary workaround: replace this curl with `gh pr comment --attach` / `gh pr create --attach` once that flag ships in a stable gh release
  RESPONSE=$(curl -sS -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/json" \
    --data-binary "@$FILE" \
    "$UPLOAD_URL?name=$NAME&content_type=$MIME_TYPE&repository_id=$REPO_ID")
  URL=$(echo "$RESPONSE" | jq -r '.url // empty' 2>/dev/null || true)
  if [[ -z $URL ]]; then
    echo "Upload of $NAME failed, response was:"
    echo "$RESPONSE"
    exit 1
  fi

  case $NAME in
    *.png|*.jpg|*.jpeg|*.gif) echo "<img src=\"$URL\" alt=\"${NAME%.*}\" width=\"$IMG_WIDTH\">" ;;
    *) echo "$URL" ;;
  esac
done
