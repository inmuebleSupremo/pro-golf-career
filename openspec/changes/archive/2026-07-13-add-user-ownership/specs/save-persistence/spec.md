## MODIFIED Requirements

### Requirement: Multiple Independent Saves

The store SHALL hold multiple independent saves, each addressable by its own id within its owner's scope, without one affecting another. It SHALL be able to list the available saves for an owner (with their metadata) and delete a save. Saves SHALL be scoped to their owner: listing returns only that owner's saves, and loading or deleting a save the caller does not own SHALL report it as not found.

#### Scenario: Independent saves coexist

- **WHEN** one owner saves two different worlds under two different ids
- **THEN** each SHALL load back to its own world, and listing for that owner SHALL report both

#### Scenario: A deleted save is gone

- **WHEN** a save is deleted by its owner
- **THEN** it SHALL no longer be listed and loading it SHALL report it as not found

#### Scenario: Saves are isolated per owner

- **WHEN** two owners each save a world, possibly under the same id
- **THEN** each owner SHALL list and load only their own save, and neither SHALL be able to load or delete the other's

### Requirement: Autosave At Checkpoints

The application SHALL autosave to a reserved slot, per owner, at natural checkpoints — after each season advance and after a player event completes — so ordinary progress is not lost. The autosave SHALL be an ordinary save (same format and guarantees) under a reserved id within the owner's scope, overwritten at each checkpoint, and loadable like any other. One owner's autosave SHALL NOT affect another's.

#### Scenario: Advancing a season refreshes the autosave

- **WHEN** a session advances a season
- **THEN** the owner's reserved autosave slot SHALL be written with the world's current state, replacing that owner's previous autosave

#### Scenario: Autosaves are independent across owners

- **WHEN** two owners each advance a season
- **THEN** each owner's reserved autosave SHALL hold their own world
