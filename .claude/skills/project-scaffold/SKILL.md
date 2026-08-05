---
name: project-scaffold
description: Bootstraps a new, empty Android project — dependencies, folder skeleton, Hilt setup, Theme (from DESIGN.md), and core testing/presentation utilities. Use ONCE, only when the project is new/empty and has no architecture set up yet.
---

## Dependencies
- Compose BOM
- Hilt + Hilt Navigation Compose
- Retrofit + Kotlinx.serialization
- Room
- Coil
- Navigation Compose
- Turbine, MockK, kotlinx-coroutines-test
- Add dependencies via the Gradle Version Catalog (`libs.versions.toml`) if
  the project already has one (Android Studio's default template does) —
  do not hardcode version strings directly in build.gradle.kts.
- Use KSP (Kotlin Symbol Processing) for Hilt and Room annotation processing,
  not the older KAPT.
- Always verify the latest stable version on the internet before adding —
  do not rely on a remembered version number.

## Folders
Create (empty at this stage — no subfolders per feature yet):
- `core/domain/`
- `core/data/`
- `core/presentation/`
- `ui/theme/`
- `feature/`
- `navigation/` — only if CLAUDE.md documents a navigation structure
  (see "Root navigation" below)

In the test source set (not main):
- `core/testing/` — for shared test utilities like `MainDispatcherRule`.

## Hilt setup
- `Application` class annotated `@HiltAndroidApp`, registered in the manifest.
- Do not create empty/placeholder Hilt modules (e.g. Network/Database modules)
  yet — bindings should be created later, when a real one is actually needed
  by feature work, not guessed in advance. That later work is owned by the
  `new-hilt-module` skill, so "later" has a defined home rather than being
  improvised ad hoc.

## Theme
Read the project's `docs/project/DESIGN.md` — it contains the real color,
typography, and spacing tokens for this specific project. Do not invent
values; every color and font must come from that file.

Create in `ui/theme/`:
- `Color.kt` — all colors from DESIGN.md's `colors` section
- `Type.kt` — Typography mapped to Material3 text roles, using DESIGN.md's
  `typography` section and its documented role mapping (see below)
- `Shape.kt` — corner radius values from DESIGN.md's `rounded` section
- `Theme.kt` — the app's Theme Composable, named after the project
  (e.g. `{ProjectName}Theme`), wrapping MaterialTheme.

### Units — DESIGN.md is authored in web units
Design tokens typically arrive in `px`/`rem` (the export format of design tools).
Android needs `dp`/`sp`. Convert, don't copy:
- `rem` → `dp` at **1rem = 16dp** (0.25rem = 4dp, 0.75rem = 12dp, 1.5rem = 24dp)
- `px` → `dp` for dimensions, `sp` for font sizes and line heights
- Spacing tokens already given in px map 1:1 to dp

### Fonts
If DESIGN.md names font families, they must be bundled as resources in
`res/font/` — a `fontFamily` string alone renders as the system default and the
mismatch is easy to miss. Download the licensed font files, add them, and
reference via `FontFamily(Font(R.font.{name}, ...))`. Prefer variable fonts
with `FontVariation.Settings(FontVariation.weight(n))` when the design calls for
several weights of one family, so one file covers all of them.

### Incomplete token sets
A design export rarely defines all ~15 M3 text roles. Map the roles DESIGN.md
does define; for the rest, leave the M3 default rather than inventing values.
If DESIGN.md's prose references a token its own token block doesn't define,
that's a documentation gap — surface it and ask, don't guess a value.

### Theme mode and dynamic color
Check the project's CLAUDE.md for whether it requires Dark-only, Light-only, or
both themes, and whether dynamic color (Material You wallpaper theming) is
allowed — do not assume any of these. Build exactly what's documented. Note that
dynamic color and a fixed brand palette are mutually exclusive in practice: if
CLAUDE.md pins the palette to DESIGN.md, do not add a `dynamicColor` parameter.

## MainDispatcherRule
Create in `core/testing/MainDispatcherRule.kt` (test source set) — the
standard rule used across ViewModel unit tests in this project:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
```

## UiText
Create in `core/presentation/UiText.kt` — for projects following a rule of
never hardcoding UI strings directly (check the project's CLAUDE.md for this
convention):

```kotlin
sealed class UiText {

    data class DynamicString(val value: String) : UiText()

    class StringResource(@param:StringRes val resId: Int, vararg val args: Any) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}
```

## Root navigation (conditional)
Check the project's CLAUDE.md for a documented navigation structure
(bottom nav, tabs, etc.). If one exists, implement the NavHost + container
exactly as documented there. If none exists, skip this step entirely —
do not assume or invent a navigation pattern.

## MainActivity
Android Studio's default template pre-populates `MainActivity.kt` with a
placeholder Composable and an unrelated sample theme. Replace its content
(do not delete the file) so that it:
- Wraps its content in the Theme Composable created above
- Hosts the NavHost from "Root navigation" if one was built, or a simple
  empty placeholder Composable if no navigation was documented yet
- Removes the default sample/placeholder content entirely

## Secrets safety check (verification only — no new code yet)
Confirm `local.properties` is listed in `.gitignore` (Android Studio's
default template usually includes this — verify, don't assume). This
project will likely store API credentials there later; checking this now,
before any secret exists, prevents an accidental leak later.

## Quality criteria
- All dependencies use the latest verified stable version — not a remembered one.
- Theme colors/typography/shapes come exclusively from DESIGN.md — no invented values.
- Every font family named in DESIGN.md has an actual file in `res/font/`.
- All design tokens are converted to `dp`/`sp` — no raw `rem`/`px` values in code.
- Theme mode (dark/light/both) and dynamic-color policy match exactly what the
  project's CLAUDE.md specifies — never assumed.
- Root navigation is only implemented if CLAUDE.md documents one — never invented.
- Before considering this skill complete, run `./gradlew build`. Do not report
  success if the build fails — surface the error and fix it, or ask for help
  if the fix isn't obvious.
- This skill runs once per project. If invoked on a project that already has
  Theme/Hilt/folders set up, stop and ask before overwriting anything.
