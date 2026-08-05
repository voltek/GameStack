---
name: GameStack Core
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#cbc3d7'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#958ea0'
  outline-variant: '#494454'
  surface-tint: '#d0bcff'
  primary: '#d0bcff'
  on-primary: '#3c0091'
  primary-container: '#a078ff'
  on-primary-container: '#340080'
  inverse-primary: '#6d3bd7'
  secondary: '#b9c7df'
  on-secondary: '#233144'
  secondary-container: '#3c4a5e'
  on-secondary-container: '#abb9d1'
  tertiary: '#ffb2b7'
  on-tertiary: '#67001b'
  tertiary-container: '#ff516a'
  on-tertiary-container: '#5b0017'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e9ddff'
  primary-fixed-dim: '#d0bcff'
  on-primary-fixed: '#23005c'
  on-primary-fixed-variant: '#5516be'
  secondary-fixed: '#d5e3fc'
  secondary-fixed-dim: '#b9c7df'
  on-secondary-fixed: '#0d1c2e'
  on-secondary-fixed-variant: '#3a485b'
  tertiary-fixed: '#ffdadb'
  tertiary-fixed-dim: '#ffb2b7'
  on-tertiary-fixed: '#40000d'
  on-tertiary-fixed-variant: '#92002a'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Hanken Grotesk
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  title-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-tablet: 24px
---

## Brand & Style
The design system for this application is built upon the **Material Design 3 (M3)** framework, optimized for the high-energy world of gaming while maintaining the organizational rigor of a library tool. The brand personality is **energetic, precise, and immersive**. 

It utilizes a **Corporate Modern** style with a gaming edge—prioritizing high-readability and systematic layouts that allow game art to remain the focal point. The interface should feel like a premium console dashboard: responsive, deep, and organized. The target audience includes enthusiasts and collectors who require a high-density information display that doesn't sacrifice visual flair.

## Colors
The color system follows M3's tonal palette logic, defaulting to a **Dark Mode** experience to mimic gaming environments and reduce eye strain during long sessions.

- **Primary (Vibrant Violet):** Used for key action buttons, active states, and brand moments. It represents the "energy" of gaming.
- **Secondary (Slate/Charcoal):** Used for less prominent components, chips, and secondary actions, providing a grounded contrast to the primary violet.
- **Tertiary (Coral/Rose):** Reserved for accents, notifications, or "live" indicators (e.g., a game currently being played).
- **Neutral:** A deep navy-slated charcoal serves as the surface color, providing more depth than pure black.

**No dynamic color.** The palette above is fixed and is the single source of
truth for what renders. Material You's wallpaper-based theming would let the OS
override the violet that anchors the "Stack" identity, and would make this
document non-authoritative — so `GameStackTheme` uses `darkColorScheme()` with
these values only, and takes no `dynamicColor` parameter (CLAUDE.md, Tech Stack
→ Tier 1).

**Dark only for MVP.** There is no Light ColorScheme, by decision, not by
omission — see the Spec's "Explicitly Deferred". Do not add one, and do not
treat its absence as drift.

## Typography
This design system employs a tiered typography strategy to balance technical data with editorial headers.

- **Headlines (Hanken Grotesk):** A contemporary grotesque that feels "tech" and sharp. Used for game titles and major section headings.
- **Body (Inter):** The workhorse for descriptions, reviews, and metadata. Chosen for its exceptional legibility at small sizes.
- **Labels (JetBrains Mono):** Used for technical metadata (e.g., FPS, Release Date, File Size) to evoke a "dev" or "system" feel appropriate for gaming stats.

All type scales strictly follow the M3 ratio. Game titles on cards use `title-md`
or `title-sm` to maintain high information density.

### Font files
All three families are bundled as variable fonts in `res/font/`
(`hanken_grotesk`, `inter`, `jetbrains_mono`); weights are pinned per style with
`FontVariation.Settings`. A `fontFamily` name in this document is not enough on
its own — an unbundled family silently falls back to the system default.

### Token → Material3 role mapping
`Type.kt` implements exactly this mapping. Tokens are authored in `px`; Compose
receives them as `sp`.

| Token | M3 role | Family |
|---|---|---|
| `display-lg` | `displayLarge` | Hanken Grotesk |
| `headline-lg-mobile` (28px) | `headlineLarge` | Hanken Grotesk |
| `headline-lg` (32px) | `headlineMedium` | Hanken Grotesk |
| `title-lg` | `titleLarge` | Hanken Grotesk |
| `title-md` | `titleMedium` | Hanken Grotesk |
| `title-sm` | `titleSmall` | Hanken Grotesk |
| `body-lg` | `bodyLarge` | Inter |
| `body-md` | `bodyMedium` | Inter |
| `label-md` | `labelMedium` | JetBrains Mono |

Note the deliberate inversion on the headline row: this is a phone-only app, so
the mobile-sized 28px token takes the `headlineLarge` role that screens actually
use, and the 32px desktop-sized token maps to `headlineMedium` so it stays
available without ever being the default.

