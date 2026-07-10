## 1. Archetype: attribute-emphasis build

- [x] 1.1 Enrich `Archetype` with `Set<Attribute> strengths` / `Set<Attribute> weaknesses` (data + accessors), keeping the existing age ranges; add creation archetypes `POWER_HITTER`, `PRECISION_PLAYER`, `SHORT_GAME_ARTIST`, `ALL_ROUNDER` (empty sets), `MENTAL_FORTRESS` with sensible strengths/weaknesses. The three background archetypes get empty sets (neutral build).
- [x] 1.2 Pin `PopulationGenerator` to the three background archetypes (an explicit array) instead of `Archetype.values()`, so the generated AI world is unperturbed.
- [x] 1.3 Add `CREATION_BASELINE`, `CREATION_EMPHASIS`, `CREATION_DEEMPHASIS` to `PopulationConstants`.

## 2. GolferFactory

- [x] 2.1 Add `com.progolf.sim.population.GolferFactory.createHuman(id, firstName, lastName, nationality, startAge, archetype, referenceYear)` → `ProfessionalGolfer.human`: build attributes from the archetype (baseline ± emphasis, clamped), derive the date of birth so start age is exact, validate start age (16–30), construct+activate the `Player`.

## 3. World + app

- [x] 3.1 `World.createPlayer(firstName, lastName, nationality, startAge, archetype)`: throw if a player is already assigned; mint a stable `player-<masterSeed hex>` id; build via `GolferFactory`; `admit` at `TourTier.DEVELOPMENT`; `assignPlayer`; return the id.
- [x] 3.2 `WorldService.createPlayer(sessionId, ...)` delegating through `World`.

## 4. Verification

- [x] 4.1 Factory: a created golfer is `HUMAN` control type, has the chosen identity (name/nationality/archetype) and start age; a Power Hitter's driving distance starts above its putting; an All-Rounder is flat; determinism (same inputs → identical attributes).
- [x] 4.2 World: `createPlayer` designates the created golfer as the player, active on the Development tour, with a fresh career/account/health/staff/equipment; it can then advance and compete; a second `createPlayer` (or when a player is already assigned) throws.
- [x] 4.3 Population unperturbed: existing population/world reproducibility tests pass unchanged (pinned archetypes).
- [x] 4.4 Full suite + `ArchitecturePurityTest` pass.
- [x] 4.5 `openspec validate add-create-your-golfer --type change --strict`.
