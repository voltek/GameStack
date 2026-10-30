# ADR 0042 — A document wins over a mockup

Status: Accepted
Date: 2026-10-29
Backfilled: no
Enforced by: none (prose only — docs/project/design/README.md § When a mockup and a document disagree)

## Context
The exports under `docs/project/design/` were prototyped in Stitch before any
screen had a written spec, and Stitch allowed few edits, so an export could keep
content the documents never agreed to. Detail's export showed a price per store,
with no IGDB source checked for it. ADR 0012 kept the exports describing the app
that existed, but nothing said which side won when an export and a document
disagreed, so a reader building a screen could take either.

## Decision
When a mockup and a document disagree, the document wins, each for its own part:
behaviour and content from the capability spec (the Spec where a screen has
none yet), visual tokens from DESIGN.md, and layout from the export. A mismatch
is fixed in the export's HTML by hand and its PNG re-rendered, never
regenerated through Stitch. A new state still ships its mockup (ADR 0012).

## Consequences
A screen's spec could be written and approved without first matching an
export, and the export followed it. Accepted knowingly: until a screen had a
capability spec, its Spec bullets were thin enough that the export still
answered most layout and content questions, and a mismatch fixed by hand
depended on the render recipe still working.
