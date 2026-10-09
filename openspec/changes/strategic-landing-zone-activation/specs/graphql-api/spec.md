## ADDED Requirements

### Requirement: Structured strategic guidance projection

The current-shot GraphQL planning projection SHALL expose an additive ordered collection of backend-authored strategic guidance options. Each option SHALL identify its role, literal aim point, suggested legal club/family, and concise qualitative route/exposure explanation. The API SHALL not expose client-authored terrain classification, probability, future execution result, or final-ball prediction.

#### Scenario: Distinct options are projected without browser inference

- **WHEN** an eligible V4 current shot has genuine strategic alternatives
- **THEN** GraphQL SHALL return only the backend-authored distinct options and their explanatory facts
- **AND THEN** the client can render them without comparing coordinates or inspecting canonical geometry.

#### Scenario: Ordinary fallback remains useful

- **WHEN** no distinct strategic alternative is viable
- **THEN** GraphQL SHALL return an ordinary PRIMARY guidance option
- **AND THEN** it SHALL not project a misleading SAFE or AGGRESSIVE option.
