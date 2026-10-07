## ADDED Requirements

### Requirement: Visible-shot trace parity
Playable human shots and visible simulated/AI shots SHALL use the same execution and settlement path and SHALL materialize the same `ShotTrace` contract when observable.

#### Scenario: Human and visible AI use the shared trace shape
- **WHEN** a human or visible AI shot is resolved from equivalent canonical context and deterministic input
- **THEN** each observable outcome SHALL expose origin, aim, contact, final point, and any settlement transition through the same trace structure
- **AND THEN** no human-specific spatial playback truth is created.

### Requirement: Non-observable simulation remains summary-only
Bulk tournament resolution, calibration corpora, `simHole`, `simRound`, and `simEvent` SHALL default to the existing summary-only outcome mode unless an explicit observable consumer requests trace materialization.

#### Scenario: Background round avoids trace allocation
- **WHEN** a background round resolves without an observable playback consumer
- **THEN** it SHALL use the same resolver and settlement rules as visible play
- **AND THEN** it SHALL not require a `ShotTrace` object for every shot.

### Requirement: Stale shots produce no trace
Rejected stale or replayed ball-strike intents SHALL not mutate round state and SHALL not produce an outcome or shot trace.

#### Scenario: Losing stale submission has no spatial event
- **WHEN** a request presents a revision that is no longer current
- **THEN** the round SHALL return its existing stale result
- **AND THEN** ball position, strokes, and trace state SHALL remain unchanged.
