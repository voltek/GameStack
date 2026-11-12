# Room tests

Read when the test covers a write-path mapper, an enum Room stores or a
`@TypeConverter`.

Not exercised yet (#106): no Entity exists.
The first branch to run it corrects it and removes this note.

- **Write-path mapper.** `domain.toEntity().toDomain()` returns the original,
  including each nullable field unset. It proves the mapper's consistency, not how
  Room stores a column.
- **Stored enum.** One test asserting `entries.map { it.name }` equals the names as
  stored, whose failure message says that renaming or removing a stored constant
  needs the app's data cleared before the first release, and a version bump and a
  `Migration` after it (ADR 0068). Room stores the name and no schema check sees a
  rename, so this test is the only thing that does.
- **TypeConverter**, only for a type Room cannot store. Call its functions directly,
  without a `@Database`: value to stored form and back returns the original, and a
  stored form it cannot read has a documented result the test asserts.
