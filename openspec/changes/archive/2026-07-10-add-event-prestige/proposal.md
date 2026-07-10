## Why

The world already models the tour *ladder* (Elite / Primary / Secondary / Development), but every event **within** a tour is identical: the same purse (`PrizeStructure.standard()` everywhere), the same ranking weight for its tier, no notion of a marquee event. There are no **majors** — the pinnacle, cross-tour events that are the marquee accomplishments of a career (player-experience §8, a confirmed V1 decision). Without them, scheduling has no real texture (every week is interchangeable), a career has no summit to chase, and the "meaningful risk / long-term investment" pillars have nothing to point at.

This change adds **event prestige** — a dimension orthogonal to the tour tier. Each event is **Regular**, **Signature** (an elevated event within a tour), or a **Major** (a cross-tour marquee event drawing the strongest field across all tiers). Prestige weights **ranking points**, **prize money**, and **career legacy**, and a major victory is the biggest news in the world. It is the substance behind meaningful scheduling (the next roadmap step exposes event-by-event choice to the player) and it dovetails with the just-shipped playable event: a player who qualifies gets to **play their major**.

Naming note: the existing tournament `Tier.MAJOR` is a misnomer — it means "an Elite-tour event," not a major championship. This change renames it `Tier.ELITE` so `Tier` cleanly mirrors the tour ladder and the word "major" means what it should.

## What Changes

- Add **`com.progolf.sim.tournament.EventPrestige`** { `REGULAR`, `SIGNATURE`, `MAJOR` } carrying a ranking-weight and a purse-weight multiplier (tunables in `TournamentConstants`), plus `isMajor()`. `TournamentDefinition` gains a `prestige` (a backward-compatible convenience constructor defaults it to `REGULAR`).
- **Rename `Tier.MAJOR` → `Tier.ELITE`** (and `RankingConstants.BASE_MAJOR` → `BASE_ELITE`, the `basePoints` case, and `World.mapTier`). Purely a rename; the tier's weight is unchanged.
- **Ranking**: points are additionally scaled by event prestige — `basePoints(tier) × prestige.rankingWeight() × positionWeight × fieldStrength`. `WorldRanking.record` and `RankingPoints.award` take the prestige (backward-compatible overloads default to `REGULAR`), so a major carries far more ranking points than a regular event of the same tier.
- **Prize**: the purse scales with prestige — `PrizeStructure.standard(prestige)` multiplies the top prize by `prestige.purseWeight()`, so signature events and majors pay more. The Economy is unchanged; it awards whatever prize each finish carries.
- **Legacy**: `CareerStatistics` tracks **majors won**; `Career.recordTournament` takes the prestige and folds a major victory. The Hall-of-Fame criteria weight majors (a majors threshold joins the wins/consistency paths).
- **Statistics/records**: `Championship` records the event's prestige; the archive adds a `MOST_MAJOR_WINS` record and a major-champions query. `StatisticsArchive.observeEvent` takes the prestige.
- **Media**: a major victory publishes a distinct, high-prominence `NewsFactory.majorVictory` instead of the regular victory item.
- **World schedule**: `generateSchedule` designates a configurable number of **signature** events per tier and adds a configurable number of **majors** per season (cross-tour, spread across the calendar, on the strongest courses). `WorldConfig`/`WorldConstants` gain `majorsPerSeason` and `signatureEventsPerTier`.
- **Cross-tour major fields**: `buildEvent` draws a major's field from the **strongest active golfers across all tiers** by current ranking (with a deterministic attribute fallback before the ranking exists), rather than one tour's standings; regular and signature events draw from their tour as before. The player is entered in a major on the same cross-tour basis (and, qualifying, plays it as a `PlayableEvent`).
- Thread the prestige through `World.feedConsumers` to ranking, prize, legacy, statistics, and media.

Explicitly out of scope: no change to shot resolution or scoring (prestige affects rewards, never play — every existing score is reproduced bit-for-bit); no tour-tier purse scaling (a pre-existing calibration item); no qualification/exemption model beyond ranking-based major fields; no player-facing schedule *choice* (that is the next slice, "management breadth").

## Capabilities

### New Capabilities
- `event-prestige`: Events carry a prestige — Regular, Signature, or Major — that weights ranking points, prize money, and career legacy; majors are cross-tour marquee events drawing the strongest field across all tiers, and a major victory is the world's biggest news.

### Modified Capabilities
- `tournament-definition`: a Tournament definition additionally carries an event prestige.
- `world-ranking`: ranking points earned in an event are weighted by its prestige, so majors and signature events are worth proportionally more.
- `world-schedule`: a season's schedule includes elevated signature events within tours and a set of cross-tour majors, in addition to regular tour events.
- `career-legacy`: a career tracks majors won, and majors weigh into Hall-of-Fame recognition.
- `records-archive`: the historical archive records each championship's prestige and tracks the most major championships won.

## Impact

- **Codebase**: new `EventPrestige`; modified `Tier` (rename), `TournamentDefinition`, `PrizeStructure`, `TournamentConstants`, `WorldRanking`/`RankingPoints`/`RankingConstants`, `CareerStatistics`/`Career`/`HallOfFame`/`CareerConstants`, `Championship`/`StatisticsArchive`/`RecordType`, `NewsFactory`, `ScheduledTournament`/`World`/`WorldConfig`/`WorldConstants`. A handful of tests update for the `Tier.MAJOR`→`ELITE` rename.
- **Determinism**: majors scheduling and the cross-tour field draw are deterministic (ranking order with an id tiebreak, attribute fallback before ranking exists). Prestige changes rewards only, so all shot/score/reproducibility tests are unaffected; the world stays reproducible from its seed.
- **DAG**: unchanged direction — `EventPrestige` lives in `tournament`; `ranking`, `statistics`, `career`, `world` already depend on `tournament`/consume its primitives. Nothing lower imports `world`.
- **Boundary**: `World` remains a pure coordinator — it sets each event's prestige and field, and feeds the prestige-weighted result to the owning domains, which each own their own computation.
