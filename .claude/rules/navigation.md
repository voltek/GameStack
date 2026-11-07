---
paths:
  - "app/src/main/**/navigation/**"
  - "openspec/changes/**"
---

# Navigation
The bottom bar has exactly three destinations, Home, Search and Library, and Detail is
not one of them. Ask before changing that: it costs a lot once screens depend on it.

Each tab is a nested `*Graph` whose start destination is that tab's screen, which gives
it its own back stack (ADR 0010). So:
- `Detail` is registered inside every tab graph through `gameDetailDestination()`.
  Registered once at top level, it leaks across tabs.
- Bottom bar items and `navigateToBottomNavDestination()` target a `*Graph` route. A
  screen route stops the tab's stack from being what is saved and restored.

Back from a non-start tab goes to Home, then exits the app. That is intended.
