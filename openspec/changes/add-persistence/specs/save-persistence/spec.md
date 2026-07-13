## ADDED Requirements

### Requirement: Durable Save And Load

The application SHALL be able to write a world to durable storage and read it back, such that a loaded world is identical to the world that was saved. A save SHALL bundle everything needed to reconstruct the world: the master seed, the world configuration, and the world snapshot. Loading a save SHALL rebuild the world from those inputs. Saving and loading SHALL go through a store port so the storage medium (filesystem now, others later) is an adapter behind it.

#### Scenario: A saved world loads and continues identically

- **WHEN** a world is saved to storage, then loaded from storage, and both the loaded world and the original are advanced by the same number of steps
- **THEN** the two worlds SHALL be observably identical afterward

#### Scenario: Load rebuilds from the stored seed, config, and snapshot

- **WHEN** a save is loaded
- **THEN** the world SHALL be reconstructed from the stored master seed, world configuration, and snapshot, with no dependence on the process that created it

### Requirement: Versioned, Corruption-Detecting Save Format

Each save SHALL record a format version and a checksum of its payload. On load, the store SHALL reject a save whose format version it does not support, and SHALL reject a save whose payload does not match its recorded checksum (a corrupt or tampered file), with a distinct error for each — never silently loading a broken world.

#### Scenario: A corrupt save is rejected

- **WHEN** a saved file's payload is altered after it was written and the save is loaded
- **THEN** the load SHALL fail with a corruption error rather than returning a world

#### Scenario: An unsupported format version is rejected

- **WHEN** a save recorded with a format version the store does not support is loaded
- **THEN** the load SHALL fail with a version-mismatch error

### Requirement: Multiple Independent Saves

The store SHALL hold multiple independent saves, each addressable by its own id, without one affecting another. It SHALL be able to list the available saves (with their metadata) and delete a save.

#### Scenario: Independent saves coexist

- **WHEN** two different worlds are saved under two different ids
- **THEN** each SHALL load back to its own world, and listing SHALL report both

#### Scenario: A deleted save is gone

- **WHEN** a save is deleted
- **THEN** it SHALL no longer be listed and loading it SHALL report it as not found

### Requirement: Autosave At Checkpoints

The application SHALL autosave to a reserved slot at natural checkpoints — after each season advance and after a player event completes — so ordinary progress is not lost. The autosave SHALL be an ordinary save (same format and guarantees) under a reserved id, overwritten at each checkpoint, and loadable like any other.

#### Scenario: Advancing a season refreshes the autosave

- **WHEN** a session advances a season
- **THEN** the reserved autosave slot SHALL be written with the world's current state, replacing the previous autosave

### Requirement: Engine Stays Serialization-Free

All serialization and storage code SHALL live in the application layer. The simulation engine SHALL carry no serialization framework dependency and no serialization annotations; the snapshot it exposes SHALL be a plain immutable value the application layer serializes by reflection over its structure.

#### Scenario: The engine has no serialization dependency

- **WHEN** the engine package is inspected
- **THEN** it SHALL contain no serialization-framework imports or annotations, and the save format SHALL be produced entirely by the application layer
