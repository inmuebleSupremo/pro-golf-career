## 1. Tournament: one externally-scored competitor

- [x] 1.1 Add interactive-competitor state to `Tournament`: `designateInteractiveCompetitor(int fieldIndex)` (before play) and `submitInteractiveRoundScore(int roundNo, int scoreVsPar)`; store the designated index (nullable = unchanged) and a round→score map.
- [x] 1.2 In `playCompetitorRound` (or `playRound`), when a standing is the designated interactive competitor use the submitted round score instead of computing it; every other competitor resolves through the unchanged shared path. Guard: a submitted score must exist for that round.
- [x] 1.3 Interactive sudden-death: refactor `suddenDeath`/`resolvePlayoff` so the per-hole rival resolution (`playPlayoffHole`) is reachable by the orchestrator, and expose `beginPlayoff()` / `playoffContenders()` / `submitPlayoffHoleScore(...)` (or an equivalent per-hole seam) so the player's playoff hole can be supplied while tied rivals auto-resolve at the same playoff coordinate (`round = 90 + hole`). When the interactive competitor is not a contender, the playoff resolves automatically as today.
- [x] 1.4 With nothing designated, `Tournament` is byte-identical to today (existing tournament tests unchanged).

## 2. PlayableEvent orchestrator

- [x] 2.1 Add `com.progolf.sim.play.PlayableEvent` constructed from the confirmed `Tournament`, the player's `ProfessionalGolfer`, the player's field index, the `Course`, the `TournamentWeather`, and the seed context (worldSeed, season, tournamentId) + the player's sim `Strategy`.
- [x] 2.2 Build the player's per-round `PlayableRound`: base `SeedCoordinate(worldSeed, season, tournamentId, round, playerFieldIndex, 0, 0)`, 18 `HoleToPlay` from `course.holeModel(hole, round)` and `weather.conditionsForRound(round).environmentForHole(hole, exposure)`, the player's `GolferState` = `player.toGolferState(0.0)`, sim strategy = `player.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED)`.
- [x] 2.3 Interactive surface: `currentRound()` (the active `PlayableRound`), `situation()`, `playShot(ShotDecision)`, `simShot()`/`simHole()`/`simRound()`, `submitRound()` (locks the finished round score into the Tournament and advances the AI field for that round), `leaderboard()` (the live field leaderboard), `playerMadeCut()`.
- [x] 2.4 Round-by-round flow mirroring the Tournament lifecycle (R1, R2, CUT, R3, R4, complete/playoff): play the player's R1/R2, advance the cut, play R3/R4 only if `playerMadeCut()`; when the player misses the cut, advance the AI field's remaining rounds with no submitted player score.
- [x] 2.5 Interactive playoff: when the completed field leaves the player tied for the lead, drive sudden death hole by hole (player plays; rivals auto-resolve) via the Tournament playoff seam; skippable.
- [x] 2.6 `simEvent()` sims all remaining rounds and any playoff; `isPlayComplete()` and `result()` (the `TournamentResult`).

## 3. World: yield at the player's event, resume on completion

- [x] 3.1 Refactor `resolveEvent` into a **build** half (field draw + availability/resting gates + weather + fatigue/equipment sync + `TournamentDefinition` + confirmed `Tournament` + the context feedConsumers needs) and a **feedConsumers** half (the existing ranking/tours/careers/media/statistics/economy/health block, unchanged), shared by both paths.
- [x] 3.2 Add a `PendingPlayerEvent` holder (the `PlayableEvent`, the build context/result inputs, the week's event list, and the cursor index) and a nullable `pendingEvent` field on `World`.
- [x] 3.3 Restructure `advanceWeek` to iterate the week's events in order via a resumable helper: for each event, if the player is entered in it (designated, in the field, not resting) build it, designate the player interactive on the Tournament, wrap in a `PlayableEvent`, store `pendingEvent`, and return (pause) before recovery/transition/advance; otherwise resolve it automatically. When the loop finishes, run recovery, any seasonal transition, and the calendar advance.
- [x] 3.4 `completePlayerEvent()`: require the `PlayableEvent` play-complete; run feedConsumers on its `TournamentResult`; clear `pendingEvent`; resume the paused week from the next event.
- [x] 3.5 `advanceSeason`: after each `advanceWeek`, while a pending player event exists, `simEvent()` + `completePlayerEvent()` so bulk advance runs unattended.
- [x] 3.6 World accessors: `hasPendingPlayerEvent()` and `playerEvent()` (the `PlayableEvent` handle). Guard `advanceWeek`/`advanceSeason` against being called while an event is pending (must complete it first).

## 4. App seam

- [x] 4.1 `WorldService`: expose `hasPendingEvent`, the current `situation()`, the live `leaderboard()`, `playShot`/`simShot`/`simHole`/`simRound`/`submitRound`, `simEvent`, `playerMadeCut`, and `completeEvent`, delegating through `World`.

## 5. Verification

- [x] 5.1 Fidelity (the guardrail): a `Tournament` resolved fully automatically equals the same Tournament with the player designated interactive at index k and a `PlayableEvent` simmed in full — identical finishing order, per-competitor scores, cut, winner.
- [x] 5.2 Missed cut: a player whose R1+R2 miss the cut does not play R3/R4, the AI field finishes normally, and the result is consistent with automatic resolution.
- [x] 5.3 Interactive win: a player who plays and wins their event produces a result where they are the winner, and the world records the win (career/ranking/prize/media) on completion.
- [x] 5.4 Interactive playoff: a player tied for the lead plays sudden death hole-by-hole to a single winner; a simmed playoff reproduces the automatic winner for the same seed.
- [x] 5.5 Yield/resume: `advanceWeek` into the player's event leaves the world paused (season/week/calendar un-advanced, recovery not yet run); `completePlayerEvent` resumes the week; `advanceSeason` sims the pending event and completes the season.
- [x] 5.6 A world with no designated player, and events the player is not entered in (resting / out of field), resolve automatically with no yield (unchanged behaviour; existing world tests pass).
- [x] 5.7 Full suite + `ArchitecturePurityTest` pass (`play` stays framework-free; DAG preserved: `play` may import `tournament`; nothing lower imports `play`/`world`).
- [x] 5.8 Run `openspec validate add-playable-event --type change --strict` and resolve findings.
