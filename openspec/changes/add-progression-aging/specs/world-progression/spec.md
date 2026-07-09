## ADDED Requirements

### Requirement: Transition Applies Progression and Aging

The World's seasonal transition SHALL additionally apply progression and aging to every active golfer: award and allocate Development Points, then apply the season's attribute aging. This SHALL use the same rules for all golfers and SHALL keep the world reproducible from its seed.

#### Scenario: Golfers evolve each season

- **WHEN** the seasonal transition runs
- **THEN** every active golfer SHALL receive and allocate Development Points and have the season's aging applied to their attributes

#### Scenario: Progression is deterministic within the world

- **WHEN** two worlds with the same master seed run the same number of seasons
- **THEN** their golfers' evolved attributes SHALL be identical
