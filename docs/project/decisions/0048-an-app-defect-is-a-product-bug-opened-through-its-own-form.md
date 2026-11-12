# ADR 0048 — An app defect is a product bug, opened through its own form

Status: Accepted
Date: 2026-11-03
Backfilled: no
Supersedes: ADR-0038 — "an idea, a defect, a deferred review finding" (Decision), for a defect in the app
Supersedes: ADR-0043 — "opened through `.github/ISSUE_TEMPLATE/product.yml`" (Decision), for a bug
Supersedes: ADR-0047 — "A harness issue, an app defect among them, opens with one bucket" (Decision)
Enforced by: test: test-session-start.sh#bugs counted in the product backlog — the hook's product line counts bugs apart
Enforced by: none (prose only — docs/project/TOOLING.md § 12. Issue form) — the bug form, its labels, the Project's auto-add workflow, no harness Project
Enforced by: none (prose only — .claude/skills/verify-on-device/SKILL.md § A defect found) — a confirmed defect filed through the bug form
Enforced by: none (prose only — docs/project/PRODUCT.md § Backlog) — a bug never a line in the Backlog

## Context
Search's exploratory pass filed the first two app defects, #145 and #146,
through the harness form with the `defect` bucket: the harness pool's count
mixed them with governance defects, and the `GameStack Product` Project, where
the first release's scope was decided, never saw them. The product form asked
what the user would get, which a defect does not answer, and neither form had a
slot for the steps, the device or the capture that the verify-on-device pass
produced. A harness Project was weighed in the same analysis (#134): a board
groups only by a single-select or iteration field, never by a label.

## Decision
- **An app defect is a product issue, a bug.** It is labelled `product` and
  `bug`, never the harness `defect` bucket, and is opened through
  `.github/ISSUE_TEMPLATE/bug.yml`, which applies both labels. Its priority is
  its Horizon, like any product issue's: a severe one enters the open cycle as
  an insertion and takes Now; the rest take Next when a release criterion needs
  them, Later otherwise. A bug is never a line in `PRODUCT.md`'s *Backlog*.
- **The product Project adds every issue labelled `product` by itself**, through
  its auto-add workflow (`is:issue label:product`), owner-set Project state like
  its Horizon field. A triage still adds any older one not updated since.
- **`defect` stays the harness bucket:** a defect on `main` in the gate, the
  rules or the tooling.
- **Harness issues get no GitHub Project** while their priority is a label. A
  triage reads the harness pool grouped by bucket and priority instead, with one
  `gh` command.

## Consequences
The harness pool counted harness defects only, and each bug reached the
Project, and a Horizon, the moment it was labelled. Accepted knowingly: a third
form duplicated the product form's Horizon dropdown; the auto-add workflow was
GitHub state a clone did not carry, and added an older issue only once that
issue was next updated; and the harness pool stayed a list, read once a cycle. Ruled out:
reusing `defect` on product issues, which mixed the counts again; one product
form widened to "does not do yet, or does wrong", whose optional fields fitted
neither kind; an issue type `bug`, defined per organization only; a form's
`projects:` key, which covered the form alone and wrote one owner's Project
number into every clone; and a harness Project, whose fields would mirror the
labels as a second source of priority or replace them and leave `gh issue list`
blind to it. A Project for the harness reopens when more than one person works
its pool, or when GitHub lets a view group by label.
