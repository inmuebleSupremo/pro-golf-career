## ADDED Requirements

### Requirement: Existing Careers Preserve Development State

Supported existing saves SHALL load with their recorded attributes, potential, banked Development Points, calendar position, and development history unchanged. Loading SHALL NOT assign a new starting build or recalculate a golfer from a Starting Background. Current-season purchased allowance SHALL be reconstructed from reliable recorded development changes where available, so saving and reloading cannot reset it. No additional save field SHALL be required when the existing history supplies this information.

#### Scenario: Existing career is not rebuilt

- **WHEN** a career saved before Starting Backgrounds is loaded
- **THEN** its attributes, potential, banked points, and prior development history SHALL equal the saved values

#### Scenario: Same-season reload preserves availability

- **WHEN** a career is saved after one or more development purchases and reloaded before the calendar advances
- **THEN** remaining purchased allowance and point balance SHALL match the uninterrupted career

#### Scenario: Already-high historical gains are grandfathered

- **WHEN** a supported older career has already recorded current-season gains above the new limit
- **THEN** those gains SHALL remain intact, no additional gain to that attribute SHALL be purchasable that season, and a fresh allowance SHALL begin next season

#### Scenario: Supported legacy history is incomplete

- **WHEN** a supported older save lacks reliable records for earlier current-season purchases
- **THEN** existing ratings and points SHALL be preserved, earlier unrecorded purchases SHALL not be guessed from current ratings, and purchases after load SHALL be tracked and limited deterministically
