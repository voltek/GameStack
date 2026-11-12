# Seeing a regression test fail

Read when a test guards a fix that was written before it.

Why a regression test must be seen to fail, and that the commit says how:
`.claude/rules/tests.md`. A test for brand-new code has no previous behaviour to fail
against; writing the test first and watching it fail gives the same guarantee more
cheaply.

When the old code still compiles, park the fix:

```bash
git stash push -- app/src/main/java/com/gamestack/<path>/Foo.kt   # the fix, never the test
./gradlew testDebugUnitTest --tests "com.gamestack.<path>.FooTest"  # must fail
git stash pop
```

When it does not compile, because the fix removed a `UiEffect`, renamed a field or
changed a signature, break the fix instead: delete or invert one line the new test
claims to cover, run the class, and restore the line. Exactly that test must fail: if
none fails, it proves nothing; if several do, the mutation was too broad to
attribute. This is hand-run mutation testing; PIT, its automated form, is in #64.

Either way:
- Read the failure message: it should describe the bug. A compile error, an NPE or
  MockK's `no answer found for …` is red for the wrong reason. The last is common
  here: `runTest` runs pending work after the body, so a debounced query never meant
  to be dispatched still reaches the mock; stub it, with a comment that the stub
  exists only for that drain.
- Run the one class, not the suite, so the failure is the test's and not something
  disturbed.
