## Why

The world is full of stories no one is telling. Golfers win, get upset by unknowns, reach world number one, earn promotions, retire, go down with injuries and battle back, all under record-breaking weather — and every one of those moments already happens in the simulation and then vanishes unremarked. Media, News & World Narrative is the layer that gives the living world a voice: it watches the real event stream and turns it into a persistent, discoverable feed of news, so the world feels active whether or not the player is watching, and every golfer accrues an evolving story — the breakthrough kid, the dominant champion, the veteran resurgence, the long drought. It is the connective tissue that makes fifteen deterministic engines read like a sport with a history.

## What Changes

- Add a world-owned **Media System** (REQ-240/247) that observes significant World events and generates **News automatically** from actual outcomes — it communicates information only and never alters gameplay, provides no hidden advantage, and mutates nothing.
- Generate diverse **News Events** (REQ-241/248) from real gameplay: tournament victories, major upsets (a low-ranked winner), world **number-one changes**, promotions, retirements, maiden victories and other career milestones, injuries and comebacks, and severe-weather tournaments — recognising many forms of success, not only champions.
- Guarantee **narrative integrity** (REQ-242): every News Event references real World data, none are fabricated, and the narrative interprets history without replacing it.
- Maintain a persistent **World Narrative** (REQ-243/245/246): a feed belonging to the World that surfaces events beyond the player's direct experience (rival results, tour developments, records), with historically significant events remaining **discoverable** long after they occur.
- Derive an evolving, descriptive **Career Narrative** per golfer (REQ-244/248/249) from accumulated events — rising prospect, breakthrough season, consistent contender, dominant champion, veteran resurgence, championship drought — descriptive rather than prescriptive, and available to prioritise for the player without changing what is true in the world.
- **Wire the Media System into the World** (modifies `world-progression`): the World publishes news as it resolves events and runs seasonal transitions, and exposes the feed and per-golfer narratives — deterministically, without media affecting any outcome.

Explicitly out of scope, per REQ-247/250: the Media domain is **not responsible for tournament simulation, rankings, player progression, world progression, or gameplay mechanics** — it reads real outcomes and produces news and narrative, and never writes back to any domain. Also deferred: player-facing presentation/prioritisation UI (Presentation; personalisation is exposed as data, not rendered), long-form generated prose, and configurable editorial tone. Media is deterministic and pure — every News Event is a function of real event data with no randomness — so a seeded world produces an identical narrative.

## Capabilities

### New Capabilities
- `news-generation`: A world-owned Media System that automatically generates News Events from actual gameplay outcomes across diverse categories, where every event references real World data (no fabrication) and the system is neutral — it communicates information only and never alters outcomes or grants advantage (REQ-240/241/242/247).
- `world-narrative`: A persistent World Narrative belonging to the World, emerging from tournament history, careers, rankings, retirements, and achievements, that communicates events beyond the player's direct experience and keeps historically significant events discoverable (REQ-243/245/246).
- `career-narrative`: An evolving, descriptive Career Narrative for every golfer, emerging from accumulated career events and recognising diverse forms of success (not only champions), which may be prioritised for the player as presentation without changing that all world events remain equally valid (REQ-244/248/249).

### Modified Capabilities
- `world-progression`: The World runs the Media System — publishing News Events as it resolves events and runs seasonal transitions, and exposing the news feed and per-golfer career narratives — deterministically and without media affecting any simulation outcome.

## Impact

- **Codebase**: New framework-free `com.progolf.sim.media` package (`NewsEvent`, `NewsType`, `NewsFactory`, `CareerNarrative`, `CareerSummary`, `NarrativeClassifier`, `MediaSystem`, `MediaConstants`). `World` gains a `MediaSystem` it publishes to at event resolution and seasonal transition, capturing the tour season-review movements it currently discards, plus feed/narrative accessors.
- **Determinism**: news generation and narrative classification are pure functions of real event data — no randomness — so a seeded world yields an identical narrative; this is verified by extending the world reproducibility test to the news feed.
- **DAG**: `media` depends only on `core`. The World feeds it primitives (ids, names, tiers, positions, seasons, severities) and reads back news/narratives, so `media` imports no other domain; only `world` imports `media`.
- **Downstream consumers (future changes)**: the Presentation layer renders the feed and prioritises by the player's career; Statistics/records can cross-reference historically significant news; Persistence stores the feed.
- **Boundary/risk**: media is a pure observer — it reads real outcomes and writes only news/narrative, never touching scores, rankings, progression, or mechanics (REQ-247/250); the only World change is additive publishing at existing seams, guarded by the full-run reproducibility test.
