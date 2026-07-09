## ADDED Requirements

### Requirement: Tour Definition

A Tour SHALL be a persistent world entity defining at least a name and a tier. Each Tour SHALL possess its own identity and SHALL organise a collection of tournaments.

#### Scenario: Tour exposes its definition

- **WHEN** a Tour is created
- **THEN** it SHALL expose a name and a tier and be uniquely identifiable

### Requirement: Tier Hierarchy

Tours SHALL be organised into ordered competitive tiers, where higher tiers represent stronger competition and lower tiers represent developmental competition. The hierarchy SHALL support both upward and downward movement, and SHALL provide a complete pathway from the entry-level tier to the elite tier.

#### Scenario: Tiers are ordered with a complete pathway

- **WHEN** the tour hierarchy is inspected
- **THEN** every tier SHALL have a defined rank, and there SHALL be a continuous path from the lowest tier to the highest

#### Scenario: Movement is possible in both directions

- **WHEN** a tier that is neither the highest nor the lowest is considered
- **THEN** a tier immediately above (for promotion) and immediately below (for relegation) SHALL exist

### Requirement: Tournament Allocation

Every Tournament SHALL belong to exactly one Tour. Tournament eligibility SHALL derive primarily from Tour membership; invitation events MAY define additional eligibility as a documented exception.

#### Scenario: Each tournament belongs to one tour

- **WHEN** a Tournament is allocated
- **THEN** it SHALL be associated with exactly one Tour

#### Scenario: Eligibility derives from membership

- **WHEN** a golfer's eligibility for a Tour's tournament is evaluated
- **THEN** it SHALL be granted primarily on the basis of that golfer's Tour membership, except where an invitation exception applies

### Requirement: Tour Independence

Each Tour SHALL operate independently, maintaining its own membership and records. World-level coordination SHALL NOT merge the identities or records of separate Tours.

#### Scenario: Tours keep separate records

- **WHEN** two Tours run their competitions
- **THEN** each SHALL maintain its own membership and records without commingling

### Requirement: Domain Boundary

The Tour domain SHALL organise competition only. It SHALL NOT perform shot resolution, World Ranking calculation, financial management, or tournament scoring; it consumes tournament results and never mutates player attributes or tournament results.

#### Scenario: Tours do not compute out-of-domain concerns

- **WHEN** the Tour domain processes results
- **THEN** it SHALL NOT modify player attributes, tournament results, rankings, or finances
