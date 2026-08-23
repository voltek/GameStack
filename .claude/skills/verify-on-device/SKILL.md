---
name: verify-on-device
description: Verify a UI change on the emulator — installing, driving the app, choosing between a uiautomator dump and a screenshot, and covering every state a screen declares. Use whenever a change adds or alters a screen, before asking for a merge.
---

## Why this exists
`build`/`test`/`lint` proves the code compiles and that the logic does what a
test asserts. It never proves a screen looks right or that a control is
reachable. Every UI defect in this project so far — a premature empty state, a
keyboard covering a button, clipped genre text, a banner's contrast — was
invisible to that gate.

Data or domain work with no UI change does not need this pass.

## Getting a device
`adb` is not on PATH. Use `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`.

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
MSYS_NO_PATHCONV=1 "$ADB" devices
ANDROID_SERIAL=emulator-5554 ./gradlew installDebug
```

`MSYS_NO_PATHCONV=1` matters on any command with a device-side path
(`/sdcard/...`), or Git Bash rewrites it into a Windows path.

**If no emulator is running, ask for one.** Do not launch an AVD unprompted — it
opens a window on the user's desktop. Do not declare a UI feature done without
this pass.

## Two tools, two questions
**`Grep` over a `uiautomator dump`** — for assertions that can be stated in
advance: which tab is `selected="true"`, whether a text or node exists, what a
node's bounds are. Only matching lines enter context, so it is by far the
cheaper path.

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump /sdcard/d.xml
MSYS_NO_PATHCONV=1 "$ADB" shell cat /sdcard/d.xml | tr '>' '\n' | grep -oE 'text="[^"]+"'
```

**`adb exec-out screencap -p`**, read as an image — when the criterion is visual
(clipping, spacing, contrast, alignment) and **whenever a keyboard may be on
screen**.

> The dump contains only the app window, never the IME. It reports as visible
> controls that the keyboard is actually covering. This cost real debugging time
> once: taps aimed from dump coordinates landed on keyboard keys.
> `dumpsys input_method` is unreliable on this AVD for the same question.

**Close every verification with at least one screenshot.** The dump cannot find
what nobody thought to assert, and three of the Search bugs were purely visual.

## Cover every state the screen declares
Initial, loading, content, empty, error — plus the keyboard raised if there is a
text field. Any state without a deterministic way to reach it should also have a
`@Preview`.

Force network-dependent states:

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell svc data disable
MSYS_NO_PATHCONV=1 "$ADB" shell svc wifi disable
# ... verify ...
MSYS_NO_PATHCONV=1 "$ADB" shell svc wifi enable
MSYS_NO_PATHCONV=1 "$ADB" shell svc data enable
```

Always restore the network before finishing — the next run will otherwise fail
for a reason that has nothing to do with the code.

## Cost
Screenshot cost is set by **aspect ratio, not AVD resolution** — the long edge is
clamped before billing. Shrinking the AVD saves nothing. See
`docs/project/design/README.md`.

## Finishing
Save the captures outside the repo, named for what they show, and hand the paths
over in chat — see `ship-a-branch` → Screenshots. The PR body says what was
checked, not merely that something was.
