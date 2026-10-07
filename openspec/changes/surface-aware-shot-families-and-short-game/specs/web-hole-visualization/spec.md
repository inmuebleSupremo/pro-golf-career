## ADDED Requirements

### Requirement: Canonical roll playback
The canonical renderer SHALL consume optional backend-authored trace roll endpoints for a resolved shot. It MAY animate contact-to-roll-end as display timing, but SHALL not calculate roll distance, terrain response, or final placement client-side.

#### Scenario: Chip feedback uses trace roll
- **WHEN** a chip result includes an authoritative roll phase
- **THEN** playback SHALL show the returned contact and roll endpoint
- **AND THEN** the final marker SHALL use the returned final point.

### Requirement: Roll and recovery are visually distinct
Canonical playback SHALL distinguish calculated ground response from a rules-based recovery/replay transition through labels and styling, including reduced-motion presentation.

#### Scenario: Water never appears as roll
- **WHEN** a trace contains water contact and a recovery transition
- **THEN** the renderer SHALL show the rules transition distinctly
- **AND THEN** it SHALL not render contact-to-drop as a roll.

### Requirement: Contact versus final-state feedback
The renderer SHALL preserve separate semantic markers for first contact and final settled ball when they differ. It SHALL retain those facts in reduced-motion mode without relying on animation or colour alone.

#### Scenario: Release reaches a different final lie
- **WHEN** bounded resolver rollout changes the final settled point after playable contact
- **THEN** feedback SHALL identify contact and final ball as different authoritative states
- **AND THEN** it SHALL not infer either from carry/lateral or planning guidance.
