## Context

The shot engine already consumes `player.state().fatigue()` (via `Player.toGolferState`), and the World draws each field from tour standings — but fatigue is never filled and no golfer is ever unavailable, so the dormant `sim.player` fatigue/injury scaffold does nothing. This change adds a `sim.health` domain that owns each golfer's authoritative **Physical State** (fitness, fatigue, injury, availability), then wires it into the World: availability gates entry, fatigue is synced into shot resolution, and events/weeks accrue and recover physical state. Locked world model: weekly turn-based with seasonal seams; deterministic AI (no control-type branching).

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: recovery and accrual are pure; the only randomness (injury rolls) draws from the world seed hierarchy (`Seeds`/`SplitMix64Rng`), never `java.util.Random`.

## Goals / Non-Goals

**Goals:**
- A persistent per-golfer Physical State (fitness, fatigue, optional injury) with a derived Availability.
- Fatigue that accrues from participation/travel and recovers gradually, modulated by fitness and age; fatigue felt in shot resolution.
- Injuries that occur believably, remove a golfer from competition, and heal only through gradual rehabilitation; significant ones recorded.
- Availability that gates event entry, creating workload tension; health events feeding career narrative.
- Wire into the World deterministically; keep it reproducible; identical for all control types.

**Non-Goals:**
- Staff-driven recovery/prevention (Staff domain, deferred).
- Player-facing schedule/rest UI (Presentation; availability derives automatically).
- Injuries permanently regressing attributes (progression remains the only attribute mutator, REQ-049).
- Fine-grained travel/geography or per-round intra-event fatigue (fatigue is per-event in V1).
- Health performing scoring/ranking (forbidden, REQ-227).

## Decisions

### D1. `sim.health` owns the authoritative Physical State; it depends only on `core`
A `PhysicalState` (fitness, fatigue, optional `Injury`) is the authoritative physical readiness (REQ-215). The domain defines its own `Injury`/`InjuryType`/`InjurySeverity` and imports only `core` (RNG). It never imports `player`, `tournament`, `world`, etc. *Why:* REQ-225/227 — health is the source of Physical State and a pure producer, not a rules actor. *Alternative rejected:* driving the package-private `sim.player` injury slot from health — couples health to `player` internals and splits authority.

### D2. Physical State is immutable; `HealthSystem` computes transitions
`PhysicalState` is a record with functional updates; `HealthSystem` is a pure engine: `afterParticipation(state, age, rng)` returns the post-event state (added fatigue, an injury roll); `recoverWeek(state, age)` returns the post-recovery state (reduced fatigue, one week of rehabilitation, healing when done). *Why:* keeps the rules pure/testable and confines change to one engine; mirrors `progression`/`weather`. *Alternative rejected:* mutable state with scattered setters — unauditable and order-sensitive.

### D3. Availability is derived from Physical State, not stored
`PhysicalState.availability()` returns `INJURED` (active injury, early rehab) / `RECOVERING` (active injury, late rehab) / `RESTING` (no injury but fatigue ≥ a rest threshold) / `AVAILABLE`. `canCompete()` is `AVAILABLE` only. *Why:* REQ-221 "Availability is derived from the golfer's current Physical State"; the rest threshold makes overwork force a bench week (REQ-222 workload tension). *Alternative rejected:* a stored availability field — can desync from the underlying state.

### D4. Fitness and age modulate accrual, recovery, and injury risk
Higher fitness lowers fatigue accrual, speeds recovery, and lowers injury chance; higher age raises accrual and injury chance and slows recovery. Fitness is seeded per golfer at creation and held constant in V1 (age carries the long-term variation). *Why:* REQ-216 (fitness influences recovery/durability) and REQ-226 (resilience varies, emergent). *Alternative rejected:* uniform physiology — erases durability as a differentiator.

### D5. Injuries gate entry rather than penalising shots
An injured golfer is `INJURED`/`RECOVERING` and simply does not enter events (REQ-221); the shot engine is fed fatigue only. Rehabilitation is a positive recovery countdown advanced one week at a time, so return is never instantaneous (REQ-220). *Why:* keeps the shot engine untouched (REQ-227) while injuries still bite competitively (lost starts); avoids a second shot-penalty channel beyond fatigue. *Alternative rejected:* feeding an injury performance penalty into shots — duplicates the fatigue channel and lets injured golfers still play, weakening availability.

