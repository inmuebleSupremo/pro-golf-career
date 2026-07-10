# career-goals Specification

## Purpose
TBD - created by archiving change add-career-goals. Update Purpose after archive.
## Requirements
### Requirement: Self-Chosen Career Goals

The player SHALL be able to set a lightweight set of self-chosen career goals for their golfer — for example reaching the top tour, winning a major, becoming world number one, a career-wins target, a career-earnings target, or making the Hall of Fame. Goals SHALL be the player's own framing and MAY be changed. A world with no designated player SHALL have no goals.

#### Scenario: The player sets career goals

- **WHEN** the player sets a set of career goals for their golfer
- **THEN** those goals SHALL be recorded as the player's standing ambitions and SHALL be retrievable

### Requirement: Goals Never Gate Play

Career goals SHALL be purely observational: they SHALL NOT change any competitive outcome, unlock or restrict any event, or otherwise gate play. Setting goals SHALL leave the world's competitive outcomes identical to not setting them.

#### Scenario: Goals do not change outcomes

- **WHEN** two identical worlds advance the same seasons, one with career goals set and one without
- **THEN** their rankings, careers, and records SHALL be identical (differing only in the world narrative's goal-achievement news)

### Requirement: Tracked Progress Toward Goals

For each of the player's goals the World SHALL surface progress against the golfer's real career state — the current value, the target, and whether the goal is achieved — evaluated from the live world (tour tier, ranking position, career wins, majors won, earnings, Hall-of-Fame eligibility).

#### Scenario: Progress reflects real career state

- **WHEN** the player reviews their career goals
- **THEN** each goal SHALL show its current value, its target, and whether it is achieved, derived from the golfer's actual career state

### Requirement: Achieving a Goal Is a Narrative Moment

When one of the player's goals first becomes achieved during an advance, the World SHALL announce it once in the world narrative. A goal already announced SHALL NOT be announced again.

#### Scenario: A newly achieved goal is announced once

- **WHEN** a goal transitions to achieved as the world advances
- **THEN** a goal-achievement item SHALL be published to the world narrative exactly once for that goal

