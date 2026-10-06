## ADDED Requirements

### Requirement: Career Inbox Discoverability

The career experience SHALL provide a discoverable entry point to the Career Inbox during normal career navigation. When qualifying personal attention exists, the entry point SHALL show the current number of Inbox items; when none exists, it SHALL not imply unresolved personal attention.

#### Scenario: Attention is discoverable in career mode

- **WHEN** one or more qualifying Career Inbox items exist for the loaded career
- **THEN** the player SHALL be able to discover and open the Career Inbox from normal career navigation and see its item count

#### Scenario: No attention does not create noise

- **WHEN** no qualifying Career Inbox items exist for the loaded career
- **THEN** the career navigation SHALL not display an unresolved-attention count

### Requirement: Schedule Review Completion

The Schedule page SHALL record a Schedule review acknowledgement only after it has been entered and the player subsequently leaves it. The Inbox page and navigation badge SHALL continue to derive their displayed state from the authoritative Career Inbox result after that acknowledgement. The acknowledgement SHALL apply only to the current season and SHALL not alter Schedule decisions or the schedule itself.

#### Scenario: A Schedule visit completes the preseason review

- **WHEN** a player opens Schedule from Inbox and later navigates away
- **THEN** the Schedule Inbox item and navigation count SHALL update to reflect its completion

#### Scenario: Schedule acknowledgement is seasonal

- **WHEN** a player has acknowledged Schedule in one season and reaches a qualifying new season
- **THEN** the normal navigation and Inbox page SHALL show the new season's Schedule item

### Requirement: Staff Review Completion

The Staff landing page SHALL record a Staff review acknowledgement once its candidate context has successfully loaded. The Inbox page and navigation badge SHALL continue to derive their displayed state from the authoritative Career Inbox result after that acknowledgement. The acknowledgement SHALL apply only to the current season and SHALL not alter candidates, hiring, release, staffing, funds, or the simulation.

#### Scenario: Staff landing completes the optional review

- **WHEN** a player follows a Staff Inbox item and the Staff landing loads successfully
- **THEN** the Staff Inbox item and navigation count SHALL update to reflect completion without requiring a staffing action

#### Scenario: New Staff candidates can surface in a later season

- **WHEN** a later season creates qualifying Staff candidates
- **THEN** the normal navigation and Inbox page SHALL show that season's Staff item
