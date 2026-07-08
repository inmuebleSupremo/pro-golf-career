## Context

The backend has a working simulation spine — `sim.core` (numerical model + RNG), `sim.spatial`, `sim.shot` (resolution + `GolferState`), and `sim.course` (generation). What's missing is *who plays*: a canonical golfer, the human/AI distinction, and a population to field tournaments. Per REQ-014 the Player Entity is the central domain object every other system references.

This change defines Player + Professional Golfer + population **only**. Career lifecycle (REQ-025–037) and progression/aging (REQ-151–165) are deferred; a Professional Golfer references its Career by id. Packages stay framework-free like the rest of `sim.*` (Java 21 / Spring Boot 3).

## Goals / Non-Goals

**Goals:**
- A canonical, immutable-identity `Player` that *references* the existing `core.Attributes` (no duplication) and owns temporary State.
- Model State cleanly: Fatigue, a volatile self-correcting Live Skill Rating, zero-or-one Injury, and a validated Career Status state machine.
- A `ProfessionalGolfer` = Player + Career id + immutable Control Type, with one shared rule set and a decision seam for simulation control.
- Deterministic, diverse population generation from the seed hierarchy, plus replenishment.
- Enforce the ownership/boundary rules (State never mutates Attributes; permanent data owned once) structurally and by test.

**Non-Goals:**
- Career entity, seasons, calendar, milestones, history storage.
- Progression, aging, regression.
- The Live Skill Rating's full tournament-coupled calibration (a minimal, testable rule is provided).
- The AI decision *implementation* (only the seam/interface).
- Persistence/GraphQL wiring.

## Decisions

### D1. Player is a mutable aggregate over immutable Identity + referenced Attributes + mutable State
`Identity` and `Attributes` are immutable records; `Player` holds them plus a mutable `PlayerState`. Identity is set once and never exposed for mutation. *Why:* attributes/identity are permanent (REQ-014/017) while state changes constantly (REQ-020); separating them makes the "State never mutates Attributes" invariant structural — the state API has no handle to attributes. *Alternative rejected:* one flat mutable object — invites accidental attribute mutation and duplication.

### D2. Reuse `core.Attributes`; Player produces `shot.GolferState` on demand
The Player holds a reference to `core.Attributes` and exposes a method to build the `shot.GolferState` (fatigue, pressure) the engine consumes, derived from current State. *Why:* single source of truth (REQ-024/276); the shot engine already owns its per-shot input type, so we adapt rather than duplicate. *Alternative rejected:* copying attributes/fatigue into player-local fields — violates ownership and risks drift.

### D3. Career Status as an explicit transition table
A `CareerStatus` enum plus a `canTransitionTo` table (CREATED→ACTIVE; ACTIVE↔INJURED; ACTIVE/INJURED→RETIRED; any→DECEASED; RETIRED/DECEASED terminal). Transitions go through one guarded method. *Why:* REQ-022 requires invalid transitions be rejected; a table makes the legal set explicit and testable. *Alternative rejected:* free-form status setter — can't enforce validity.

### D4. Live Skill Rating: bounded mean-reverting value with a simple, documented rule
Rating starts at a baseline; `recordPerformance(delta)` nudges it and clamps; `decayTowardBaseline(step)` applies inactivity reversion. The exact magnitudes live in one constants surface (like `SimConstants`) and are explicitly placeholder pending tournament coupling. *Why:* satisfies REQ-019's observable behaviour now (up on good, down on bad, reverts on inactivity) without fabricating tournament math that doesn't exist yet. *Alternative rejected:* leaving it an inert field — fails REQ-019's acceptance scenarios.

### D5. Injury as an optional value object with a recovery clock
`Injury(type, severity, recoveryDurationRemaining, effects)`; `Player` holds `Optional<Injury>`. Applying an injury when one is active is rejected; `advanceRecovery(step)` decrements and auto-clears at zero, and drives INJURED↔ACTIVE status coordination. *Why:* REQ-021 zero-or-one, recovery over time, no attribute mutation. *Alternative rejected:* a list of injuries — REQ-021 forbids multiple simultaneous active injuries in V1.

### D6. Control Type immutable; simulation decisions via a `DecisionPolicy` seam
`ControlType { HUMAN, SIMULATION }` set at construction. A `DecisionPolicy` interface is the seam simulation golfers use; no gameplay system branches on Control Type — the shared engine never reads it (mirroring the shot engine's control-free design). *Why:* REQ-114/123 identical rules, REQ-117 independent decisions. *Alternative rejected:* an `isAi` flag consulted in gameplay — reintroduces the divergence the architecture forbids.

### D7. Population generation seeds each golfer independently with a randomized attribute profile
`PopulationGenerator.generate(seedCoordinate, size)` derives a per-golfer seed and builds a diverse `Attributes` profile: an overall skill level plus a randomized strength/weakness shape (some strong putters, some bombers). Replenishment reuses the same per-golfer generation at a fresh index. *Why:* REQ-120 diversity + REQ-125 replenishment, all reproducible via the hierarchy. *Alternative rejected:* uniform attributes — produces a homogeneous, unbelievable field.

## Risks / Trade-offs

- **[State accidentally mutating Attributes]** → D1 removes the handle; a test asserts attributes are unchanged after fatigue/rating/injury changes.
- **[Live Skill Rating math is a placeholder]** → Isolate magnitudes in a constants surface; assert only the directional/reversion behaviour now; revisit when tournaments feed real performance.
- **[Population diversity is subjective]** → Test measurable diversity: variance in overall skill and in which attribute is each golfer's strongest, across a generated field.
- **[Career-by-id could tempt embedding Career logic here]** → Keep the reference an opaque id; requirements/tests forbid a Career entity in this change.
- **[Duplication creep of attributes/state into consumers]** → Reinforce single-source-of-truth in specs; the Player exposes read accessors and a `GolferState` factory so consumers never need copies.

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) `Identity` + `CareerStatus` transition table; (2) `PlayerState` (Fatigue, `LiveSkillRating`, `Injury`) with invariants; (3) `Player` aggregate reusing `core.Attributes` + `GolferState` factory; (4) `ControlType` + `ProfessionalGolfer` + `DecisionPolicy` seam; (5) `PopulationGenerator` + replenishment; (6) test suites (ownership/immutability, status machine, rating behaviour, injury clock, diversity, reproducibility, no-control-type-branch). Each layer testable before the next.

## Open Questions

- Live Skill Rating baseline and step magnitudes — placeholder now; finalise when the tournament engine records real round performance.
- Whether Derived Statistics get a concrete first example here (e.g. an expected-scoring value) or remain an interface until rankings/stats land — lean interface + one illustrative derived value.
- Nationality representation (enum vs ISO code list) — a simple validated code set suffices for V1; the full predefined list is a presentation concern.
- Population size defaults and per-tier distribution — belongs with the Tournament/Tour change that consumes the population; this change parameterises size without fixing tour structure.
