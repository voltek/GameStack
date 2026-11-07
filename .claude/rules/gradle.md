---
paths:
  - "**/*.gradle.kts"
  - "gradle/**"
---

# Build files
Before adding or updating a library, read its `maven-metadata.xml` and sort the
versions. A remembered version is usually stale, and Maven Central's search does not
order by version.
