## MODIFIED Requirements

### Requirement: World Applies the Player's Decisions

When a world has a designated player, the World SHALL apply that player's decisions for their golfer during advance — directing development to the player's focus, **entering the player only in the events they have chosen (excluding skipped events and honoring the blanket resting choice, and resolving the excluded events automatically)**, deferring sponsorship acceptance to the player's pending offers, and deferring staff hiring and equipment purchases to the player (holding generated candidates and upgrades as pending offers rather than hiring or buying automatically, while still applying mandatory salaries and under-debt release) — while using the existing automatic paths for all other golfers. A world with no designated player SHALL be unchanged.

#### Scenario: The player's golfer follows player decisions, others follow AI

- **WHEN** a world with a designated player advances
- **THEN** the player's golfer SHALL follow the player's development, event entry, sponsorship, staff, and equipment choices, and every other golfer SHALL follow the automatic policies

#### Scenario: A skipped event is resolved without the player

- **WHEN** the player has skipped an event they were eligible for and the world advances to it
- **THEN** the event SHALL be resolved automatically without the player, and the player SHALL recover that week

#### Scenario: The player's staff and equipment are not auto-managed

- **WHEN** a world with a designated player runs a season transition
- **THEN** no staff SHALL be hired and no equipment SHALL be bought automatically for the player's golfer; instead candidates and upgrades SHALL be held as pending offers for the player to decide

#### Scenario: No player leaves the world unchanged

- **WHEN** a world with no designated player advances
- **THEN** its outcomes SHALL be identical to the same world advanced without the player-control feature
