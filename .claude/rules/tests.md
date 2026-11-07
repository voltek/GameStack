---
paths:
  - "app/src/test/**"
  - "app/src/androidTest/**"
  - "openspec/changes/**"
---

# Writing tests

## What runs where
Everything under `test/` runs on the JVM, with no emulator, real database or network,
so `./gradlew test` stays fast enough to run on every change. What it cannot cover is
deferred, with its triggers in issue #132, so a DAO is verified indirectly, through
Repository tests with a mocked DAO.

## A regression test is seen to fail
A test written after the fix can pass whether or not the fix is there, and then it
looks like coverage and proves nothing. So see it fail against the code it guards,
with a message that describes the bug: a compile error or an NPE is red for the wrong
reason. State in the commit which method was used, because "the tests pass" is not by
itself evidence of anything. The recipes, for a patch and for a redesign: `write-tests`.
