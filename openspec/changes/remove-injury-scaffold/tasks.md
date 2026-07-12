## 1. Delete the injury data type

- [x] 1.1 Delete `sim/player/Injury.java` (record + `InjuryType` + `Severity`).

## 2. Remove injury plumbing from player state/entity

- [x] 2.1 In `PlayerState`, remove the `injury` field, `injury()`, `hasActiveInjury()`, `applyInjury`, `advanceInjuryRecovery`, the `this.injury = null` initialiser, and the now-unused `java.util.Optional` import; update the class doc (drop the REQ-021 injury mention).
- [x] 2.2 In `Player`, remove `applyInjury` and `advanceInjuryRecovery`.
- [x] 2.3 In `Player.deriveFormRating`, remove the `injuryPenalty` term (keep the rating + fatigue computation).

## 3. Remove the INJURED career status

- [x] 3.1 In `CareerStatus`, remove the `INJURED` constant, drop it from `ACTIVE`'s allowed set, and remove the `INJURED` transition row; update the enum doc.
- [x] 3.2 Update the stale comment in `Career.retire()` (ACTIVE/INJURED→RETIRED becomes ACTIVE→RETIRED).

## 4. Tests

- [x] 4.1 Delete `InjuryStateTest` (entirely about the removed path).
- [x] 4.2 In `PlayerEntityTest`, remove the `applyInjury` line from `stateChangesNeverMutatePermanentAttributes` and confirm the `deriveFormRating` test still passes.

## 5. Verify

- [x] 5.1 Repo-wide search confirms no remaining reference to `sim.player` `Injury`, `applyInjury`, `advanceInjuryRecovery`, or `CareerStatus.INJURED` (the `sim.health` injury path is untouched).
- [x] 5.2 Full backend test suite compiles and passes.
