## ADDED Requirements

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
