# ADR 0038 — Pending work is a GitHub issue, opened through one form

Status: Accepted
Date: 2026-10-28
Backfilled: no
Enforced by: none (prose only — TOOLING.md § 12. Issue form) — the title form, the body's sections, labels when opened
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Then check whether a triage is owed) — when a triage is owed, what it covers, the priorities
Enforced by: none (prose only — TOOLING-BACKLOG.md § Where each item went) — the file holds only the table

## Context
Pending work had four homes with no shared triage: the handoff's *Next* list,
kept outside the repository, `TOOLING-BACKLOG.md`, the `review-debt` issues and
analysis plans. The open issues used three title shapes, had no template, and
`review-debt` was the only project label. Keeping TOOLING-BACKLOG's text in its
file with one issue per item was weighed and rejected: the priority would sit in
a label and the description in a file, the hand-kept index that file's own item
7 ruled out. A single file read cheaper for an agent, but it linked to no pull
request and bound nobody opening work by hand.

## Decision
Every piece of pending work on the harness and the repository — tooling not yet
built, an improvement, an idea, a defect, a deferred review finding — is one
GitHub issue, and no file keeps a list of it. Product scope deferred past the
MVP is not this kind of work: it stays in the Spec's *Explicitly Deferred*,
where CLAUDE.md points, its OpenSpec item included, since that is a choice of
how the project writes specs.

- **Title:** the commit form of ADR 0034, `type(scope): Imperative summary`,
  which `.githooks/rules.sh title` accepts. It is the title of the pull request
  that closes the issue, and that pull request says `Closes #n`.
- **Body:** the sections of `.github/ISSUE_TEMPLATE/backlog.yml`, in its order.
  Blank issues are off, so a person opening an issue on GitHub gets the form; an
  agent opening one with `gh issue create` writes the same headings. A
  `review-debt` issue that a branch opens has its own shape instead: one per
  branch, titled in the commit form as settling that branch's review leftovers,
  its body listing each deferred finding with its round, number and grounds.
- **Labels when opened:** one bucket — `blocks-product`, `defect`,
  `improvement` or `idea`; `export` when the outcome changes TOOLING.md
  § Rebuilding this elsewhere; `gated` when the item waits on the event its
  *Trigger* names. Labels by role, not by number: an issue carries several. A
  `review-debt` issue groups findings of several kinds, so its bucket waits for
  the triage; so do the labels of an issue whose author could not set them.
- **Labels at triage:** the owner confirms the bucket and sets one priority
  within it. `p1`: done first, it avoids rework on the work chosen next; or a
  defect that costs every pull request; or the owner names it. `p2`: a measured
  cost that recurs, such as tokens per request or a manual step per pull
  request, without blocking. `p3`: a one-off or unmeasured benefit. `export`
  breaks a tie; a `gated` item takes no priority until its event fires.
- **A triage is owed** once a merge changes `app/src/main`, or three merges
  have landed since the last triage. The agent proposes, the owner decides: a
  bucket for every issue with none; labels that match the body's *Bucket*,
  *Export value* and *Trigger* fields, or the reason they differ; each `gated`
  item's *Trigger* check run, and one whose event fired loses `gated`; a
  priority for every ungated issue; and what is worked next.
- **The last triage is recorded in the pinned issue titled `Triage log`**,
  whose body is rewritten at each triage, never appended: a `Last triage:` line
  naming the date and `main`'s commit, then at most 15 lines of decisions.
  GitHub's edit history keeps the earlier ones, so the body never explains them.
  It is a record, not pending work: no title form, no labels, no triage.
- **`TOOLING-BACKLOG.md` holds only a table** from each former item number to
  its issue, since frozen ADRs cite items by number. Shipping an item closes its
  issue, and what the issue held goes where it belongs before it closes: its
  rule to CLAUDE.md, its procedure to a Skill, its decision to an ADR, the
  incident to the commit body, what it now is and the shapes ruled out to
  TOOLING.md, and every clause it now polices to that ADR's `Enforced by:` line.
  Every document citing the issue's number is repointed in the same change,
  since a closed issue leaves no diff for a detector to see.
- Nothing from the human's separate study programme goes into an issue: the
  repository is public.

## Consequences
One query listed everything pending, and each item could link to the pull
request that closed it; the form and the labels were files and settings another
project could copy. Accepted knowingly: an issue body was not in a clone and was
not reviewed by `/code-review` as a diff was, so what exported was what got
built, recorded in TOOLING.md. `gh issue create` bypassed the form, so the
body's shape bound an agent only as prose. Before this, two items waiting on
a trigger had passed it with nobody noticing, which is why a triage runs every
*Trigger* check rather than trusting someone to remember one.
