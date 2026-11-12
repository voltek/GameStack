# Screen tests

Read when the test drives a Compose screen.

Under `test/` with Robolectric, run by `./gradlew test`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class FooScreenTest {
    @get:Rule val composeRule = createComposeRule()
```

- No `@Config(sdk = …)`: the SDK is pinned once in
  `app/src/test/resources/robolectric.properties`, and a per-class annotation
  overrides that pin.
- Drive the `internal` `{Name}Content` with a `UiState`, collecting events into a
  list: no Hilt graph, no ViewModel, no mocks.
- Cover each state the screen declares, the list the device pass also uses
  (`verify-on-device`), and Composable-level logic no unit test reaches, such as a
  click guard or a `remember`ed derivation; also which controls are enabled and the
  semantics a node carries.
- A screen test cannot show what TalkBack speaks, gestures, the IME or a real device:
  asserting a `liveRegion` is declared is not asserting it is announced, so say which
  one the test proves.
- Screen tests cost about a second each, so adding the second screen-test class is
  the trigger of #67, which splits them into their own Gradle task.
