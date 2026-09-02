# ADR 0001 — No dynamic color; the palette is the fixed one

Status: Accepted
Date: 2026-08-04
Backfilled: yes — DRIFT-LOG.md, row 2026-08-04 ("DESIGN.md asked for dynamic color")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Tech Stack

## Context
DESIGN.md's prose asked for Material You wallpaper-based theming while `Theme.kt`
had never implemented it, and nothing said which of the two was authoritative.
The violet palette is not decoration — it is what carries the "Stack" brand
identity, and dynamic color hands the OS the right to override it from whatever
wallpaper the user happens to have set. It would also make DESIGN.md
non-authoritative for what actually renders, which defeats the point of having a
design document at all.

## Decision
The app uses no dynamic color. `GameStackTheme` takes no `dynamicColor`
parameter and calls no `dynamicDarkColorScheme()`. The palette is the fixed one
DESIGN.md defines, and DESIGN.md is authoritative for it.

## Consequences
What renders is knowable from a document rather than from the device it renders
on, so a colour question is answered by reading DESIGN.md and a screenshot can
be compared against a mockup at all.

The cost, accepted knowingly: the app declines a platform personalization
feature that some users expect, and every future theming question has to be
settled against DESIGN.md instead of inherited free from Material.

The code was already right here — this ADR records the document being brought
into line with it, not a change of behaviour.
