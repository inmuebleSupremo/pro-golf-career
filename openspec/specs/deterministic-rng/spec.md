# deterministic-rng Specification

## Purpose
TBD - created by archiving change add-shot-resolution-core. Update Purpose after archive.
## Requirements
### Requirement: Per-World Master Seed

Every save-world SHALL own a single immutable master seed, assigned at world creation and persisted for the life of the world. All randomness within that world SHALL derive from this master seed.

#### Scenario: Master seed persisted at creation

- **WHEN** a new world is created
- **THEN** a master seed SHALL be generated, recorded with the world, and never changed thereafter

#### Scenario: No ungoverned randomness

- **WHEN** any simulation code requires a random value
- **THEN** it SHALL obtain it from a generator derived from the world master seed, and SHALL NOT use an ambient or global random source

### Requirement: Deterministic Seed Derivation Hierarchy

Seeds SHALL be derived hierarchically and deterministically: master → season → tournament → round → shot. A derived seed SHALL be a pure function of its parent seed and the stable identifier of the child scope, so the same coordinates always yield the same seed regardless of evaluation order or timing.

#### Scenario: Same coordinates yield same seed

- **WHEN** a seed is derived for a given (world, season, tournament, round, golfer, hole, shot) coordinate
- **THEN** deriving it again with the same coordinate SHALL produce an identical seed

#### Scenario: Order independence

- **WHEN** two shots in different scopes are resolved in either order
- **THEN** each SHALL receive the seed determined solely by its own coordinate, independent of what was evaluated before it

#### Scenario: Sibling isolation

- **WHEN** two distinct scopes share the same parent but differ in identifier (e.g., two golfers on the same hole)
- **THEN** their derived seeds SHALL differ, producing independent random streams

### Requirement: Save/Load Reproducibility

Saving and loading a world SHALL NOT introduce any observable difference in subsequent simulation behavior. Resolving the same pending events before saving and after loading SHALL produce identical outcomes.

#### Scenario: Identical continuation after reload

- **WHEN** a world is saved at a point with pending events, then loaded, and the pending events are resolved
- **THEN** the resolved outcomes SHALL be identical to resolving the same events without the save/load cycle

#### Scenario: Re-resolving a shot is stable

- **WHEN** a single shot is resolved twice from the same world state and coordinate
- **THEN** both resolutions SHALL yield the identical sampled outcome

### Requirement: Documented Generator Algorithm

The pseudo-random generation and seed-derivation algorithm SHALL be explicitly documented and version-stable, so that reproducibility holds across application restarts and code deployments that do not intentionally change the algorithm.

#### Scenario: Cross-process stability

- **WHEN** the same world seed and coordinate are used in two separate process runs of the same application version
- **THEN** the derived seeds and sampled values SHALL be identical

