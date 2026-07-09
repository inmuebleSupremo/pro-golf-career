## ADDED Requirements

### Requirement: World Runs the Media System

The World SHALL run a Media System, publishing News Events from real outcomes as it resolves events and runs seasonal transitions, and exposing the news feed and per-golfer career narratives. Media SHALL NOT affect any simulation outcome, and generation SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: The World publishes news from real events

- **WHEN** the World resolves events and runs a seasonal transition
- **THEN** it SHALL publish News Events for the significant outcomes that occurred (such as victories, upsets, number-one changes, promotions, and retirements)

#### Scenario: Media does not change outcomes

- **WHEN** the Media System publishes news during World progression
- **THEN** no tournament result, ranking, career, or progression outcome SHALL be changed by it

#### Scenario: The narrative is reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their news feeds SHALL be identical
