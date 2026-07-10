# player-control Specification

## Purpose
TBD - created by archiving change add-player-control. Update Purpose after archive.
## Requirements
### Requirement: Designated Player Golfer

A world MAY have a single golfer designated as human-controlled. When none is designated, the world SHALL run fully autonomously. Designating a player SHALL NOT change how any other golfer is handled.

#### Scenario: A golfer is designated as the player's

- **WHEN** a golfer is designated human-controlled in a world
- **THEN** that golfer SHALL be player-controlled and every other golfer SHALL remain autonomous

#### Scenario: An unassigned world is autonomous

- **WHEN** a world has no designated player
- **THEN** it SHALL run exactly as a fully autonomous world

### Requirement: Standing Decisions Applied at Advance

The player SHALL configure standing decisions for their golfer — a development focus, a resting choice, and sponsorship acceptances — and the world SHALL apply them for that golfer during a normal advance (configure-then-advance), without pausing mid-advance.

#### Scenario: Development follows the player's focus

- **WHEN** the player sets a development focus and the world advances a season
- **THEN** their golfer's development SHALL be directed to the chosen attributes rather than the automatic allocation

#### Scenario: A resting golfer sits out and recovers

- **WHEN** the player sets their golfer to rest and the world advances
- **THEN** their golfer SHALL be excluded from event fields and SHALL recover rather than compete

### Requirement: Sponsorship Accept or Decline

For the player's golfer, generated sponsorship offers SHALL be presented as pending for the player to accept or decline, rather than being accepted automatically. Accepting an offer SHALL sign it within the permitted limits; unaccepted offers SHALL lapse.

#### Scenario: Offers await the player's decision

- **WHEN** the world generates sponsorship offers for the player's golfer
- **THEN** they SHALL be held as pending offers and SHALL NOT be signed automatically

#### Scenario: Accepting an offer signs it

- **WHEN** the player accepts a pending offer they are permitted to hold
- **THEN** it SHALL be signed to their financial account and removed from the pending offers

