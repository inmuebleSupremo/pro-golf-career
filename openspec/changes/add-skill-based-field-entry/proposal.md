## Why

A newly-created golfer can't enter tournament fields, so a player who onboards and advances the calendar never reaches an event (verified: default-config created golfer advanced 60 weeks with no pending event). The cause: a tour's field is drawn from season standings capped at `fieldSize`, and season standings **reset each season** to zero points — so at season start the tie-break decides the field, and it currently uses arbitrary **golfer-id order**. A created golfer (id `player-…`) sorts last and is buried, never plays, never scores, and stays buried (a catch-22). This blocks the core loop the frontend just shipped.

## What Changes

- Change the regular/signature field draw so that, when season-standings points are equal, golfers are ordered by **attribute-based ability** (the mean of a golfer's attributes) rather than by golfer id — a deterministic, merit-based tie-break. (The engine's live *form* rating was found to start uniform for every golfer and is unusable as an ordering signal — attributes are the real ability.)
- Raise the **created-golfer starting build** to a "talented prospect" level (above the entry-tier median) so a newly-created golfer is competitive enough to make entry-tier fields on merit. Ability-ordering alone was insufficient: a baseline created golfer sat at the entry-tier median and was cut by ~1 point.
- Effect: each season starts **ability-ordered** (all points reset to zero), and a created golfer now makes early-season fields, scores, and stays in contention — verified reaching a playable event at full world scale. Same rule for human and AI golfers (no special-casing; REQ-104).
- Majors (cross-tier elite fields) are unchanged in this change.

## Capabilities

### New Capabilities
- `competitive-entry`: The rule that a tournament field is drawn from tier standings and, on equal season points, ordered by golfer skill — so ability, not id order, decides who plays when results haven't yet separated the field, letting new golfers enter.

### Modified Capabilities
<!-- The field-draw behaviour described under event-prestige is refined here; no requirement is removed. Captured as a new capability to avoid rewriting archived deltas. -->

## Impact

- **Backend only**: `com.progolf.sim.world.World#buildEvent` field draw gains a skill tie-break; a small `skillOf(id)` helper reads `golfer.player().state().rating().value()`. Deterministic → snapshot round-trip determinism holds. Field composition on equal points changes from id-order to skill-order (more realistic); domain tests asserting specific id-order fields may need updating.
- **No API/schema/frontend change** — the frontend play loop already works once an event is reachable.
