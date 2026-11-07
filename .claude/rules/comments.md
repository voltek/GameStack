---
paths:
  - "app/src/**/*.kt"
  - "**/*.gradle.kts"
  - "**/*.sh"
  - ".githooks/**"
  - "**/*.toml"
  - "**/*.properties"
  - "**/*.yml"
  - "**/*.yaml"
  - ".editorconfig"
  - ".gitattributes"
  - ".claude/hooks/**"
---

# Comments
A comment carries the why, never the what or the history. Before leaving one, ask:
1. Does it restate the code? Delete it.
2. Is it about how we got here? That belongs in the commit message.
3. Would a correct refactor make it false? Rewrite it as the constraint, or delete it.

Prefer a name, a type or a test: a test fails when the invariant breaks, and a comment
cannot.

A block holds at most 4 lines, next to the line it constrains, in every hand-written
file except documentation; generated or template text is exempt. A reason that needs
more goes to a test, an ADR, TOOLING.md or the owning Skill, named in a one-line
pointer, or to the commit body (ADR 0037). Past about 50 lines, a file with more than
15% comment lines is where to look first.
