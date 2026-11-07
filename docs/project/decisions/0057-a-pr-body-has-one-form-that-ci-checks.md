# ADR 0057 — A PR body has one form that CI checks

Status: Accepted
Date: 2026-11-07
Backfilled: no
Enforced by: commits.yml — the headings and their order, the bound on each section, `Evidence:`, one review record after it with no `TODO`, a keyword per closed number
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 4. Write the PR body) — that what the body says is true, the shape of each record line

## Context
The template's review record sat open in the body and pushed *What changed* and
*Why* off the first screen, as #78 reported. Only prose kept a body in shape, so
a `TODO` left in the record could reach the merge with nothing to stop it. Across
55 PRs the gate's box was ticked 55 times, so it said nothing, the
OpenSpec box was never ticked, the device box 8 times in 48, and the detectors'
line averaged 326 characters. Over the last 20 PRs *What changed* had a median of
335 characters and a maximum of 527; *Why* had 234 and 503. Eight PRs closed
issues, six of them among the last 12, and those numbers sat in free text.

## Decision
A PR body follows `.github/pull_request_template.md`, and `commits.yml` fails when
it does not, so the order after *Verification* and a written record hold without
any Skill being loaded.

The headings are the template's, in its order; *Issues* and *Screenshots* are
optional. A note that needs more goes in *Why*, not under a new heading.

*Verification* keeps only the lines that apply, then `Evidence:`, then one review
record collapsed under a summary of its counts. A branch exempt from review writes
its exemption in one line instead. A `TODO` left there fails the check.

Every closed number carries its own keyword in *Issues*, outside `<details>` and
comments. A PR from the Revert button keeps GitHub's own body and is exempt.

The numbers live in `.githooks/rules.sh`, never here.

## Consequences
The bound was set at 600 characters, comments and the collapsed record excluded:
above every recent *What changed* and *Why*, so it stops a body that grew rather
than an ordinary one. The record has no bound, because a dismissal needs its words and a closed
block costs the first screen nothing.

The boxes for the gate and for `/code-review` left the template. `gate.yml` is a
required check, and the written record says more than a tick. The detectors' line
says only what they found, since the diff and each item's `Runs when:` say which
ran. Dropped too was the template's note on where to paste screenshots, which
`verify-on-device` and `ship-a-branch` already gave.

The check reads form and never truth: a filled record can still be wrong, and only
the human's reading of the PR sees that. Editing a body reruns `commits.yml`, so a
required check stays red on the same commit until the new run ends. A PR that once
added a heading of its own for a limitation now puts it in *Why*.

The check does not stop a skipped review round by itself: it sees a record that is
missing, not one written without a round. That gap stays with `ship-a-branch` and
the owner, and a guard on loading the Skill is designed and gated in #182.
