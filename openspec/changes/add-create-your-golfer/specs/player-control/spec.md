## MODIFIED Requirements

### Requirement: Designated Player Golfer

A world MAY have a single golfer designated as human-controlled — either an existing golfer designated from the population, or a **custom golfer the player creates** (a chosen identity and archetype build). When none is designated, the world SHALL run fully autonomously. Designating or creating a player SHALL NOT change how any other golfer is handled, and a world SHALL have at most one designated player.

#### Scenario: A golfer is designated as the player's

- **WHEN** a golfer is designated human-controlled in a world
- **THEN** that golfer SHALL be player-controlled and every other golfer SHALL remain autonomous

#### Scenario: A created golfer becomes the player's

- **WHEN** the player creates a custom golfer in a world with no designated player
- **THEN** the created golfer SHALL become the single player-controlled golfer and every other golfer SHALL remain autonomous

#### Scenario: An unassigned world is autonomous

- **WHEN** a world has no designated player
- **THEN** it SHALL run exactly as a fully autonomous world
