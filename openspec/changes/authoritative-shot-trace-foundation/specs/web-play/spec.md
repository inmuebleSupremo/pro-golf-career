## ADDED Requirements

### Requirement: Trace-owned result feedback
During visible-shot playback and feedback, the play UI SHALL use the returned trace's intended aim, contact, transition, and final point. It SHALL not derive result feedback from current guidance, selected next aim, strategy, carry/lateral, or client-generated terrain.

#### Scenario: Refetch cannot replace the completed shot target
- **WHEN** the server advances to the next pending shot while the prior visible shot is still being presented
- **THEN** result feedback SHALL display the completed trace's intended aim point
- **AND THEN** it SHALL not substitute the new shot's guidance/default target.

### Requirement: Accessible semantic result feedback
The play UI SHALL distinguish intended target, actual contact, final legal ball position, and any recovery/replay transition with labels and non-colour-only cues. It MAY show a miss vector from aim to contact when helpful.

#### Scenario: Penalty result is understandable without inferred motion
- **WHEN** a trace contains a water-drop or replay transition
- **THEN** the UI SHALL identify the contact and legal recovery/replay destination
- **AND THEN** it SHALL not imply the ball physically travelled between them.

### Requirement: Reduced-motion trace comprehension
When reduced motion is enabled, the play UI SHALL still expose all trace semantic markers and recovery/replay distinction without requiring animation to understand the result.

#### Scenario: Reduced-motion player receives complete result
- **WHEN** a visible shot resolves with reduced motion enabled
- **THEN** the UI SHALL render target, contact, final point, and applicable transition in a stable state
- **AND THEN** it SHALL retain accessible textual result feedback.
