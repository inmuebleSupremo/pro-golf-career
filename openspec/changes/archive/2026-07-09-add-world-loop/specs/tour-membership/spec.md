## ADDED Requirements

### Requirement: Membership Removal on Departure

A golfer MAY be removed from Tour membership when they leave the active world (for example on retirement). A removed golfer SHALL no longer be a member of any Tour, SHALL be excluded from future Season Standings and promotion/relegation reviews, and their recorded movement history SHALL be preserved.

#### Scenario: Removed golfer leaves active competition

- **WHEN** a golfer is deregistered from Tour membership
- **THEN** they SHALL have no current Tour membership and SHALL not appear in any tier's standings or be moved by a season-end review

#### Scenario: History survives removal

- **WHEN** a golfer who had recorded movements is deregistered
- **THEN** their prior movement history SHALL remain retrievable
