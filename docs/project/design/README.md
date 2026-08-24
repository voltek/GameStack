# Design references

One folder per screen (`home/`, `search/`, `library/`, `detail/`) plus `states/`
for the shared loading/empty/error states. Each screen keeps the approved Stitch
export as `{state}.html` and its render as `{state}.png`.

The PNG is what most readers actually look at, so it must agree with the HTML
next to it. Regenerate it rather than re-screenshotting by hand — a manual
capture depends on window size, zoom and OS scaling, none of which are recorded
anywhere, so the next person cannot reproduce it.

## Rendering a mockup to PNG

Headless Chrome — "headless" meaning the browser runs with no visible window: it
lays the page out and rasterises it exactly as normal, then writes the result
straight to a file instead of painting a screen. No extension, no manual step,
same output every time.

```bash
"/c/Program Files/Google/Chrome/Application/chrome.exe" \
  --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --virtual-time-budget=6000 \
  --window-size=600,1775 \
  --screenshot="C:\absolute\path\out.png" \
  "file:///C:/absolute/path/to/mockup.html"
```

Five things that are not obvious and each cost a failed attempt:

- **`--screenshot` needs an absolute Windows path.** A relative path fails with
  `Failed to write file … Acceso denegado`, which reads like a permissions
  problem and is not one.
- **`--virtual-time-budget` is required.** These exports pull Tailwind and the
  webfonts from a CDN; without it Chrome captures before any of that applies and
  you get an unstyled page.
- **Width matters.** Below ~600px the Stitch markup clips its own fixed header
  and search bar at the right edge. 600×1775 matches the existing assets.
- **A `sticky` element with a `top` offset displaces itself downward and covers
  whatever follows it.** These exports use `sticky top-[64px]` on the search bar
  to clear the fixed header, which in a static render just hides the next block —
  it cost five renders to find, because the element is present in `--dump-dom`
  and simply painted over. For a new state, drop the `sticky` and give `main` a
  `pt-[72px]` instead: same picture, nothing hidden.
- The Chrome path above is the Windows default; adjust per machine.

## Screenshot cost, and why the AVD size is not a lever

Relevant when an agent reads these renders, or emulator captures, into context.

An image is billed by **area after downscaling**: the long edge is clamped to
1568px, then cost is roughly `width × height / 750` tokens. Because the long
edge is always clamped, a tall phone screenshot costs the same whatever the
native resolution — only the **aspect ratio** survives the downscale:

| Screen | Ratio | ≈ tokens |
|---|---|---|
| 1080×2400 or 720×1600 (modern phone) | 20:9 | ~1,475 |
| 1080×1920 | 16:9 | ~1,845 |
| Tablet | 4:3 | ~2,460 |

So dropping the AVD from 1080p to 720p saves **nothing** — same ratio, same
clamp, same cost. It only makes the render worse to read. A modern tall phone is
already the cheapest sensible shape.

The real levers are **cropping to the region in question**, or downscaling below
the 1568 clamp before reading — at an 800px long edge the same screenshot costs
~385 tokens, roughly a quarter, still enough for gross layout though not for
fine text. `System.Drawing` does this with no extra dependency:

```powershell
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile($in)
$scale = 800 / [Math]::Max($src.Width, $src.Height)
$w = [int]($src.Width * $scale); $h = [int]($src.Height * $scale)
$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = 'HighQualityBicubic'
$g.DrawImage($src, 0, 0, $w, $h)
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose(); $src.Dispose()
```

For emulator verification specifically, see CLAUDE.md → Commands → Device
verification: `Grep` over a `uiautomator dump` answers pre-stated questions for
a fraction of a screenshot's cost, but never anything involving the keyboard.

## Coverage gap

No Skill owns "produce or refresh a design reference". Rendering currently
happens ad hoc against this file. If it recurs, that is a Skill worth adding
rather than improvising each time — see CLAUDE.md → Available Skills.

## A new state needs its own mockup

Any element or screen state that departs from what the approved exports already
show gets its own `.html` and `.png` here, in the folder for its screen, named
for the state (`search-refresh-error`, not `search-2`).

Both files, not one. The HTML is what an agent reads and re-renders; the PNG is
what a human looks at without a browser. A state with only a PNG cannot be
regenerated, and a state with only HTML will not be looked at.

This exists because the refresh-failure banner shipped, was verified on device,
and then had no design reference at all — the screen's mockup still showed the
state it replaced. The gap is invisible until someone asks what the screen is
supposed to look like.
