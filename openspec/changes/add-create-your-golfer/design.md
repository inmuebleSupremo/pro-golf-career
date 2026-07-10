# Design — add-create-your-golfer

## Context

`assignPlayer` designates an existing generated golfer as the human's. Create-your-golfer instead builds a new one from the player's choices. The pieces exist — `Player`, `Identity` (which already carries an `Archetype`), `Attributes`, and `ProfessionalGolfer.human` (a human golfer with no decision policy) — but the `Archetype` only sets a starting-age range, and nothing assembles a created golfer or admits it. This change makes the archetype a real build and adds the assembly.

## Decisions

### D1 — Preset playing-style archetypes (the chosen build model)
The player picks a starting archetype that shapes the build, rather than allocating raw attribute points. Each archetype declares its **strengths** and **weaknesses** (sets of attributes); the build is a rookie baseline raised on strengths and lowered on weaknesses. New archetypes: `POWER_HITTER`, `PRECISION_PLAYER`, `SHORT_GAME_ARTIST`, `ALL_ROUNDER` (neutral), `MENTAL_FORTRESS`. The three existing background archetypes stay with empty strengths/weaknesses (a neutral build).

- **Alternative — point-buy:** more control but a larger input/validation surface and needs UI to be pleasant; deferred. Archetype presets match "a starting build/archetype" and reuse `Identity.archetype`.

### D2 — Emphasis data on `Archetype`, generation in the population domain
`Archetype` gains `Set<Attribute> strengths` and `Set<Attribute> weaknesses` (data). The attribute *generation* — baseline ± emphasis, clamped — lives in `GolferFactory` in `sim.population`, honoring REQ-004 ("the actual starting-attribute generation lives in the population domain"). Magnitudes (`CREATION_BASELINE`, `CREATION_EMPHASIS`, `CREATION_DEEMPHASIS`) are tunables in `PopulationConstants`. The build is deterministic (no RNG): the player chose it, so two identical creations are identical.

### D3 — Population is pinned to the background archetypes
`PopulationGenerator` currently picks a random archetype via `Archetype.values()` (for age). Adding creation archetypes would change `values()` and perturb every generated world. So `PopulationGenerator` is pinned to an explicit array of the three background archetypes; the creation archetypes are creation-only. The AI world stays byte-identical, and archetype remains an age/label concern for AI golfers (their attributes are still the existing overall-plus-deviation profile, unchanged).

### D4 — A created golfer is human, enters at the bottom, and is the player
`GolferFactory.createHuman(id, firstName, lastName, nationality, startAge, archetype, referenceYear)` returns a `ProfessionalGolfer.human` (no policy — it sims with the balanced default like any human golfer). `World.createPlayer(...)` mints a stable id (`player-<masterSeed hex>`, distinct from population `golfer-<…>` ids), builds the golfer, `admit`s it at `TourTier.DEVELOPMENT` (the entry tier — a created pro starts at the bottom and climbs, giving the full career arc), and `assignPlayer`s it. One player per world: it throws if a player is already assigned. Start age is validated to a pro-rookie range (16–30); the date of birth is derived so `startAgeOf` returns exactly the chosen age.

## Risks

- **Perturbing the AI population** — avoided by pinning `PopulationGenerator` to the background archetypes; covered by the existing population/world reproducibility tests continuing to pass unchanged.
- **Id collision** — the created id uses a distinct `player-` prefix from population `golfer-` ids; one player per world is enforced.
- **Build balance** — the emphasis magnitudes are isolated tunables; tests assert the *shape* (a Power Hitter drives farther than they putt; an All-Rounder is flat) rather than exact values.
