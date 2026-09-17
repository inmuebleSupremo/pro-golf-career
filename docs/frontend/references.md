# REFERENCES.md — Design direction

## Purpose

These define the visual and experiential direction of the frontend.

They are references for principles — not templates to copy. Learn from them; do not imitate them.

> **History:** an earlier version of this file pointed at an Augusta/Masters "golf magazine"
> direction — editorial, calm, timeless, spacious, photography-first, and explicitly *not*
> dashboard-first, *not* dark. That was right for a small game that fit on one page. The game has
> since grown a dozen feature areas (rankings, rivals, records, finances, health, development, …),
> and a reading-magazine idiom cannot carry them — it produced long scrolling columns and a
> wall-of-content feel. This direction supersedes it.

---

# The thesis: a command centre, not a magazine

The app is a **sports-management command centre** — the player runs a golf career the way they'd
run a FIFA/EA FC or PGA TOUR 2K career: a dense, glanceable hub that portals into dedicated pages,
operated rather than read. Broadcast-grade, not brochure-grade.

Reference artifact (the agreed look): the redesign mockup published at
`claude.ai/code/artifact/0db41d71-1aad-4461-9e7e-1b128b627c3c`. Match its register.

**It should feel:** operational · premium · dense-but-calm · broadcast-grade · confident · fast · alive.

**It should NOT feel:** a magazine · a long scroll · a generic SaaS dashboard · a startup template ·
neon/glassmorphic · over-decorated · cramped (density is earned through spacing, never crowding).

---

# Learn from

**PGA TOUR 2K / EA Sports FC career hubs** — the primary reference. A tiled home hub where card
*size* encodes importance, a persistent identity strip, dedicated pages behind each tile, broadcast
telemetry (sparklines, form strings, stat bars). Learn the *structure and confidence*; don't copy
their exact chrome or licensed styling.

**Sports broadcast graphics (Sky Sports / broadcast golf overlays)** — for the data language:
tabular figures, position chips, a restrained accent + gold-for-honours palette, dark grounds that
make data glow. Learn the *clarity of live data*; not the motion-heavy lower-thirds.

**Linear** — interaction quality, keyboard-first speed, dark-theme craft, micro-interactions. (The
old doc said "don't copy the dark aesthetic" — reversed: dark, done with Linear's precision, is now
the target.) Don't copy its product structure.

**Raycast** — information density done calmly; dense panels that never feel crowded.

**Apple** — simplicity, spacing discipline, motion quality, obsessive detail. Not the marketing style.

---

# Information architecture: hub-and-spoke

The old single-scroll `manage` page (five stacked sections) does not scale. The structure is:

- **A persistent shell** — a grouped left nav (Compete · World · Career · Manage) plus a player
  identity strip — that **stays mounted across navigation**. Only the page body swaps.
- **A hub dashboard** (`/career/[id]`) — a **bento** grid of live summary cards, each a portal into
  its full page. Card size is the hierarchy: Next Event is the hero; rank/money/form are medium;
  news/records fill the rest.
- **Dedicated spokes**, one route per domain: Schedule · Play · Leaderboard · Rankings · Rivals ·
  Records · News · Profile · Season Stats · Goals · Hall of Fame · Development · Team · Equipment ·
  Finances · Fitness.

Bento is the *hub's* device. Deep pages are purpose-built layouts (a rankings table, a finance
ledger, a records timeline) — not more bento everywhere.

---

# Cumulative Layout Shift is a first-class constraint

The player asked for as little layout shift as possible. It is designed in, not patched on:

1. **Nav lives in a layout** (App Router `layout.tsx` per route group) so the shell never remounts —
   the single biggest perceived-speed and zero-CLS win.
2. **`loading.tsx` skeletons at the exact final dimensions.** Reserve the space; nothing jumps when
   data lands. A card that is "empty" and "loaded" occupies the same box.
3. **Suspense with stable fallbacks**, streaming per card so the shell paints instantly.
4. **Fixed-height headers and cards; no conditional layout jumps.**
5. **Prefetch on hover/viewport** so spokes open instantly.

---

# Craft carries over

What stays from the old direction — because it is what separates this from a generic admin template:
premium *restraint* (decoration is earned), a tight token system (no ad-hoc values), precise spacing,
and quality over ornament. Premium comes from quality, not decoration. The idiom changed; the
discipline did not.

See `design-tokens.md` for the concrete palette, type, spacing, and motion tokens.
