## Why

The `sim.player` package carries a complete but **dead** injury scaffold — `Injury` (type + severity + recovery + `performancePenalty`), `PlayerState.applyInjury`/`advanceInjuryRecovery`, `Player.applyInjury`/`advanceInjuryRecovery`, and the `CareerStatus.INJURED` status. It predates the Health, Fitness & Recovery domain (`sim.health`), which now owns injuries end-to-end for the live World: named body-area types, MINOR/MODERATE/SEVERE severities with rehab weeks, injury rolls scaled by fatigue/fitness/age, weekly rehabilitation, availability gating, and health history. The `sim.player` path has **zero callers in `main/`** (only its own unit tests) — the World never touches it. It is a near-exact duplicate of `sim.health.Injury` (identical `InjuryType` set and severities) whose only distinct concept, `performancePenalty`, is consumed solely by the also-dead `Player.deriveFormRating`. Two parallel injury models are a maintenance hazard and a false source of truth; the dead one should go.

`CareerStatus.INJURED` is orphaned by the same fact: nothing but the removed `applyInjury` sets it, and the health domain correctly models injury as a transient `Availability`, not a career-lifecycle stage. A golfer stays ACTIVE while hurt; their availability reflects the injury. So INJURED is removed from the career-status machine as well.

## What Changes

- **Remove** `sim.player.Injury` (record + `InjuryType` + `Severity`).
- **Remove** the injury plumbing from `PlayerState` (`injury` field, `injury()`, `hasActiveInjury()`, `applyInjury`, `advanceInjuryRecovery`) and from `Player` (`applyInjury`, `advanceInjuryRecovery`).
- **Remove** the injury term from `Player.deriveFormRating` (it keeps demonstrating derived stats via rating + fatigue).
- **Remove** `CareerStatus.INJURED` from the enum and its transition table; a hurt golfer stays ACTIVE (health `Availability` carries the injury signal). Update the retirement comment in `Career.java` accordingly.
- **Remove** `InjuryStateTest` (entirely about the removed path) and the injury references in `PlayerEntityTest`.
- No behavior change to the live World: `sim.health` is untouched and remains the sole injury model.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `player-entity`: **REMOVE** the "Injury State" requirement (superseded by the `injury-recovery` capability in `sim.health`); **MODIFY** the "Career Status State Machine" requirement to drop INJURED from the status set and its transitions.

## Impact

- `com.progolf.sim.player`: `Injury` (deleted), `PlayerState`, `Player`, `CareerStatus`.
- `com.progolf.sim.career.Career`: a stale comment referencing the INJURED transition.
- Tests: `InjuryStateTest` (deleted), `PlayerEntityTest` (injury lines removed).
- No change to `sim.health`, `World`, serialization, or any live simulation path.
