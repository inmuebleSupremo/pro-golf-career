## ADDED Requirements

### Requirement: Generator 8 principles-driven architecture preserves historical provenance

Generator 8 SHALL retain the accepted principles-driven routing and composition implementation as a separate deterministic generator version. Version 8 SHALL be available only for newly created worlds through an explicit development opt-in and SHALL NOT replace the configured normal default without separate approval. Restored worlds SHALL continue to use their persisted generator version; Generator 8 SHALL NOT reinterpret V1–V7 courses or saved careers.

#### Scenario: A new world explicitly opts into Generator 8

- **WHEN** a newly created world requests the explicit Generator 8 development opt-in
- **THEN** its course pool SHALL be generated through Generator 8 and retain version 8 as its saved provenance

#### Scenario: A historical world is restored after Generator 8 exists

- **WHEN** a world pinned to any retained Generator 1–7 version is restored
- **THEN** it SHALL regenerate through that retained version and SHALL NOT acquire Generator 8 routes, terrain, landing intent, or strategy data

### Requirement: Generator 8 strategic architecture remains canonical and deterministic

Generator 8 SHALL derive varied route geometry and candidate landing intent from deterministic continuous inputs rather than by selecting a fixed named hole template. It SHALL compile fairway, rough, boundary, green, recovery, hazards, and vegetation through backend-authoritative canonical geometry. Route-derived landing intent SHALL remain advisory: human and AI play, surface lookup, penalties, settlement, and target availability SHALL use the same emitted canonical geometry and existing shared target evaluation.

#### Scenario: Generator 8 regenerates a course

- **WHEN** Generator 8 receives the same seed coordinate and environment classification twice
- **THEN** it SHALL emit equal route-derived strategy intent and equal canonical course geometry

#### Scenario: Generator 8 evaluates a strategic target during play

- **WHEN** a human or AI strategy evaluates a Generator 8 landing option
- **THEN** the option SHALL be actionable only when its aim point resolves to an ordinary canonical landing surface with a reachable existing club choice
- **AND THEN** shot resolution, hazards, recovery, and settlement SHALL use that same canonical geometry

#### Scenario: Strategy policy requests a Generator 8 decision

- **WHEN** an existing conservative, balanced, or aggressive strategy policy evaluates a Generator 8 hole with displayed options
- **THEN** it SHALL select its corresponding available shared-planner option or retain the established primary fallback

### Requirement: Generator 8 review evidence remains complete

The development review surface SHALL retain every hole from its documented fixed Parkland and Links seed corpus through the existing canonical playable-hole renderer. It SHALL label held-out seeds as unseen and MAY provide a decision-only filter, but that filter SHALL NOT replace access to the complete course corpus.

#### Scenario: A reviewer selects an unseen Generator 8 course

- **WHEN** a reviewer selects an unseen fixed-seed Parkland or Links Generator 8 course
- **THEN** the review surface SHALL provide that course's complete canonical 18-hole geometry in the playable-hole renderer

#### Scenario: A reviewer uses the decision-only view

- **WHEN** a reviewer enables the decision-only filter
- **THEN** the surface SHALL show only holes with more than one shared-planner strategic option while retaining a control to return to all 18 holes
