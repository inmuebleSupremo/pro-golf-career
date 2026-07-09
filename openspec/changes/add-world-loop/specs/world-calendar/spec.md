## ADDED Requirements

### Requirement: Persistent World

The simulation SHALL contain one persistent World that is the highest-level gameplay domain. The World SHALL own the calendar, the seasons, and the professional population, and SHALL coordinate the other domains without performing their work.

#### Scenario: World owns top-level state

- **WHEN** a World is created from a master seed
- **THEN** it SHALL own a calendar, a current season, and a professional population, and expose them

#### Scenario: World coordinates, it does not replace

- **WHEN** the World progresses
- **THEN** it SHALL invoke the owning domains (tournaments, ranking, tours, careers) rather than reimplementing shot resolution, rankings math, or finances

### Requirement: Weekly Calendar Progression

The World SHALL advance through a shared calendar in discrete weekly turns. A season SHALL consist of a fixed number of weeks; advancing the final week of a season SHALL trigger the seasonal transition.

#### Scenario: A week advances

- **WHEN** the World advances one week
- **THEN** the calendar SHALL move forward by exactly one week and any events scheduled that week SHALL be resolved

#### Scenario: Season boundary

- **WHEN** the final week of a season is advanced
- **THEN** the World SHALL perform the seasonal transition and begin the next season

### Requirement: Player-Independent Progression

The World SHALL progress regardless of the human player's participation. Scheduled events SHALL still occur, winners SHALL be determined, rankings SHALL update, and history SHALL be recorded whether or not the player participates. The World SHALL NOT pause solely because the player is absent.

#### Scenario: The world runs without the player

- **WHEN** the World advances with no player action
- **THEN** events SHALL still be resolved and their winners, rankings, and history SHALL still be produced

### Requirement: Identical Rules For All

Automatic (AI) event resolution SHALL use the same gameplay engine and rules as any player-controlled participation. No participant SHALL receive a hidden advantage based on control type.

#### Scenario: No control-type advantage in world progression

- **WHEN** the World resolves an event automatically
- **THEN** every competitor SHALL be resolved through the same shared engine with no control-type-specific rule

### Requirement: Historical Continuity

The World SHALL preserve historical information permanently across progression. Completed seasons, results, rankings history, and career records SHALL NOT be discarded while the world is active.

#### Scenario: History persists across seasons

- **WHEN** multiple seasons have been advanced
- **THEN** each completed season's results and records SHALL remain retrievable

### Requirement: Deterministic World

Given the same master seed, the World SHALL reproduce the same progression. The same seed and the same number of advances SHALL yield the same events, results, rankings, and movements.

#### Scenario: Same seed reproduces the world

- **WHEN** two Worlds are created from the same master seed and advanced the same number of weeks
- **THEN** their schedules, results, rankings, and tour movements SHALL be identical
