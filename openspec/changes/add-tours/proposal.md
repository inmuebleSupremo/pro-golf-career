## Why

Tournaments exist, but every event is an island — there is no competitive structure that says *where* a golfer plays, why some events are more prestigious, or how a golfer rises from developmental golf to the elite level. Tours are that structure: a tier hierarchy with membership and season-standings-driven promotion and relegation. This is the orchestration layer above tournaments and the last big competitive-structure piece — it turns a flat set of events into a career ladder, and it provides the eligibility and movement that the World loop, economy, and objectives will all lean on.

## What Changes

- Define a **Tour** (REQ-126): name, tier, and its own identity; Tours are persistent world entities that organise competition.
- Organise Tours into a **tier hierarchy** (REQ-127): higher tiers hold stronger competition, lower tiers are developmental, and the hierarchy supports upward and downward movement. A complete **development pathway** from entry level to elite must exist (REQ-137).
- Allocate every **Tournament to exactly one Tour** (REQ-129); tournament eligibility derives primarily from Tour membership, with invitation events as defined exceptions (REQ-133).
- Give each golfer **exactly one primary Tour membership** at a time (REQ-128); membership determines eligibility and is recorded in history. Provide **qualification** pathways (REQ-132) that let a golfer earn a Tour, transparent to the player.
- Add a resetting **Season Standings ledger** — cumulative season points awarded from tournament results by finishing position, reset each season — the separate second ledger (distinct from the rolling World Ranking) that drives movement.
- At season end, run a **membership review** (REQ-136): promote top performers to a higher tier and relegate bottom performers to a lower tier per published rules (REQ-130/131). Movement is **performance-based, deterministic, never random** (REQ-130/139), recorded in history, and takes effect at the following season's start. Over time stronger golfers migrate up and developing golfers have a real route up (REQ-134).
- Keep each **Tour independent** (REQ-138): its own membership and records; the world coordinates Tours without merging them.

Explicitly out of scope: shot resolution, World Ranking calculations, financial management, and tournament scoring (REQ-140) — Tours consume tournament results and organise competition, nothing more; calendar/schedule generation and season cadence (the World loop drives the season-review operation); and the economy tie-in of tour prestige. Tours are analytical: they never mutate player attributes or tournament results.

## Capabilities

### New Capabilities
- `tour-structure`: The Tour entity, the tiered hierarchy supporting up/down movement, tournament-to-tour allocation with membership-based eligibility (and invitation exceptions), tour independence, and a complete development pathway (REQ-126/127/129/133/137/138/140).
- `tour-membership`: Exactly-one primary Tour membership per golfer, membership-derived eligibility recorded in history, and transparent qualification pathways (REQ-128/132).
- `tour-movement`: The resetting Season Standings ledger, and the season-end membership review that promotes/relegates per published rules — performance-based, deterministic, reproducible, recorded, effective next season (REQ-130/131/134/136/139).

### Modified Capabilities
<!-- None. This change DEPENDS ON existing capabilities — professional-golfer/player (ids),
     tournament-definition (Tier), tournament-completion (TournamentResult) — but changes none of
     their requirements. Tours organise tournaments and consume their results; they do not modify them. -->

## Impact

- **Codebase**: New framework-free `com.progolf.sim.tour` package in the existing `backend/` module. It references golfer/tournament ids and `tournament.Tier`, consumes `tournament.TournamentResult`, and holds tour structure, memberships, the season-standings ledger, and movement history. No changes to existing packages.
- **Determinism**: Season points and promotion/relegation are pure functions of the recorded results and the published thresholds; the same results reproduce the same movement (REQ-139).
- **Downstream consumers (future changes)**: the World/season loop will drive the season-review operation and calendar; the economy will scale prize/prestige by tier; career objectives and sponsorships will read Tour membership and movement. The membership and standings contracts defined here are the seams those build on.
- **Deferred seams**: the season-review is triggered externally (World drives cadence), invitation-event criteria are a defined exception surface, and the exact promotion/relegation counts and points curve are tunable constants. Each is documented, not a hidden shortcut.
