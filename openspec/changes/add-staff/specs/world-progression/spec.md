## ADDED Requirements

### Requirement: World Runs the Staff Cycle

The World SHALL give every golfer a Support Team and run a seasonal staff cycle: pay employed staff salaries and hiring costs through the Economy, release staff under financial pressure, and hire new staff per a deterministic policy considering affordability and career stage. The World SHALL apply staff influence — coach-boosted development and fitness/physiotherapist-boosted recovery — without any staff member directly modifying tournament results. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: The seasonal staff cycle runs through the Economy

- **WHEN** the seasonal transition runs
- **THEN** for each active golfer the World SHALL charge staff salaries, possibly release or hire staff per the deterministic policy, and record the changes

#### Scenario: Staff influence is applied by the World

- **WHEN** a golfer employs a Coach or recovery staff
- **THEN** the World SHALL enhance that golfer's development or recovery accordingly, without modifying tournament results

#### Scenario: Staff keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' Support Teams and staff history SHALL be identical
