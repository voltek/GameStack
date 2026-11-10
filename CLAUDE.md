# CLAUDE.md — GameStack

## What is this project
GameStack is a native Android app for exploring video games via the IGDB API:
search, game details, personal lists and ratings. A catalog and personal library,
not a store or gaming platform. Scope and what is deferred: the PRD
(`docs/project/PRODUCT.md`). This document governs technical rules.

## Purpose
Half product, half a harness meant for other projects, and both a case study
(PRD → *Why this project exists*). Weigh every proposal and decision against
both halves and say what it gives each. A harness decision lives where a clone
reads it — an ADR, TOOLING.md, a Skill — never only outside the repo. Governance
that serves neither the next product step nor what another project would take
is a cost.

## Working here
- Before committing, run `./gradlew build`, `./gradlew test` and `./gradlew lint`;
  all three must be green, read from the exit code, which a pipe through `tail`
  hides. A lint Error fails the gate, a warning does not.
- Put code and its documentation in the same commit.
- Before pushing, `git fetch` and reconcile with `origin/main`.
- One branch per unit of work, checked per commit: does this exist because of this
  branch? Anything else goes on its own branch.
- Push your own feature branch without asking. Ask before every PR action GitHub
  shows.
- Never use `--no-verify`, never force-push `main` or a branch another person may
  hold, and never use bare `--force`, which overwrites the remote without checking.
  `--force-with-lease` may clean your own feature branch until it merges.
- Never change branch protection or repository settings.
- Touching the same code a third time for the same class of defect means the design
  is the problem: stop patching and redesign.
- Ask first: any decision the documents or an existing code pattern do not settle,
  such as a visual or UX choice, product scope, the name of a user-facing concept, or a trade-off with no
  single answer. Propose it and wait for approval, even when the request looks small.
- When code and a document disagree, say which is right and why, and put
  `Drift: code fixed`, `Drift: doc updated` or `Drift: both` on the commit that
  resolves it. A rule that is born or changes is a new ADR.
- Ship a branch by moment: `clean-branch` when its work is done, `review-round`
  before any review, `open-pr` before the PR, `merge-pr` before the merge.

## Code map
One `app` module: Jetpack Compose, MVI over Clean Architecture. `presentation` reaches
data only through `domain`, and `data` implements `domain`'s interfaces. `feature/{name}/`
holds what one feature owns, `core/` what two or more share. The rules for each area load
from `.claude/rules/` when a file there is touched.
