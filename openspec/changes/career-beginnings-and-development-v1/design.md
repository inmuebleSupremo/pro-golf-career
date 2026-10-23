## Context

`GolferFactory` currently derives a created golfer's potential from Playing Style and starting attributes from age-based maturity. Human Development Points are banked at the seasonal transition and spent manually through `World.spendDevelopmentPoints`; successful purchases already create season-stamped `DEVELOPMENT` attribute changes. `WorldService` projects banked points to GraphQL and independently checks whether Development belongs in the Career Inbox. This change keeps those seams and does not alter AI allocation.

## Goals / Non-Goals

**Goals:** distinct deterministic starts; gradual, manually directed growth; one authoritative purchase rule across mutation, training, and Inbox; unchanged old-career ratings and balances.

**Non-Goals:** persistent background narratives or identity display, AI parity, economy/tour redesign, new training resources or routes, and cleanup of legacy `developmentFocus` documentation.

## Decisions

### Background modifies the created build, not potential

Keep Playing Style's current potential and relative strengths/weaknesses, then derive age-based starting attributes as today. Apply a uniform background adjustment to those initial attributes, bounded by each attribute's potential. Elite Amateur has zero adjustment and therefore exactly matches existing creation for every age and Playing Style. Initial age-18 All-Rounder calibration targets are about 60 / 63 / 69–70, corresponding roughly to -3 / 0 / +6 or +7; tune the nonstandard offsets against focused gameplay. The background is a creation input with an omitted-input default of Elite Amateur. Existing saved golfers are never regenerated, and V1 does not require a persistent background label.

### The calendar season owns purchased-growth allowance

Start with a human base award of 240 Development Points and a maximum of +6 *purchased* gains per attribute per calendar season. Keep the current age-stage, performance, and coach scaling of awards and the existing cost curve/potential ceiling. These are V1 defaults to be adjusted only after gameplay validation. AI awards and automatic allocation remain unchanged.

For each human attribute, sum positive `DEVELOPMENT` changes stamped with the world's current calendar season. The mutation checks the proposed raises against that cumulative total before any point debit or attribute change, rejecting the whole invalid request. `AGING` changes do not consume allowance. The allowance resets when the calendar advances into the next season; seasonal-transition aging and point awards occur before that advance and do not count as purchases. Banked points remain untouched when a cap, potential, or affordability blocks spending.

### One eligibility calculation serves every surface

Expose a read-only per-attribute purchase option from the engine or an equivalent shared domain rule: current-season allowance remaining, potential headroom, next-point cost, whether one point is currently purchasable, and its blocking reason. The mutation remains the final authority and revalidates the entire submitted bundle. The application maps these options into the `playerDevelopment` GraphQL read; the existing training screen uses them for controls and explanation. `WorldService` qualifies the Development Inbox item when at least one option is purchasable, preserving the active Inbox change's current-state projection and all other source rules. Client-side pending-cost previews may remain, but cannot establish eligibility independently.

### Older careers retain their state

Supported saves already capture golfer attributes, potential, banked Development Points, calendar position, and attribute-change history. Derive current-season purchased gains from that history without adding a background field or annual counter. Previously purchased gains above the new limit are grandfathered; no further purchase for that attribute is allowed until next season. Verify the supported legacy save shape. If a supported older save lacks usable current-season purchase history, treat prior purchases as untracked for that one season, preserve its attributes and bank, and record/enforce all purchases made after load; do not infer or subtract points from ratings. Save/reload must reproduce the same eligibility and subsequent results.

## Risks / Trade-offs

- A Club Champion near 60 may still face poor early cut and prize outcomes in the default 640-golfer world. Tune its opening offset with a small default-world viability check; do not broaden into economy rebalance.
- A focused player may deliberately bank many points after reaching one attribute's annual limit. This is acceptable choice; routine inability to spend across *all* improvable attributes is a balancing defect.
- Existing `player-development` and `world-progression` prose describes automatic or identical AI/human allocation. This change specifies the human manual-purchase path without expanding into unrelated legacy-spec cleanup.
- `add-career-inbox` is active. Do not edit its files here; coordinate its eventual canonical `career-inbox` delta so Development eligibility includes affordability, potential, and seasonal allowance. Other Inbox sources and acknowledgements remain unchanged.

## Validation / Migration

No planned save-schema migration. Verify older-save load, same-season multi-transaction and save/reload enforcement, transition reset, aging exclusion, atomic rejection, and consistency of GraphQL availability and Inbox. Run a small default-world gameplay sample for the three backgrounds and both focused and balanced development over multiple seasons. Adjust the provisional numbers only if results fail the stated career viability and pacing goals.
