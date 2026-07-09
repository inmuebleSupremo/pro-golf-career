## Context

The World runs and Careers age, but `Player` attributes are fixed after creation (`Attributes` is immutable and the reference is final). This change makes golfers *evolve*: a `sim.progression` engine computes attribute change as a pure function of attributes, age, and Development-Point allocation; `Player` gains a guarded evolution operation; and `World.seasonalTransition` applies it to every active golfer each season. Two locked decisions apply — Development Points are player-allocated (AI via policy), aging uses attribute-specific curves.

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical (no RNG; tournament randomness never touches attributes).

## Goals / Non-Goals

**Goals:**
- Development Points earned per season and allocated to raise attributes gradually, with a deterministic AI policy.
- Attribute-specific aging (different peaks/decline rates); career stages; diverse, believable trajectories.
- A guarded `Player` attribute-evolution operation (clamped 0–100, recorded), the first sanctioned mutation of a permanent value.
- Wire progression + aging into the World seasonal transition; keep the world reproducible.

**Non-Goals:**
- The human-facing allocation UI (engine takes an allocation input; AI policy drives it for now).
- Long-term-injury regression (Health domain, deferred).
- Any attribute change from tournament randomness (explicitly forbidden, REQ-049).
- Re-tuning shot realism (progression changes attributes, not the shot model).

## Decisions

### D1. `sim.progression` is a pure engine; `Player` owns the mutation
`ProgressionEngine` computes a new `Attributes` from (current attributes, age, allocation) with no side effects. `Player.evolveAttributes(Attributes next)` validates and replaces the stored attributes and appends a change record. *Why:* keeps the rules pure/testable and confines mutation to the owning entity (REQ-153/049). *Alternative rejected:* the progression engine writing into Player directly — spreads mutation and ownership.

### D2. `Player` attributes become a guarded mutable reference with a change log
Drop `final` on the attributes reference; add `evolveAttributes(next)` that clamps to 0–100 (via `Attributes`) and records an `AttributeChange` (attribute deltas + reason + when). No public setter beyond this guarded path. *Why:* REQ-017/048/049/153 — evolvable but only via progression, clamped, recorded. *Alternative rejected:* exposing a raw setter — invites tournament code mutating attributes.

### D3. Development Points: a per-golfer balance with a deterministic allocation
`DevelopmentPoints` holds a balance; `award(n)` adds; an `AllocationPolicy` maps (attributes, balance) → a list of `(Attribute, points)` allocations; applying them raises attributes by `points / POINTS_PER_RATING` and spends the balance. The AI policy specialises: it invests in the golfer's already-stronger attributes (with a small spread), deterministically. *Why:* REQ-151/154/156/163. Magnitudes (`DP_PER_SEASON`, `POINTS_PER_RATING`, per-season cap) live in `ProgressionConstants`. *Alternative rejected:* random allocation — non-deterministic and un-strategic.

### D4. Gradual growth via a per-season cap and a cost curve
Development raises an attribute by a bounded amount per season (a cap), and higher attributes cost more points per rating point (diminishing returns), preventing runaway inflation. *Why:* REQ-156 gradual, no inflation. Constants tunable.

### D5. Attribute-specific aging curves keyed by an aging class
Each `Attribute` maps to an `AgingClass` (PHYSICAL, SKILL, MENTAL) with a peak age and pre-/post-peak slopes. `agingDelta(attribute, age)` returns a small signed per-season change: rise toward the peak, decline after — physical peaks ~27 and fades, skill peaks ~33 and holds, mental keeps rising into the 40s then eases. *Why:* REQ-159 non-uniform, REQ-160 gradual. All curve parameters in `ProgressionConstants`. *Alternative rejected:* a single global curve — the spec forbids uniform decline.

### D6. Career stages derived from age; overall ability is a derived read
`CareerStage.of(age)` → DEVELOPMENT (<25) / PRIME (25–34) / LATE_CAREER (35+). Stages are descriptive (may scale DP awards) and never change rules. An `overallAbility(attributes)` mean is exposed for tests/consumers to trace a trajectory. *Why:* REQ-152/158/161.

### D7. World wiring: transition awards+allocates, then ages, per active golfer
In `World.seasonalTransition`, after `Career.advanceSeason` (age advanced) and for each still-active golfer: award DP (scaled by stage), run the AI allocation, and apply the season's aging via `Player.evolveAttributes`. Order: development then aging, using the new age. Retirees (who just retired) are not evolved. *Why:* REQ-152/159 and the living-world goal; deterministic so the world stays reproducible (REQ-299).

## Risks / Trade-offs

- **[First permanent-attribute mutation could leak to tournament code]** → Confine mutation to `Player.evolveAttributes`; a `player-entity` test asserts tournament resolution never changes attributes (the shot engine reads attributes, never writes).
- **[Curve/point constants uncalibrated]** → Isolate in `ProgressionConstants`; assert structural properties now (development raises, aging is non-uniform, a peak exists, growth is bounded), not magnitudes; tune later.
- **[Determinism across the world]** → All progression is a pure function of attributes/age/allocation; the AI policy is deterministic; a world reproducibility test covers it.
- **[Runaway inflation or collapse over long careers]** → Per-season cap + cost curve bound development; aging deltas are small and bounded; a multi-decade trace test asserts overall ability rises then declines gradually and stays in range.
- **[Attribute change history growth]** → Change records are small; kept as the golfer's development history (REQ-153/164); acceptable, prunable later.

## Migration Plan

Greenfield `sim.progression` + two additive modifications (`Player.evolveAttributes`; a progression step in `World.seasonalTransition`). Sequencing: (1) `ProgressionConstants`, `AgingClass`, `CareerStage`, `AttributeChange`; (2) `Player.evolveAttributes` + change log (modifies player-entity); (3) `DevelopmentPoints` + `AllocationPolicy` (AI) + apply; (4) `AgingCurves.agingDelta` + `ProgressionEngine.applySeason(attributes, age, allocation)`; (5) wire into `World.seasonalTransition`; (6) tests — development raises/gradual/AI-deterministic/recorded; aging non-uniform/gradual/stages; player-entity evolvable+clamped+not-by-tournament; world evolves golfers + stays reproducible; a multi-decade single-golfer trajectory (rise→peak→gradual decline, in range). Each layer testable before the next.

## Open Questions

- Exact DP-per-season, points-per-rating, per-season cap, and peak ages/slopes — placeholder `ProgressionConstants`; tuned later against desired career arcs.
- Whether development history lives in `sim.progression` or is merged into `Career` history — record in progression now; a later change can surface it in Career/Media.
- How the human player's allocation input is captured — the engine accepts an explicit allocation; the AI policy fills in until the Presentation layer adds player choice.
- Stage effect on DP awards — a mild scaling now (more opportunity in Development/Prime); revisit with staff/economy.
