## 1. Physical state model

- [x] 1.1 Create framework-free package `com.progolf.sim.health` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `HealthConstants` (fitness distribution, fatigue per event, recovery per week, rest threshold, base injury chance + severity mix, age/fitness modifiers, recovering-vs-injured rehab threshold, health seed salt) as the single tunables surface.
- [x] 1.3 Define `InjuryType` (WRIST, BACK, SHOULDER, KNEE, ELBOW, HAND), `InjurySeverity` (MINOR/MODERATE/SEVERE with rehab weeks + significance), and an immutable `Injury` (type, severity, rehab weeks remaining) with `advance(weeks)`, `isHealed()`, `of(type, severity)`.
- [x] 1.4 Define `Availability` (AVAILABLE, RESTING, RECOVERING, INJURED) and an immutable `PhysicalState` (fitness, fatigue, optional injury) with derived `availability()` / `canCompete()` and functional updates.

## 2. Health engine

- [x] 2.1 Implement `HealthSystem.initialState(rng)` — seeded fitness, zero fatigue, no injury.
- [x] 2.2 Implement `HealthSystem.afterParticipation(state, age, rng)` — add fatigue (moderated by fitness/age), then roll an injury (chance from fatigue/fitness/age); return the new state and any new injury.
- [x] 2.3 Implement `HealthSystem.recoverWeek(state, age)` — reduce fatigue (moderated by fitness/age), advance any rehabilitation by one week, clear a healed injury; pure, gradual.
- [x] 2.4 Define `HealthEvent` (season, golfer, type, summary) for significant injuries and comebacks (health history).

## 3. World wiring (modified: world-progression)

- [x] 3.1 In `admit`, create a `PhysicalState` (seeded fitness) for each golfer; add a read-only `physicalStateOf(id)` accessor and a `healthHistory()` accessor.
- [x] 3.2 In `resolveEvent`, filter the field by `canCompete()`; before play, sync each competitor's fatigue into `player.state().setFatigue(...)` so the shot engine reflects it.
- [x] 3.3 In `resolveEvent`, after play, advance each competitor's state via `afterParticipation` (added fatigue + injury roll); record significant injuries to health history.
- [x] 3.4 In `advanceWeek`, after resolving events, recover every active golfer one week via `recoverWeek`; record comebacks (return from injury) to health history.
- [x] 3.5 Derive the health seed stream from the world hierarchy with a dedicated salt (isolated from shot/weather/economy streams); age from `Career.age()`.

## 4. Verification

- [x] 4.1 Physical-state tests: every golfer has a state; fatigue rises with participation and falls with recovery; recovery is gradual; fitter/younger golfers wear down less and recover faster.
- [x] 4.2 Injury tests: injuries occur under strain; rehabilitation takes multiple weeks (never instantaneous); a healed injury clears and restores the ability to compete; significant injuries are recorded.
- [x] 4.3 Availability tests: availability derives from state (injury ⇒ Injured/Recovering; excessive fatigue ⇒ Resting; else Available); `canCompete()` only when Available.
- [x] 4.4 Boundary test: the Health domain changes no scoring, ranking, financial, progression, or shot-resolution state (REQ-225/227); `sim.health` imports only `core`.
- [x] 4.5 World tests: fields exclude unavailable golfers; competing accrues fatigue that is felt in shots; weeks recover fatigue and advance rehab; significant health events recorded; two worlds with the same seed produce identical physical state (reproducible).
- [x] 4.6 Run `openspec validate add-health --type change --strict` and resolve findings.
