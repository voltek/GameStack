# ADR 0064 — Repository text cites only what a clone can open

Status: Accepted
Date: 2026-11-11
Backfilled: no
Enforced by: commits.yml — every commit message, and the title and body of a pull request: a plan's branch label, a cycle, a `plans/` path, the handoff's `~/Documents` folder, a Windows home written in full, and a triage id in a commit message or a title
Enforced by: git hook — .githooks/commit-msg — the same forms in a commit message
Enforced by: none (prose only — CLAUDE.md § Working here) — every other spelling, the repository's files and the text of an issue

## Context
PR #191 cited the row label of a plan in its body and in ADR 0062's Context. The
plan was kept outside the repository, on the machine where the work was done, so
no reader of either could decode it. The class was older than that PR: commit
messages on `main` named cycle items and the handoff's directory, and pull
request bodies such as #169, #136 and #105 numbered the items of a cycle kept in
the same place. Issue text did the same, #88 among them, the issue where the
case was gathered. The only guard was an agent's memory on one machine, and ADR
0063 had ruled out one form, a triage id, in a PR body and a `review-debt` issue.

## Decision
Text written to the repository, a commit message, a pull request or an issue
cites only what a clone can open: a PR, an issue, an ADR or a repository path. It
never cites a label from a plan or a cycle kept outside the repository, nor a
path that exists on one machine; where one is needed, it says in words what it
meant. An issue is cited by its number, which resolves for any reader. A triage
id is one such label, so ADR 0063's line on `review-debt` issues is a case of
this rule.

The rule is a line in CLAUDE.md, since a commit message and an issue are written
in any session and no rule loaded by path reaches them. `rules.sh` rejects the
forms it recognises in commit messages and in a pull request's title and body: a
plan's branch label (`branch 3b`), a cycle (`cycle 4`), a `plans/` path, the
handoff's `~/Documents` folder, a Windows home written in full (`C:\Users\…`,
`/c/Users/…`), and a triage id in a commit message or a pull request's title.
Inline code does not exempt a reference, since a path is most often quoted; it
still exempts a token that only looks like a triage id.

## Consequences
A pattern matches one spelling: an item number with no cycle beside it, or a path
written another way, passed the check, and the CLAUDE.md line was what covered
them. It also rejected a count written after "branch" or "cycle", which its
author reworded; the 400 commits on `main` before it held no such message. A
Linux or macOS home was left out, since `/home/…` was also how an app route or a
deep link read, and a quoted route could not be reworded. The repository's files
were left unchecked, because the `checkpoint` Skill
and TOOLING named the handoff's directory on purpose. `clean-branch` and
`open-pr` gained no question of their own: the line was loaded every turn, and
the check's error named this ADR when it fired. Writing in words what a label
meant cost a few words per reference, and an issue's title stayed a courtesy for
the reader of a conversation rather than a requirement.
