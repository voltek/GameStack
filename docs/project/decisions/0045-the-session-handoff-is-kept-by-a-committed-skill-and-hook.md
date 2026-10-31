# ADR 0045 — The session handoff is kept by a committed Skill and hook

Status: Accepted
Date: 2026-10-30
Backfilled: no
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § HANDOFF.md — session state only) — what the handoff holds, rewrite never append, the bound, nothing derivable, the repository winning
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § CYCLE.md — one finite cycle) — admission, insertions, the breakers, closing a cycle
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § One task per session) — one branch per session, stopping at its merge
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § When to checkpoint) — when a checkpoint runs
Enforced by: none (prose only — docs/project/TOOLING.md § 14. Session handoff) — the hook's place and its registration
Enforced by: test: test-session-start.sh#default path from a worktree — where the hook reads the files when the variable is unset
Enforced by: test: test-session-start.sh#no handoff — the derived facts and a notice when no handoff exists

## Context
From 2026-10-22 a session started from a hot handoff and, later, an open cycle:
two small files outside the repository that a SessionStart hook injected with
facts it derived live. The hook script, its registration and the rules both
files followed lived only on the owner's machine, the rules as each file's own
header, so ADR 0039's rule that a harness decision lives where a clone reads it
was broken from the day it was written. Those headers were also paid for on
every session, and the hook's output had reached about 9,400 of the 10,000
characters past which Claude Code shows only a preview.

## Decision
The live state stays out of the repository; what keeps it moves in.

- **The rules live in the `checkpoint` Skill:** what `HANDOFF.md` and
  `CYCLE.md` hold, their bounds, rewriting the handoff whole at each checkpoint,
  the cycle's admission clauses and breakers, and one task per session. The
  files keep a one-line pointer to the Skill instead of a header.
- **The hook is `.claude/hooks/session-start.sh`, registered in the committed
  `.claude/settings.json`**, so every clone runs it. It reads the files from
  `$CLAUDE_HANDOFF_DIR`, or `~/Documents/<main checkout's folder name>` when
  unset, and
  never names one machine's path. With no handoff it prints the derived facts
  and says so.
- **A checkpoint runs when a cycle status changes and at session end**, not per
  commit.

## Consequences
A clone got the start routine and its rules; only the state itself stayed
personal, and the hook's output dropped by the size of the two headers. The
bounds were checked only by the hook's warnings, which nothing in the
`Enforced by:` vocabulary named, so every rule here was prose; only the hook's
own behaviour had cases, in `test-session-start.sh`. Accepted
knowingly: a hook in committed settings ran on every clone that trusted the
repository, and the archive conventions and a handoff template stayed in #87.
