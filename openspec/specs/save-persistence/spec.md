# save-persistence Specification

## Purpose
A world can be saved to and loaded from durable storage, with format versioning, checksum-based corruption detection, multiple isolated saves, and autosave; a loaded world continues identically to the saved one. All serialization and storage lives in the application layer, keeping the engine framework-free.
## Requirements
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

### Requirement: Engine Stays Serialization-Free

All serialization and storage code SHALL live in the application layer. The simulation engine SHALL carry no serialization framework dependency and no serialization annotations; the snapshot it exposes SHALL be a plain immutable value the application layer serializes by reflection over its structure.

#### Scenario: The engine has no serialization dependency

- **WHEN** the engine package is inspected
- **THEN** it SHALL contain no serialization-framework imports or annotations, and the save format SHALL be produced entirely by the application layer

### Requirement: Deterministic Spatial Continuity

Canonical generated course geometry and version-supported associated semantic plans SHALL be restored by deterministic regeneration from the saved world seed, configuration, and explicitly persisted course-generator version rather than by independently mutable polygon or semantic presentation copies. New saves SHALL record that provenance. A supported historical save lacking generator-version provenance SHALL restore through the documented historical V1 generator, never through the latest generator. The current save scope SHALL remain unchanged: it does not persist a pending playable event.

#### Scenario: Version-pinned V3 save restores its course identity

- **WHEN** a world saved with explicit supported V3 provenance is loaded
- **THEN** its course pool, route/zone/green semantics, progression targets, and canonical geometry SHALL be regenerated with that same version

#### Scenario: Save contains no duplicate terrain authority

- **WHEN** a V3 world is saved
- **THEN** it SHALL persist generator provenance and normal world state without serializing a separately mutable copy of course polygons or semantic terrain

### Requirement: V4 persistence stores provenance rather than hazard copies

Save persistence SHALL retain the world course-generator version pin needed to reproduce V4 hazard planning. It SHALL NOT store full canonical hazard polygons or duplicate mutable hazard-plan payloads merely because V4 introduces semantic feature metadata. Missing legacy provenance SHALL retain its documented V1 fallback and unsupported pins SHALL fail explicitly.

#### Scenario: V4 save does not need polygon serialization

- **WHEN** a V4 save is written and restored
- **THEN** its role-based hazard terrain SHALL be regenerated from deterministic provenance rather than a saved polygon copy

