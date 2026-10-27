## What changed

<!-- The one claim in one to three sentences; point at an ADR, if any, or the commits for the rest. If the claim needs an "and", split the PR. -->

## Why

<!-- One to three sentences. Expand only for nuance a reviewer cannot get from the diff. -->

## Verification

- [ ] `./gradlew build` `./gradlew test` `./gradlew lint` — all green
- [ ] `/code-review` run and every finding triaged in a table the human confirmed (unless `ship-a-branch` step 3 exempts the branch)
- [ ] Device pass on the emulator (UI changes only) — states checked:
- [ ] Documentation-integrity detectors run (governing-document, Skill or `.claude/settings.json` changes only) — conditional items run: / skipped: / found:

Deferred: TODO <!-- local /code-review only: each review-debt issue (#n), "none", or "not run — exempt: <the step-3 exemption>". -->

Dismissed: TODO <!-- local /code-review only: each dismissed finding as r<k>-<n>, what it claimed and its verified reason, in about twenty words, "none", or "not run — exempt: <the step-3 exemption>". A bot finding's record lives in its thread. -->

### Screenshots

<!-- UI changes only. DRAG OR PASTE THE IMAGES BELOW THIS COMMENT, into this edit box (on an open PR: Edit, top right of the description); GitHub uploads them and inserts the markdown. Never commit emulator captures: only design references under docs/project/design/ are versioned, and they can be linked by URL instead of uploaded. -->

