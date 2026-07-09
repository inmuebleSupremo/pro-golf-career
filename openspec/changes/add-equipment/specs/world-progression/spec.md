## ADDED Requirements

### Requirement: World Runs the Equipment Cycle

The World SHALL give every golfer an Equipment Inventory and Tournament Loadout, run Economy-integrated equipment acquisition each season through a deterministic policy, sync each competitor's active Golf Bag characteristics into shot resolution before play, and preserve equipment ownership history. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Golfers acquire equipment through the Economy

- **WHEN** the seasonal transition runs
- **THEN** each active golfer MAY acquire an affordable equipment upgrade, charged through their financial account and recorded in ownership history

#### Scenario: The active bag is applied to play

- **WHEN** the World resolves an event
- **THEN** each competitor's active bag characteristics SHALL be applied to their shot resolution, and only owned equipment SHALL be used

#### Scenario: Equipment keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' inventories and ownership history SHALL be identical
