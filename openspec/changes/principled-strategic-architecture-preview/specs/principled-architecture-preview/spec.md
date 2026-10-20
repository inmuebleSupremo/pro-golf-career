## ADDED Requirements

### Requirement: Preview isolation preserves retained course generation

The system SHALL provide the principles-driven architecture experiment only through a development-only, non-persistent preview path. It SHALL NOT alter the retained generator-version registry, current generator selection, world creation, saved-world interpretation, or existing historical generator output.

#### Scenario: Existing or new normal world requests a course

- **WHEN** a normal world requests a course through its selected generator version
- **THEN** it SHALL use its retained version path and SHALL NOT invoke the principles-driven preview

#### Scenario: A preview course is generated twice

- **WHEN** the development preview receives the same seed coordinate and environment classification twice
- **THEN** it SHALL emit equal canonical course geometry and equal preview strategy intent

### Requirement: Preview targets are grounded in emitted canonical geometry

The preview SHALL compile its route-derived landforms through the existing canonical terrain and hazard pipeline. Any displayed strategic option SHALL be produced by the shared target evaluator from the emitted geometry, current ball state, reachable club envelope, and ordinary landing surface; preview role metadata alone SHALL NOT make an unavailable target actionable.

#### Scenario: A displayed target is evaluated

- **WHEN** the preview displays a strategic target option
- **THEN** its aim point SHALL resolve to an ordinary canonical landing surface and a reachable existing club choice

#### Scenario: Strategy policy requests a preview decision

- **WHEN** an existing conservative, balanced, or aggressive strategy policy evaluates a preview hole with displayed options
- **THEN** it SHALL select its corresponding available shared-planner option or retain the established primary fallback

### Requirement: Complete fixed-corpus review uses the real renderer

The development review surface SHALL expose every hole from its documented fixed Parkland and Links seed corpus through the existing canonical playable-hole renderer. It SHALL label held-out seeds as unseen and MAY provide a decision-only filter, but that filter SHALL NOT replace access to the complete course corpus.

#### Scenario: A reviewer selects an unseen course

- **WHEN** a reviewer selects an unseen fixed-seed Parkland or Links course
- **THEN** the review surface SHALL provide that course's complete canonical 18-hole geometry in the playable-hole renderer

#### Scenario: A reviewer uses the decision-only view

- **WHEN** a reviewer enables the decision-only filter
- **THEN** the surface SHALL show only holes with more than one shared-planner strategic option while retaining a control to return to all 18 holes
