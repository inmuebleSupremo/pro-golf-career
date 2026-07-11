# Design — add-equipment-workability-feel

## Context

`GolfBag` aggregates forgiveness and power into above-baseline bonuses the shot engine reads; workability and feel are carried on every item (upgrades set all four via `uniform(quality)`) but never aggregated or applied. This change adds the two missing aggregates and picks shot-engine hooks that are differentiated from forgiveness/power and meaningful in the world.

## Decisions

### D1 — Two more bag aggregates, same neutral-by-default seam
`GolfBag.workabilityBonus()` and `feelBonus()` mirror the existing forgiveness/power aggregates: `aboveBaseline(mean) × scale`, zero at the standard baseline. They flow through `PlayerState.setEquipment(forgiveness, power, workability, feel)` and `Player.toGolferState` into `GolferState`, and the World passes all four in the pre-play sync it already performs. Standard gear → 0 → byte-identical play.

### D2 — The hooks: workability → wind control, feel → distance control
Chosen to be distinct from forgiveness (general dispersion) and power (reach):
- **Workability** raises the golfer's **effective wind resistance**: `windResist = min(windResist + equipmentWorkability, 1)`. Wind resistance already damps the crosswind dispersion penalty and the headwind carry loss, so a workable bag holds its line in wind. In calm conditions it has no effect (there is no wind term) — realistic.
- **Feel** tightens the **distance dispersion**: `sigmaDistance *= (1 - equipmentFeel)`, so shots finish nearer the intended distance (proximity/touch). It leaves lateral dispersion to forgiveness, so the two do not simply duplicate.
Both are neutral at 0.

### D3 — Append the new `GolferState` inputs to avoid constructor ambiguity
`GolferState` already has 6 positional fields (fatigue, pressure, forgiveness, power, mentalSupport, strategicSupport). Adding two doubles risks ambiguous overloads. The new fields are **appended** as positions 7–8 (`equipmentWorkability`, `equipmentFeel`), so the existing 2-, 4-, and 6-arg convenience constructors remain natural prefixes and every current call site (including the staff test's 6-arg support form and the equipment test's 4-arg form) keeps its exact meaning. Equipment ends up split across positions 3–4 and 7–8 — a small readability cost documented in the record, taken to avoid churn and ambiguity.

## Risks

- **Over-negating wind** — `windResist` is clamped at 1, so workability never turns wind into a tailwind bonus; at realistic full-upgrade bonuses (~0.14) it only softens wind. A test asserts a bag with workability disperses no worse than a plain bag in a crosswind.
- **Feel overlapping forgiveness** — feel is confined to distance dispersion, forgiveness spans both; they stack but are not duplicates. A calm-conditions test asserts feel improves distance-controlled scoring with no wind dependence.
- **Reproducibility ripple** — upgraded AI golfers now also gain workability/feel, shifting multi-season world outcomes; neutral tests are unchanged and same-seed worlds stay identical (direction-based world tests; re-base any golden value).
