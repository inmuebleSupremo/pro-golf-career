## ADDED Requirements

### Requirement: World Applies the Player's Decisions

When a world has a designated player, the World SHALL apply that player's decisions for their golfer during advance — directing development to the player's focus, excluding a resting golfer from event fields, and deferring sponsorship acceptance to the player's pending offers — while using the existing automatic paths for all other golfers. A world with no designated player SHALL be unchanged.

#### Scenario: The player's golfer follows player decisions, others follow AI

- **WHEN** a world with a designated player advances
- **THEN** the player's golfer SHALL follow the player's development focus, resting, and sponsorship choices, and every other golfer SHALL follow the automatic policies

#### Scenario: No player leaves the world unchanged

- **WHEN** a world with no designated player advances
- **THEN** its outcomes SHALL be identical to the same world advanced without the player-control feature
