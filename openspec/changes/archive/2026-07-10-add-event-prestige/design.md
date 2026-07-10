# Design — add-event-prestige

## Context

The tour ladder (`TourTier`) models *which tour* a golfer belongs to; the tournament `Tier` is a 1:1 mirror of it used to weight ranking points. Every event is otherwise identical — same purse, same weight for its tier, no marquee events. This change adds **event prestige** as a second, orthogonal dimension and introduces **majors** as cross-tour marquee events. Prestige is a *reward* weight: it must never touch shot resolution or scoring (so the whole engine stays reproducible), only the ranking/prize/legacy that a completed result feeds.

## Decisions

### D1 — Prestige is a new dimension orthogonal to the tour tier
`EventPrestige { REGULAR, SIGNATURE, MAJOR }` is independent of `Tier`. An event has both: a `Tier` (its competitive base level) and a `prestige` (its elevation). Ranking weight = `basePoints(tier) × prestige.rankingWeight()`; purse = `basePurse × prestige.purseWeight()`. This composes cleanly: a signature Primary-tour event and a regular Elite event can differ independently, and a major sits at the top via the largest multiplier.

- **Alternative — fold prestige into `Tier` (add MAJOR/SIGNATURE as tiers):** conflates "which tour you belong to" (membership, promotion/relegation) with "how big is this event," and majors are explicitly cross-tour (no single tour). Rejected.

### D2 — Rename `Tier.MAJOR` → `Tier.ELITE`
`Tier.MAJOR` currently means "an Elite-tour event," which directly collides with the real concept this change adds. Leaving it would be a genuine trap (`tier == Tier.MAJOR` reads as "is this a major?" but is not). The rename (5 references across 4 files) makes `Tier` = `{DEVELOPMENT, STANDARD, PREMIER, ELITE}` mirror `TourTier` exactly, and frees "major" for `EventPrestige.MAJOR`. Weights are unchanged (`BASE_MAJOR` → `BASE_ELITE`, same value).

### D3 — Majors are cross-tour; signature events stay within a tour
A **major** draws its field from the strongest active golfers **across all tiers** by current World Ranking (deterministic, id-tiebroken; a mean-attribute fallback before the ranking exists in early season 1). Its base tier is `ELITE` and its prestige `MAJOR`, so it carries the highest ranking/prize weight and the strongest field — the pinnacle. A **signature** event is a tour's flagship: same within-tour field draw as a regular event, but elevated purse and points (its differentiation is reward and prestige, not a different field, since a tour's field is already its top standings). This matches real golf (majors are cross-tour marquees; signature events are elevated tour stops).

### D4 — Backward-compatible signatures so play/score tests are untouched
Prestige affects only rewards. To keep the large body of shot/score/tournament tests byte-identical:
- `TournamentDefinition` gets a convenience constructor without prestige (defaults `REGULAR`).
- `WorldRanking.record` and `RankingPoints.award` get overloads without prestige (default `REGULAR`).
- `PrizeStructure.standard()` stays (`REGULAR`); `standard(prestige)` scales it.
Existing tests change only for the mechanical `Tier.MAJOR` → `Tier.ELITE` rename. New behaviour gets new tests.

### D5 — Weight calibration (tunable in `TournamentConstants`)
Ranking-weight multipliers: `REGULAR 1.0`, `SIGNATURE 1.75`, `MAJOR 3.0`. Purse-weight multipliers: `REGULAR 1.0`, `SIGNATURE 2.0`, `MAJOR 4.0`. So an Elite major is worth `100 × 3.0 = 300` base ranking points (vs `100` for a regular Elite event) and pays `4×` the standard top prize — decisively the biggest prizes and the most ranking movement, as the vision requires. Values are placeholders on the tunables surface; they are monotone (major > signature > regular) by construction.

### D6 — Legacy: majors won as a first-class career statistic
`CareerStatistics` gains `majorsWon`, folded when a golfer's finish is a win (`position == 1`) in a major. `Career.recordTournament(result, prestige, date)` threads the prestige. `HallOfFame.evaluate` adds a majors path (`majorsWon >= HOF_MIN_MAJORS`) alongside the existing wins/consistency paths — majors are the marquee accomplishment. `StatisticsArchive` records each `Championship`'s prestige and maintains a `MOST_MAJOR_WINS` record plus a major-champions query.

### D7a — A golfer plays at most one event per week; majors take precedence
Because a major's field is cross-tour, a strong golfer could otherwise be drawn into both a major and their own tour's event in the same week. That is unrealistic and would double-count fatigue, earnings, and ranking. So within a week the World resolves majors **first** and tracks the golfers committed to an event; every subsequent (tour) event that week excludes already-committed golfers. A qualifying golfer therefore plays only the major, and the concurrent tour event becomes an opposite-field event of the remaining players (skipped entirely if no eligible field remains). This is deterministic (majors-first is a stable sort; the committed set is insertion-ordered) and preserves reproducibility.

### D7 — World schedules prestige; consumers receive it
`generateSchedule` marks the first `signatureEventsPerTier` events of each tier as `SIGNATURE` and appends `majorsPerSeason` majors (cross-tour, spread across the weeks, on the strongest courses). `ScheduledTournament` carries the `prestige`. `buildEvent` selects the field (tour standings for regular/signature; cross-tour ranking for majors) and stamps the definition's prestige and purse; `feedConsumers` passes the prestige to ranking, career, statistics, and media. The player is entered in a major on the cross-tour basis, so a qualifying player plays their major as a `PlayableEvent` (the prior slice) with no special-casing.

## Risks

- **Determinism of the cross-tour draw** — must be a total order. Uses ranking position, then a stable id tiebreak; before any ranking exists, mean attribute then id. Covered by a reproducibility test (two same-seed worlds identical across seasons).
- **Rename churn** — mechanical; the compiler and the existing ranking tests catch every site.
- **Weight balance** — majors could dominate ranking too much or too little; the multipliers are isolated tunables and covered by tests asserting the monotone ordering (major > signature > regular) rather than exact values.
