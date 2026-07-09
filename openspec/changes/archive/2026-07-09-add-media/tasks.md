## 1. News model

- [x] 1.1 Create framework-free package `com.progolf.sim.media` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `MediaConstants` (per-type prominence, significance threshold, upset ranking threshold, narrative thresholds: veteran age, drought seasons, dominant wins, prospect age/rank, contender minimum seasons) as the single tunables surface.
- [x] 1.3 Define `NewsType` (TOURNAMENT_VICTORY, MAJOR_UPSET, WORLD_NUMBER_ONE, PROMOTION, RETIREMENT, CAREER_MILESTONE, INJURY, COMEBACK, SEVERE_WEATHER, RISING_PROSPECT) and an immutable `NewsEvent` (season, type, optional subject golfer id, headline, prominence) with an `isSignificant()` derived from prominence.

## 2. Generators & narrative

- [x] 2.1 Implement `NewsFactory` pure generators for each category (victory, maiden victory, upset, number-one, promotion, retirement, injury, comeback, severe weather, rising prospect), each taking real event primitives and returning a `NewsEvent` with a deterministic headline and prominence.
- [x] 2.2 Define `CareerNarrative` (RISING_PROSPECT, BREAKTHROUGH_SEASON, CONSISTENT_CONTENDER, DOMINANT_CHAMPION, VETERAN_RESURGENCE, CHAMPIONSHIP_DROUGHT, ESTABLISHED_PROFESSIONAL) with a description, `CareerSummary` (age, seasonsPlayed, careerWins, rankingPosition, seasonsSinceLastWin), and `NarrativeClassifier.classify(summary)`.

## 3. Media system

- [x] 3.1 Implement `MediaSystem`: an append-only feed with `publish(NewsEvent)`; tracks each golfer's last-win season from published victories; queries `feed()`, `significantNews()`, `newsForGolfer(id)`, `recent(n)`; and `careerNarrative(id, age, seasonsPlayed, careerWins, rankingPosition, currentSeason)` computing `seasonsSinceLastWin` from its own feed. No external mutation.

## 4. World wiring (modified: world-progression)

- [x] 4.1 In `World`, construct one `MediaSystem`; add read-only `newsFeed()`, `significantNews()`, `newsForGolfer(id)`, `careerNarrativeOf(id)` accessors.
- [x] 4.2 In `resolveEvent`, publish tournament victory, maiden victory (winner `wins() == 1`), and major upset (winner unranked or below the ranking threshold); publish severe weather where environmental history is recorded; publish injuries where health injuries are recorded.
- [x] 4.3 In `recoverHealth`, publish comebacks where health comebacks are recorded.
- [x] 4.4 In `seasonalTransition`, capture `tours.reviewSeasonEnd()`'s result for promotion news; detect a world number-one change against the previous leader; publish retirements; publish rising prospects (young, well-ranked, winless).
- [x] 4.5 Provide golfer display names from the `Player`/`Identity` (passed as strings so media stays core-only); no randomness anywhere.

## 5. Verification

- [x] 5.1 Generator tests: each generator references its real inputs (subject id, season) and sets a prominence; significant types are flagged significant.
- [x] 5.2 Classifier tests: clear cases map correctly (young winless well-ranked ⇒ rising prospect; early first wins ⇒ breakthrough; top-ranked many-time winner ⇒ dominant champion; old recent winner ⇒ veteran resurgence; past winner long without a win ⇒ drought).
- [x] 5.3 Media-system tests: publish appends to the feed; significant events remain discoverable; `newsForGolfer` filters by subject; last-win tracking drives `seasonsSinceLastWin`; the system mutates nothing external.
- [x] 5.4 Boundary test: the Media domain changes no score, ranking, or progression state (REQ-247/250); `sim.media` imports only `core`.
- [x] 5.5 World tests: resolving events and transitions publishes diverse news from real outcomes (victories, retirements, number-one/promotions appear); every published event references a real subject/season; career narratives are derivable; two worlds with the same seed produce identical feeds (reproducible).
- [x] 5.6 Run `openspec validate add-media --type change --strict` and resolve findings.
