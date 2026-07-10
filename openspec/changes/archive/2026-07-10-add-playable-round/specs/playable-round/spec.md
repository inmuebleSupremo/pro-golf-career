## ADDED Requirements

### Requirement: Interactive Shot-by-Shot Round

The system SHALL provide an interactive round in which a human plays an 18-hole round for a golfer one shot at a time through the shot engine. For the current shot it SHALL expose the situation needed to decide — at least the hole and its par, the shot number, the distance to the pin, the current lie, and the reachable surfaces/hazards — and it SHALL accept the human's decision as a club, target, and risk selection, resolving exactly one shot per decision.

#### Scenario: A human plays a shot from the current situation

- **WHEN** the player is presented the current shot's situation and submits a club/target/risk decision
- **THEN** exactly one shot SHALL be resolved through the shot engine and the round SHALL advance to the next shot (or hole, or completion)

#### Scenario: The round completes with a score

- **WHEN** all eighteen holes have been played
- **THEN** the round SHALL report total strokes, per-hole scores, and the score relative to par

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
