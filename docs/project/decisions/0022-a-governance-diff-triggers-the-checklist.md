# ADR 0022 — A governance diff triggers the checklist

Status: Accepted
Date: 2026-09-01
Backfilled: yes — DRIFT-LOG.md, row 2026-09-01 ("The Spec's copy of the Skills list named twelve procedures")
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § A governance diff earns the checklist)

## Context
`DRIFT-CHECKLIST.md` told the reader to run it "periodically, or after a batch of
agent-generated features". The first half named no event at all, and the
consequence showed up on 2026-09-01: running the *Documentation integrity*
detectors as a set — the first time anyone had — turned up drift that had been
sitting in the repository for weeks, including a Spec that still listed twelve
Skills after two more had shipped. Those detectors had been correctly written
from the day they were added; nothing about them had failed. What was missing was
the moment that made someone read them. This repository had already learned the
same lesson from the opposite direction, when a Robolectric cost figure written
as a number went stale inside the branch that wrote it and was replaced by an
event.

## Decision
The *Documentation integrity* detectors run when a pull request touches a
document that governs how this repository works: `CLAUDE.md`, any `.md` under
`docs/project/` or `.github/`, or any Skill under `.claude/skills/`. That is a
shape rather than a list, so nothing needs keeping in sync when a governing
document is added, and no document owns the enumeration — this repository has
already been burned by a list that had to be updated in a second place. Mockup
and image assets are excluded because no detector reads them, and so are the
`.github/` workflows.

The event is visible in the diff, and it is exactly when those detectors bite:
they audit the harness, and the harness moves when its own documents move. The
second event already in the checklist header — a batch of agent-generated
features landing — stays, and it is the one that covers the rest of the file,
the detectors that read code rather than documents.

The step lives in `ship-a-branch`, in the round before the PR exists, and not in
CLAUDE.md. It is a procedure attached to a named task, and CLAUDE.md's own three
questions send procedures to Skills, where they are paid for when invoked instead
of on every turn.

## Consequences
Those detectors now run at the moment they can find something, rather than on a
cadence nobody owned. It also lands where the gap was widest: a docs-only diff is
exempt from `/code-review` by that Skill's own rule, so until now a change to a
governing document was reviewed by nothing at all.

Defining the trigger by shape closes holes an enumeration had left open.
`docs/project/retired-skills/` is read by a detector in this checklist and sat
outside the listed set. `docs/project/TOOLING-BACKLOG.md` is read by no detector,
but this checklist cites it by item number, so renumbering that backlog breaks a
live reference — and that too sat outside. So did the document that would have
owned the list, which meant editing what fires the audit fired nothing.
`.github/` is in for the same reason: the PR template carries the box that
records the run, and an edit that dropped it would leave the Skill pointing at
nothing.

The cost, accepted knowingly: a branch touching none of those paths does not fire
this, so drift introduced purely in code waits for the other event or for the
next governance PR. That is the right trade only because these particular
detectors check documents against documents.

Enforcement is prose, and nothing fails if the step is skipped — an ADR with no
fitness function behind it is a wish, as `decisions/README.md` puts it, and this
one is a wish until the automated checker in `TOOLING-BACKLOG.md` item 3 exists,
which that backlog ranks below items 1 and 2 because the manual pass currently
works. A checkbox in the PR template is the whole of the artifact it leaves.
