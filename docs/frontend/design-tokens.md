# DESIGN_TOKENS.md

## Purpose

This document defines the visual token system used throughout the application.

All visual styling must originate from these tokens.

Never introduce ad-hoc visual values.

Never hardcode colours, spacing, typography, shadows or radii inside components.

The role framework below is the philosophy. The **Canonical values** section immediately after it is
the source of truth for the command-centre direction (see `references.md`) — concrete, committed
values so the system cannot drift back into "abstract roles, no palette" ambiguity.

---

# Canonical values — Command Centre (v2)

**Dark-first, with light mode planned.** Define every token on `:root` (dark). When light mode is
added, redefine only the token *values* under `:root[data-theme="light"]` and
`@media (prefers-color-scheme: light)`; components consume tokens and never change. Do not hardcode
either theme's values in components.

## Colour — a three-accent system, each with a job

Green is *your performance and actions*. Azure is *the world and informational data*. Gold is
*honours only*. Keeping them in separate roles is what stops the UI reading as "too much green".

| Token | Value | Role |
|---|---|---|
| `--bg` | `#0e1311` | app ground (near-neutral dark, whisper of green) |
| `--bg-2` | `#0a0f0d` | sidebar / deepest ground |
| `--surface` | `#141b18` | card base |
| `--surface-2` | `#18201c` | card top / raised panel |
| `--surface-3` | `#202a25` | inset tracks, hover |
| `--line` | `#263029` | hairline border |
| `--line-2` | `#33423a` | stronger border / control edge |
| `--ink` | `#eef2f0` | primary text |
| `--muted` | `#a2b1a9` | secondary text (≥4.5:1 on surface) |
| `--muted-2` | `#728177` | tertiary / captions |
| `--accent` (green) | `#57d08a` | **your** CTAs, positive form, earnings, "up" trends |
| `--accent-ink` | `#04160c` | text on green |
| `--info` (azure) | `#63a8ec` | **world** data: rank, standings, dev bars, sparklines |
| `--gold` | `#ecca7e` | honours: majors, records, wins, Hall of Fame |
| `--gold-metal` | `linear-gradient(125deg,#f7e6ad,#e6bd6a 34%,#cf9f47 52%,#f4dfa0 74%,#d9ab55)` | metallic gold fill |
| `--down` | `#f0817f` | negative / missed cut |
| `--warn` | `#e6b455` | warning |

**Gold must read as metal, not a flat swatch** (an explicit request). Honour elements use
`--gold-metal` as a fill with a soft glow (`0 0 18px -4px rgba(236,202,126,.5)`) and a slow shine
sweep (a `translateX` highlight band, `@media (prefers-reduced-motion: reduce)` removes it). Reserve
it for genuine moments (records, majors), never as decoration.

## Typography

One system sans (`-apple-system, "Segoe UI", Roboto, …`); a mono stack (`ui-monospace, …`) for
telemetry labels. Product UI: **fixed rem scale, not fluid clamps.** Personality comes from weight,
tracking, and `font-variant-numeric: tabular-nums` on every aligned figure — not from a display face.

- Big stat figure ~34–36px / 760 wt / -0.035em · Card heading ~23px / 720 · Section label 11px / 700
  / .12em uppercase / muted · Body 13–15px / 1.5 · Caption 11–12px / muted-2.

## Spacing, radius, motion

- **Spacing scale** (px): 4 · 8 · 10 · 14 · 18 · 22 · 26 · 30 · 40 · 52 · 66. Cards pad `22px`; bento
  gap `18px`; section gap `40–52px`. **Density comes from a tight scale used with room, never from
  crowding** — the first-round mockup was too dense; this scale is the corrected rhythm.
- **Radius:** `--r: 16px` (cards), `--r-sm: 10px` (controls, chips).
- **Motion:** 150–250ms, ease-out; conveys state, not decoration. One restrained card-rise on load
  (staggered, reduced-motion-safe); the gold shine is the only ambient motion, and it is a moment.

---

# Philosophy

The design system is built on consistency rather than variety.

A small number of well-defined tokens should be reused throughout the application.

If a new token appears necessary, first determine whether an existing token can satisfy the requirement.

---

# Token Categories

The design system consists of the following token groups:

* Colours
* Typography
* Spacing
* Sizing
* Border Radius
* Borders
* Shadows
* Motion
* Opacity
* Z-Index
* Breakpoints

No additional token categories should be introduced without architectural justification.

---

# Colours

Colours describe **roles**, not specific values.

Examples:

* Background
* Surface
* Surface Elevated
* Primary
* Secondary
* Accent
* Success
* Warning
* Error
* Border
* Divider
* Text Primary
* Text Secondary
* Text Muted

Components should consume semantic colour tokens.

Never reference raw colour values.

---

# Typography

Typography establishes hierarchy.

Define tokens for:

* Display
* Heading
* Title
* Body
* Caption
* Label
* Monospace

Typography should communicate importance before colour or decoration.

---

# Spacing

Use a single spacing scale throughout the application.

All spacing should derive from this scale.

Avoid arbitrary spacing values.

Whitespace is the primary organisational tool of the interface.

---

# Sizing

Component dimensions should follow a consistent sizing system.

Define standard sizes for:

* Controls
* Icons
* Inputs
* Buttons
* Avatars
* Containers

Avoid creating component-specific sizing systems.

---

# Border Radius

Use a small, consistent set of radius tokens.

Border radius should reinforce the product identity.

Do not mix multiple visual styles within the same interface.

---

# Borders

Borders exist to define structure, not decoration.

Prefer subtle borders.

Use borders sparingly where whitespace or hierarchy cannot communicate separation.

---

# Shadows

Shadows communicate elevation.

They should never become decorative.

Use the minimum number of elevation levels required by the interface.

Avoid dramatic or heavily blurred shadows.

---

# Motion

Motion tokens define:

* Duration
* Easing
* Delay

All animations should consume motion tokens.

Interaction speed should remain consistent across the application.

---

# Opacity

Opacity should communicate:

* Disabled
* Hover
* Selected
* Focus
* Loading

Avoid arbitrary opacity values.

---

# Z-Index

Layering should follow predefined tokens.

Typical layers include:

* Base Content
* Sticky Elements
* Navigation
* Popovers
* Drawers
* Modals
* Notifications

Avoid using arbitrary z-index values.

---

# Breakpoints

Responsive behaviour should use a single breakpoint system.

Components should adapt through layout changes rather than creating entirely different interfaces.

---

# Component Consumption

Every component should consume design tokens rather than defining visual properties directly.

Example:

```text
✓ Button → Primary Colour Token

✗ Button → #0F7A42
```

```text
✓ Card → Surface Token

✗ Card → Custom White
```

```text
✓ Heading → Heading Typography Token

✗ Heading → 38px Font Size
```

---

# Evolution

The token system should evolve by extending existing roles rather than introducing exceptions.

Changes to a token should improve the entire application rather than a single screen.

---

# Anti-Patterns

Never:

* Hardcode colour values.
* Hardcode spacing values.
* Hardcode typography values.
* Create page-specific tokens.
* Create feature-specific tokens.
* Introduce one-off visual values.
* Duplicate existing tokens under different names.

---

# Guiding Principle

Design tokens are the single source of truth for the application's visual language.

Every component should express the design system through tokens rather than through individual styling decisions.
