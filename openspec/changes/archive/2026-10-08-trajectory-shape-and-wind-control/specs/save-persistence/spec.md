## ADDED Requirements

### Requirement: Handedness save compatibility
Player handedness SHALL be persisted and restored with player identity. Snapshots created before handedness exists SHALL load deterministically as `RIGHT` without changing unrelated player, career, world, or pending-event state. Observable `ShotTrace` airborne paths remain immediate result data and SHALL NOT introduce trace-history or replay persistence.

#### Scenario: Historical save defaults handedness safely
- **WHEN** a compatible existing save lacks a handedness field
- **THEN** loading it SHALL restore the player as `RIGHT`
- **AND THEN** the save's unrelated world and player values SHALL retain their existing meanings.

#### Scenario: Trace does not become save history
- **WHEN** an observable shot returns an airborne trace path
- **THEN** no completed-shot trace history or replay persistence SHALL be required to save or load the world.
