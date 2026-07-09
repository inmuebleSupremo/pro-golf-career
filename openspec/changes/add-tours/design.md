## Context

Tournaments run and produce `TournamentResult`s, golfers exist, and the World Ranking measures rolling prestige. What's missing is competitive *structure*: which golfers play where, and how they rise or fall. This change adds `sim.tour`: a tiered ladder of Tours, per-golfer membership, and season-standings-driven promotion/relegation. It is the orchestration layer above tournaments — analytical and deterministic, consuming results and organising competition, never mutating gameplay.

Season boundaries come from an explicit season-review the World will drive later (calendar generation stays deferred). Framework-free, Java 21 / Spring Boot 3, consistent with existing `sim.*`.

## Goals / Non-Goals

**Goals:**
- A `TourTier` ladder and `Tour`s (one per tier for V1) with a complete entry→elite pathway.
- Exactly-one primary membership per golfer; membership-based eligibility with invitation exceptions.
- A resetting Season Standings ledger (cumulative season points from results), distinct from the World Ranking.
- A deterministic season-end review that promotes top-N / relegates bottom-N per tier, records movement, and takes effect next season.
- Reproducible from the same results; analytical only; clean dependency DAG.

**Non-Goals:**
- Shot resolution, ranking calculation, finances, tournament scoring (REQ-140).
- Calendar / season cadence (the World drives the review).
- Multiple tours per tier (V1 is a linear ladder; extensible later).
- Wiring movement into `Career` history (a consumer concern; Tours record their own movement history).

## Decisions

### D1. `TourTier` ordered enum is the ladder; `Tour` is one-per-tier in V1
`TourTier` (DEVELOPMENT < SECONDARY < PRIMARY < ELITE) declares rank by order, with `above()`/`below()` as `Optional`. A `Tour` (id, name, tier) is created one per tier, giving a clean linear promotion/relegation ladder and a complete pathway (REQ-127/137). *Why:* the ladder is what makes movement meaningful; one-per-tier keeps V1 simple. *Alternative rejected:* multiple tours per tier now — more structure than V1 needs; the tier enum leaves room to add it.

### D2. `TourSystem` is the analytical engine owning memberships, standings, and movement history
It holds `membership: Map<golferId, TourTier>`, a `SeasonStandings`, a `movementHistory` list, and a season index. It never touches players or tournament results beyond reading finishing positions. *Why:* REQ-140 boundary — Tours organise, they don't compute out-of-domain concerns. *Alternative rejected:* spreading membership onto the `Player` — violates single ownership and the domain boundary.

### D3. Season Standings = per-golfer cumulative points from results, reset at review
`SeasonStandings` maps golferId → season points; `award(golferId, position)` adds `pointsFor(position)` (a decreasing curve in `TourConstants`); `reset()` clears it. It is explicitly the *second* ledger, separate from `sim.ranking`'s rolling decayed points. *Why:* REQ-130/136 and the two-ledger decision — season standings drive movement, world ranking measures prestige. *Alternative rejected:* reusing the ranking ledger — conflates prestige with promotion and breaks the yearly reset.

### D4. Season-end review moves top-N up and bottom-N down per tier, deterministically
`reviewSeasonEnd()` groups members by tier; within each tier it sorts by season points descending (tie-break golfer id); the top `PROMOTE_COUNT` move to the tier above (if any), the bottom `RELEGATE_COUNT` to the tier below (if any); movements are recorded, memberships updated (effective next season = applied now at the boundary), standings reset, season index incremented. *Why:* REQ-130/131/136 performance-based movement; sorting + tie-break makes it deterministic and reproducible (REQ-139). *Alternative rejected:* any randomness or variety-balancing — explicitly forbidden (REQ-130/139).

### D5. Eligibility is membership-based with an explicit invitation exception
`isEligible(golferId, tier, invited)` returns `membership==tier || invited`. Invitations are a documented exception surface, not hidden logic. *Why:* REQ-129/133.

### D6. Qualification pathways are transparent and go through recorded membership grants
The primary pathway is the season review (top of a tour qualifies for the one above). An explicit `grantMembership(golferId, tier, reason)` supports other pathways (qualifying school, development-tour graduation), always recorded in movement history. *Why:* REQ-132 transparency; keeps every membership change auditable.

### D7. Tournament→Tour allocation is carried on the result-recording call
Results are fed as `recordResult(result, tier)` — the tour the tournament belonged to. The "exactly one Tour per tournament" invariant lives at allocation time (a tournament has one tier). *Why:* keeps Tours decoupled from the tournament engine while honoring REQ-129; the World coordinator supplies the association.

## Risks / Trade-offs

- **[Promotion/relegation counts and points curve are uncalibrated]** → Isolate in `TourConstants`; assert structural properties (top rises, bottom falls, deterministic) now; tune magnitudes with the World/economy later.
- **[Small tiers could over-churn]** → Guard counts against tier size (never promote/relegate more than a fraction of members); assert a tier can't be emptied by a single review.
- **[Movement effective "next season" without a live calendar]** → Apply at the review boundary and increment the season index; the World loop calls review between seasons, so "applied now" == "effective next season". Documented.
- **[Duplicate/însufficient membership on registration]** → Enforce exactly-one membership on register and on every move; a golfer always has precisely one tier.
- **[Coupling temptation with Career/Ranking]** → Tours record their own movement history; wiring into Career history or ranking eligibility is a consumer concern, kept out of this domain.

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) `TourTier` + `Tour` + `TourConstants` + `TourMovement`/`MovementType` + `SeasonReviewResult`; (2) `SeasonStandings` (points curve, award, reset); (3) `TourSystem` — registration/membership (exactly one), eligibility + invitation exception, tournament allocation via `recordResult`; (4) `reviewSeasonEnd()` promote/relegate per tier with deterministic ordering, movement recording, standings reset, season increment; (5) qualification grant pathway; (6) test suites (single membership; one tour per tournament; standings accumulate + reset; review promotes top / relegates bottom; determinism/reproducibility; movement recorded; multi-season upward migration; boundary — no mutation of players/results). Each layer testable before the next.

## Open Questions

- Number of tiers and members per tier for V1 — start with 4 tiers and small counts in `TourConstants`; the World/population sizing refines it.
- Exact promotion/relegation counts and the season-points curve — placeholder constants; tune against desired churn once the World loop runs multiple seasons.
- Whether invitation criteria (rankings, past champions) live here or are supplied by consumers — expose the exception flag now; consumers compute the criteria later.
- How Tour movement notifies Career/Ranking — emit movement records now; a World coordinator wires them to those domains later.
