#!/bin/zsh

# Refreshes the scripts this repo shares with common-configs from the local checkout (repo-scripts/ there),
# so every checked-in copy stays byte-identical to its source. A script is shared when the same file name
# exists under both scripts/ and repo-scripts/; a name that exists only here is repo-specific and untouched.
# Without a common-configs checkout this is a no-op, so clones elsewhere still work from the copies.
# Runs as a pre-commit hook: a refresh exits 1 so the refreshed files get staged and the commit retried.

set -e

cd "$(dirname "$0")/.."

source_dir=${COMMON_CONFIGS:-$HOME/repos/common-configs}/repo-scripts
[[ -d $source_dir ]] || exit 0

changed=0
for src in "$source_dir"/*(N.); do
  dest=scripts/${src:t}
  [[ -f $dest ]] || continue
  cmp -s "$src" "$dest" && continue
  cp -p "$src" "$dest"
  echo "$dest refreshed from common-configs; edit it there, never here." >&2
  changed=1
done

exit $changed
