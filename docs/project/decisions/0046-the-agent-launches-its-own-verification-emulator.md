# ADR 0046 — The agent launches its own verification emulator

Status: Accepted
Date: 2026-10-31
Backfilled: no
Supersedes: ADR-0005 — "The emulator is *asked for*, never launched unprompted" (Decision)
Enforced by: none (prose only — .claude/skills/verify-on-device/SKILL.md § Getting a device) — the dedicated AVD alone, launched headless from wiped data, a window on request and on a procedure's first run, closing it at the end, and closing one left running before launching

## Context
ADR 0005 had the agent ask for an emulator rather than launch one, because a
launch opened a window on the human's desktop. Every device pass therefore
waited on the human, which defeated the point of automating it. A headless
emulator opens no window, and the screenshots and dumps the pass relies on work
the same without one. The machine also held the human's own AVDs, one of them
personal, and a pass that booted one of those could leave its state changed.

## Decision
`GameStack_Verify` is the agent's alone. For every pass the agent launches it
headless, from wiped data and without a snapshot, and closes it when the pass
ends; one found already running is a pass left behind, and is closed and
launched again. Any other emulator is the human's, even one already running:
the agent never selects it, boots it or closes it. If `GameStack_Verify` is
missing, it asks the human to create it.

The emulator runs with a window when the human asks, and on the first run after
the verification procedure changes, so the human can watch for a blind spot no
rule names yet.

## Consequences
A device pass no longer waits on the human, and every launch starts from a
factory-fresh device, so a pass interrupted with the network off cannot leave
the next one starting offline. The cost is a boot of about a minute per launch,
about half a minute more than a cold boot that keeps the data, and the
machine's memory while it runs; a clone needs the AVD created once, by hand.
The human tests on any other AVD: a `GameStack_Verify` left open is closed by
the next pass.
Headless also means the human sees a routine pass only through its screenshots
and recordings, which was accepted: the known blind spots are rules in the
Skill, and a watched run is kept for when the procedure is new.
