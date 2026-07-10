# player-control Specification

## Purpose
TBD - created by archiving change add-player-control. Update Purpose after archive.
## Requirements
### Requirement: Designated Player Golfer

A world MAY have a single golfer designated as human-controlled. When none is designated, the world SHALL run fully autonomously. Designating a player SHALL NOT change how any other golfer is handled.

#### Scenario: A golfer is designated as the player's

- **WHEN** a golfer is designated human-controlled in a world
- **THEN** that golfer SHALL be player-controlled and every other golfer SHALL remain autonomous

#### Scenario: An unassigned world is autonomous

- **WHEN** a world has no designated player
- **THEN** it SHALL run exactly as a fully autonomous world

### Requirement: Standing Decisions Applied at Advance

The player SHALL configure standing decisions for their golfer — a development focus, event entry (which events to enter or skip, and a blanket resting choice), sponsorship acceptances, and staff and equipment decisions — and the world SHALL apply them for that golfer during a normal advance (configure-then-advance), without pausing mid-advance except to let the player play an event they entered.

#### Scenario: Development follows the player's focus

- **WHEN** the player sets a development focus and the world advances a season
- **THEN** their golfer's development SHALL be directed to the chosen attributes rather than the automatic allocation

#### Scenario: A resting golfer sits out and recovers

- **WHEN** the player sets their golfer to rest and the world advances
- **THEN** their golfer SHALL be excluded from event fields and SHALL recover rather than compete

### Requirement: Sponsorship Accept or Decline

For the player's golfer, generated sponsorship offers SHALL be presented as pending for the player to accept or decline, rather than being accepted automatically. Accepting an offer SHALL sign it within the permitted limits; unaccepted offers SHALL lapse.

#### Scenario: Offers await the player's decision

- **WHEN** the world generates sponsorship offers for the player's golfer
- **THEN** they SHALL be held as pending offers and SHALL NOT be signed automatically

#### Scenario: Accepting an offer signs it

- **WHEN** the player accepts a pending offer they are permitted to hold
- **THEN** it SHALL be signed to their financial account and removed from the pending offers

### Requirement: Staff Hire and Release

For the player's golfer, staff hiring SHALL be a player decision rather than automatic. The World SHALL present a market of generated candidates for the player's open roles as pending staff offers; the player MAY hire a candidate (spending its hiring cost, if affordable) or release a current member. Mandatory salaries and under-debt release SHALL still apply automatically. When no candidate is hired, no staff SHALL be added.

#### Scenario: Candidates await the player's decision

- **WHEN** the world runs the player's season transition
- **THEN** a market of staff candidates for the player's open roles SHALL be held as pending offers and none SHALL be hired automatically

#### Scenario: Hiring a candidate signs and pays for them

- **WHEN** the player hires a pending candidate they can afford
- **THEN** that member SHALL join the support team, the hiring cost SHALL be charged to the player's account, and the offer SHALL be removed

#### Scenario: An unaffordable hire is declined

- **WHEN** the player attempts to hire a candidate they cannot afford
- **THEN** no member SHALL be hired and no charge SHALL be made

#### Scenario: The player releases a member

- **WHEN** the player releases one of their staff
- **THEN** that role SHALL become vacant and the departure SHALL be recorded in the team history

### Requirement: Equipment Purchase and Loadout

For the player's golfer, equipment purchases SHALL be a player decision rather than automatic. The World SHALL present generated upgrade offers as pending; the player MAY buy an upgrade (spending its cost, if affordable), which is added to their inventory, and MAY set their tournament loadout by selecting an owned item for a category. When nothing is bought, no equipment SHALL be acquired.

#### Scenario: Upgrades await the player's decision

- **WHEN** the world runs the player's season transition
- **THEN** equipment upgrade offers SHALL be held as pending and none SHALL be purchased automatically

#### Scenario: Buying an upgrade acquires and pays for it

- **WHEN** the player buys a pending upgrade they can afford
- **THEN** the item SHALL be added to their inventory, its cost SHALL be charged, and it SHALL be selectable in their loadout

#### Scenario: The player sets their loadout from owned gear

- **WHEN** the player selects an owned item for a category
- **THEN** their tournament loadout SHALL use that item for that category

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

