## ADDED Requirements

### Requirement: Explicit Pin-Policy Adoption API

The authenticated GraphQL API SHALL expose a narrowly scoped owner-authorized read/mutation contract for a career's pin-placement policy. The read projection SHALL identify the current future-schedule policy and whether adoption is currently permitted. The mutation SHALL explicitly adopt V5 only at a clean event boundary, return the resulting policy status, and make no other career-management change. It SHALL reject a pending-event request without partial state mutation and SHALL be idempotent once V5 already governs all eligible future events.

#### Scenario: Owner adopts V5 at a clean boundary

- **WHEN** the owning player invokes the explicit adoption mutation with no event pending
- **THEN** the response SHALL report V5 as the future policy
- **AND THEN** subsequent policy reads SHALL agree

#### Scenario: Pending event blocks adoption

- **WHEN** the owning player invokes adoption while an event is pending
- **THEN** the API SHALL return a client-classified error and leave policy provenance unchanged
