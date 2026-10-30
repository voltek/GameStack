# ADR 0044 — Learning outranks product completeness

Status: Accepted
Date: 2026-10-30
Backfilled: no
Enforced by: none (prose only — docs/project/PRODUCT.md § Why this project exists, and what it optimises for)

## Context
ADR 0039 made the purpose a rule: half product, half a harness, both a case
study, and every proposal weighed against both halves. It said nothing about a
product decision where serving users more fully and learning the path to a
release pulled different ways, such as an account or offline mode against
shipping what was already defined. In the PRD interview the owner said the app
had to be useful enough that publishing it meant something, but that learning
the whole path, from the architecture and the harness to a Play Store release,
was what it was for, and that some ideas might belong to another app.

## Decision
When serving users more fully and learning the path to a release disagree, the
path wins. The app stays useful enough that publishing it means something; past
that, scope that teaches nothing new on the way to a release waits or goes to
another app. This adds a tie-break to ADR 0039 and replaces none of it: a
proposal is still weighed against both halves, and says what it gives each.

## Consequences
A scope question with no other answer got one, and *Not in this app* became a
legitimate place for a feature rather than a refusal. Accepted knowingly: a
user-facing gap could ship because closing it taught nothing new, and the PRD,
not CLAUDE.md, carried the tie-break, so it reached a session only when that
session read the PRD.
