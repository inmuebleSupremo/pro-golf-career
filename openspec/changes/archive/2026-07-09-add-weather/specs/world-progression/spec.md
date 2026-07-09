## ADDED Requirements

### Requirement: World Generates Event Weather

When the World resolves a scheduled event, it SHALL generate that event's Playing Conditions through its Weather System before play and supply them to the Tournament, deterministically from the world seed. The World MAY preserve significant environmental context (severe or record-setting conditions) as environmental history. Weather generation SHALL keep the World reproducible from its seed.

#### Scenario: Each event is played under generated weather

- **WHEN** the World resolves a scheduled tournament
- **THEN** the World SHALL generate that event's Playing Conditions and supply them to the Tournament before play

#### Scenario: Weather keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same events
- **THEN** the generated Playing Conditions and the resulting play SHALL be identical

#### Scenario: Significant conditions may be recorded

- **WHEN** an event is played under exceptionally severe conditions
- **THEN** the World MAY append that environmental context to its history
