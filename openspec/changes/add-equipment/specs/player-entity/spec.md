## ADDED Requirements

### Requirement: Temporary Equipment Profile

A Player's temporary state SHALL be able to carry a transient equipment profile (forgiveness and power) set before play from the active Golf Bag and surfaced to the shot engine. This profile SHALL be temporary state, not a permanent Attribute, and SHALL NEVER persist into permanent attributes.

#### Scenario: The equipment profile rides temporary state

- **WHEN** the active bag's characteristics are applied to a golfer before play
- **THEN** they SHALL be held as temporary state and surfaced to shot resolution, without changing any permanent Attribute
