## Why

The tournament engine now produces complete results — finishing order, winner, field — but nothing measures a golfer's standing *across* events. World ranking is how the game answers "who is the best right now?", and it is a dependency for invitations, sponsorships, milestones, and the player's sense of progress. It is the most natural consumer of what we can already produce: a pure, analytical function of tournament results. Building it now turns a pile of individual events into a living competitive order.

## What Changes

- Define the **World Ranking**: every eligible golfer has exactly one current ranking, derived from ranking points earned in tournaments (REQ-141).
- Define **ranking eligibility** (REQ-142): eligible golfers appear in the ranking; ineligible golfers (e.g. retired) are removed from the active ranking while their historical records are preserved.
- Define the **points model** (REQ-143): a completed Tournament awards ranking points to each competitor as a function of finishing position, the Tournament's tier, and field strength. Points **decay over a rolling ~2-year window**; a golfer's ranking value is the sum of their decayed points, and the ranking is the ordering by that value.
- Time comes from each Tournament's **`scheduledDate`**; decayed values are computed relative to an **as-of date**. No calendar/World domain is required — that remains deferred.
- Include a **field-strength bootstrap** (REQ-143): tier-based base points scaled by a field-strength proxy, so early events rank sensibly before rankings have converged.
- Guarantee **ranking integrity** (REQ-147): identical methodology for every golfer regardless of control type; the ranking is deterministic and reproducible from the same set of results.
- Preserve **historical rankings** (REQ-144): previous rankings, career-high ranking, weeks at World #1, and season-ending ranking; record **ranking movement** and milestone entry/exit (REQ-145); and take **ranking snapshots** at significant moments (REQ-148). History is append-only.
- Recognise **prestige milestones** (REQ-149): reaching Top 100 / Top 50 / Top 10 / World #1.
- Establish the **read-only dependency contract** (REQ-146): other systems consume ranking information without modifying it; the Ranking System is the single authoritative source of competitive standing (REQ-150).

Explicitly out of scope: the resetting **Season Standings** ledger and tour promotion/relegation (REQ-126–140) — that belongs to the deferred Tour domain; any calendar/season orchestration (dates come from tournament results); and any modification of gameplay — the ranking is analytical only.

## Capabilities

### New Capabilities
- `world-ranking`: The current ranking of eligible golfers derived from a rolling, decaying, field-strength-weighted points model, with eligibility, deterministic updates from tournament results, and the domain boundary (REQ-141/142/143/147/150).
- `ranking-history`: Preserved historical rankings, career-high, weeks at #1, season-ending ranking, recorded movement, and snapshots — append-only and permanent (REQ-144/145/148).
- `ranking-recognition`: Prestige recognition of Top 100 / Top 50 / Top 10 / World #1, and the read-only dependency contract making the ranking the single source of competitive standing (REQ-146/149).

### Modified Capabilities
<!-- None. This change DEPENDS ON existing capabilities — tournament-completion (TournamentResult),
     tournament-definition (tier, scheduledDate), player-entity/professional-golfer (ids) — but changes
     none of their requirements. It consumes results; it does not modify the tournament engine. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.ranking` package in the existing `backend/` module. It consumes `tournament.TournamentResult` and player ids; it holds a ledger of dated ranking awards and computes decayed standings on demand. No changes to existing packages.
- **Determinism**: Points are a pure function of results; there is no RNG. The same set of tournament results and an as-of date always yield the same ranking (REQ-147).
- **Downstream consumers (future changes)**: Tournament invitations, sponsorships/economy, awards, and the Tour qualification system will all read the world ranking. The ranking-value and standing types defined here become the read-only contract those domains consume.
- **Deferred seams**: field strength uses a bootstrap proxy until rankings converge; "weeks at #1" and season-ending snapshots derive from tournament dates rather than a real calendar (the World/calendar domain refines this later). Each is a documented seam, not a hidden shortcut.
