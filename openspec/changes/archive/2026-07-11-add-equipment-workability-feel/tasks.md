## 1. Bag aggregates + constants

- [x] 1.1 `EquipmentConstants`: add `WORKABILITY_SCALE` (0.30) and `FEEL_SCALE` (0.15).
- [x] 1.2 `GolfBag`: add `workabilityBonus()` and `feelBonus()` — `aboveBaseline(mean) × scale`, mirroring forgiveness/power (0 at baseline).

## 2. GolferState + PlayerState carry workability/feel

- [x] 2.1 `GolferState`: append `equipmentWorkability`, `equipmentFeel` ([0,1], default 0) as fields 7–8; canonical 8-arg ctor; keep 2-/4-/6-arg convenience ctors as prefixes (defaulting the new fields to 0).
- [x] 2.2 `PlayerState`: add transient `equipmentWorkability`/`equipmentFeel`; extend `setEquipment(forgiveness, power, workability, feel)`; `Player.toGolferState` surfaces all four.

## 3. ShotResolver consumes them

- [x] 3.1 Workability: `windResist = min(windResist + state.equipmentWorkability(), 1.0)` (softens crosswind + headwind). Neutral at 0.
- [x] 3.2 Feel: `sigmaDistance *= (1 - state.equipmentFeel())`. Neutral at 0.

## 4. World sync + docs

- [x] 4.1 `World`: pass all four bonuses — `setEquipment(bag.forgivenessBonus(), bag.powerBonus(), bag.workabilityBonus(), bag.feelBonus())`.
- [x] 4.2 `EquipmentCharacteristics` javadoc: workability and feel are now applied (not "data for future use").

## 5. Verification

- [x] 5.1 Neutrality: baseline gear (all bonuses 0) equals no-equipment influence (existing shot/tournament/equipment tests unchanged).
- [x] 5.2 Feel: in calm conditions, a bag with feel yields no-worse aggregate scores than none (tighter distance dispersion); with zero feel, no effect.
- [x] 5.3 Workability: in a crosswind, a bag with workability disperses/scores no worse than none; in calm conditions workability has no effect.
- [x] 5.4 World: the bag's four bonuses are synced into play (a golfer's `toGolferState` reflects workability/feel after `setEquipment`); two same-seed worlds stay byte-identical; re-base any golden multi-season value.
- [x] 5.5 Full suite + `ArchitecturePurityTest` pass.
- [x] 5.6 `openspec validate add-equipment-workability-feel --type change --strict`.
