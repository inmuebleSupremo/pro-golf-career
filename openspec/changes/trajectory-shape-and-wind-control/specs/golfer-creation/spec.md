## MODIFIED Requirements

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
