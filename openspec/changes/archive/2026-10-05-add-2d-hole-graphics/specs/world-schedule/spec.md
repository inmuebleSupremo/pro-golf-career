## ADDED Requirements

### Requirement: Permanent Venues For Marquee Events

Marquee named events — the cross-tour majors and the per-tour Tour Championships — SHALL keep a permanent venue across seasons: the same named marquee event SHALL be assigned the same course every season it is held, rather than rotating through the course pool by a running event counter. The assignment SHALL remain deterministic and reproducible from the world seed. Regular and signature events MAY continue to draw their course from the pool by rotation.

#### Scenario: A major returns to the same course each season

- **WHEN** the same major is scheduled in two different seasons of a world
- **THEN** it SHALL be assigned the same course in both seasons

#### Scenario: A tour championship keeps its venue

- **WHEN** a tour's Tour Championship is scheduled across multiple seasons
- **THEN** it SHALL be held at the same course each season

#### Scenario: Marquee venues are reproducible from the seed

- **WHEN** two worlds with the same seed generate their schedules
- **THEN** each marquee event SHALL be assigned the same course in both worlds

#### Scenario: Regular events may still rotate

- **WHEN** regular and signature events are scheduled across seasons
- **THEN** their course assignment MAY vary season to season, unaffected by the marquee-venue rule
