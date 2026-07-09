## Context

The World already produces every event a sports media desk would report — winners and upsets (`TournamentResult` + ranking positions), world number-one changes (`rankingSnapshots`), promotions/relegations (`tours.reviewSeasonEnd()` returns `TourMovement`s the World currently discards), retirements (the `retirees` list), career milestones (maiden wins detectable from `CareerStatistics.wins()`), injuries/comebacks (`healthHistory`), and severe weather (`environmentalHistory`) — but nothing narrates them. This change adds a `sim.media` domain that turns those real outcomes into a persistent, discoverable news feed and an evolving per-golfer career narrative, wired in at the World's existing seams. It is the first purely observational domain: it reads and reports, and writes back to nothing.

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: news and narrative are pure functions of real event data — there is no randomness anywhere in the domain.

## Goals / Non-Goals

**Goals:**
- A world-owned Media System that automatically publishes diverse News Events from real outcomes.
- Narrative integrity: every News Event references real data; nothing is fabricated.
- A persistent, discoverable World Narrative surfacing events beyond the player's direct experience.
- An evolving, descriptive Career Narrative per golfer recognising diverse forms of success.
- Wire into the World deterministically; media affects no outcome (neutrality).

**Non-Goals:**
- Player-facing presentation/prioritisation UI, long-form prose, or editorial tone (Presentation; personalisation is exposed as data only).
- Media influencing any simulation outcome, ranking, progression, or mechanic (forbidden, REQ-247/250).
- Any randomness — the narrative is a deterministic function of history.

## Decisions

### D1. `sim.media` depends only on `core`; the World feeds it primitives
The Media System is constructed and driven by the World, which passes primitives (ids, names, tiers, ranking positions, seasons, severities) into pure `NewsFactory` generators and reads back `NewsEvent`s and narratives. `media` imports only `core`; it never imports `tournament`, `ranking`, `career`, `world`, etc. *Why:* REQ-250 responsibility boundary and the established pattern (every recent domain depends only on core; the World orchestrates). *Alternative rejected:* media importing the other domains to read their state — couples the observer to everything it observes and risks it reaching back.

### D2. Media is a pure, neutral observer — it writes back to nothing
`MediaSystem` only accumulates `NewsEvent`s and answers queries. It exposes no operation that mutates another domain, and the World never lets media influence resolution, ranking, or progression. *Why:* REQ-247 (media alters no outcome, no hidden advantage, communicates only) and REQ-242 (interpret history, do not replace it). *Alternative rejected:* media feeding "storylines" back into gameplay — violates neutrality.

