# event-prestige Specification

## Purpose
TBD - created by archiving change add-event-prestige. Update Purpose after archive.
## Requirements
### Requirement: Event Prestige Levels

Every tournament SHALL carry an event prestige — one of Regular, Signature, Tour Championship, or Major — distinct from and orthogonal to its tour tier. The prestige levels SHALL be totally ordered **Regular < Signature < Tour Championship < Major**. Prestige SHALL weight the event's rewards (ranking points and prize money) and its career significance monotonically along that order, with a higher-prestige event weighted above a lower one for every reward it affects. Prestige SHALL additionally scale the situational closing-round pressure of the event along the same order — a Major's closing rounds carry the most pressure — so higher-prestige events are harder to close. Prestige SHALL further scale the event's course-setup difficulty along the same order — a Major is set up hardest (most tucked pins, tightest, windiest) — so higher-prestige events play tougher for the whole field, not only in contention. Prestige SHALL NOT enter the base shot mechanics directly; it affects play only through situational pressure and course setup (the resolution math for a given hole, pin, and condition is prestige-independent).

#### Scenario: Prestige is independent of tour tier

- **WHEN** an event is defined
- **THEN** it SHALL have both a tour tier and an event prestige, and either may vary without the other

#### Scenario: Higher prestige means greater rewards, monotonically

- **WHEN** two events of the same tour tier and field strength but different prestige are compared
- **THEN** the higher-prestige event (by the order Regular < Signature < Tour Championship < Major) SHALL award more ranking points and a larger purse for the same finishing position

#### Scenario: A Tour Championship sits between Signature and Major

- **WHEN** Regular, Signature, Tour Championship, and Major events of the same tour tier and field strength are compared
- **THEN** the Tour Championship SHALL award more than a Signature and less than a Major for the same finishing position

#### Scenario: Higher prestige raises closing-round pressure

- **WHEN** the same competitive situation is evaluated at different prestige levels
- **THEN** the higher-prestige event SHALL impose more closing-round pressure on the golfers in contention, so its closing play is harder — while the base shot mechanics remain prestige-independent

#### Scenario: Higher prestige raises course-setup difficulty

- **WHEN** two events of the same tour tier but different prestige are set up
- **THEN** the higher-prestige event SHALL have a harder course setup (more tucked pins, tighter effective width, harsher wind), raising the whole field's scoring average

### Requirement: Majors Are Cross-Tour Marquee Events

A Major SHALL draw its field from the strongest active golfers across all tour tiers, rather than from a single tour's members, and SHALL carry the greatest ranking-point weight, the largest purse, and the greatest career significance of any event. The selection of the strongest field SHALL be deterministic.

#### Scenario: A major's field spans the tours

- **WHEN** a major is contested
- **THEN** its field SHALL be drawn from the strongest eligible active golfers across all tiers, not restricted to one tour's membership

#### Scenario: A major is the biggest news

- **WHEN** a golfer wins a major
- **THEN** the victory SHALL be reported as a distinct, high-prominence event in the world narrative

#### Scenario: A golfer plays at most one event per week

- **WHEN** a major and a tour event fall in the same week and a golfer qualifies for the major
- **THEN** that golfer SHALL be entered only in the major and SHALL NOT also be drawn into the concurrent tour event

