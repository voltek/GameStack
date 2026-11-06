---
paths:
  - "app/src/test/**"
  - "app/src/androidTest/**"
  - "app/build.gradle.kts"
  - "gradle/libs.versions.toml"
  - "openspec/changes/**"
---

# Writing tests
Conventions per layer, naming and Happy/Sad paths are in the `write-tests` Skill.

## Stack
Turbine, MockK and TestDispatcher are baked into `write-tests`' patterns, so changing
one breaks consistency across every test. ViewModel tests use `MainDispatcherRule`
(`core/testing/`) instead of calling `Dispatchers.setMain()`/`resetMain()` by hand.

Run one class with `./gradlew testDebugUnitTest --tests "com.gamestack.Foo"`: plain
`test` is a lifecycle task and rejects `--tests`.

## What runs where
Everything under `test/` runs on the JVM, with no emulator, real database or network,
so `./gradlew test` stays fast enough to run on every change. What needs a real device
(a Room DAO against SQLite, real gestures or IME) is an instrumented test under
`androidTest/`, still deferred: issue #132 lists the triggers. So a DAO interface is
verified indirectly, through Repository tests with a mocked DAO.

## Robolectric
Robolectric is for Compose screen tests and nothing else: not for DAOs against real
SQLite, and never as a substitute for a test that already runs pure. Its cost scales
with the number of tests rather than amortising across them. The second screen-test
class is the trigger to split the suites into separate Gradle tasks.
Measure when you need it: `./gradlew testDebugUnitTest --rerun-tasks`, then read
`app/build/test-results/testDebugUnitTest/TEST-*.xml`.

A screen test asserts what renders in each declared state and what semantics a node
carries. It cannot assert that TalkBack speaks an announcement, or anything gesture-
or IME-dependent: those need a device.

## A regression test is seen to fail
A test written after the fix can pass whether or not the fix is there, and then it
looks like coverage and proves nothing. So see it fail against the code it guards,
with a message that describes the bug: a compile error or an NPE is red for the wrong
reason. State in the commit which method was used, because "the tests pass" is not by
itself evidence of anything. The recipes, for a patch and for a redesign: `write-tests`.