### D3. News is generated from real outcomes at the World's existing seams
The World publishes at the points where the source events already occur: in `resolveEvent` — tournament victory, maiden victory (winner's `wins() == 1`), major upset (winner unranked or ranked below a threshold), injuries, and severe weather; in `recoverHealth` — comebacks; in `seasonalTransition` — world number-one changes (comparing the season-end ranking leader to the previous), promotions (from the captured `SeasonReviewResult`), retirements, and rising prospects. Every event references real data. *Why:* REQ-241/242/243 — news originates from actual gameplay and covers events beyond the player. *Alternative rejected:* a post-hoc scan that re-derives events — duplicates logic and risks divergence from what actually happened.

### D4. `NewsEvent` carries a prominence; significance drives discoverability
`NewsEvent(season, type, subjectGolferId?, headline, prominence)` — prominence (0–100) is assigned per type and instance (a world-number-one change outranks a routine promotion). Historically significant events (prominence ≥ a threshold) remain discoverable via a dedicated query; personalisation orders by relevance to a golfer without changing validity. *Why:* REQ-246 (significant events discoverable), REQ-249 (prioritisation is presentation only). *Alternative rejected:* a boolean "significant" flag — too coarse for ordering and personalisation.

### D5. Career Narrative is a descriptive classification recomputed from accumulated history
`NarrativeClassifier.classify(CareerSummary)` maps `(age, seasonsPlayed, careerWins, rankingPosition, seasonsSinceLastWin)` to a `CareerNarrative` (RISING_PROSPECT, BREAKTHROUGH_SEASON, CONSISTENT_CONTENDER, DOMINANT_CHAMPION, VETERAN_RESURGENCE, CHAMPIONSHIP_DROUGHT, ESTABLISHED_PROFESSIONAL). `MediaSystem` tracks each golfer's last-win season from published victories, so `seasonsSinceLastWin` — and thus drought/resurgence — comes from its own feed. It is recomputed on demand, so it evolves as a career accrues. *Why:* REQ-244 (evolving, descriptive, from accumulated events) and REQ-248 (diverse forms of success). *Alternative rejected:* a stored, mutated narrative — drifts from the underlying history; a prescriptive narrative — the spec requires descriptive.

### D6. World wiring: own the Media System, publish at seams, expose reads
`World` holds one `MediaSystem`. It captures `tours.reviewSeasonEnd()`'s result (today discarded) for promotion news and tracks the previous number one for change detection. Golfer display names come from the World (`Player`/`Identity`), passed as strings so media stays core-only. Read accessors: `newsFeed()`, `significantNews()`, `newsForGolfer(id)`, `careerNarrativeOf(id)`. *Why:* REQ-240 (world maintains the system), REQ-245 (world narrative belongs to the world). *Alternative rejected:* generating news inside `Tournament`/`Career` — those must not own media (REQ-250).

## Risks / Trade-offs

- **[Media leaking into gameplay]** → `media` imports only `core`; `MediaSystem` exposes no external mutation; an architecture/boundary test asserts media changes no score/ranking/progression state, and the World never reads media back into resolution (REQ-247/250).
- **[Fabricated or inconsistent narrative]** → every generator takes real event data as input and is published only when that event actually occurs; a test asserts each News Event references a real subject/outcome and that no news exists without a corresponding event (REQ-242).
- **[Determinism]** → no randomness; news/narrative are pure functions of history; the world reproducibility test is extended to assert two same-seed worlds produce identical feeds.
- **[Feed volume]** → prominence gates significance and ordering so the feed stays navigable; magnitudes isolated in `MediaConstants`; tests assert structure (diverse types appear; significant events discoverable), not volume.
- **[Narrative thresholds uncalibrated]** → all thresholds (upset rank, veteran age, drought seasons, dominant wins, prospect age/rank) live in `MediaConstants`; tests assert the classifier separates clear cases, not exact tunings.

## Migration Plan

Greenfield `sim.media` + one additive modification (World publishing at existing seams). Sequencing: (1) `MediaConstants`, `NewsType`, `NewsEvent`; (2) `NewsFactory` generators for each category; (3) `CareerNarrative`, `CareerSummary`, `NarrativeClassifier`; (4) `MediaSystem` (feed, publish, last-win tracking, queries, career-narrative); (5) wire the World — own the system, capture the season-review result, publish victories/upsets/maiden-wins/injuries/severe-weather in `resolveEvent`, comebacks in `recoverHealth`, number-one/promotions/retirements/rising-prospects in `seasonalTransition`, expose accessors; (6) tests — generators reference real data and set prominence; classifier separates the narrative cases; the system preserves and makes significant events discoverable and neutral; boundary; world publishes diverse news from real events and stays reproducible (identical feeds). Each layer testable before the next.

## Open Questions

- Exact prominence values and narrative thresholds — placeholder `MediaConstants`, tuned later against a desired feed feel.
- Whether promotions to lower tiers are newsworthy — V1 publishes all promotions at modest prominence, marking top-tier ones significant; revisit with Presentation.
- Whether the career narrative should also be published as a News Event on notable transitions — exposed on demand in V1; a later change can emit transition news.
- How personalisation ranking is surfaced — prominence + subject are exposed now; the Presentation layer computes player-relative ordering.
