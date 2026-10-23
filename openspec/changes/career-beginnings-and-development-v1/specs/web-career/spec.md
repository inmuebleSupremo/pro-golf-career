## ADDED Requirements

### Requirement: Training Explains Purchase Availability

The existing Development interface SHALL display banked Development Points and, for each attribute, its next-point cost, remaining potential, remaining allowable purchased gain in the current season, and a clear reason when another point cannot currently be purchased. It SHALL use the backend's current purchase availability for its controls and SHALL keep the backend spending mutation authoritative for submitted raises. It SHALL not introduce another development-management screen.

#### Scenario: A purchase is available

- **WHEN** an attribute has potential headroom, seasonal allowance, and sufficient banked points
- **THEN** the player SHALL see its next cost and remaining allowance and be able to propose a raise

#### Scenario: A purchase is blocked

- **WHEN** potential, seasonal allowance, or available points prevent another raise
- **THEN** the control SHALL prevent an invalid proposal and explain the applicable blocker

#### Scenario: Points remain visible after reaching a limit

- **WHEN** an attribute reaches its seasonal purchased limit while points remain banked
- **THEN** the balance SHALL remain visible and the interface SHALL explain that the attribute can be raised again in a later season if potential permits

### Requirement: Development Attention Matches Purchase Eligibility

The career experience SHALL show Development attention in the existing Career Inbox only when the backend reports at least one currently affordable, potential-bounded, seasonally permitted attribute raise. The Inbox SHALL remain a route to the existing Development interface, not a second purchase path. This requirement constrains the Development source of the active `add-career-inbox` change; its other sources and acknowledgements remain unchanged.

#### Scenario: Seasonal limits clear Development attention

- **WHEN** banked points remain but no attribute has a valid current-season purchase
- **THEN** the Career Inbox SHALL omit the Development item and count

#### Scenario: Development attention returns when valid

- **WHEN** a new season or changed balance makes at least one attribute purchase valid
- **THEN** the Career Inbox SHALL include Development attention derived from that current state
