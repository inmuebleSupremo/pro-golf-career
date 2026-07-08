## Why

Every gameplay domain in the simulation — tournaments, rankings, progression, economy, the entire AI world — ultimately consumes the outcome of golf shots. The specification (`docs/explore.md`) deliberately defers the actual mathematics to a "Shot Resolution Specification" that does not yet exist. Nothing else can be built correctly until this mathematical core is defined: it establishes the universal numerical language (REQ-038, REQ-044, REQ-050), the single shared engine that guarantees identical rules for human and AI golfers (REQ-104/110/123), and the determinism that makes save/load reproducible (REQ-047/265/299). This is the foundation the whole project rests on.

## What Changes

- Define the **five numerical value categories** (Attributes, Modifiers, Ratings, State, Outcomes) and the rules governing which may be trained, stored, or used as inputs (REQ-038, REQ-048–050).
- Define the concrete **7-step calculation pipeline** (Base → Career Effects → Temporary Modifiers → Environmental Effects → Controlled Randomness → Safety-Net → Outcome) that every gameplay calculation follows in a fixed order (REQ-044).
- Define the **shared shot-resolution engine** with two entry points that sample identical distributions:
  - `resolveShot` — resolves a single shot for the player, per-shot, driving the interactive UI loop.
  - `resolveRound` — resolves a full hole/round for AI golfers without a UI loop, by composing the same per-shot model.
- Define the **hybrid spatial model**: a hole is authored/drawn in 2D but *resolved* in 1D as distance-to-pin plus lateral offset evaluated against weighted **surface/hazard zone bands**.
- Introduce the **zone-band abstraction** as a first-class domain concept — the explicit contract between (future) course generation and shot resolution. Course generation produces zone bands; the shot engine consumes them.
- Define the **deterministic RNG seed hierarchy**: a per-world master seed deterministically derives seeds per season → tournament → round → shot, so any shot re-samples to the same result and a loaded world behaves identically to a saved one (REQ-047/265/299/265).
- Establish the controlled-randomness contract: continuous distributions that cluster around expected outcomes, reward strong decisions over large samples, and keep exceptional/catastrophic results rare but possible (REQ-045/046/059/060), bounded by safety-net mechanics (REQ-061).

Scope is limited to the **attribute → shot-outcome distribution model**. Explicitly out of scope for this change: course generation algorithms, tournament flow, rankings/economy/progression math, weather generation, persistence wiring, and any UI. Those consume this core but are separate changes.

## Capabilities

### New Capabilities
- `numerical-model`: The five value categories and the fixed 7-step calculation pipeline — the universal mathematical language every other calculation must speak (REQ-038, REQ-044, REQ-048–050).
- `shot-resolution`: The single shared engine (`resolveShot` / `resolveRound`) that turns a decision plus game state into a sampled, believable outcome, including the distribution, mistake, and safety-net contracts (REQ-045–067, REQ-104/110/123).
- `hole-spatial-model`: The hybrid draw-in-2D / resolve-in-1D representation and the first-class zone-band abstraction that forms the seam between course generation and shot resolution (REQ-071/072/077).
- `deterministic-rng`: The per-world master-seed hierarchy deriving reproducible per-season/tournament/round/shot seeds, guaranteeing identical simulation across save/load (REQ-047, REQ-265, REQ-299).

### Modified Capabilities
<!-- None. Greenfield project; openspec/specs/ is empty. -->

## Impact

- **Codebase**: Greenfield. Establishes the first backend domain package(s) under the Java 21 / Spring Boot 3 backend (e.g., `sim.core` for numerical model + RNG, `sim.shot` for resolution, `sim.course.spatial` for the zone-band model). Pure domain logic with no framework, persistence, or I/O dependencies so it is unit-testable in isolation (REQ-301).
- **Downstream consumers (future changes)**: Tournament engine, AI world simulation, progression, and course generation will all depend on these interfaces. The `resolveShot`/`resolveRound` signatures and the zone-band contract become stable APIs other domains build against.
- **Determinism constraint**: All randomness in the entire simulation must route through the seed hierarchy defined here — no ad-hoc `Random`/`Math.random()` anywhere downstream.
- **Dependencies**: No new third-party dependencies expected beyond the JDK; a documented pseudo-random algorithm (e.g., a splittable/streamable generator) will be chosen in design.
