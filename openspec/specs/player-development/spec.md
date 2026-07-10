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
- **THEN** it SHALL be rejected or limited to the affordable amount

### Requirement: Gradual Growth

Attribute growth through development SHALL be gradual — meaningful over multiple seasons rather than a single tournament — and SHALL avoid excessive numerical inflation over a career.

#### Scenario: Growth is bounded per season

- **WHEN** a golfer develops over one season
- **THEN** the total attribute increase SHALL be modest, so meaningful improvement accrues across seasons rather than instantly

### Requirement: Deterministic AI Allocation

Simulation-controlled golfers SHALL allocate their Development Points through a deterministic policy without external input, by the same rules used for any golfer.

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

### Requirement: Development May Follow a Player Focus

Seasonal development MAY follow a player-chosen attribute focus: the season's Development Points SHALL be allocated to the chosen attributes (in priority order) instead of the automatic allocation, still raising only permanent attributes gradually and within the per-season cap and cost curve. With no focus, development SHALL be unchanged from the automatic allocation.

#### Scenario: A focus directs development

- **WHEN** a golfer develops for a season with a development focus
- **THEN** the Development Points SHALL be spent on the focused attributes (subject to the cap and cost curve) rather than the automatic allocation

#### Scenario: No focus preserves automatic development

- **WHEN** a golfer develops for a season with no focus
- **THEN** development SHALL match the automatic allocation exactly

