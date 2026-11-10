---
paths:
  - "openspec/specs/**"
---

# The merged specs
Until a change is archived, behaviour it changes goes into its delta under
`openspec/changes/<name>/`, never into `openspec/specs/`, whoever found the fix, the
GitHub bot included. The archive merges the delta into these files, so a requirement
written here first collides with the one the delta adds (ADR 0041).
