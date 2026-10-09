## ADDED Requirements

### Requirement: Shared ground-response execution across play modes
Human interactive play, visible simulated/AI play, round resolution, and background tournament simulation SHALL resolve the same eligible ground-response semantics through the shared resolver. AI policy may choose an intent, but it SHALL receive no hidden firmness, release, boundary, or settlement advantage.

#### Scenario: Equivalent human and AI strike has ground-response parity
- **WHEN** equivalent human and AI non-putting intents resolve with the same canonical ball, conditions, seed, and golfer state
- **THEN** their contact, release endpoint, final surface, settlement, and score SHALL be equal.

#### Scenario: Background remains summary-only
- **WHEN** a background round resolves an eligible release
- **THEN** it SHALL use the same ground-response and final-ball semantics as visible play
- **AND THEN** it SHALL not require an observable `ShotTrace` allocation.

### Requirement: Final ball drives the next shot after release
When ground response changes the resting point or ordinary playable final surface after contact, the next stroke in every playable and automatic path SHALL begin from the resolver's final `BallState`, not from first contact, legacy carry/lateral data, or client-side terrain inference.

#### Scenario: Ordinary-surface release updates the next lie
- **WHEN** a permitted release finishes on a canonical ordinary playable surface different from its contact surface
- **THEN** the next shot SHALL use that final position and final lie
- **AND THEN** hole progression SHALL retain its existing scoring and completion semantics.