M3 roles not listed above (`displayMedium`, `labelLarge`, `bodySmall`, …) keep
their Material3 defaults. That is intentional — inventing values to fill the
scale would put type in the app that this document never approved. If a screen
genuinely needs one of them, add the token here first.

## Layout & Spacing
The layout is based on an **8dp rigid grid**, consistent with Android native standards. 

- **Units:** the token block above is authored in web units, because it comes
  from a design-tool export. Android code must convert, never copy: **1rem = 16dp**
  (`sm` 0.25rem = 4dp, `DEFAULT` 0.5rem = 8dp, `md` 0.75rem = 12dp, `lg` 1rem = 16dp,
  `xl` 1.5rem = 24dp, `full` = 50%), and `px` maps 1:1 to `dp` for dimensions and
  to `sp` for type. No raw `rem`/`px` value ever appears in Kotlin.
- **Grid System:** 4-column grid for mobile. MVP is phone-only — the 8-column
  tablet grid and the `margin-tablet` token are forward-looking, not something to
  build or test against now.
- **Vertical Rhythm:** Components should be separated by increments of 8dp. Use 16dp for standard logical grouping and 24-32dp for major section separation.
- **Touch Targets:** All interactive elements must maintain a minimum 48x48dp touch area, regardless of their visual size.
- **Safe Areas:** Adhere to system bars and the M3 Bottom Navigation height (80dp).

## Elevation & Depth
Depth is communicated through **Tonal Layering** and M3's standard elevation levels. 

- **Level 0 (Surface):** The base background (#0b1326).
- **Level 1+ (Cards/App Bars):** A slightly lighter tint of the surface. Do not
  hand-roll a percentage overlay — the palette already ships the resolved tonal
  steps as `surface-container-lowest` → `surface-container-highest`. Use those
  roles directly; a manual overlay would drift from them and defeat the point of
  having the tokens.
- **Level 2 (Floating Action Buttons):** Distinctive elevation with a soft, 20% opacity shadow tinted with the primary violet.
- **Glassmorphism:** Use a subtle backdrop blur (15px) on the Top App Bar and Bottom Navigation when content scrolls beneath them to maintain a sense of space and context.

## Shapes
The shape language is **Rounded** — generous radii throughout, to feel modern and
friendly. Per-component values below; the `rounded` token block is the source.

- **Cards:** 16dp (`lg`) for game covers and list items.
- **Buttons:** Fully rounded (pill-shaped) for high-level actions — the `full`
  token, implemented as `RoundedCornerShape(percent = 50)` rather than a fixed dp,
  so the pill stays correct at any button height.
- **Input Fields:** 8dp (`DEFAULT`) to maintain a sense of structure.
- **Image Containers:** 12dp (`md`) for game screenshots within detail pages.

### Token → M3 Shapes mapping
`Shape.kt` implements this. Note that the largest token is **24dp, not M3's
default 28dp** — an intentional narrowing of the top of the scale, so shapes stay
within the token set rather than following the stock M3 value:

| Token | Value | `MaterialTheme.shapes` slot |
|---|---|---|
| `sm` 0.25rem | 4dp | `extraSmall` |
| `DEFAULT` 0.5rem | 8dp | `small` |
| `md` 0.75rem | 12dp | `medium` |
| `lg` 1rem | 16dp | `large` |
| `xl` 1.5rem | 24dp | `extraLarge` |
| `full` | 50% | `FullyRoundedShape` (standalone — M3 has no pill slot) |

## Components
- **Buttons:** 
  - *Primary:* Filled with `primary` (violet), label in `on-primary`. Note that
    `on-primary` is a deep purple (#3c0091), **not white** — the palette is a
    dark-theme M3 scheme, where `primary` is a light tone and its content color
    is dark. White label text on the violet fill would be both off-system and
    low-contrast. Always use the `on-*` role paired with the container.
  - *Secondary:* Tonal (`secondary-container` background, `on-secondary-container`
    label) for library management.
- **Cards:**
  - *Elevated:* Used for the "Featured" game carousel.
  - *Outlined:* Used for the main library list to keep the UI clean and prevent shadow-clutter.
- **Chips:** Used for "Genre" tags and "Platform" indicators. Use the `label-md` (monospaced) font for platform tags (e.g., PS5, PC).
- **Bottom Navigation:** Follows M3 specs with an active "pill" indicator around icons. Use the primary violet for the active state.
- **Input Fields:** Outlined style with the primary color used for the active border and cursor.
- **Progress Bars:** Use a thick 8dp bar for "Completion" status, with `primary`
  for the filled portion and `surface-container-highest` for the track. (There is
  no "Download" progress in this app — GameStack is a catalog, not a store or a
  launcher; nothing is ever downloaded.)
- **Game Poster Aspect Ratio:** Maintain a consistent 2:3 ratio for game covers in all grids.