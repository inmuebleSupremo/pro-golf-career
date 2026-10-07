## ADDED Requirements

### Requirement: Canonical trace playback
The canonical SVG renderer SHALL consume backend-authored `ShotTrace` values for visible shot playback. It MAY use frontend-owned timing/easing and a simple display interpolation from origin to contact, but that interpolation SHALL not be represented as or imply calculated ballistic flight.

#### Scenario: Playback uses authoritative endpoints
- **WHEN** canonical trace playback starts
- **THEN** its displayed start and contact positions SHALL be trace origin and trace contact
- **AND THEN** it SHALL not calculate a separate contact, lateral miss, or route from strategy.

### Requirement: Recovery/replay has distinct presentation
The canonical renderer SHALL present a trace transition as a labelled/styled rules recovery or replay, distinct from origin-to-contact display interpolation. It SHALL use trace final point as the resting ball location.

#### Scenario: Water relief is not animated as rollout
- **WHEN** a trace contains a water-drop transition
- **THEN** the renderer SHALL show water contact and legal drop as distinct semantic states
- **AND THEN** it SHALL not animate contact-to-drop as bounce, roll, or flight.

### Requirement: Synthetic flight authority retirement
Once all active playable holes use canonical geometry and trace playback covers normal shots, water, out of bounds, recovery, hole-out, and reduced motion, no live production play component SHALL depend on client-generated flight profiles, strategy/club trajectory mappings, carry-based club inference, synthetic bounce/spin/roll, or client-generated outcome placement.

#### Scenario: Canonical event has no synthetic fallback
- **WHEN** an active playable event renders a resolved shot
- **THEN** its presentation SHALL be driven by canonical geometry and its backend trace
- **AND THEN** it SHALL not fall back to synthetic flight authority that can disagree with resolver state.
