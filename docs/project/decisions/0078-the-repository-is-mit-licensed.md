# ADR 0078 — The repository is MIT-licensed

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — README.md § License) — the license, and what it does not cover

## Context
The repository was public with no license. Under default copyright that reserves
every right: GitHub's *Licensing a repository* says no one may reproduce,
distribute or create derivative works from it, and the GitHub Terms of Service
§ D.5 leave others only viewing and forking it on GitHub. Half of the project is a
harness meant for other projects (CLAUDE.md § Purpose), which a reserved-rights
repository contradicted. The README's rewrite (#100) weighed the question against
choosealicense.com and the Apache License 2.0 text.

## Decision
License the repository under MIT, its text unmodified in `LICENSE` so GitHub
detects it. What MIT cannot cover is stated in the README's *License* section:
the game images in the screenshots belong to their owners, and the game data comes
from IGDB under its own terms, with its attribution and link.

## Consequences
The license said the question had been weighed, disclaimed warranty and liability,
and asked for attribution from anyone who copied the code. It restricted nothing:
restricting use was never its purpose. Apache-2.0 lost because its patent grant and
trademark clause served nothing here, while MIT was the shortest and the most
recognised. The cover-art exclusion could not live in the license file, which stays
unmodified so it is detected, so it lived in the README; the screenshots stayed,
unblurred. Where the app itself shows IGDB's attribution is Detail's question
(#106), outside this decision.
