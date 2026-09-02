# ADR 0012 — A new screen state ships its mockup in the same change

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("The refresh-failure banner shipped")
Enforced by: none (prose only — CLAUDE.md § Commands)

## Context
The refresh-failure banner shipped, was verified on device, and was merged with
no design reference of any kind: `search.html` still showed the state the banner
replaced. Nothing in the harness required a mockup for a new state, so the gap
stayed invisible until someone asked what the screen was supposed to look like.
Device verification had done its job — the state worked — and still nothing
recorded what it was meant to be.

## Decision
Any element or state that departs from what the approved exports show is added
to `docs/project/design/` as **both** `.html` and `.png`, in that screen's
folder, in the same change that ships it. The recipe and the naming live in
`docs/project/design/README.md`.

## Consequences
The approved exports keep describing the app that actually exists, so a mockup
stays usable as a reference instead of decaying into a record of an older
design.

The cost: a UI change now carries a rendering step, and a state that is quick to
code is not always quick to draw.

One trap is recorded with the recipe because it cost five renders to find: a
`sticky` element with a `top` offset displaces itself downward and paints over
whatever follows, so content can be present in `--dump-dom` and absent from the
screenshot. That is also how this mockup work exposed markup in `search.html`
which had been invisible long enough to mislead a reading.
