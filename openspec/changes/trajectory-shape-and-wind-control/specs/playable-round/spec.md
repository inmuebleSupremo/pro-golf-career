## ADDED Requirements

### Requirement: Shared human and AI shape intent
Human and AI non-putting strokes SHALL submit the same complete `BallStrikeIntent`, including shape, through the same eligibility, flight, contact, settlement, and scoring resolver path. AI shape selection SHALL be deterministic and SHALL not receive a hidden execution or wind advantage.

#### Scenario: Equivalent shape intent has parity
- **WHEN** human and AI paths resolve equivalent canonical context, seed, handedness, and ball-strike intent
- **THEN** their contact, settlement, score, and observable trace facts SHALL be equal.

### Requirement: Observable flight materialization is optional
Visible human shots and visible simulated/AI shots SHALL materialize authoritative airborne samples through `ShotTrace`. `simHole`, `simRound`, `simEvent`, tournament-field simulation, and calibration corpora SHALL resolve through the same `FlightSolution`/contact logic without requiring `ShotTrace` or sampled-path allocation.

#### Scenario: Summary and trace flight agree
- **WHEN** identical canonical context and seed resolve once summary-only and once with trace materialization
- **THEN** their flight-derived contact, settlement, scoring, and final ball state SHALL be equal
- **AND THEN** only observable trace data may differ.

### Requirement: Planning resumes after non-holing observable playback
The interactive play surface SHALL keep `AimPoint` targeting locked only while an authoritative non-holing result is
actively playing. Once that playback completes, planning SHALL become interactive again from the updated canonical
ball state. Clearing visual playback SHALL not erase last-shot textual feedback or modify resolver-owned ball state.

#### Scenario: Next target is submitted from the settled ball
- **WHEN** a non-holing observable shot finishes playback
- **THEN** the player SHALL be able to select a new `AimPoint` on the canonical hole
- **AND THEN** the next `BallStrikeIntent` SHALL contain that new point and resolve from the previous shot's settled ball
- **AND THEN** hole-completion and recovery/replay flows SHALL retain their existing progression behavior.
