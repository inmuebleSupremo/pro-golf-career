## ADDED Requirements

### Requirement: Per-Club Dispersion Profiles

Each club SHALL carry its own dispersion profile — an independent lateral (offline) dispersion and distance (long/short) dispersion — rather than a single shared multiplier. The profiles SHALL be calibrated so that a full-swing club's accuracy character is realistic relative to the others: the driver SHALL scatter the widest offline of any club, and the wedge SHALL be the most precise (tightest lateral and distance control). Because lateral and distance dispersion are independent per club, driving accuracy (fairways hit) and greens in regulation SHALL be tunable separately. The profiles SHALL be neutral to determinism — they change only the calibrated spread, not how randomness is sampled.

#### Scenario: A wedge is more precise than a longer club

- **WHEN** a wedge and an iron are resolved over many shots at the same distance
- **THEN** the wedge's lateral and distance spreads SHALL both be tighter than the iron's

#### Scenario: The driver scatters widest off the tee

- **WHEN** a full round is resolved
- **THEN** the driver SHALL have the widest offline dispersion of any club used

#### Scenario: Driving accuracy and greens in regulation are independently calibrated

- **WHEN** the per-club dispersion is tuned
- **THEN** a generated field SHALL produce realistic fairway-hit and green-in-regulation rates, and one SHALL be adjustable without forcing the other
