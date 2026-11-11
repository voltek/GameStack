# ADR 0067 — A branch's records are deleted with its branch

Status: Accepted
Date: 2026-11-11
Backfilled: no
Supersedes: ADR-0065 — "plans while an open item cites them, deleted once every decision they hold is in an ADR, an issue or TOOLING.md" and "review records until their branch merges" (Decision)
Enforced by: test: test-session-start.sh#a gone branch loses its records — a branch's folders deleted, and named, once it is neither local nor on a remote
Enforced by: test: test-session-start.sh#a live branch keeps its records — a local or pushed branch keeps its folders
Enforced by: test: test-session-start.sh#no repository, nothing deleted — nothing deleted where the repository cannot be read
Enforced by: test: test-session-start.sh#a record outside a branch folder — a record outside a branch folder warned about
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § What is kept) — a plan deleted by the owner, after its decisions and the measures an open item needs are checked

## Context
ADR 0065 said review records were deleted at their branch's merge, and nothing
carried it out: neither `merge-pr` nor the `checkpoint` steps deleted them, and
the hook did not look. They went only because the handoff carried a note, which
that ADR ruled out. Device captures and a pull request's screenshots had no rule,
and both folders held files from August and October that nothing read. Records
were named for their topic or their pull request, so nothing tied them to a
branch. Auto mode's classifier refused an agent's `rm` in that folder, and no
project rule changes what it reads. On the day the plans were cleared, two of
them held what nothing else did: #94's audit rows and #100's baseline measure.

## Decision
What a branch leaves beside the handoff goes when the branch goes.

- **A branch's records live in a folder named for it:** `reviews/<branch>/` for a
  review round's output and triage, `captures/<branch>/` for device captures and
  a pull request's screenshots. The review records' folder is named from the
  checked-out branch by the script that saves them, never typed.
- **The SessionStart hook deletes them** once the branch is neither local nor on
  a remote, and prints one line naming what it deleted. It deletes nothing when it
  cannot read the repository, and warns about a record outside a branch folder,
  which it never deletes.
- **A plan is deleted by the owner.** Whether it can go is a judgement: the check
  is that every decision it holds and every measure an open item needs is in an
  ADR, an issue or TOOLING.md. The checkpoint gives the owner the command and
  records it under *Owed by the owner* until it runs.

## Consequences
No step or note had to remember the records: a merge on GitHub was enough, once
the local branch was deleted and a fetch had pruned the remote one. A branch
pushed but deleted locally kept its records until a prune. A second clone sharing
the same folder name on one machine would see the other's local branches as
gone, so only pushed branches were safe there. Captures were still named by hand,
so a capture folder that misspelled its branch was deleted as gone. The folders from before the rule
were deleted by hand once. A plan waited for the owner's command, at the cost of
one line in the handoff until it ran.
