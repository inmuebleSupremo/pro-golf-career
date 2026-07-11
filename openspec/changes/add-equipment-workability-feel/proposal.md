## Why

An equipment item carries four characteristics — forgiveness, power, **workability**, and **feel** — and upgrades raise all four (a better club is uniformly better). But only forgiveness and power reach the shot: `EquipmentCharacteristics` is explicit that *"V1 applies forgiveness and power … and keeps workability and feel as data for future use."* So half of what you pay for when you buy a club is inert. Now that buying equipment is a real player decision, that is a shallower choice than the data implies. This change makes **all four characteristics matter** by wiring the bag's aggregate workability and feel into shot resolution, through the same neutral-by-default seam forgiveness and power already use.

## What Changes

- **The bag aggregates workability and feel** into above-baseline bonuses (`GolfBag.workabilityBonus()` / `feelBonus()`), exactly like forgiveness and power — zero for standard gear, positive for upgrades.
- **The shot engine consumes them (spec: shot-resolution):**
  - **Workability → ball-flight control in wind.** Higher workability raises the golfer's effective wind resistance, reducing the crosswind dispersion penalty and the headwind carry loss. A workable bag holds its line in the wind.
  - **Feel → distance control / proximity.** Higher feel tightens the *distance* dispersion, so shots finish closer to the intended distance (better proximity to the pin). A bag with feel is more precise on the number.
- **`GolferState` carries the two new inputs** (`equipmentWorkability`, `equipmentFeel`, [0,1], default 0), appended after the existing inputs so every shorter constructor stays valid; `PlayerState.setEquipment(...)` takes all four bag bonuses and `Player.toGolferState` surfaces them. The World already syncs the bag before play — it now passes all four.
- Update `EquipmentConstants` (`WORKABILITY_SCALE`, `FEEL_SCALE`) and the `EquipmentCharacteristics` doc (no longer "data for future use").

Explicitly out of scope: per-club influence — the bag is still an aggregate mean across categories (a known simplification; a specialist wedge helping only wedge shots needs the per-club model, deferred); no change to how equipment is bought, costed, or generated; no change to forgiveness/power.

## Capabilities

### Modified Capabilities
- `equipment-influence`: the active bag's workability and feel now influence shot resolution — workability improves ball-flight control in wind, feel improves distance control — in addition to forgiveness and power, so all four characteristics are felt.
- `shot-resolution`: a shot additionally consumes the golfer's equipment workability and feel — workability raises effective wind resistance, feel tightens distance dispersion — neutral by default so standard equipment is unchanged.

## Impact

- **Codebase**: `GolfBag` (+two aggregate bonuses), `EquipmentConstants` (+two scales), `GolferState` (+two inputs, appended; new convenience constructors as prefixes), `PlayerState.setEquipment` (four bonuses) + `Player.toGolferState`, `ShotResolver` (consume workability at wind resistance, feel at distance dispersion), `World` (pass all four bonuses in the pre-play sync), `EquipmentCharacteristics` doc. No change to equipment generation, cost, or the acquisition policy.
- **Determinism / reproducibility**: neutral (baseline gear) → both bonuses 0, so all existing shot and tournament tests reproduce exactly and same-seed worlds stay byte-identical. Golfers who upgrade now also gain workability/feel, so multi-season world outcomes shift from before (identically for same seeds); direction-based world tests hold and any golden value is re-based.
- **Boundary**: the Equipment domain still only exposes characteristics; the shot engine reads the bag and computes the shot (`equipment-influence` boundary preserved).
