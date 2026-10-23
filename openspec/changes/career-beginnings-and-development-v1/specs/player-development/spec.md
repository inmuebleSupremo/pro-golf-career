## ADDED Requirements

### Requirement: Human Development Points Remain Manual and Bankable

The human player SHALL receive a season-end Development Point award based on a human-specific base, initially 240, with the existing age-stage, performance, and coach factors retained. The player SHALL continue to choose which permanent attributes to raise, subject to cost, potential, and the seasonal purchased-growth limit. Unspent points SHALL carry forward without expiry or arbitrary reduction. This SHALL NOT change simulation-controlled golfers' award or allocation rules.

#### Scenario: Points remain banked at a limit

- **WHEN** the player has points but every affordable desired raise is blocked by the current-season limit
- **THEN** no points SHALL be removed and those points SHALL remain available in later seasons

#### Scenario: Focused and balanced investment

- **WHEN** the player chooses to invest repeatedly in one attribute or distribute purchases among several
- **THEN** either strategy SHALL be permitted within the same cost, potential, and per-attribute seasonal rules

### Requirement: Purchased Growth Has a Cumulative Per-Attribute Season Limit

The human player's purchased gain to each attribute SHALL be limited per calendar season, initially to +6. All purchases stamped with the current season SHALL count together, including separate transactions and transactions across save/reload. A request exceeding any allowance SHALL be rejected in full before debiting points or changing attributes. Aging changes SHALL NOT consume purchased allowance. The allowance SHALL reset only when the world calendar advances into the next season.

#### Scenario: Separate purchases share the limit

- **WHEN** a player purchases part of an attribute's current-season allowance and later requests further gains
- **THEN** only the unused portion SHALL remain purchasable during that season

#### Scenario: Reload does not reset the limit

- **WHEN** the player saves after a purchase, reloads in the same season, and requests another raise
- **THEN** the earlier purchased gain SHALL still count toward the limit

#### Scenario: An invalid bundle changes nothing

- **WHEN** a multi-attribute request exceeds an attribute's seasonal allowance
- **THEN** the entire request SHALL be rejected without spending points or changing any attribute

#### Scenario: Aging does not consume allowance

- **WHEN** aging changes an attribute at the season transition
- **THEN** that change SHALL NOT reduce the attribute's available purchased gain

#### Scenario: A new season restores allowance

- **WHEN** the calendar advances into the next season
- **THEN** each attribute SHALL have its new season's purchased allowance, still bounded by potential and affordability

### Requirement: Purchase Availability Is Consistent

The simulation SHALL provide an authoritative, read-only answer for whether the human player can purchase the next point of each attribute, considering banked points, next-point cost, potential, and current-season allowance. The spending mutation SHALL validate against the same rules. Existing permanent attribute-change history SHALL remain the record of purchased gains and aging.

#### Scenario: No purchase is available

- **WHEN** no attribute has potential headroom, seasonal allowance, and enough points for its next cost
- **THEN** the authoritative answer SHALL report no purchasable raise

#### Scenario: Older purchase history is respected

- **WHEN** a supported existing career has recorded current-season development purchases before this rule is introduced
- **THEN** those gains SHALL count toward remaining allowance without being reversed or charged again
