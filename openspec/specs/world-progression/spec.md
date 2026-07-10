# world-progression Specification

## Purpose
TBD - created by archiving change add-world-loop. Update Purpose after archive.
## Requirements
### Requirement: Automatic Event Resolution

When the World advances a week, it SHALL automatically resolve each tournament scheduled that week: draw the field from the event's tour's eligible members, run the Tournament to completion via the shared engine, and record the result. Resolution SHALL use the same rules for all competitors.

#### Scenario: Scheduled events resolve automatically

- **WHEN** a week containing scheduled tournaments is advanced
- **THEN** each such tournament SHALL be played to completion and produce a winner and full results

#### Scenario: Field is drawn from tour membership

- **WHEN** an event is resolved
- **THEN** its field SHALL consist of eligible members of the event's tour

### Requirement: Results Feed Ranking, Standings, and Careers

Each resolved tournament result SHALL be fed to the World Ranking, the event tour's Season Standings, and every competitor's Career. These consumers SHALL be updated from the same result.

#### Scenario: One result updates all consumers

- **WHEN** a tournament result is produced
- **THEN** the World Ranking, the tour's Season Standings, and each competitor's Career record SHALL all be updated from that result

### Requirement: Seasonal Transition

At season end the World SHALL run a seasonal transition that: takes a season-ending ranking snapshot; runs Tour promotion/relegation; advances every golfer's Career by one season (retiring those who reach the mandatory retirement age); replenishes the population for departures; and generates the next season's calendar.

#### Scenario: Transition fires every seam

- **WHEN** the seasonal transition runs
- **THEN** a ranking snapshot SHALL be taken, tour promotion/relegation SHALL be applied, every Career SHALL advance one season, departures SHALL be replenished, and the next season's schedule SHALL be generated

#### Scenario: Retirement occurs through the transition

- **WHEN** a golfer reaches the mandatory retirement age during the transition
- **THEN** that golfer SHALL retire and SHALL be replaced so the population remains sufficient

### Requirement: Coordination Boundary

The World SHALL drive only the public operations of the owning domains. It SHALL NOT compute shot outcomes, ranking points, prize finances, or promotion/relegation itself — it composes the domains that own those computations.

#### Scenario: World delegates domain work

- **WHEN** the World runs progression or a transition
- **THEN** shot outcomes, ranking values, and tour movement SHALL be produced by their owning domains, invoked by the World

### Requirement: Transition Applies Progression and Aging

The World's seasonal transition SHALL additionally apply progression and aging to every active golfer: award and allocate Development Points, then apply the season's attribute aging. This SHALL use the same rules for all golfers and SHALL keep the world reproducible from its seed.

#### Scenario: Golfers evolve each season

- **WHEN** the seasonal transition runs
- **THEN** every active golfer SHALL receive and allocate Development Points and have the season's aging applied to their attributes

#### Scenario: Progression is deterministic within the world

- **WHEN** two worlds with the same master seed run the same number of seasons
- **THEN** their golfers' evolved attributes SHALL be identical

### Requirement: World Generates Event Weather

When the World resolves a scheduled event, it SHALL generate that event's Playing Conditions through its Weather System before play and supply them to the Tournament, deterministically from the world seed. The World MAY preserve significant environmental context (severe or record-setting conditions) as environmental history. Weather generation SHALL keep the World reproducible from its seed.

#### Scenario: Each event is played under generated weather

- **WHEN** the World resolves a scheduled tournament
- **THEN** the World SHALL generate that event's Playing Conditions and supply them to the Tournament before play

#### Scenario: Weather keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same events
- **THEN** the generated Playing Conditions and the resulting play SHALL be identical

#### Scenario: Significant conditions may be recorded

- **WHEN** an event is played under exceptionally severe conditions
- **THEN** the World MAY append that environmental context to its history

### Requirement: World Runs the Financial Cycle

The World SHALL give every golfer a financial identity, award prize money and charge career expenses when it resolves each event, and run a seasonal financial cycle — paying active sponsorships, evaluating their objectives, concluding or renewing agreements, and generating and accepting new reputation-gated offers. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Events move every competitor's finances

- **WHEN** the World resolves a scheduled event
- **THEN** each competitor SHALL have their career expenses charged and their prize money awarded to their financial identity

#### Scenario: The seasonal financial cycle runs

- **WHEN** the seasonal transition runs
- **THEN** for each active golfer the World SHALL pay active sponsorships, evaluate objectives, conclude or renew agreements, and generate and accept new reputation-gated offers

#### Scenario: Finances are reproducible within the world

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' financial state SHALL be identical

### Requirement: World Runs the Health Cycle

