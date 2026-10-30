# ADR 0043 — The product is a living PRD, and its backlog is product issues

Status: Accepted
Date: 2026-10-30
Backfilled: no
Supersedes: ADR-0038 — "Product scope deferred past the MVP is not this kind of work" (Decision)
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — the *Capabilities* table's *Spec* column names exactly the folders under `openspec/specs/`
Enforced by: none (prose only — docs/project/PRODUCT.md § How this document changes) — when it is read and when it is edited, a technical deferral kept out of it
Enforced by: none (prose only — docs/project/TOOLING.md § 12. Issue form) — the product form, the `product` label, the Horizon field, the Project holding product issues only
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Then check whether a triage is owed) — a triage adds missing product issues to the Project, then sets or confirms each horizon

## Context
The Spec was named v1 and edited in place. It held product scope beside a tech
stack and data sources that CLAUDE.md already ruled, a list of every Skill that a
checklist item kept in sync, and an *Explicitly Deferred* section of fifteen
items that mixed product with technical deferrals such as instrumented tests and
a KMP migration, each carrying its full reasoning. It named no user, no reason
to come back and nothing that had to be true before a release. Once behaviour
moved to `openspec/specs/` (ADR 0041), what was left had no stated role. The
owner rewrote it in an interview on 2026-10-30.

## Decision
The product is `docs/project/PRODUCT.md`, with no version in its name: git
versions it. It holds what the app is and for whom, why the project exists, a
*Capabilities* table with each capability's bullets until its first change
replaces them with a spec, the criteria for the first release, one line per
Later backlog item pointing at its issue, what is not in this app, and the open
questions. How the app is built stays in CLAUDE.md.

It is living, and changed only on a trigger, never on a schedule: a change adds
or retires a capability, an item moves in or out of scope, an open question is
answered, or a release criterion changes. It is read at fixed moments: a change
reads it for scope, and a triage reads its release criteria, backlog and open
questions. The *Capabilities* table's *Spec* column names exactly the folders
under `openspec/specs/`.

Every product item is a GitHub issue labelled `product`, opened through
`.github/ISSUE_TEMPLATE/product.yml`, titled in the commit form. It takes no
bucket and no `p1`–`p3`: it takes a horizon, the Horizon field of the
`GameStack Product` GitHub Project. Now is the current cycle, Next is needed for
the first release, Later comes after it. A triage adds any product issue the
Project lacks, then sets or confirms its horizon. That Project holds product
issues only; harness issues keep ADR 0038's labels. A release criterion is
product scope whatever its kind; a technical deferral that no release criterion
needs is harness work, never a line in the PRD.

## Consequences
Each capability had one row naming where its behaviour lived, and a detector
could compare that row with the folders it named. The first release gained a
definition the backlog's Next items mapped onto one by one, and the backlog's
reasoning moved into the issue that would use it. Accepted knowingly: a
horizon in a Project field was invisible to `gh issue list`, so reading it took
the `project` token scope and `gh project item-list`, and the board's view was
GitHub state that a clone did not carry.
