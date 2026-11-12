---
name: verify-screen-on-device
description: Verify a screen on an emulator — launching the agent's own emulator, installing and driving the app, choosing between a window dump and a screenshot, covering every device scenario and every state the screen declares, and opening an issue for a defect found. Use whenever a change adds or alters a screen, before asking for a merge, and for an exploratory pass over a merged screen.
---

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
tap() {
  [ $# -eq 2 ] || { echo 'tap: x y' >&2; return 1; }
  "$ADB" emu event mouse "$@" 0 1 >/dev/null; "$ADB" emu event mouse "$@" 0 0 >/dev/null
}
drag() {   # x, from y, to y: a vertical drag, such as a pull or a scroll
  [ $# -eq 3 ] || { echo 'drag: x from-y to-y' >&2; return 1; }
  local x=${@:1:1} from=${@:2:1} to=${@:3:1} i
  "$ADB" emu event mouse "$x" "$from" 0 1 >/dev/null
  for i in 1 2 3 4 5 6 7 8 9 10; do "$ADB" emu event mouse "$x" $(( from + (to - from) * i / 10 )) 0 1 >/dev/null; done
  "$ADB" emu event mouse "$x" "$to" 0 0 >/dev/null
}
```

`drag` reads its arguments as `${@:1:1}` and the device loop below uses
`grep -oE`, not `awk`: a `$` followed by a digit in a Skill is replaced with a
word of the Skill's arguments before the agent reads it.

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
Studio → Device Manager, any phone profile, API 30 or later, which
`uiautomator dump --windows` needs). Never fall back to another AVD.

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

**Then launch it**, headless by default, from a background shell (`&` dies with
the command that started it). A fixed port fixes its serial, `emulator-5580`,
whatever else is attached:

```bash
"$EMULATOR" -avd GameStack_Verify -port 5580 -wipe-data -no-snapshot -no-window -no-boot-anim
```

Add `export ANDROID_SERIAL=emulator-5580` to `device.env` before anything else
talks to the device: `adb` and `installDebug` both read it, and without it
`installDebug` installs on every attached device, the human's emulator or phone
included. Then wait for the boot, about a minute:

```bash
for i in $(seq 180); do
  [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 1
done
"$ADB" shell getprop sys.boot_completed   # anything but 1: it did not boot
[ "$("$ADB" shell getprop ro.build.version.sdk | tr -d '\r')" -ge 30 ] || echo 'below API 30: ask the human to recreate GameStack_Verify'
```

Below API 30, `uiautomator` ignores `--windows`, and the dump never shows the
keyboard: close the emulator and ask, as for a missing AVD.

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

The app's data lives as long as the launch: `installDebug` reinstalls over it,
keeping it, so a fix can be installed onto the state a pass built. A state that
needs stored data, such as a library with games in it, is built in the pass
through the UI, since the next launch starts wiped. Not exercised yet (#106):
no screen shows data the user stored. The first branch to run it corrects it
and removes this note.

## Driving the app

```bash
"$ADB" shell input tap 540 425
"$ADB" shell input swipe 540 700 540 1600 500   # a pull to refresh
"$ADB" shell input text 'the%switcher'          # %s is a space
"$ADB" shell input keyevent KEYCODE_BACK
```

`input` injects its touches past the emulator's touchscreen: a swipe flings as
a finger does, and nothing on screen shows the touch.

### Touches the human can see
When the emulator has a window or a recording runs, turn on Show taps, which
the wipe resets, and touch with `tap` and `drag` from `device.env`, which go
through the touchscreen as the human's mouse does, so Show taps draws them:

```bash
"$ADB" shell settings put system show_touches 1
tap 540 425
drag 540 700 1600                               # x, from y, to y
```

Show taps draws a touch only while it is held, so a capture taken after it
shows nothing of it. `drag` moves its distance less the touch slop and stops:
the console delivers its moves about 36 ms apart, too far apart for a fling. A
check that needs a fling uses `input swipe`, which is not drawn.

`input text` types ASCII only: one character outside it fails the whole
command, and nothing is typed.

### Two tools, two questions
**`Grep` over a window dump** — for assertions that can be stated in advance:
which tab is `selected="true"`, whether a text or node exists, what a node's
bounds are, whether the keyboard is up. Only matching lines enter context, so
it is by far the cheaper path.

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump --windows /sdcard/d.xml
MSYS_NO_PATHCONV=1 "$ADB" shell cat /sdcard/d.xml | tr '>' '\n' | grep 'package="com.gamestack"' | grep -oE 'text="[^"]+"'
```

`--windows` dumps every window on screen; without it the dump holds only the
app's, and reports as visible the controls the keyboard covers. The status
bar's clock and the keyboard's keys are text nodes too, so an assertion about
the app keeps only the lines of its package. A dump is one instant: a node
still animating in is missing from it. Dump again before concluding that
something is absent.

**`"$ADB" exec-out screencap -p > <file>.png`**, read as an image — when the
criterion is visual (clipping, spacing, contrast, alignment).

### The keyboard
While the keyboard is up, the dump holds a window of type `TYPE_INPUT_METHOD`,
and its bounds are what it covers: a tap aimed at a node inside them lands on a
key. **Before tapping from dump coordinates while a text field may hold focus,
dump again and look for it:**

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump --windows /sdcard/d.xml
MSYS_NO_PATHCONV=1 "$ADB" shell cat /sdcard/d.xml | tr '>' '\n' | grep -oE 'bounds="[^"]+"[^>]*TYPE_INPUT_METHOD|type="TYPE_APPLICATION"'
```

`TYPE_APPLICATION` alone: the keyboard is hidden. A line of bounds as well: it
is up, over those bounds. No output: the dump failed, so dump once more; still
nothing, take a screenshot. Only while the keyboard is up does `KEYCODE_BACK` hide it
without navigating; with it hidden, Back navigates, and twice leaves the app.
When how the keyboard covers a control is the question, take a screenshot.

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
headless and stops at 180 seconds per file. Start it from a background shell
and wait until `"$ADB" shell pgrep screenrecord` finds it: stopped before its
first frame, the file is empty. Drive the app, then stop it with an interrupt,
which finalises the file (killing the shell leaves it unplayable), and pull it:

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
4. **A screen the branch adds** also gets the exploratory pass below (ADR 0075).

**Close every verification with at least one screenshot.** The dump cannot find
what nobody thought to assert, and a purely visual defect is the common case.

A commit after the pass that alters a screen, such as a review's fix or a merge
of `main` that touches it, repeats the pass for the states it changed before
the merge is requested: what was verified is what merges (ADR 0075).

## Exploratory pass
For what no scenario names: over a screen a branch adds, and over a merged
screen. Cover what the section above covers, then what has found defects
before:

- **The system in light mode** (`"$ADB" shell cmd uimode night no`): the app
  is dark-only, but whatever it leaves to the system follows the system.
- **A configuration change.** The app is portrait-only (ADR 0019), so toggle
  `cmd uimode night yes` and back: the Activity is recreated and the screen's
  state must survive it.
- **Process death.** Home, `"$ADB" shell am kill com.gamestack`, which kills
  only a process in the background, then reopen from Recents
  (`KEYCODE_APP_SWITCH`, tap the card). Not from a launcher intent: a task
  started with `am start -n` has another root intent, so the launcher opens a
  new task and nothing looks restored.
- **From every state:** offline and back, Back, a tab switch and return, two
  rapid taps, a tap while the keyboard is up.
- **Every way to retry** (the Retry button, a pull, the keyboard's action key),
  both still offline and once the network is back: a retry that changes nothing
  on screen reads as a broken app.
- **Input at the edges:** whitespace only, quotes, a long string.
- **Wrong data is not yet a defect.** Send IGDB the same query (token from
  `id.twitch.tv/oauth2/token` with the `local.properties` credentials, never
  printed) to tell IGDB's own data from a mapping bug.

## A defect found
One the branch's own change causes is fixed in the branch. Any other confirmed
defect becomes an issue with every heading of `.github/ISSUE_TEMPLATE/bug.yml`
in its body (ADR 0048), opened with `--label product --label bug`, since
`gh issue create` applies no form's labels. `gh` cannot attach the capture that
shows it: hand its path to the human with the pass's other captures.

## Finishing
- **Close `GameStack_Verify`**: `"$ADB" emu kill`. The next launch wipes it, so
  nothing the pass changed needs restoring.
- Save the captures in `captures/<branch>/` beside the handoff (the
  `checkpoint` Skill's § What is kept), named for what they show, never in
  `%TEMP%` or a scratchpad, which get cleaned, and never commit them; the
  SessionStart hook deletes the folder once the branch is gone. Hand the
  paths over in chat and say plainly that the PR needs them dragged into its
  body's edit box on the web before the merge: that is the one part only the
  human can finish. They go in as they are: an attachment is not stored in the
  repository, so nothing needs compressing.
- The PR body lists the scenarios and states checked, not merely that something
  was.
