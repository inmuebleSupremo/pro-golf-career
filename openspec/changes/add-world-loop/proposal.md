## Why

Nine domains exist as capabilities, but nothing turns the cranks: seasons don't advance, AI events don't happen on their own, and the promotion/relegation, career-aging, and ranking-snapshot seams sit idle. The World is the top-level orchestrator that makes the simulation *alive* — a persistent world that advances week by week, resolves events whether or not the player is watching, and runs a seasonal transition that fires every seam at once. This is the integration change that composes the whole build into a self-progressing professional golf ecosystem.

## What Changes

- Define one persistent **World** as the highest-level domain (REQ-101): it owns the calendar, seasons, and the professional population, and coordinates the other domains without absorbing their responsibilities.
- Advance the world through a **shared calendar in discrete weekly turns** (REQ-105) via an `advanceWeek()` operation, with a season composed of a fixed number of weeks.
- Progress **independently of the player** (REQ-102/108): scheduled events still occur, winners are determined, rankings update, and history is written regardless of player participation; the world never pauses solely because the player is absent.
- **Generate a season schedule** deterministically at each season start (REQ-101/106): tournaments allocated to tours, courses, and weeks; the schedule is fixed for the season and archived on completion.
- **Automatically resolve** each week's scheduled events (REQ-109): draw each event's field from its tour's eligible members, run the Tournament engine to completion via the shared resolution path, and feed the result into **World Ranking**, that tour's **Season Standings**, and each competitor's **Career**.
- Run the **seasonal transition** at season end (REQ-106): take a season-ending ranking snapshot, run Tour **promotion/relegation** (`reviewSeasonEnd`), **advance every Career by a season** (`advanceSeason`, retiring at 65), **replenish** the population for departures, and generate the next season's calendar.
- Apply **identical rules to all golfers** (REQ-104/110) — automatic AI resolution uses the same engine and rules as the player — and preserve **historical continuity** permanently across progression (REQ-107).
- Guarantee the world is **deterministic and reproducible** from its master seed (REQ-299).

Explicitly out of scope: shot resolution, rankings math, financial management, rendering, UI, and input (REQ-112) — the World composes the domains that own those; **progression/aging of attributes** (Careers age and retire but attributes do not yet evolve — a later Progression domain); and **Postgres persistence** (the World exposes serialisable state; a later Persistence domain stores it). The human player's interactive participation is resolved headlessly here like any other competitor; a UI layer is a later concern.

## Capabilities

### New Capabilities
- `world-calendar`: The persistent World entity and shared weekly calendar/seasons; player-independent progression; identical rules for all; permanent historical continuity; the coordination boundary (REQ-101/102/104/105/107/108/110/112).
- `world-schedule`: Deterministic per-season generation of a fixed competitive calendar — tournaments allocated to tours, courses, and weeks — archived on completion (REQ-101/106).
- `world-progression`: Automatic weekly event resolution feeding results into ranking, tour standings, and careers, and the season-end transition that snapshots rankings, promotes/relegates, ages careers, replenishes the population, and generates the next calendar (REQ-104/106/107/109/110).

### Modified Capabilities
- `tour-membership`: Add membership **removal** — a golfer may be deregistered from Tour membership when they leave the active world (e.g. retirement), so they are excluded from future season standings and promotion/relegation reviews while their history is preserved. This is additive (a small `TourSystem.deregister`) and is required for a living world where golfers retire (supports population management, REQ-125).

## Impact

- **Codebase**: New framework-free `com.progolf.sim.world` package in the existing `backend/` module. It is the first domain that composes nearly all others: `population`, `tour`, `course`, `tournament`, `ranking`, `career`, and `core` (seed hierarchy). No changes to existing packages — it calls their public operations.
- **Determinism**: The world master seed derives all sub-seeds (schedule, per-event, per-shot), so an entire multi-season world is reproducible bit-for-bit (REQ-299).
- **Downstream consumers (future changes)**: the Progression/aging domain hooks into the seasonal transition; the Economy scales prize/prestige and reads results; Media/statistics read world history; Persistence serialises the World; the GraphQL API + React frontend present it. The `advanceWeek`/seasonal-transition contract is the seam those build on.
- **Risk**: this is the largest integration surface in the project — a bug here can implicate several domains. Mitigated by driving only public domain operations, keeping the World a pure coordinator, and testing a full season (and multi-season) run end-to-end for reproducibility and correct seam firing.
