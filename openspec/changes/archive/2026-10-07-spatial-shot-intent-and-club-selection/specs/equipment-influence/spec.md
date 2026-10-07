## ADDED Requirements

### Requirement: Catalogue-family equipment compatibility
The equipment model SHALL support static individual shot-club catalogue entries by mapping each stable club ID to an existing equipment family. Existing aggregate bag/loadout effects SHALL remain the sole equipment influence on an individual strike in this change.

#### Scenario: Selected iron uses current family influence
- **WHEN** a player selects a numbered iron from the catalogue
- **THEN** its base calibration is combined with the current irons-family equipment effects
- **AND THEN** the player is not required to own or separately equip that individual iron.

### Requirement: No equipment progression expansion
Spatial club selection SHALL NOT introduce per-club inventory, individual purchase/upgrade, rarity, durability, or loadout redesign.

#### Scenario: Existing loadout remains valid
- **WHEN** a pre-change loadout is used in a playable round
- **THEN** it continues to provide its existing family-level effects to eligible catalogue clubs.
