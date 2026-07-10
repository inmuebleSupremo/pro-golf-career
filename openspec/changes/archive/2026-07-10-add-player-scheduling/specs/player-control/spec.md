## ADDED Requirements

### Requirement: Event-by-Event Scheduling

The player SHALL choose which events to enter on an event-by-event basis. By default the player SHALL be entered in every event they are eligible for; the player MAY skip specific eligible events to manage fatigue and travel, and MAY re-enter a previously skipped event. The blanket resting choice SHALL remain available as a way to sit out all events. A player SHALL only be able to enter events they are eligible for (a member of the event's tour, or a major, which is cross-tour).

#### Scenario: Eligible events are entered by default

- **WHEN** the player has made no scheduling choice for an event they are eligible for
- **THEN** the player SHALL be entered in that event

#### Scenario: Skipping an eligible event sits it out

- **WHEN** the player skips an event they are eligible for and the world advances to it
- **THEN** the player SHALL be excluded from that event's field and SHALL recover instead, and the event SHALL be resolved automatically

#### Scenario: Re-entering a skipped event

- **WHEN** the player re-enters an event they had skipped
- **THEN** the player SHALL again be entered in that event by default

### Requirement: Reviewable Eligible Schedule

The player SHALL be able to review their eligible upcoming schedule — the events they may enter, each with its week, tour tier, event prestige, and whether the player is currently entered — so the human can weigh which events to play.

#### Scenario: The player reviews their schedule

- **WHEN** the player inspects their schedule
- **THEN** they SHALL see each eligible upcoming event with its prestige and their current entry choice

## MODIFIED Requirements

### Requirement: Standing Decisions Applied at Advance

The player SHALL configure standing decisions for their golfer — a development focus, event entry (which events to enter or skip, and a blanket resting choice), sponsorship acceptances, and staff and equipment decisions — and the world SHALL apply them for that golfer during a normal advance (configure-then-advance), without pausing mid-advance except to let the player play an event they entered.

#### Scenario: Development follows the player's focus

- **WHEN** the player sets a development focus and the world advances a season
- **THEN** their golfer's development SHALL be directed to the chosen attributes rather than the automatic allocation

#### Scenario: A resting golfer sits out and recovers

- **WHEN** the player sets their golfer to rest and the world advances
- **THEN** their golfer SHALL be excluded from event fields and SHALL recover rather than compete