### D6. Fatigue reaches shots by syncing into the player's transient state
Before an event, the World sets each competitor's `player.state().setFatigue(profile.fatigue())` (a public op the shot engine already reads via `toGolferState`). The player's fatigue is a just-in-time mirror of health's authoritative value; health writes nothing back. *Why:* least-invasive path to REQ-225 (dependent systems reference physical state) without changing the Tournament/shot API. *Alternative rejected:* threading per-golfer fatigue through the Tournament — a wider API change than reusing the existing player→shot fatigue seam.

### D7. World wiring: gate, sync, accrue, recover, record
`admit` creates a `PhysicalState` (seeded fitness). In `resolveEvent`, the field is filtered by `canCompete()`; each competitor's fatigue is synced into the player before play; after play, each competitor's state advances via `afterParticipation` (added fatigue + injury roll). In `advanceWeek`, after events, every active golfer recovers one week via `recoverWeek`. Significant injuries and comebacks append to a world-held `List<HealthEvent>` history. Age comes from `Career.age()`; injury rolls use an isolated per-golfer/season/week seed. *Why:* REQ-217/218/220/221/223 and the living-world goal; REQ-299 reproducibility. *Alternative rejected:* recovering inside `Career`/`Tournament` — those must not own physical state (REQ-227).

### D8. Isolated health seed stream; accrual/recovery pure
Injury rolls derive a per-golfer/season/week seed via `Seeds.deriveSeed(...)` with a dedicated health salt, isolated from shot, weather, and economy streams. Fatigue accrual and recovery are pure (no RNG). *Why:* reproducibility (REQ-299) and no perturbation of play. *Alternative rejected:* a shared RNG — breaks isolation.

## Risks / Trade-offs

- **[Health becoming a scoring/ranking actor]** → `health` imports only `core`; it returns Physical State; the sole cross-domain write is `player.state().setFatigue` (a public op); an architecture/boundary test asserts health touches no attribute/score/ranking, and the World never lets health compute outcomes (REQ-225/227).
- **[Fields emptying when too many are unavailable]** → injury chance and the rest threshold are tuned so unavailability is the exception; `resolveEvent` already no-ops on an empty field; a test asserts fields still resolve across seasons.
- **[Determinism across the world]** → accrual/recovery are pure; injury rolls use an isolated seeded stream; the existing world reproducibility test (two seeds → identical world) covers it, extended to physical state. Note: feeding fatigue into shots changes absolute outcomes vs. before this change, but structural world tests assert properties, not fixed winners.
- **[Uncalibrated magnitudes]** → all magnitudes isolated in `HealthConstants`; tests assert structural properties (fatigue rises with play and falls with rest, injuries occur and heal gradually, unfit/old golfers are less durable, availability gates entry), not magnitudes; tune later.
- **[Two injury representations (player scaffold vs. health)]** → the dormant `sim.player` injury slot is left unused; health owns injuries; documented so a later cleanup can remove the scaffold if desired.

## Migration Plan

Greenfield `sim.health` + one additive modification (World health wiring). Sequencing: (1) `HealthConstants`, `InjuryType`, `InjurySeverity`, `Injury`; (2) `Availability`, `PhysicalState` (derived availability, functional updates); (3) `HealthSystem` (`initialState`, `afterParticipation`, `recoverWeek`) + `HealthEvent`; (4) wire the World — physical state at `admit`, availability filter + fatigue sync + post-event accrual in `resolveEvent`, weekly recovery in `advanceWeek`, health-event history, a read-only `physicalStateOf` accessor; (5) tests — state/fitness/fatigue accrual+recovery; injury occurrence + gradual rehab + healing; availability derivation + entry gating; boundary; world accrues/recovers/gates and stays reproducible, with fatigue felt in shots. Each layer testable before the next.

## Open Questions

- Exact magnitudes (fitness distribution, fatigue per event, recovery per week, rest threshold, base injury chance and severity mix, age/fitness modifiers) — placeholder `HealthConstants`, tuned later against desired durability and availability rates.
- Whether fitness should drift with age/training — held constant in V1 (age carries variation); a later change can add drift.
- Whether health events fold into `Career` history or stay world-held — world-held now (like environmental history); a later legacy/Media change can surface them.
- Staff influence on recovery/prevention — deferred to the Staff domain; the recovery function is the natural hook.
