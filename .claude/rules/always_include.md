<!-- Shared rules: sync-common keeps every repo's copy identical to the original in the tooling checkout; edit the original only. -->

# Shared rules for every repo

## Shared files

- Files headed `Shared script:`, `Shared workflow:`, `Shared config:` or `Shared rules:` are copies of files in a separate tooling checkout. `sync-common` overwrites them, matched by name. Edit them at the source, never here, and do not name a repo-owned file after a shared one.
- `sync-common` lists every shared file this repo has no copy of as `skipped`. When one fits the work at hand (a rule file for a platform the repo now targets, a script it now needs), adopt it as the line says: create it empty at the path shown and run `sync-common` again, or for a rule file, ask the user to.

## How we work

- Every change after the initial scaffold ships as a PR against `main`, using the PR template. Code changes get an adversarial-review pass and `/simplify` on the branch. Then, for code and docs alike, ask: can this be made meaningfully simpler without losing functionality? Generated docs run verbose, so they need the question as much as code. If so, make those changes before opening the PR. Only a trivial change, like a few words, skips it.
- A passing test is not a passing feature: for UI changes, run the app, screenshot, and look at the image before calling it done.
- User-visible changes carry screenshots (or a recording) in the PR's "Screenshots / recording" section:
  - Shoot every state the change touches, not one before and one after. Empty and filled, and before and after an action, are separate states. Light and dark theme are separate states only when the change is about colour or theming; otherwise one theme is enough.
  - Take the before shots on `main` and the after shots on the branch.
  - Publish with `scripts/pr-media.sh`, one caption per file, and paste its table as is, at most about four states. A screen that is new in the PR gets an after shot only.
  - Media is uploaded as GitHub attachments, never committed. Shots must never show a signed-in account.

## Conventions

- Comments explain *why*, never *what*. Self-evident code gets no comment.
- Commit messages describe the change and the reason. No `Co-Authored-By` trailers.
- PR template: check or uncheck items, never delete them.
- Add a library only when a feature needs it.
