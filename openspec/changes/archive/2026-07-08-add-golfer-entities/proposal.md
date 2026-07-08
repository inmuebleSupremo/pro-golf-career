## Why

The world needs golfers. The shot engine can resolve rounds and the course domain can build venues, but nothing yet represents *who* is playing — no canonical golfer, no distinction between the human and the AI field, no population to fill a tournament. The Player Entity is the central domain object of the whole application (REQ-014): every other system (tournaments, rankings, economy, progression) references it. Defining it now — together with the Professional Golfer participant and a persistent population — unblocks the Tournament engine, which needs a field of golfers to compete.

## What Changes

- Define the canonical **Player** entity (REQ-014–024): immutable identity, a reference to the existing `Attributes` (reused, never duplicated), derived statistics that are recalculated rather than stored, and temporary State.
- Define the four Player **data categories** explicitly (REQ-015): Identity, Attributes, Derived Statistics, State — with the rule that permanent data is owned once and derived values are never persisted.
- Model Player **State**: Fatigue (REQ-020), a volatile self-correcting **Live Skill Rating** (REQ-019), zero-or-one active **Injury** with type/severity/recovery-duration/effects (REQ-021), and a **Career Status** state machine — CREATED, ACTIVE, INJURED, RETIRED, DECEASED — with validated transitions (REQ-022).
- Enforce **canonical ownership** (REQ-024): the Player is the single source of truth for permanent golfer data; other systems reference it and never duplicate permanent attributes.
- Define the **Professional Golfer** participant (REQ-113–119, 122–124): Player + a Career **reference (id only)** + a **Control Type** (Human or Simulation) that is set at creation, immutable, and changes no gameplay rules. Both control types share identical systems (REQ-104/110/123). Include a **decision-making seam** for simulation golfers (interface only; behaviour deferred, REQ-117).
- Define a persistent, diverse **golfer population** (REQ-103/120/125): deterministic generation of a varied initial field via the seed hierarchy, and replenishment as golfers leave so the world stays populated. Rivalries remain emergent, never scripted (REQ-121).

Explicitly out of scope (deferred to later changes): the Career lifecycle entity — seasons, career age, milestones, career history (REQ-025–037); progression, aging, and regression (REQ-151–165); the Live Skill Rating's exact tournament-coupled update math (a minimal, testable rule is defined here, full calibration later); and any persistence/GraphQL wiring. A Professional Golfer references its Career by id; the Career entity itself is a separate change.

## Capabilities

### New Capabilities
- `player-entity`: The canonical golfer (REQ-014–024) — immutable identity, referenced Attributes, non-stored derived statistics, and State (Fatigue, Live Skill Rating, Injury, Career Status) with validated status transitions and single-source-of-truth ownership.
- `professional-golfer`: The competitive participant (REQ-113–124) — Player + Career reference + immutable Control Type (Human/Simulation) under one shared rule set, with a decision-making seam for simulation control and persistent identity/legacy.
- `golfer-population`: A persistent, diverse population of Professional Golfers (REQ-103/120/125) — deterministic seeded generation of a varied field and replenishment that keeps the world populated, with rivalries left emergent.

### Modified Capabilities
<!-- None. This change DEPENDS ON the existing numerical-model (Attributes/Attribute) and
     deterministic-rng (seed hierarchy) capabilities but changes none of their requirements.
     It reuses Attributes rather than redefining them. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.player` and `com.progolf.sim.population` packages in the existing `backend/` module, consistent with `sim.core` / `sim.spatial` / `sim.shot` / `sim.course`. The Player references `core.Attributes` and can produce the `shot.GolferState` the shot engine consumes — no attribute or state duplication.
- **Downstream consumers (future changes)**: The Tournament engine will draw fields from the population and read Player state for eligibility; the Career, progression, rankings, and economy domains will all reference the Player. The Control Type and population interfaces defined here become stable seams those changes build on.
- **Dependencies**: No new third-party dependencies. Population generation routes all randomness through the existing seed hierarchy, keeping the field reproducible.
- **Boundary risk**: Care is required so State (Fatigue, Live Skill Rating, Injury) never mutates permanent Attributes (REQ-019/020) and so no consumer duplicates permanent data (REQ-024/276); requirements and tests enforce both.
