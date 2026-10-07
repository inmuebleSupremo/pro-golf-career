## ADDED Requirements

### Requirement: Intent migration preserves existing saves
Existing world saves SHALL load without requiring a pending-shot intent migration. Static catalogue identifiers introduced for this capability SHALL remain stable for future saved references.

#### Scenario: Legacy save loads into current planning model
- **WHEN** a save created before spatial shot intent is loaded
- **THEN** its world state restores successfully
- **AND THEN** a newly started playable shot receives current guidance and revision data.

### Requirement: Pending-action persistence remains explicit
If a future save format persists a pending shot intent, it SHALL persist its intent and revision through an explicit versioned migration rather than serializing transient request state implicitly.

#### Scenario: Current save contains no pending request
- **WHEN** a current save is written during normal world play
- **THEN** it does not depend on an unvalidated in-flight GraphQL intent object.
