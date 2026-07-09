## ADDED Requirements

### Requirement: Tournament Loadout

Before competitive play, the golfer SHALL prepare a Tournament Loadout representing the equipment selected for use during the Tournament. Only the active Tournament Loadout SHALL be consumed by gameplay systems. Preparation occurs before play and SHALL NOT modify completed Tournament history.

#### Scenario: A loadout is prepared before play

- **WHEN** a golfer is about to compete
- **THEN** a Tournament Loadout SHALL be prepared and used as the equipment for that Tournament

### Requirement: Golf Bag

The Golf Bag SHALL represent the active collection of equipment carried during play, derived from the Tournament Loadout, and the Shot Resolution Engine SHALL consume the Golf Bag rather than the complete Equipment Inventory. There SHALL be one active Golf Bag per Tournament Entry, it SHALL remain fixed during play, and it SHALL always reference equipment owned by the golfer.

#### Scenario: The shot engine consumes the derived bag

- **WHEN** a golfer's shots are resolved in a Tournament
- **THEN** they SHALL be resolved using the Golf Bag derived from that golfer's loadout, not the full inventory

#### Scenario: The active bag references only owned equipment

- **WHEN** a Golf Bag is active for a Tournament entry
- **THEN** every item in it SHALL be owned by the golfer

### Requirement: Equipment Integrity

Only owned equipment MAY be included within a Tournament Loadout, and the simulation SHALL maintain consistency between the Inventory, the Tournament Loadout, and the Golf Bag: invalid loadouts SHALL be prevented and the active Golf Bag SHALL always be valid.

#### Scenario: An unowned selection is rejected

- **WHEN** a Tournament Loadout would include equipment the golfer does not own
- **THEN** it SHALL be treated as invalid

### Requirement: Equipment Selection

Professional Golfers MAY choose equipment for a Tournament based on factors such as preference, course, weather, playing style, or strategic preparation. Equipment selection SHALL represent a meaningful pre-competition decision — a deliberate selection of owned equipment prepared before play.

#### Scenario: Selection is a prepared decision

- **WHEN** a loadout is prepared
- **THEN** it SHALL be a deliberate selection of owned equipment for the Tournament
