## 1. Progression value types

- [x] 1.1 Create framework-free package `com.progolf.sim.progression` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `ProgressionConstants` (DP per season, points per rating, per-season development cap, per-attribute aging class + peak ages/slopes, stage boundaries) as the single tunables surface.
- [x] 1.3 Define `AgingClass` (PHYSICAL, SKILL, MENTAL) and map each `Attribute` to one; define `CareerStage` (DEVELOPMENT, PRIME, LATE_CAREER) with `of(age)`.
- [x] 1.4 Define an immutable `AttributeChange` (attribute, delta, reason, season/date) for the development/aging record.

## 2. Player attribute evolution (modified: player-entity)

- [x] 2.1 Make `Player`'s attributes an evolvable reference; add a guarded `evolveAttributes(Attributes next, reason)` that clamps to 0–100 and appends an `AttributeChange` record.
- [x] 2.2 Expose the attribute-change history (append-only) read accessor on `Player`.
- [x] 2.3 Confirm no non-progression path mutates attributes (shot engine reads only) — code/test check.

## 3. Development Points

- [x] 3.1 Implement `DevelopmentPoints` (balance; `award(n)`; spend) with the per-season cap and diminishing-returns cost curve.
- [x] 3.2 Implement `AllocationPolicy` — a deterministic AI policy mapping (attributes, balance) to attribute allocations (specialisation with a small spread).
- [x] 3.3 Implement applying an allocation to `Attributes`, raising attributes gradually and returning the new attributes + spent points.

## 4. Aging & engine

- [x] 4.1 Implement `AgingCurves.agingDelta(attribute, age)` — small signed per-season change from the attribute's aging class (rise to peak, gradual decline after); non-uniform across classes.
- [x] 4.2 Implement `ProgressionEngine.applySeason(attributes, age, allocation)` — apply development allocation, then aging, returning the evolved `Attributes` (clamped). Pure function, no RNG.
- [x] 4.3 Provide `overallAbility(attributes)` and career-stage helpers for trajectory reads.

## 5. World wiring (modified: world-progression)

- [x] 5.1 In `World.seasonalTransition`, for each still-active golfer after `advanceSeason`: award DP (scaled by career stage), run the AI allocation, and apply the season's aging via `Player.evolveAttributes`.
- [x] 5.2 Keep the world deterministic; retirees are not evolved.

## 6. Verification

- [x] 6.1 Development tests: allocation raises the chosen attribute and spends points; only attributes are developable; growth is bounded per season; AI allocation is deterministic; changes recorded.
- [x] 6.2 Aging tests: aging is attribute-specific (physical declines while mental rises across ages) and never uniform; changes are gradual; career stage derives from age.
- [x] 6.3 Player-entity tests: attributes evolve up (development) and down (aging), clamp to 0–100, are recorded, and are never changed by resolving a tournament.
- [x] 6.4 Engine test: `applySeason` is a pure deterministic function; same inputs → same evolved attributes.
- [x] 6.5 Trajectory test: over a multi-decade career a golfer's overall ability rises to a peak then declines gradually, staying within range, with non-uniform per-attribute change.
- [x] 6.6 World test: running seasons evolves active golfers' attributes; two worlds with the same seed produce identical evolved attributes (reproducible).
- [x] 6.7 Run `openspec validate add-progression-aging --type change --strict` and resolve findings.
