# golfer-creation Specification

## Purpose
TBD - created by archiving change add-create-your-golfer. Update Purpose after archive.
## Requirements
### Requirement: Create a Custom Golfer
The player SHALL be able to create a custom golfer by choosing a first name, last name, nationality, start age, starting playing-style archetype, and optional handedness. When omitted, handedness SHALL default to `RIGHT` at the application/API boundary. The created golfer SHALL be human-controlled and SHALL become the player's designated golfer. Creation SHALL be deterministic from these inputs.

#### Scenario: A created golfer is human-controlled and designated
- **WHEN** the player creates a golfer with a chosen identity and archetype
- **THEN** a human-controlled golfer SHALL be created with that identity and SHALL become the player's designated golfer.

#### Scenario: Creation is deterministic
- **WHEN** two golfers are created with the same name, nationality, start age, archetype, and handedness
- **THEN** they SHALL have identical starting attributes and identity.

#### Scenario: Omitted handedness remains compatible
- **WHEN** a legacy caller creates a golfer without handedness
- **THEN** the created golfer SHALL have `RIGHT` handedness
- **AND THEN** all existing creation inputs shall retain their established meanings.

### Requirement: Archetype Shapes the Starting Build

A created golfer's starting attributes SHALL be derived from the chosen archetype around a rookie baseline: attributes the archetype is strong in SHALL start higher, attributes it is weak in SHALL start lower, and the rest SHALL start at the baseline. A balanced (all-rounder) archetype SHALL start with an even build.

#### Scenario: A specialist archetype is stronger where it specialises

- **WHEN** a golfer is created with a playing-style archetype
- **THEN** the attributes that archetype is strong in SHALL start higher than the attributes it is weak in

#### Scenario: An all-rounder starts balanced

- **WHEN** a golfer is created with the balanced archetype
- **THEN** all of its starting attributes SHALL be equal

### Requirement: Created Golfer Enters at the Entry Tier

A created golfer SHALL enter the world as an active professional on the entry (lowest) tour tier, with the same career, finances, health, staff, and equipment setup as any other golfer, so they begin at the bottom and progress on merit.

#### Scenario: The created golfer starts at the bottom and can compete

- **WHEN** the player's golfer is created into a world
- **THEN** it SHALL be an active member of the entry-tier tour with a fresh career, able to compete in that tour's events

