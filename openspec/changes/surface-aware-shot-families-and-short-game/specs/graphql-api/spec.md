## ADDED Requirements

### Requirement: Additive ball-strike family input
The GraphQL ball-strike input SHALL accept an additive shot-family value using engine enum names. For compatible callers that omit it, the API boundary MAY supply `FULL`; the engine SHALL receive a non-null family.

#### Scenario: Legacy-compatible omission becomes full swing
- **WHEN** an otherwise valid legacy ball-strike request omits shot family
- **THEN** the API SHALL map it to `FULL`
- **AND THEN** it SHALL not expose internal execution values.

### Requirement: Explicit existing putting route
The API SHALL expose dedicated `playPutt(expectedShotRevision)` for minimal `PuttIntent` submission. It SHALL preserve the existing non-spatial putting model and SHALL not require callers to submit a ball-strike family or spatial aiming controls.

#### Scenario: Green uses putting operation rather than ball strike
- **WHEN** current lie is GREEN and a client submits a current expected revision through `playPutt`
- **THEN** the API SHALL resolve the existing putting route
- **AND THEN** it SHALL not require or infer a `BallStrikeIntent`.

### Requirement: Authoritative family validation
The API SHALL reject invalid lie/club/family combinations before resolving a stroke. A rejected or stale submission SHALL contain no outcome, trace, roll, or state change.

#### Scenario: Invalid bunker full shot is rejected
- **WHEN** a client submits FULL from a bunker in the first-slice ruleset
- **THEN** the API SHALL return a client-classified validation error
- **AND THEN** no stroke or trace SHALL be created.

### Requirement: Family guidance projection
The current shot situation SHALL expose server-derived family availability for the current lie and selectable club, including concise disabled reasons. It SHALL not expose carry caps, variance multipliers, or rollout tuning controls.

#### Scenario: UI can explain unavailable family
- **WHEN** a selected club is not compatible with PITCH
- **THEN** guidance SHALL mark PITCH unavailable with an explanation
- **AND THEN** the client need not infer rules from club names.

### Requirement: Trace roll projection
Observable shot results SHALL project optional authoritative roll facts separately from recovery/replay transition, along with contact and final settled state.

#### Scenario: Recovery remains non-flight
- **WHEN** water or out-of-bounds settlement occurs
- **THEN** the result SHALL project recovery/replay transition when applicable
- **AND THEN** it SHALL not project that rules move as a roll phase.
