# ADR 0004 — Hilt bindings stay private unless they are `@Singleton`

Status: Accepted
Date: 2026-08-05
Backfilled: yes — DRIFT-LOG.md, row 2026-08-05 ("Hilt visibility rule")
Enforced by: none (prose only — CLAUDE.md § Code Conventions)

## Context
Two rules collided the first time they were applied together. "Keep a
`@Provides` private if nothing outside the module injects it" is about not
widening the DI graph for no reason. Applied literally to `NetworkModule`'s
OkHttp and Retrofit providers it would have produced one client per consumer,
because Hilt's scope applies to *bindings* and not to functions: a private
helper is called again at every call site, and `@Singleton` on it means nothing.
The code was already right; the rule as written would have broken it.

## Decision
Keep a provider private when nothing outside its module injects it directly —
**except** that anything which must be `@Singleton` stays a binding even when
only same-module consumers inject it. The dividing line is whether a duplicate
instance would be harmful.

## Consequences
The DI graph stays as narrow as it can be without silently multiplying objects
that exist to be shared, and the exception states the mechanism rather than the
symptom, so the next module does not have to rediscover it.

The cost: the visibility question can no longer be answered from the call sites
alone — it needs the lifetime too, which is one more thing to think about per
provider.
