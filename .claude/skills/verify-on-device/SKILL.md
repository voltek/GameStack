---
name: verify-on-device
description: Verify a UI change on an emulator — getting a device, installing and driving the app, choosing between a uiautomator dump and a screenshot, and covering every device scenario and every state a screen declares. Use whenever a change adds or alters a screen, before asking for a merge.
---

## Why this exists
`build`/`test`/`lint` proves the code compiles and that the logic does what a
test asserts. It never proves a screen looks right or that a control is
reachable. Every UI defect in this project so far — a premature empty state, a
keyboard covering a button, clipped genre text, a banner's contrast — was
invisible to that gate. No UI change is done without this pass.

Data or domain work with no UI change does not need this pass.

## Before you start
Find the SDK. `adb` is rarely on PATH; these are Android Studio's default
locations, and `sdk.dir` in `local.properties` names any other:

```bash
for d in "${ANDROID_HOME:-}" "${LOCALAPPDATA:-}/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
  [ -d "$d/platform-tools" ] && SDK="$d" && break
done
[ -n "${SDK:-}" ] || { echo 'SDK not found: take its path from sdk.dir in local.properties'; return 1; }
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
```

**Each Bash call starts a fresh shell**, so nothing set in one reaches the
next. Write these lines to a `device.env` in your scratchpad, add the
`ANDROID_SERIAL` export to it once the device is chosen, and begin every call
with `. <scratchpad>/device.env`.

**Check the IGDB credentials.** `local.properties` must hold non-empty
`IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` (README → Running the project).
Without them the build falls back to empty values, installs, and every search
lands on the error state — that is setup, not a defect. If they are missing, ask.

```bash
grep -E '^IGDB_CLIENT_(ID|SECRET)[[:space:]]*=[[:space:]]*[^[:space:]]' local.properties | cut -d= -f1 | tr -d ' ' | sort -u | wc -l   # must print 2
```

On Git Bash (Windows), prefix any command with a device-side path
(`/sdcard/...`) with `MSYS_NO_PATHCONV=1`, or it is rewritten into a Windows
path. That also stops the conversion of the *local* path, so `adb pull` into
`.` after a `cd` to the destination.

## Getting a device
**Only `GameStack_Verify` is ever used, and it is the agent's alone**
(ADR 0046). Any other emulator is the human's, even one already running: a
pass clears app data and changes the network and the system's night mode, so
never select it, never close it.

If `GameStack_Verify` does not exist, ask the human to create it once (Android
Studio → Device Manager, any phone profile, API ≥ `minSdk`). Never fall back to
another AVD.

```bash
"$EMULATOR" -list-avds | tr -d '\r' | grep -qx GameStack_Verify || echo 'GameStack_Verify does not exist'
```

**If it is already running, close it first**: it is a pass left behind, and
reusing it would carry over whatever that pass changed.

```bash
for s in $("$ADB" devices | grep -oE '^emulator-[0-9]+'); do
  [ "$("$ADB" -s "$s" emu avd name | head -1 | tr -d '\r')" = GameStack_Verify ] || continue
  "$ADB" -s "$s" emu kill
  for i in $(seq 30); do "$ADB" devices | grep -q "^$s" || break; sleep 1; done
done
```

