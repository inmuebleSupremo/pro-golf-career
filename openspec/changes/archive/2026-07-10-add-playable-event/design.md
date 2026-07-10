# Design — add-playable-event

## Context

`add-playable-round` delivered `PlayableRound`: an interactive 18-hole round, provably byte-identical to `RoundResolver.resolveHole` when simmed. This change makes the player's *tournament* interactive so a played round counts. The three seams touched are `Tournament` (resolve one competitor externally), a new `PlayableEvent` orchestrator, and `World` (yield the player's event and resume on completion).

The load-bearing fact that makes this tractable: **each competitor's per-round score is independent of every other competitor.** `Tournament.playCompetitorRound` reads only that golfer's own attributes, state, seed coordinate, and the shared conditions — never another competitor's result. The cut affects *which* rounds a competitor plays, not the *score* of any round (each round is seeded independently of the cut). So the player's rounds can be produced interactively and substituted into the tournament's existing staging (R1 → R2 → CUT → R3 → R4 → PLAYOFF) without perturbing anyone else.

## Decisions

### D1 — Tournament gains one externally-scored competitor, rather than a re-implemented parallel event
The Tournament stays the single authority for the field draw, cut, leaderboard, playoff, and completion (REQ-104: same engine). We add the minimum seam: `designateInteractiveCompetitor(fieldIndex)` marks one standing, and `submitInteractiveRoundScore(round, scoreVsPar)` supplies its round score. In `playCompetitorRound`, the interactive standing's score is taken from the submitted map instead of being computed; every other competitor resolves through the unchanged path. With nothing designated, behaviour is byte-identical to today.

- **Alternative — re-implement cut/leaderboard/playoff inside `PlayableEvent`:** duplicates the competition rules and risks divergence from the AI path. Rejected: violates "same engine."
- **Alternative — pause *inside* `Tournament.advance()` for the player's shot:** turns the engine into a coroutine and couples it to interactivity. Rejected: the orchestrator paces play and submits a finished round score; the Tournament stays synchronous.

### D2 — `PlayableEvent` orchestrates the Tournament round-by-round, producing the player's rounds first
`PlayableEvent` owns the confirmed `Tournament` and the player's identity/field index. For each round the player participates in it builds a fresh `PlayableRound` for that round — base seed `SeedCoordinate(worldSeed, season, tournamentId, round, playerFieldIndex, 0, 0)`, 18 `HoleToPlay` from `course.holeModel(hole, round)` and `weather.conditionsForRound(round).environmentForHole(hole, exposure)` — lets the human play or sim it, then `submitInteractiveRoundScore(round, playerRound.scoreVsPar())` and `tournament.advance()` to resolve the AI field for that round. The mapping of `advance()` calls to lifecycle is exactly today's (R1, R2, CUT, R3, R4, complete/playoff); the player plays R1, R2, then (if they made the cut) R3, R4. If the player misses the cut, `playRound` already skips their standing for R3/R4, so no score is submitted for those rounds and the AI field finishes normally.

This ordering guarantees the submitted score is present before the Tournament plays that round. The player's `GolferState` is `player.toGolferState(0.0)` captured after the World's pre-play fatigue/equipment sync (identical to what the AI path reads), and the sim strategy is `player.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED)` — the exact expression `playCompetitorRound` uses — so a simmed player round equals the automatic one.

### D3 — Interactive sudden-death playoff (the chosen scope)
When round 4 ends with the player tied for the lead, the Tournament enters `PLAYOFF`. `PlayableEvent` drives it interactively: for each sudden-death hole the player plays a single hole (a one-hole `PlayableRound`, seeded with the same playoff coordinate automatic sudden death uses — `round = 90 + playoffHole`), and the tied rivals auto-resolve that hole through the Tournament; the low score(s) advance until one remains. The existing static `suddenDeath` is refactored so its per-hole rival resolution is reachable by the orchestrator, and a fully-simmed playoff reproduces the automatic winner (same seeds). If the player is not among the tied contenders, the whole playoff auto-resolves as today.

- **Alternative — auto-sim the playoff:** simpler, but the playoff is the single most career-defining moment the vision names (a walk-off putt). Chosen the interactive path per the scope decision; auto-sim remains available via `simEvent()`/`advanceSeason`.

### D4 — `World` yields the player's event via an explicit pending-event, and resumes on completion
`resolveEvent` is split into two halves that both paths share: **build** (draw the field, gate availability/resting, generate weather, sync fatigue+equipment, construct the `TournamentDefinition` and confirmed `Tournament`) and **feedConsumers** (ranking, tour standings, careers, media, statistics, economy, health — the existing block, unchanged). Automatic resolution runs build → `playToCompletion()` → feedConsumers as today.

For the player's event, `advanceWeek` iterates the week's scheduled events in order; when it reaches one the player is entered in, it runs *build*, wraps the confirmed Tournament in a `PlayableEvent`, stores a `PendingPlayerEvent` (the event, the built context needed by feedConsumers, the week's event list, and the cursor position), and **returns without** running recovery, the seasonal transition, or the calendar advance. `completePlayerEvent()` requires the `PlayableEvent` to be play-complete, runs feedConsumers on its real `TournamentResult`, clears the pending event, and resumes the paused week from the next event (remaining events → recovery → seasonal transition if season-end → calendar advance). `advanceSeason` sims any pending event to completion so bulk advance still runs the world unattended.

- **Alternative — a callback/continuation the app supplies:** more flexible but couples the engine to app-supplied code. Rejected: explicit pending-event state is simpler, testable, and snapshot-friendly for the later persistence slice.

### D5 — Whether to yield when the player intends to sim the whole event
The World always yields the player's event as a `PlayableEvent`; skippability lives on the handle (`simEvent()`), matching player-experience §6 ("play-when-it-happens, skippable"). The app decides whether to present shots or immediately sim. A world with no designated player, and any event the player is not entered in (resting, out of the field, or unassigned), resolves automatically with no yield — unchanged behaviour.

## Fidelity & determinism

The guardrail test: build a `Tournament` with a field and resolve it fully automatically → result A; build the identical Tournament (same seed/definition/field) with the player designated interactive at index k and drive a `PlayableEvent` that sims every round → result B. Assert A == B (finishing order, per-competitor scores, winner, cut line). This isolates the mechanic from world-level noise. A world-level test then advances a player into their event, sims it, completes, and asserts the result counted (career/ranking updated) and the world proceeded. Because assigning a player also changes sponsorship/development handling (`add-player-control`), full world byte-identity is not asserted here — the tournament-result equality is the precise fidelity claim.

## Risks

- **Ordering bug** — the interactive score must be submitted before the Tournament plays that round; `PlayableEvent` enforces the order and guards against submitting for a round the player did not play (missed cut). Covered by the fidelity test and a missed-cut test.
- **Resume correctness** — the paused week must resume with the exact remaining work and event order so a simmed player world stays consistent; the pending-event holds the week's event list + cursor, and `advanceSeason`'s sim path is tested to keep advancing.
