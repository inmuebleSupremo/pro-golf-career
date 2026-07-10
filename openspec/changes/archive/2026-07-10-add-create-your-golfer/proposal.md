## Why

Today the human "player" is an *existing generated golfer* the world designates — `assignPlayer` picks one of the AI population. But the vision (player-experience §8, a confirmed V1 decision) is **create-your-golfer**: the player builds a custom golfer — their own identity and a starting playing-style archetype — as the career they own from the first tee, then rises from the bottom. This is the front door to the whole game: onboarding a career you authored rather than adopting a stranger's. The engine already has every piece — `Player`, `Identity`, `Attributes`, `ProfessionalGolfer.human`, and an `Archetype` chosen at creation (REQ-004) — but nothing assembles them into a player-created golfer, and the `Archetype` only affects starting age, not the build.

## What Changes

- **Archetype becomes a real starting build.** Enrich `Archetype` with an attribute emphasis — each playing-style archetype declares which attributes it is strong and weak in — and add playing-style archetypes for creation: **Power Hitter**, **Precision Player**, **Short-Game Artist**, **All-Rounder**, **Mental Fortress** (the existing background archetypes stay, with a neutral build). The emphasis is *data on the archetype*; the attribute *generation* stays in the population domain (per REQ-004).
- **A golfer factory.** Add `com.progolf.sim.population.GolferFactory.createHuman(...)` — builds a **human-controlled** `ProfessionalGolfer` from a chosen name, nationality, start age, and archetype, deriving the starting attributes from the archetype's emphasis around a rookie baseline (deterministic — no RNG; the player chose this build).
- **Create the player's golfer.** `World.createPlayer(firstName, lastName, nationality, startAge, archetype)` creates the golfer, admits it into the world at the entry tier (Development — the bottom of the ladder, to climb), and designates it as the player — an alternative to `assignPlayer` (designating an existing golfer). One player per world. Exposed on `WorldService`.
- **Population is unperturbed.** `PopulationGenerator` is pinned to the original three background archetypes for its random age routing, so adding creation archetypes does not change the generated AI world (reproducibility preserved).

Explicitly out of scope: point-buy / custom attribute distribution (the build is archetype-preset, the chosen model); choosing a starting tour tier or difficulty (created golfers start at Development); editing identity after creation (identity is immutable, REQ-002/015); custom career goals (the immediately following onboarding piece); any change to how the AI population is generated or how attributes evolve.

## Capabilities

### New Capabilities
- `golfer-creation`: the player creates a custom golfer — a chosen name, nationality, and start age, plus a playing-style archetype that shapes the starting attribute build around a rookie baseline — as a human-controlled golfer that enters the world at the bottom tier and becomes the player's own.

### Modified Capabilities
- `player-control`: the player's golfer may be **created** (a custom identity and archetype build) as well as designated from the existing population; a created golfer is human-controlled and starts at the entry tier.

## Impact

- **Codebase**: `Archetype` (attribute-emphasis data + creation archetypes), new `GolferFactory`, `PopulationConstants` (rookie baseline + emphasis magnitudes), `World.createPlayer`, `WorldService.createPlayer`, and pinning `PopulationGenerator` to the background archetypes. No change to attribute evolution, the shot engine, or the world loop.
- **Determinism**: creation is deterministic from its inputs (archetype-derived build, no RNG); the created golfer is reproducible. The AI population is unchanged (pinned archetypes), so existing worlds remain byte-identical.
- **DAG**: `GolferFactory` lives in `population` (which already composes `player`/`core`); `World` calls it as it already calls `PopulationGenerator`. Archetype gaining an emphasis couples `player` to `core.Attribute` (already a dependency). No inversion.
- **Boundary**: `World` assembles a created golfer through the factory and its existing `admit`/`assignPlayer` seams; it invents no new attribute or identity rules.
