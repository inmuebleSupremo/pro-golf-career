## 1. Determinism harness (write the test first, let it drive)

- [ ] 1.1 Add a `WorldSnapshotRoundTripTest` with a structured world **digest** (rankings, tour standings + memberships, per-career age/statistics/runtime, finances, media feed, Hall of Fame, calendar position, counters) and a helper asserting `snapshot → restore → advance N` digest equals `advance N` digest.
- [ ] 1.2 Start it red against an autonomous world over a few seasons; extend snapshot + digest together until green.

## 2. Golfer graph snapshots (player domain) — DONE

- [x] 2.1 `PlayerState` snapshot (fatigue, live rating; transient equipment/support/injury inputs default 0 between events).
- [x] 2.2 `Player` snapshot (id, identity, attributes, status, attribute-change log) + rebuild (`Player.restore`, bypasses the transition machine).
- [x] 2.3 `ProfessionalGolfer` snapshot (player snapshot + fixed-strategy policy captured as a `Strategy`) + rebuild.

## 3. Career snapshot (career domain) — DONE

- [x] 3.1 `CareerStatistics` snapshot + `restoreFrom` (in place, statistics is a final field).
- [x] 3.2 `Career` snapshot (age, statistics, milestones, history, seasons, runtime state, Hall-of-Fame result) + `restore(player, snapshot)` via a private no-validation constructor. Also `WorldCalendar.restoreTo(season, week)`.

## 4. Per-golfer domain snapshots

- [x] 4.1 `FinancialAccount` snapshot (funds, earnings buckets, sequence, ledger, milestones, agreements, momentum) + `restore` via a private no-op constructor (no opening transaction).
- [x] 4.2 `SupportTeam` and `EquipmentInventory` (+ `TournamentLoadout`) snapshots + rebuild. Health `PhysicalState` is already an immutable record — capture directly.

## 5. Registry snapshots

- [x] 5.1 `TourSystem` snapshot (memberships, season standings via `SeasonStandings.snapshot`/`restoreFrom`, movement history, season). Tours rebuilt deterministically.
- [x] 5.2 `WorldRanking` snapshot (award ledger replayed via `RankingLedger.restore`, ineligible set).
- [x] 5.3 `StatisticsArchive` snapshot (career/seasonal stat lines, championships, record book, consecutive cuts, seasons appeared, major wins).
- [x] 5.4 `MediaSystem` snapshot (feed, last-win-season).

## 6. World bookkeeping + history

- [x] 6.1 `TournamentResultSnapshot` (finishes by golfer id) + `SeasonArchive` rebuild against the registry; capture `seasonResults` and `archives`.
- [x] 6.2 Capture the remaining World state: `activeGolfers`, Hall-of-Fame registry (inductions/members/retirementSeason), `rankingSnapshots`, environmental/health history, `previousNumberOne`, announced prospects, `schedule`, counters (`nextTournamentId`, `replenishCounter`), and the calendar position. (`committedThisWeek` is cleared each week-start, so it is not captured.)

## 7. World snapshot / restore

- [x] 7.1 `WorldSnapshot` record aggregating all of the above.
- [x] 7.2 `World.snapshot()` — reject if a player event is pending or a player is assigned (autonomous-only this slice).
- [x] 7.3 `World.restore(masterSeed, config, WorldSnapshot)` — build the shell (regenerate courses/weather/markets), then rebuild in dependency order (golfers → careers → per-golfer maps → registries → history).

## 8. Verify

- [x] 8.1 Round-trip determinism green across several seeds and season counts (including snapshot mid-season and at a season boundary).
- [x] 8.2 `snapshot()` on a player world and on a pending-event world both throw. Full backend suite green.
