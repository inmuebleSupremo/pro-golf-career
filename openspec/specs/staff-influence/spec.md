# staff-influence Specification

## Purpose
TBD - created by archiving change add-staff. Update Purpose after archive.
## Requirements
### Requirement: Role-Consistent Career Influence

Support Team members MAY influence long-term Career development, recovery, preparation, and strategic guidance. Any influence SHALL remain consistent with the staff member's role, and Support Team members SHALL NOT directly modify tournament results.

#### Scenario: A coach influences development, not results

- **WHEN** a golfer employs a Coach
- **THEN** their long-term development SHALL be enhanced consistent with the coaching role, and no tournament result SHALL be directly modified by the staff member

#### Scenario: Recovery staff influence recovery

- **WHEN** a golfer employs a Fitness Coach or Physiotherapist
- **THEN** their physical recovery SHALL be enhanced consistent with that role

### Requirement: Hiring Decisions

Professional Golfers MAY hire new staff members. Hiring decisions SHALL consider factors including financial affordability and career stage, and a hire SHALL become effective only after the relationship is established. Simulation-controlled golfers SHALL decide through a deterministic policy, by the same rules used for any golfer.

#### Scenario: Hiring considers affordability

- **WHEN** a golfer cannot afford a staff member's hiring cost and salary
- **THEN** the staff member SHALL NOT be hired

#### Scenario: AI hiring is deterministic

- **WHEN** a simulation-controlled golfer faces the same team, age, and funds
- **THEN** a deterministic policy SHALL always make the same hiring decision

### Requirement: Financial Relationship

Employing staff SHALL form part of the Career Economy: staff MAY require hiring costs and ongoing financial commitments, and these obligations SHALL integrate with the Economy domain.

#### Scenario: Employment charges the golfer's finances

- **WHEN** a golfer hires and continues to employ a staff member
- **THEN** the hiring cost and ongoing salary SHALL be charged through the golfer's financial account

