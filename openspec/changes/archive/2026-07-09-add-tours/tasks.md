## 1. Value types

- [x] 1.1 Create framework-free package `com.progolf.sim.tour` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Define `TourTier` ordered enum (DEVELOPMENT < SECONDARY < PRIMARY < ELITE) with rank and `above()`/`below()` as `Optional`.
- [x] 1.3 Define `Tour` (id, name, tier) and `TourConstants` (promotion/relegation counts, season-points curve) as the single tunables surface.
- [x] 1.4 Define `MovementType` (PROMOTION, RELEGATION, QUALIFICATION), immutable `TourMovement` (golferId, fromTier, toTier, season, type), and `SeasonReviewResult` (season, movements).

## 2. Season Standings

- [x] 2.1 Implement `SeasonStandings` — golferId → cumulative season points; `pointsFor(position)` a decreasing curve; `award(golferId, position)`; `reset()`.
- [x] 2.2 Provide sorted standings for a set of members (points descending, deterministic golfer-id tie-break).

## 3. Tour system & membership

- [x] 3.1 Implement `TourSystem` holding the tier ladder, `membership` map, current `SeasonStandings`, movement history, and season index.
- [x] 3.2 Register golfers with exactly one primary membership; enforce exactly-one on every change; `membership(golferId)` accessor.
- [x] 3.3 Implement `isEligible(golferId, tier, invited)` — membership-based with an invitation exception.
- [x] 3.4 Implement `recordResult(TournamentResult, tier)` — associate the event with one tour and award season points to finishers by position (analytical; never mutates the result).

## 4. Season-end movement

- [x] 4.1 Implement `reviewSeasonEnd()` — per tier, sort members by season points; promote top `PROMOTE_COUNT` to the tier above (if any); relegate bottom `RELEGATE_COUNT` to the tier below (if any).
- [x] 4.2 Guard counts against tier size so a review can never empty a tier.
- [x] 4.3 Apply movements to membership (effective next season), record each `TourMovement` in history, reset standings, and increment the season index; return a `SeasonReviewResult`.

## 5. Qualification

- [x] 5.1 Implement `grantMembership(golferId, tier, reason)` for transparent qualification pathways, recorded as a QUALIFICATION movement.

## 6. Verification

- [x] 6.1 Membership tests: exactly one membership; changing membership replaces the previous; recorded in history.
- [x] 6.2 Allocation/eligibility tests: eligibility derives from membership; invitation exception works; a tournament associates to one tour.
- [x] 6.3 Standings tests: points accumulate (better finish more); standings reset each season; deterministic ordering with tie-break.
- [x] 6.4 Review tests: top performers promoted, bottom relegated per counts; highest tier has no promotion, lowest no relegation; a tier is never emptied.
- [x] 6.5 Determinism test: the same season's results produce identical promotions/relegations; no randomness.
- [x] 6.6 Movement-record test: promotions, relegations, and qualifications are recorded in history.
- [x] 6.7 Migration test: across several seasons, a consistently strong golfer rises through tiers.
- [x] 6.8 Boundary test: the tour package mutates no player attributes or tournament results (source-level and behavioural checks).
- [x] 6.9 Run `openspec validate add-tours --type change --strict` and resolve findings.
