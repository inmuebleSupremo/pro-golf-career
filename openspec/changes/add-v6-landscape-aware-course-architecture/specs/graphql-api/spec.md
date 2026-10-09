# graphql-api Specification

## ADDED Requirements

### Requirement: V6 exposes read-only landscape context projections

For a V6 playing hole, the API SHALL expose a stable read-only local landscape-context projection and stable feature
identity sufficient for rendering. It MAY expose a read-only course-map projection. It SHALL not expose simulation
records, SVG commands, candidate pools, mutable terrain operations or a second gameplay-surface authority.

#### Scenario: Client context does not alter a shot

- **WHEN** a client reads or omits V6 landscape context then submits a shot
- **THEN** server validation and settlement SHALL use effective canonical geometry and existing authoritative state
