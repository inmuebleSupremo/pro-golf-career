## 1. GolferState + PlayerState carry support

- [x] 1.1 `GolferState`: add `mentalSupport` and `strategicSupport` ([0,1], default 0) as canonical fields; keep backward-compatible constructors (2-arg, 4-arg equipment, `fresh()`) defaulting them to 0.
- [x] 1.2 `PlayerState`: add transient `mentalSupport`/`strategicSupport` + `setSupport(mental, strategic)` (validated [0,1]); `Player.toGolferState` surfaces them.

## 2. ShotResolver consumes support

- [x] 2.1 Caddie: multiply the mishit probability by `(1 - state.strategicSupport())` (folds into the existing management-relief term).
- [x] 2.2 Psychologist: compute `effectiveFatigue = state.fatigue() * (1 - state.mentalSupport())` and use it at both fatigue sites (the fatigue dispersion penalty and the fatigue carry reduction). Neutral at 0 → identical to today.

## 3. World sync + constants

- [x] 3.1 In the pre-play sync loop, `g.player().state().setSupport(effects.mentalSupport(), effects.strategicSupport())` from `supportTeams.get(id).effects()`.
- [x] 3.2 `StaffConstants`: update the caddie/psych comments (now applied); values unchanged.

## 4. Verification

- [x] 4.1 Neutrality: a shot / round with zero support equals the outcome with no support (existing shot & tournament tests unchanged, using neutral state).
- [x] 4.2 Caddie: over many seeds, strategic support yields no-greater mishit frequency and no-worse mean score than none.
- [x] 4.3 Psychologist: a fatigued golfer with mental support disperses no more than the same fatigued golfer without it; with zero fatigue, mental support has no effect.
- [x] 4.4 World: a golfer who employs a caddie/psychologist has the support synced into play (their `toGolferState` reflects it); two same-seed worlds remain byte-identical; re-base any golden multi-season assertion.
- [x] 4.5 Full suite + `ArchitecturePurityTest` pass.
- [x] 4.6 `openspec validate add-staff-shot-effects --type change --strict`.
