## 1. EventPrestige + Tier rename (tournament)

- [x] 1.1 Add `com.progolf.sim.tournament.EventPrestige { REGULAR, SIGNATURE, MAJOR }` with `rankingWeight()` and `purseWeight()` (values from `TournamentConstants`) and `isMajor()`.
- [x] 1.2 Add prestige multiplier tunables to `TournamentConstants` (`SIGNATURE_RANKING_WEIGHT` 1.75, `MAJOR_RANKING_WEIGHT` 3.0, `SIGNATURE_PURSE_WEIGHT` 2.0, `MAJOR_PURSE_WEIGHT` 4.0).
- [x] 1.3 Rename `Tier.MAJOR` → `Tier.ELITE`; update `RankingConstants` (`BASE_MAJOR` → `BASE_ELITE`, `basePoints` case) and `World.mapTier` (ELITE tour → `Tier.ELITE`). Update the two ranking tests referencing `Tier.MAJOR`.
- [x] 1.4 `TournamentDefinition`: add `EventPrestige prestige` (canonical), plus a convenience constructor without prestige defaulting to `REGULAR` (keeps existing callers/tests compiling and byte-identical).
- [x] 1.5 `PrizeStructure`: add `standard(EventPrestige prestige)` scaling `TOP_PRIZE` by `prestige.purseWeight()`; keep `standard()` = REGULAR.

## 2. Ranking weighting

- [x] 2.1 `RankingPoints.award`: overload taking prestige — `basePoints(tier) × prestige.rankingWeight() × positionWeight × fieldStrength`; keep the no-prestige overload (REGULAR).
- [x] 2.2 `WorldRanking.record`: overload taking prestige, threaded to `award`; keep the no-prestige overload (REGULAR) for backward compatibility.

## 3. Career legacy

- [x] 3.1 `CareerStatistics`: add `majorsWon` + `recordResult(..., boolean majorWin)` overload (increment when `position == 1 && majorWin`); keep the existing overload.
- [x] 3.2 `Career.recordTournament(result, EventPrestige, LocalDate)`: thread prestige, fold major win; keep/deprecate the 2-arg form (default REGULAR) if any caller/test needs it.
- [x] 3.3 `HallOfFame.evaluate`: add a majors path (`majorsWon >= HOF_MIN_MAJORS`); add `HOF_MIN_MAJORS` to `CareerConstants`.

## 4. Statistics / records

- [x] 4.1 `Championship`: add `prestige` (string); World passes `prestige.name()`.
- [x] 4.2 `RecordType`: add `MOST_MAJOR_WINS`.
- [x] 4.3 `StatisticsArchive.observeEvent(outcome, tournamentName, tier, prestige, isWinner)`: record the championship's prestige, track per-golfer major-win counts, and `challenge(MOST_MAJOR_WINS, ...)` on a major win; add a major-champions/majors-won query.

## 5. Media

- [x] 5.1 `NewsFactory.majorVictory(season, winnerId, name, tournamentName)` — a distinct, high-prominence victory item.

## 6. World: schedule prestige + cross-tour major fields

- [x] 6.1 `ScheduledTournament`: add `EventPrestige prestige`.
- [x] 6.2 `WorldConfig`/`WorldConstants`: add `majorsPerSeason` (default 4) and `signatureEventsPerTier` (default 1); update `defaults()`.
- [x] 6.3 `generateSchedule`: mark the first `signatureEventsPerTier` events of each tier `SIGNATURE`; append `majorsPerSeason` majors (Tier.ELITE + MAJOR prestige, spread across weeks, strongest courses).
- [x] 6.4 `buildEvent`: for a major, draw the field cross-tour (top `fieldSize` active/canCompete/non-resting by current ranking, deterministic attribute fallback + id tiebreak); for regular/signature draw from the tour standings as today. Stamp the definition's prestige and prestige-scaled purse.
- [x] 6.5 `isPlayerEntered`: for a major, drop the tour-tier check (the player qualifies cross-tour; buildEvent's field/`playerFieldIndex` decides if they make it — a qualifying player plays their major as a `PlayableEvent`).
- [x] 6.6 `feedConsumers`: thread prestige into `ranking.record`, `career.recordTournament`, `statistics.observeEvent`, and publish `majorVictory` for a major win.

## 7. Verification

- [x] 7.1 Prestige weighting: for the same tier/position/field, MAJOR awards more ranking points than SIGNATURE than REGULAR; a major's purse (top prize) exceeds a signature's exceeds a regular's.
- [x] 7.2 Play is unaffected: a Tournament resolved at REGULAR vs MAJOR yields identical per-competitor scores (prestige changes rewards only).
- [x] 7.3 Legacy: winning a major increments `majorsWon`; a non-major win does not; a career meeting `HOF_MIN_MAJORS` is HoF-eligible.
- [x] 7.4 Statistics: a major championship is recorded with MAJOR prestige and is distinguishable; `MOST_MAJOR_WINS` tracks the leader and preserves the previous holder.
- [x] 7.5 World schedule: a season contains the configured signature events and majors; the schedule (including prestige) is reproducible across same-seed worlds.
- [x] 7.6 Cross-tour major: a major's field spans tiers (not one tour); a strong player is entered in a major and can play it; the world stays reproducible across seasons.
- [x] 7.7 Media: a major win publishes a high-prominence `majorVictory`.
- [x] 7.8 Full suite + `ArchitecturePurityTest` pass; rename compiles everywhere; existing shot/score/tournament tests unchanged.
- [x] 7.9 `openspec validate add-event-prestige --type change --strict`.
