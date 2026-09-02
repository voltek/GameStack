# ADR 0013 — The MVP does not target accessibility, with two bounds and an expiry

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Neither CLAUDE.md nor the Spec ever said whether the MVP targets accessibility")
Enforced by: none (prose only — CLAUDE.md § Commands)

## Context
Neither CLAUDE.md nor the Spec ever said whether the MVP targets accessibility.
A review round then raised a missing TalkBack live region on the new refresh
banner, and there was no basis on which to triage it — neither the reviewer nor
the agent could tell "we are not doing accessibility yet" from "we just broke
accessibility". Those are opposite situations with opposite responses, and the
documents could not distinguish between them.

## Decision
The MVP does not target accessibility: TalkBack passes, large-font and contrast
audits, and touch-target sizing are backlog. An accessibility finding from a
review is therefore **non-blocking by default** — open an issue, link it in the
thread, merge. Do not build accessibility work that was not asked for, and do
not hold a PR for it.

Two bounds, because both cost far more to retrofit than to keep:

- **Never drop an affordance that already worked.** Material components carry
  semantics for free; replacing one with a hand-built equivalent loses them
  silently, and the loss is invisible to `build`, `test`, `lint` and to a
  screenshot. Restoring it in the same change is *not* new accessibility work —
  it is not regressing.
- **The deferral expires before the first public release.** The app is headed
  for the Play Store, where retrofitting semantics across a finished UI is a
  rewrite.

## Consequences
A review finding of this class now has a defined answer, so the loop does not
stall on a question nobody had the authority to settle.

The cost is stated rather than hidden: the app will ship an MVP that is not
accessible, and the bill comes due before release rather than being written off.

The second bound is the one that does work today — the live region was still
fixed under it, as un-regressing. That fix is recorded as **unverified**:
Compose live regions fire reliably on a text change to an existing node and may
not announce a node that has just appeared, which is exactly this case. Settling
it needs a manual TalkBack pass, which is what the deferral covers.
