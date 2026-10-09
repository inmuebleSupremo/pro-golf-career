## ADDED Requirements

### Requirement: Truthful V4 strategic landing options

For an eligible non-putting V4 hole, the simulation SHALL derive strategic landing options from the current ball, V4 route/landing-zone semantics, canonical terrain, legal club/family capability, and supported current ground-response facts. An option SHALL contain a literal canonical aim point and SHALL be publishable only when it is viable and materially distinct from the ordinary route.

#### Scenario: Strategic par 5 has a real safe/aggressive trade-off

- **WHEN** a deterministic V4 strategic par-5 fixture has reachable SAFE and AGGRESSIVE landing zones
- **THEN** the planner SHALL return valid distinct options using legal club/family choices
- **AND THEN** AGGRESSIVE SHALL improve the deterministic route/next-shot consequence while carrying greater supported canonical exposure than SAFE
- **AND THEN** SAFE SHALL retain its safer positional trade-off rather than being a duplicate marker.

### Requirement: No fabricated strategic choice

The simulation SHALL not expose SAFE, PRIMARY, or AGGRESSIVE as distinct choices merely because a generated plan contains zones. It SHALL reject unreachable, illegal, non-playable, redundant, or unsupported candidates and fall back to the established ordinary PRIMARY target when no real choice remains.

#### Scenario: Positional route can be the sole honest option

- **WHEN** a V4 positional par 4 has an approved route/landing target but no distinct viable alternative
- **THEN** the planner SHALL publish that route as the ordinary PRIMARY option
- **AND THEN** it SHALL not claim a direct green/pin attack or synthetic SAFE/AGGRESSIVE trade-off.

### Requirement: Deterministic shared strategy selection

AI strategy policy SHALL choose only from the same deterministic strategic option facts exposed to human guidance. It SHALL use current geometry, club/family, lie, conditions, and qualitative exposure facts, and SHALL not inspect or consume a future random shot outcome.

#### Scenario: Disposition follows published candidates

- **WHEN** SAFE, PRIMARY, and AGGRESSIVE options are viable in a fixed V4 fixture
- **THEN** conservative AI SHALL prefer SAFE, balanced AI SHALL prefer PRIMARY, and aggressive AI SHALL prefer AGGRESSIVE
- **AND THEN** repeated planning SHALL return the same choice without altering the next resolver seed/result.

### Requirement: Legacy and non-eligible compatibility

V1/V2 holes and V4 situations without eligible distinct strategic options SHALL retain the existing target/policy path. Completed historical holes/events SHALL not be re-resolved; future unresolved V4 strokes MAY use the planner without a persisted strategy migration.

#### Scenario: Legacy generated hole retains prior aim path

- **WHEN** a V1 or V2 compatibility fixture requests an AI or ordinary guidance target
- **THEN** it SHALL retain its established target behaviour
- **AND THEN** no V4 strategic option SHALL be invented.
