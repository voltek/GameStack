## What changed

<!-- One reviewable claim. If this needs an "and", split the PR. -->

## Why

<!-- One paragraph. Expand only if the decision has real nuance a reviewer can't get from the diff. -->

## Verification

- [ ] `./gradlew build` `./gradlew test` `./gradlew lint` — all green
- [ ] `/code-review` run and every finding triaged in a table the human confirmed (unless `ship-a-branch` step 3 exempts the branch)
- [ ] Device pass on the emulator (UI changes only) — states checked:
- [ ] Documentation-integrity detectors run (governing-document or Skill changes only) — found:

Deferred: TODO <!-- local /code-review only: each review-debt issue (#n), "none", or "not run — exempt: <the step-3 exemption>". -->

Dismissed: TODO <!-- local /code-review only: each dismissed finding and its verified reason, "none", or "not run — exempt: <the step-3 exemption>". A bot finding's record lives in its thread. -->

### Screenshots

<!--
UI changes only. DRAG OR PASTE THE IMAGES DIRECTLY BELOW THIS LINE, into this
edit box (Edit button, top right of the description). GitHub uploads them to its
own CDN and inserts the markdown for you. Do not commit emulator captures to the
repo — only design references under docs/project/design/ are versioned, and
those can be linked by URL instead.
-->

