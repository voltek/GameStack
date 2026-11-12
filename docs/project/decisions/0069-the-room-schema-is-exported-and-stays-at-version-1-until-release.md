# ADR 0069 — The Room schema is exported, and stays at version 1 until release

Status: Accepted
Date: 2026-11-11
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Data Sources — `fallbackToDestructiveMigration()` anywhere, and a schema changed after the first release without a version bump and a `Migration`
Enforced by: none (prose only — .claude/skills/new-room-dao/SKILL.md § Database) — the exported schema committed, and app data cleared on a development device after a schema change

## Context
No Room code existed yet; Detail (#106) was to create the first Database, holding the
user's lists and ratings. The build declared `room-runtime`, `room-ktx` and
`room-compiler`, not the Room Gradle plugin, so Room would have exported no schema.
Room's migration guide said the exported JSON should be kept in version control to
test migrations and generate automated ones. `new-room-dao` had the plugin set up by
its first run, a step that stopped repeating once done, and allowed
`fallbackToDestructiveMigration()` with a version bump per schema change until
release. Room stores a hash of the schema in the database file and refuses to open
one whose hash differs from the code's at the same version; `./gradlew installDebug`
keeps the app's data. No user had any data before the first release (PRODUCT.md §
First release).

## Decision
- The build applies the Room Gradle plugin, declared `apply false` at the root like
  every plugin, with `schemaDirectory` set to `app/schemas/`. Each exported schema is
  committed with the change that produced it.
- Until the first release the Database stays at version 1: a schema change rewrites
  `1.json`, and a development device clears the app's data
  (`adb shell pm clear com.gamestack`) before the next launch.
- From the first release on, every schema change bumps the version and ships a
  `Migration`. The release is recorded in PRODUCT.md § First release, with the
  committed schema checked against the build that ships (#114).
- `fallbackToDestructiveMigration()` is never used.

## Consequences
The version the first release was to ship stayed 1, with no versions nobody
installed. A reinstall kept the data a test had saved, unless the
schema had changed; then Room's error said so, and the data was cleared and entered
again. Keeping test data across a schema change before release would have needed a
`Migration` nobody would ship, and was not provided. The plugin was in the build
before any Database used it, so a project taking the harness found the setup in the
build files instead of in a Skill step that ran once.
