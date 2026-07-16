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