The World SHALL give every golfer a Physical State, gate each event's field by availability, feed fatigue into shot resolution, accrue fatigue and roll injuries after events, recover fatigue and advance rehabilitation over time, and record significant health events. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Only available golfers enter events

- **WHEN** the World forms a field for a scheduled event
- **THEN** golfers who are injured, recovering, or resting SHALL be excluded from the field

#### Scenario: Competing accrues fatigue and recovery restores it

- **WHEN** the World resolves events and advances weeks
- **THEN** competitors SHALL accrue fatigue from participation, and golfers SHALL recover fatigue and advance any rehabilitation over time

#### Scenario: Health keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' physical state SHALL be identical

### Requirement: World Runs the Staff Cycle

The World SHALL give every golfer a Support Team and run a seasonal staff cycle: pay employed staff salaries and hiring costs through the Economy, release staff under financial pressure, and hire new staff per a deterministic policy considering affordability and career stage. The World SHALL apply staff influence — coach-boosted development and fitness/physiotherapist-boosted recovery — without any staff member directly modifying tournament results. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: The seasonal staff cycle runs through the Economy

- **WHEN** the seasonal transition runs
- **THEN** for each active golfer the World SHALL charge staff salaries, possibly release or hire staff per the deterministic policy, and record the changes

#### Scenario: Staff influence is applied by the World

- **WHEN** a golfer employs a Coach or recovery staff
- **THEN** the World SHALL enhance that golfer's development or recovery accordingly, without modifying tournament results

#### Scenario: Staff keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' Support Teams and staff history SHALL be identical

### Requirement: World Runs the Equipment Cycle

The World SHALL give every golfer an Equipment Inventory and Tournament Loadout, run Economy-integrated equipment acquisition each season through a deterministic policy, sync each competitor's active Golf Bag characteristics into shot resolution before play, and preserve equipment ownership history. This SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Golfers acquire equipment through the Economy

- **WHEN** the seasonal transition runs
- **THEN** each active golfer MAY acquire an affordable equipment upgrade, charged through their financial account and recorded in ownership history

#### Scenario: The active bag is applied to play

- **WHEN** the World resolves an event
- **THEN** each competitor's active bag characteristics SHALL be applied to their shot resolution, and only owned equipment SHALL be used

#### Scenario: Equipment keeps the world reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their golfers' inventories and ownership history SHALL be identical

### Requirement: World Runs the Media System

The World SHALL run a Media System, publishing News Events from real outcomes as it resolves events and runs seasonal transitions, and exposing the news feed and per-golfer career narratives. Media SHALL NOT affect any simulation outcome, and generation SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: The World publishes news from real events

- **WHEN** the World resolves events and runs a seasonal transition
- **THEN** it SHALL publish News Events for the significant outcomes that occurred (such as victories, upsets, number-one changes, promotions, and retirements)

#### Scenario: Media does not change outcomes

- **WHEN** the Media System publishes news during World progression
- **THEN** no tournament result, ranking, career, or progression outcome SHALL be changed by it

#### Scenario: The narrative is reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their news feeds SHALL be identical

### Requirement: World Runs the Statistics Archive

The World SHALL run a Statistics archive, feeding each tournament result and season boundary into it to accumulate per-golfer statistics, register champions, and update records. The archive SHALL NOT affect any simulation outcome, and its accumulation SHALL be deterministic, keeping the World reproducible from its seed.

#### Scenario: Results and seasons feed the archive

- **WHEN** the World resolves events and completes seasons
- **THEN** it SHALL feed those outcomes into the Statistics archive, accumulating statistics, registering champions, and updating records

#### Scenario: The archive changes no outcome

- **WHEN** the Statistics archive records outcomes during World progression
- **THEN** no tournament result, ranking, career, or progression outcome SHALL be changed by it

#### Scenario: The archive is reproducible

- **WHEN** two worlds with the same master seed advance through the same seasons
- **THEN** their statistics, records, and champions archives SHALL be identical

### Requirement: World Applies the Player's Decisions

When a world has a designated player, the World SHALL apply that player's decisions for their golfer during advance — directing development to the player's focus, excluding a resting golfer from event fields, and deferring sponsorship acceptance to the player's pending offers — while using the existing automatic paths for all other golfers. A world with no designated player SHALL be unchanged.

#### Scenario: The player's golfer follows player decisions, others follow AI

- **WHEN** a world with a designated player advances
- **THEN** the player's golfer SHALL follow the player's development focus, resting, and sponsorship choices, and every other golfer SHALL follow the automatic policies

#### Scenario: No player leaves the world unchanged

- **WHEN** a world with no designated player advances
- **THEN** its outcomes SHALL be identical to the same world advanced without the player-control feature

