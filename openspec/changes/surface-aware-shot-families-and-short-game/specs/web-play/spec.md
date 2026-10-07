## ADDED Requirements

### Requirement: Deliberate technique selection
The play UI SHALL support the minimal planning sequence Club → Technique → Landing target → Play shot. It SHALL submit the chosen family in `BallStrikeIntent` and SHALL not infer or silently replace technique from club, distance, lie, or human strategy.

#### Scenario: Unavailable family is understandable
- **WHEN** current guidance marks a family unavailable for the selected club and lie
- **THEN** the UI SHALL present it disabled with its concise reason
- **AND THEN** it SHALL not silently hide or substitute the selection.

### Requirement: Landing-target semantics
For PITCH and CHIP, the UI SHALL identify AimPoint as landing target / intended first contact. It SHALL not predict or present an uncomputed final resting position during planning.

#### Scenario: Planned chip target is not ball rest
- **WHEN** a player selects CHIP and adjusts the marker
- **THEN** the UI SHALL describe that marker as the landing target
- **AND THEN** final position remains resolver-owned until the shot resolves.

### Requirement: Putting boundary remains explicit
The UI SHALL preserve the existing green putting interaction through dedicated `playPutt` while allowing deliberate eligible fringe ball-strike choices. It SHALL not add spatial putt line, break, speed, or slope controls.

#### Scenario: Green does not show ball-strike family selector
- **WHEN** current lie is GREEN
- **THEN** the UI SHALL use the putting path
- **AND THEN** it SHALL not present normal ball-strike family choices.
