## ADDED Requirements

### Requirement: World Runs the Financial Cycle

The World SHALL give every golfer a financial identity, award prize money and charge career expenses when it resolves each event, and run a seasonal financial cycle — paying active sponsorships, evaluating their objectives, concluding or renewing agreements, and generating and accepting new reputation-gated offers. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Events move every competitor's finances

- **WHEN** the World resolves a scheduled event
- **THEN** each competitor SHALL have their career expenses charged and their prize money awarded to their financial identity

#### Scenario: The seasonal financial cycle runs

- **WHEN** the seasonal transition runs
- **THEN** for each active golfer the World SHALL pay active sponsorships, evaluate objectives, conclude or renew agreements, and generate and accept new reputation-gated offers

#### Scenario: Finances are reproducible within the world

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' financial state SHALL be identical
