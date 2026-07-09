## ADDED Requirements

### Requirement: Attribute-Specific Aging Curves

Aging SHALL affect attributes according to attribute-specific curves: different attributes SHALL peak at different ages and decline at different rates. Physical attributes (driving distance, driving accuracy) SHALL peak earlier and decline sooner; skill attributes (irons, wedges, putting) SHALL peak later and hold longer; mental attributes (composure, course management) SHALL keep improving into the later career. Aging SHALL NOT be uniform decline across all attributes.

#### Scenario: Peaks differ by attribute

- **WHEN** a golfer ages from prime into the late career
- **THEN** physical attributes SHALL decline while mental attributes may still rise, so aging is non-uniform

#### Scenario: Aging changes are gradual

- **WHEN** aging is applied for one season
- **THEN** each attribute change SHALL be small, producing believable gradual regression rather than sudden collapse

### Requirement: Career Stages

Every golfer SHALL be classifiable into a career stage — Development, Prime, or Late Career — derived from age. Stages MAY influence development opportunity but SHALL NOT change the underlying gameplay rules.

#### Scenario: Stage derives from age

- **WHEN** a golfer's career stage is evaluated
- **THEN** it SHALL be Development, Prime, or Late Career according to their age

### Requirement: Peak, Longevity, and Diversity

Every golfer SHALL have a period of peak performance, older golfers SHALL remain capable of meaningful competition, and career trajectories SHALL be diverse rather than identical.

#### Scenario: A peak exists and viability persists

- **WHEN** a golfer's overall ability is traced across their career
- **THEN** it SHALL rise to a peak and decline gradually, remaining competitive well past the peak rather than collapsing

#### Scenario: Trajectories differ

- **WHEN** two golfers with different attribute profiles age
- **THEN** their overall trajectories SHALL differ, reflecting their distinct strengths
