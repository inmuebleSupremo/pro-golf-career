# playable-round Specification

## Purpose
TBD - created by archiving change add-playable-round. Update Purpose after archive.
## Requirements
### Requirement: Interactive Shot-by-Shot Round
The system SHALL provide an interactive 18-hole round in which a human plays one shot at a time through the shot engine. For a pending shot it SHALL expose the hole and par, shot number, canonical ball state, current lie, reachable surfaces/hazards, opaque shot revision, aim envelope, available individual clubs, and guidance. It SHALL accept a ball-strike intent rather than a distance/lateral/strategy decision and resolve exactly one shot per current revision.

#### Scenario: Manual shot starts with a planning projection
- **WHEN** a playable human shot is pending
- **THEN** the round exposes ball state, shot revision, aim envelope, available clubs, and guidance
- **AND THEN** the player can submit a ball-strike intent using that projection.

#### Scenario: The round completes with a score
- **WHEN** all eighteen holes have been played
- **THEN** the round SHALL report total strokes, per-hole scores, and score relative to par.

### Requirement: Always Skippable

The player SHALL be able to skip play at shot, hole, or round granularity — auto-playing the remaining shots using the same automatic policy the simulation uses — so that no round need be played by hand.

#### Scenario: Simming the rest of a round

- **WHEN** the player chooses to sim the remainder of the round
- **THEN** the remaining shots SHALL be auto-played by the automatic policy and the round SHALL complete with a score

### Requirement: Fidelity to Automatic Resolution

A fully-simmed interactive round SHALL produce the same score as the simulation's automatic round resolution for the same golfer, course, conditions, and seed — so that playing by hand differs from the automatic outcome only by the human's own decisions.

#### Scenario: A simmed round matches the automatic round

- **WHEN** an interactive round is simmed in full for a golfer, course, conditions, and seed
- **THEN** its total strokes SHALL equal the automatic round resolution for the same inputs

### Requirement: Persistent Spatial Origin

An interactive round and automatic round resolution SHALL retain the canonical playable `BallState` for the current hole. Every next shot SHALL originate from the previous shot's resulting playable settlement position rather than from only a remaining-distance scalar. Human and automatic paths SHALL apply identical ball-state transitions.

#### Scenario: Next shot starts at the prior settlement

- **WHEN** a non-terminal shot finishes and the hole continues
- **THEN** the next shot's origin SHALL equal the previous outcome's playable settlement position and its lie SHALL match that ball state

#### Scenario: Simmed and interactive paths agree spatially

- **WHEN** the same decisions and seeds are resolved through a fully simmed interactive round and automatic round resolution
- **THEN** their scores, final lies, and per-shot playable settlement positions SHALL be identical

### Requirement: Spatial Penalty Recovery Fidelity

Playable rounds SHALL preserve the existing water-drop and out-of-bounds scoring semantics while applying them through canonical settlement state. The displayed/reportable contact position SHALL not be confused with the legal origin of the following shot.

#### Scenario: Water resumes from the drop

- **WHEN** a playable-round shot contacts water
- **THEN** the next shot SHALL begin from the recorded rough recovery position, not from the water contact position

#### Scenario: Out of bounds resumes from the prior ball state

- **WHEN** a playable-round shot contacts out of bounds
- **THEN** the next shot SHALL begin from the pre-shot playable ball state after the applicable penalty

### Requirement: Shared human and AI intent seam
Playable rounds SHALL execute both manual and simulated strokes through `ShotIntent`.

#### Scenario: AI strategy emits an intent
- **WHEN** AI policy selects a tactical strategy
- **THEN** it selects a catalogue club and resolves a spatial point into `BallStrikeIntent`
- **AND THEN** the shared resolver executes that intent.

### Requirement: Human strategy is not authoritative shot control
Manual rounds SHALL not accept conservative, balanced, or aggressive strategy as an authoritative shot-control input. Internal AI strategy MAY remain for policy selection.

#### Scenario: Human selected point governs direction
- **WHEN** a human submits a valid aim point
- **THEN** no strategy or `ShotAim` substitution changes that point's intended direction.

### Requirement: Stale shot submissions are safe
Each pending playable shot SHALL expose an opaque revision. Resolution SHALL atomically verify the expected revision before mutating round state and SHALL reject stale or replayed submissions without changing strokes, ball position, or round state.

#### Scenario: Duplicate submission loses the race
- **WHEN** two requests use the same pending-shot revision
- **THEN** at most one resolves a stroke
- **AND THEN** the other returns a retryable stale-shot conflict.

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

