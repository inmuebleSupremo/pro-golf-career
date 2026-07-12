## 1. Health domain: eligibility, impairment, rehab freeze

- [x] 1.1 Add an `impairment` value to `InjurySeverity` (MINOR ~0.05, MODERATE ~0.15, SEVERE ~0.30) with an accessor.
- [x] 1.2 In `PhysicalState`, add `canPlayThroughInjury()` (true iff `availability() == RECOVERING`) and `injuryImpairment()` (the injury's severity impairment when `canPlayThroughInjury()`, else 0.0). Leave `canCompete()` (AVAILABLE-only) unchanged.
- [x] 1.3 In `HealthSystem`, add `recoverWeek(state, age, boolean competed)`: recover fatigue as today, but advance rehab by one week only when `!competed`; keep a 2-arg overload delegating with `competed = false`.

## 2. Shot engine: impairment input

- [x] 2.1 Append an `injuryImpairment` component (in [0,1], neutral 0) to `GolferState`, with validation, an updated `fresh()`, and a back-compat 8-arg convenience constructor so existing call sites keep compiling.
- [x] 2.2 Add `INJURY_SIGMA_WEIGHT`, `INJURY_MEAN_WEIGHT`, and `PUTT_INJURY_PENALTY` to `SimConstants`.
- [x] 2.3 In `ShotResolver`, apply `state.injuryImpairment()` (raw, NOT reduced by `mentalSupport`): widen `sigmaLateral`/`sigmaDistance`, trim mean carry, and lower putt make-rate. Zero impairment must leave the math identical to today.

## 3. Player state carrier

- [x] 3.1 In `PlayerState`, add a transient `injuryImpairment` shot input (scalar in [0,1], default 0) with getter + setter, documented as synced from `sim.health` before play like fatigue.
- [x] 3.2 In `Player.toGolferState`, pass `state.injuryImpairment()` into the `GolferState`.

## 4. World wiring

- [x] 4.1 Add a `canEnterField(id)` helper = `canCompete() || (isPlayer(id) && canPlayThroughInjury())` and use it in the regular-field filter and in `majorField` (replacing the bare `canCompete()` checks).
- [x] 4.2 In `isPlayerEligible`, allow entry when `canCompete() || canPlayThroughInjury()`.
- [x] 4.3 In `buildEvent`'s pre-play sync loop, set each competitor's `injuryImpairment` from `physicalStates.get(id).injuryImpairment()` (0 for all but a grinding player).
- [x] 4.4 In `recoverHealth`, pass `committedThisWeek.contains(id)` as the `competed` flag to `recoverWeek`.

## 5. Tests

- [x] 5.1 Health test: `canPlayThroughInjury()`/`injuryImpairment()` true+scaled for Recovering (MINOR always; MODERATE/SEVERE tail), false+0 for early Injured and healthy; `recoverWeek(competed=true)` freezes rehab, `competed=false` advances it.
- [x] 5.2 Shot test: monotonic degradation — higher `injuryImpairment` widens dispersion / worsens scores; and mental support does NOT relieve it; zero impairment reproduces prior outcomes.
- [x] 5.3 World test: a controlled player Recovering from an injury who enters an event competes (impaired) and does NOT advance rehab that week; the same player who skips heals; an AI Recovering golfer stays out of fields. Reproducibility (same-seed worlds identical) still holds.
- [x] 5.4 Confirm existing shot/world/calibration tests stay green (zero-impairment neutrality).

## 6. Verify

- [x] 6.1 Full backend suite green.
- [x] 6.2 Drive a short scenario (a controlled golfer with a Recovering injury) to confirm grind-vs-rest: entering keeps rehab frozen and scores worse; resting heals.