(Not `awk`: a `$` followed by a digit in a Skill is replaced with a word of the
Skill's arguments before the agent reads it, so field references break.)

**Then launch it**, headless by default, from a background shell (`&` dies with
the command that started it). A fixed port fixes its serial, `emulator-5580`,
whatever else is attached:

```bash
"$EMULATOR" -avd GameStack_Verify -port 5580 -wipe-data -no-snapshot -no-window -no-boot-anim
```

Add `export ANDROID_SERIAL=emulator-5580` to `device.env` before anything else
talks to the device: every `adb` command and `installDebug` read it, so the
human's emulator or a phone attached at the same time is never picked. Then
wait for the boot, about a minute:

```bash
for i in $(seq 180); do
  [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 1
done
"$ADB" shell getprop sys.boot_completed   # anything but 1: it did not boot
```

If it did not boot in those three minutes, read the background shell's output
rather than waiting again. A busy port: launch on another even port from 5554
to 5682 and change `ANDROID_SERIAL` to match. Anything else (no hypervisor):
run it with a window and look.

- **Drop `-no-window`** when the human asks to watch, and on the first run
  after this Skill's procedure changes: a watched run is how a blind spot no
  rule names yet gets found.
- **`-wipe-data -no-snapshot`** starts every launch from a factory-fresh
  device, so no pass inherits another's network, night mode or data. It costs
  about half a minute per launch, measured against a cold boot that keeps the
  data.

## Install and launch

```bash
./gradlew installDebug
"$ADB" shell pm clear com.gamestack   # mid-pass only: back to first run, Room data included
"$ADB" shell am start -W -n com.gamestack/.MainActivity
```

## Driving the app

```bash
"$ADB" shell input tap 540 425
"$ADB" shell input text 'the%switcher'     # %s is a space
"$ADB" shell input keyevent KEYCODE_BACK
```

### Two tools, two questions
**`Grep` over a `uiautomator dump`** — for assertions that can be stated in
advance: which tab is `selected="true"`, whether a text or node exists, what a
node's bounds are. Only matching lines enter context, so it is by far the
cheaper path.

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump /sdcard/d.xml
MSYS_NO_PATHCONV=1 "$ADB" shell cat /sdcard/d.xml | tr '>' '\n' | grep -oE 'text="[^"]+"'
```

A dump is one instant: a node still animating in is missing from it. Dump again
before concluding that something is absent.

**`"$ADB" exec-out screencap -p > <file>.png`**, read as an image — when the
criterion is visual (clipping, spacing, contrast, alignment).

### The keyboard
The dump contains only the app window, never the IME. It reports as visible
controls the keyboard is actually covering, so taps aimed from dump coordinates
land on keyboard keys. **Before tapping from dump coordinates while a text
field may hold focus, ask the window manager:**

```bash
"$ADB" shell dumpsys window InputMethod | grep -m1 -oE 'isVisible=(true|false)'
```

Anything but `false`, no output included, counts as visible: take a screenshot.
If `true`, `KEYCODE_BACK` hides the keyboard without navigating. Never send it
unless the probe says `true`: with the keyboard hidden it navigates, and twice
leaves the app. Use this probe, not `dumpsys input_method`, which reported the
keyboard hidden while it was up. When what the keyboard covers *is* the
question, take a screenshot.

### The network
Force network-dependent states, then restore the network before going on: it
takes a few seconds to come back, and a later step that runs before it fails
for a reason that has nothing to do with the code.

```bash
"$ADB" shell svc wifi disable; "$ADB" shell svc data disable
# ... verify ...
"$ADB" shell svc wifi enable; "$ADB" shell svc data enable
"$ADB" shell 'for i in $(seq 30); do ping -c1 -W2 8.8.8.8 >/dev/null 2>&1 && exit 0; sleep 1; done; exit 1'
```

A non-zero exit can mean the network only drops ping: check that the app loads
before calling it offline.

### Recording
For a run the human will review afterwards instead of watching. It works
headless, records only frames that change, and stops at 180 seconds per file.
Start it from a background shell and wait until
`"$ADB" shell pgrep screenrecord` finds it: stopped before its first frame, the
file is empty. Drive the app, then stop it
with an interrupt, which finalises the file (killing the shell leaves it
unplayable), and pull it:

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell screenrecord --time-limit 180 /sdcard/pass-1.mp4   # background shell
"$ADB" shell pkill -INT screenrecord
MSYS_NO_PATHCONV=1 "$ADB" pull /sdcard/pass-1.mp4 .
```

## What to cover
1. **Every scenario whose verifier is the device** (ADR 0041): in the change's
   delta (`openspec/changes/<change>/specs`) if the branch carries one,
   otherwise in the capability's spec (`openspec/specs/<capability>`). A
   capability with no spec yet has its behaviour in `PRODUCT.md`'s
   *Capabilities* bullets: cover those.

   ```bash
   grep -rhE '^#### Scenario|VERIFIED BY\*\* device' <specs folder> | grep -B1 'VERIFIED BY\*\* device' | grep Scenario
   ```

2. **Every state the screen declares** — initial, loading, content, empty,
   error, and the keyboard raised when there is a text field — including those
   a screen test already asserts: the test proves semantics, only the device
   sees the pixels.
3. **A state with no deterministic way to reach it on the device** is asserted
   by a Compose screen test (`write-tests`); a `@Preview` lets the human look.

**Close every verification with at least one screenshot.** The dump cannot find
what nobody thought to assert, and a purely visual defect is the common case.

## Exploratory pass
A pass over a merged screen, for what no scenario names. Cover what the
section above covers, then what has found defects before:

- **The system in light mode** (`"$ADB" shell cmd uimode night no`): the app
  is dark-only, but whatever it leaves to the system follows the system.
- **A configuration change.** The app is portrait-only (ADR 0019), so toggle
  `cmd uimode night yes` and back: the Activity is recreated and the screen's
  state must survive it.
- **Process death.** Home, `"$ADB" shell am kill com.gamestack`, then reopen
  from Recents (`KEYCODE_APP_SWITCH`, tap the card). Not from a launcher
  intent: a task started with `am start -n` has another root intent, so the
  launcher opens a new task and nothing looks restored.
- **From every state:** offline and back, Back, a tab switch and return, two
  rapid taps, a tap while the keyboard is up.
- **Every way to retry** (the Retry button, a pull, the keyboard's action key),
  both still offline and once the network is back: a retry that changes nothing
  on screen reads as a broken app.
- **Input at the edges:** whitespace only, quotes, a long string. `input text`
  types ASCII only.
- **Wrong data is not yet a defect.** Send IGDB the same query (token from
  `id.twitch.tv/oauth2/token` with the `local.properties` credentials, never
  printed) to tell IGDB's own data from a mapping bug.

Each confirmed defect becomes an issue in the shape of
`.github/ISSUE_TEMPLATE/bug.yml` (ADR 0048), opened with `--label product
--label bug`, since `gh issue create` applies no form's labels, its Horizon
proposed under the form's heading, with the capture that shows it,
saying whether a spec covers the behaviour: if none does, its fix carries a
scenario (`ship-a-branch`).

## Cost
Screenshot cost is set by **aspect ratio, not AVD resolution** — the long edge is
clamped before billing. Shrinking the AVD saves nothing. See
`docs/project/design/README.md`.

## Finishing
- **Close `GameStack_Verify`**: `"$ADB" emu kill`. The next launch wipes it, so
  nothing the pass changed needs restoring.
- Save the captures outside the repo, named for what they show, and hand the
  paths over in chat — see `ship-a-branch` → Screenshots. They go into the PR
  as they are: an attachment is not stored in the repository, so nothing needs
  compressing.
- The PR body lists the scenarios and states checked, not merely that something
  was.
