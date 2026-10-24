# player-development Specification

## Purpose
TBD - created by archiving change add-progression-aging. Update Purpose after archive.
## Requirements
### Requirement: Development Points

Golfers SHALL earn Development Points over their career (for example each season from competitive participation). Development Points SHALL be allocatable to raise chosen permanent attributes. Only permanent Attributes SHALL be developable — modifiers, ratings, and state SHALL NOT.

#### Scenario: Points are earned and allocatable

- **WHEN** a golfer completes a season of competition
- **THEN** they SHALL be awarded Development Points that can be allocated to permanent attributes

#### Scenario: Only attributes are developable

- **WHEN** development is directed at a non-attribute value
- **THEN** it SHALL be rejected

### Requirement: Allocation Raises Attributes

Allocating Development Points to an attribute SHALL raise that attribute (clamped to 0–100), spending the points. Allocation SHALL enable specialisation (concentrating on chosen attributes) while allowing balanced development.

#### Scenario: Allocation raises the chosen attribute

- **WHEN** Development Points are allocated to an attribute the golfer can afford
- **THEN** that attribute SHALL increase and the points SHALL be spent

#### Scenario: Allocation cannot exceed the points balance

- **WHEN** an allocation would cost more points than the golfer has
- **THEN** a human purchase request SHALL be rejected in full, and automated allocation SHALL not spend more than the available points

### Requirement: Gradual Growth

Attribute growth through development SHALL be gradual — meaningful over multiple seasons rather than a single tournament — and SHALL avoid excessive numerical inflation over a career.

#### Scenario: Growth is bounded per season

- **WHEN** a golfer develops over one season
- **THEN** the total attribute increase SHALL be modest, so meaningful improvement accrues across seasons rather than instantly

### Requirement: Deterministic AI Allocation

Simulation-controlled golfers SHALL allocate their Development Points through a deterministic policy without external input, under their own award and allocation rules. Human players SHALL retain manual control of their banked points.

#### Scenario: AI develops without input

- **WHEN** a simulation golfer is awarded Development Points
- **THEN** a deterministic policy SHALL allocate them, and the same golfer and points SHALL always yield the same allocation

### Requirement: Development Is Recorded

Every permanent attribute change from development SHALL be recorded as part of the golfer's development history.

#### Scenario: Development change is recorded

- **WHEN** development raises an attribute
- **THEN** the change SHALL be recorded (attribute, amount, when)

### Requirement: Development May Be Enhanced by Support

Seasonal development MAY be enhanced by development support (such as a Coach): the Development Points awarded for a season MAY be scaled by a support factor. Without support, development SHALL be unchanged. Enhanced development SHALL still raise only permanent attributes, gradually and clamped, through the sanctioned development path.

#### Scenario: A coach increases development

- **WHEN** a golfer develops for a season with development support
- **THEN** the Development Points awarded that season SHALL be scaled up, yielding greater attribute development than without support

#### Scenario: No support leaves development unchanged

- **WHEN** a golfer develops for a season with no development support
- **THEN** the awarded Development Points SHALL be unchanged from the unsupported baseline

### Requirement: A Player Focus Does Not Replace Manual Development

The human player MAY record an attribute focus, but a season-end award SHALL remain banked until the player manually purchases permanent attribute raises. Setting or omitting a focus SHALL NOT automatically spend points or bypass the cost, potential, and current-season purchased-growth rules.

#### Scenario: A focus does not spend points

- **WHEN** a human golfer receives a seasonal award with a development focus set
- **THEN** the points SHALL remain banked until the player chooses and purchases attribute raises

#### Scenario: No focus preserves manual development

- **WHEN** a human golfer receives a seasonal award without a development focus
- **THEN** the points SHALL remain available for manual allocation rather than being spent automatically

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

