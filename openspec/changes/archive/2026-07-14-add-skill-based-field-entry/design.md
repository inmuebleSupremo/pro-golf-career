## Context

`World#buildEvent` draws a regular/signature field as `tours.standings(tier)` (members ordered by season points desc, then golfer-id asc) filtered by availability/commitment, capped at `config.fieldSize()`. `SeasonStandings.reset()` clears points each season, so at season start every golfer has 0 points and the golfer-id tie-break alone orders the field. `World.createPlayer` appends a golfer with id `player-<hex>`, which sorts after the generated population — so the created golfer never makes an initial field, never scores, and stays excluded. Both World Ranking and season standings are purely results-based, so neither helps a golfer with no results; ability (skill rating) is the only signal that separates unraced golfers.

## Goals / Non-Goals

**Goals:** let a competent new golfer enter entry-tier fields by tie-breaking equal season points with skill; keep it deterministic and apply the same rule to all golfers.

**Non-Goals:** changing season-standings or World-Ranking semantics; changing the major (cross-tier) field draw; guaranteeing a specific golfer a spot (still merit-based — a weak build may still miss the field); any API/frontend change.

## Decisions

### D1 — Tie-break the field draw by attribute-based ability

**Decision:** In `buildEvent`, sort the filtered candidate ids by `Comparator.comparingInt(tours::seasonPointsOf).reversed().thenComparing(id -> abilityOf(id), reverseOrder).thenComparing(naturalOrder)` before `limit(fieldSize)`, where `abilityOf(id)` is the mean of the golfer's attributes. The final id comparator preserves total-order determinism.

**Why:** season points remain the primary signal (results earned this season win); ability only orders golfers whom results haven't separated (season start, or anyone yet to score). This makes early-season fields talent-ordered — realistic — and fixes the new-golfer catch-22. Applies uniformly (REQ-104).

**Diagnostic finding that shaped this:** the first attempt tie-broke on the live skill rating (`player().state().rating().value()`) — but that rating is a live *form* value that starts at a uniform 50.0 for every golfer, so it can't order unraced golfers (all tied → falls back to id). Attributes are the real ability signal.

**Alternatives rejected:** special-casing the human player into their entered events (violates same-rules); seeding an initial World-Ranking value from attributes (changes ranking semantics, broader blast radius).

### D2 — Raise the created-golfer starting build to a talented prospect

**Decision:** Bump `PopulationConstants.CREATION_BASELINE` (50 → 56), the baseline every created attribute starts from (used only by `GolferFactory.buildAttributes`, i.e. created humans — not the AI population). This lifts a created golfer above the entry-tier median.

**Why:** ability-ordering alone was insufficient — a diagnostic showed a baseline created golfer (overall ~50) sat exactly at the Development median and was cut from a top-N field by ~1 point (34 of 73 members ranked higher). A "talented prospect" build (overall ~56) makes entry-tier fields on merit. Isolated to created golfers, so the generated field is unchanged.

## Risks / Trade-offs

- **Field composition changes on equal points (id-order → skill-order)** → determinism is preserved (round-trip snapshot tests unaffected), but domain tests that assert exact finishers/scores under the old id-order fields may change and need updating. Mitigation: run the full suite; update assertions that legitimately changed (the old id-order fields were not meaningful).
- **A weak created build may still miss the field** → intended (merit-based); a reasonable entry-tier build should qualify. Verify with a test and the frontend E2E at default scale.
