# web-career Specification

## Purpose
TBD - created by archiving change add-career-hub. Update Purpose after archive.
## Requirements
### Requirement: Resume a saved career

The system SHALL let an authenticated player resume a saved career from the saves screen, loading it into a session and taking them to that career's hub.

#### Scenario: Resume opens the career hub

- **WHEN** the player selects the resume action on a saved career
- **THEN** the save is loaded into a session and the player is taken to that session's career hub

#### Scenario: Resume failure is surfaced

- **WHEN** loading the save fails
- **THEN** a clear error is shown and the player remains on the saves screen

### Requirement: Career overview

The system SHALL show a read-only overview of a loaded career: its world status, its career goals with progress, and its upcoming schedule.

#### Scenario: World status is shown

- **WHEN** the career hub loads for a valid session
- **THEN** the current season, week, and the size of the active field are displayed

#### Scenario: Career goals with progress

- **WHEN** the career has self-chosen goals
- **THEN** each goal is shown with a readable label, its progress toward the target, and whether it is achieved

#### Scenario: No goals chosen

- **WHEN** the career has no goals
- **THEN** an intentional empty state is shown rather than a blank section

#### Scenario: Upcoming schedule

- **WHEN** the career has eligible upcoming events
- **THEN** the next events are listed with their week, tour tier, event prestige, and whether the player is entered

### Requirement: Read-only hub

The career hub SHALL be read-only in this capability: it presents career state and offers no actions that advance time, play events, or change career decisions.

#### Scenario: No state-changing actions

- **WHEN** the player views the career hub
- **THEN** it exposes only navigation and read views, and no control that advances the season/week, plays an event, or edits goals, staff, equipment, sponsorship, or schedule

### Requirement: Expired session handling

Because a loaded session is held in memory and may no longer exist (for example after a server restart), the system SHALL detect an unknown session and guide the player back to their saves rather than showing a broken screen.

#### Scenario: Session no longer exists

- **WHEN** the career hub is opened for a session that is not found
- **THEN** the hub explains the session is no longer available and offers a way back to the saves screen to reopen the save

#### Scenario: Loading and error states

- **WHEN** the career overview is loading or fails to load
- **THEN** the hub shows the corresponding loading or error state

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

