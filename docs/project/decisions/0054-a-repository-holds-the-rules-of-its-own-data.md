# ADR 0054 — A Repository holds the rules of its own data

Status: Accepted
Date: 2026-11-06
Backfilled: no
Supersedes: ADR-0002 — "as a documented exception to" (Decision)
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Architecture

## Context
"Repositories contain no business logic" came with the Clean Architecture scaffold and
needed an exception as soon as a write depended on stored state: ADR 0002's timestamps.
Android's data layer guidance lists "Containing business logic" among a repository's
tasks, and treats the domain layer as optional. The project keeps that layer, but the
rule's absolute wording made ordinary data work look like a breach.

## Decision
Product rules live in UseCases. A Repository holds the rules of its own data: where it
comes from and how a write changes what is stored. ADR 0002's timestamps are such a
rule, not an exception to one. A rule needing more than old and new stored state still
goes to a UseCase, the boundary ADR 0002 drew.

## Consequences
The layers kept their shape, and no code changed. The wording stopped contradicting the
platform's guidance, and a reviewer judged a Repository by what its rule depends on
rather than by whether it held logic at all. The line stayed a judgement: nothing
mechanical could tell a product rule from a data rule.
