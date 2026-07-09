## ADDED Requirements

### Requirement: Equipment Inventory

Every Professional Golfer SHALL possess an Equipment Inventory representing all equipment currently owned. Equipment SHALL remain available until removed, ownership SHALL be recorded, and the inventory SHALL persist throughout the Career.

#### Scenario: A golfer owns a persistent inventory

- **WHEN** a Professional Golfer exists in the world
- **THEN** they SHALL have an Equipment Inventory of owned equipment that persists for the life of the Career, with ownership recorded

### Requirement: Equipment Categories

The simulation SHALL support the following V1 equipment categories: Driver, Fairway Woods, Hybrids, Irons, Wedges, Putter, and Golf Ball. Future versions MAY introduce additional categories without changing this requirement.

#### Scenario: Owned equipment falls into the V1 categories

- **WHEN** an item of equipment is owned
- **THEN** its category SHALL be one of Driver, Fairway Woods, Hybrids, Irons, Wedges, Putter, or Golf Ball

### Requirement: Equipment Acquisition

Professional Golfers MAY acquire additional equipment throughout their Careers (for example by purchase), and acquisition SHALL integrate with the Economy domain.

#### Scenario: Purchasing equipment charges the golfer's finances

- **WHEN** a golfer acquires an equipment item by purchase
- **THEN** the item SHALL be added to their inventory and its cost SHALL be charged through their financial account

#### Scenario: Unaffordable equipment is not acquired

- **WHEN** a golfer cannot afford an equipment item
- **THEN** it SHALL NOT be acquired and the inventory SHALL be unchanged

### Requirement: Equipment Ownership History

Equipment ownership SHALL form part of Career history: the simulation SHALL preserve significant equipment changes, contributing to the golfer's long-term identity.

#### Scenario: Acquisitions are preserved in history

- **WHEN** a golfer acquires equipment
- **THEN** the acquisition SHALL be recorded in the inventory's ownership history
