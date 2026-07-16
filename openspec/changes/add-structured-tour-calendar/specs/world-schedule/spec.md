## MODIFIED Requirements

### Requirement: Season Schedule Generation

At the start of every season the World SHALL deterministically generate a competitive calendar: a schedule of tournaments, each allocated to exactly one tour, one course, one week, and **one event prestige (Regular, Signature, Tour Championship, or Major)**. The schedule SHALL include regular tour events, spotlighted **signature** events elevated within a tour, a per-tour season-finale **Tour Championship**, and a configurable number of cross-tour **majors**. At the standard season scale the placement of these events SHALL follow a structured, tier-specific cadence (see *Structured Season Cadence*); at smaller scales it SHALL fall back to a simple proportional placement (see *Small-Season Schedule Degradation*). The schedule SHALL be generated from the world seed so it is reproducible.

#### Scenario: A season has a generated schedule

- **WHEN** a season begins
- **THEN** the World SHALL produce a schedule of tournaments, each with a tour, a course, a week, and an event prestige

#### Scenario: The calendar includes signature events, a championship, and majors

- **WHEN** a season's schedule is generated at the standard scale
- **THEN** it SHALL contain regular tour events, spotlighted signature events, one Tour Championship per tour, and the configured number of majors

#### Scenario: A major draws a cross-tour field

- **WHEN** a major on the schedule is contested
- **THEN** its field SHALL be drawn from the strongest active golfers across all tiers, deterministically, rather than from a single tour's members

#### Scenario: Schedule is reproducible

- **WHEN** two Worlds with the same seed generate the same season's schedule
- **THEN** the two schedules SHALL be identical, including each event's week and prestige

## ADDED Requirements

### Requirement: Structured Season Cadence

At the standard season scale, the schedule SHALL be laid out as a designed, tier-specific cadence rather than an even spread. Majors SHALL anchor fixed "chapter" weeks that divide the Elite season into distinct segments. Signature events SHALL be spotlighted at specific weeks — including a mid-season week on which both the Elite and the Development tour host a signature (a season-wide checkpoint) — rather than clustered at the season's start. Each tour SHALL carry its own rhythm: the Development (and other non-Elite) tours SHALL place their signature spotlights on weeks distinct from the Elite major weeks so their players get their own high-stakes weeks. The overall density SHALL be moderate — roughly one event per tier every other week plus the structured peaks — not an event every week.

#### Scenario: Majors anchor fixed chapter weeks

- **WHEN** a standard-scale season is generated
- **THEN** the majors SHALL fall on fixed, well-spaced weeks that divide the season into chapters, identically for the same seed

#### Scenario: Signatures are spotlighted, not clustered at the start

- **WHEN** a standard-scale season is generated
- **THEN** its signature events SHALL be distributed at spotlight weeks across the season, and SHALL NOT all fall in the opening weeks

#### Scenario: A mid-season collision week

- **WHEN** a standard-scale season is generated
- **THEN** there SHALL be one mid-season week on which both the Elite and the Development tour host a signature event

#### Scenario: Tours have distinct rhythms

- **WHEN** a standard-scale season is generated
- **THEN** the Development tour's signature spotlights SHALL fall on weeks other than the Elite major weeks

### Requirement: Tour Championship Finale

Each tour's season SHALL end with a single Tour Championship — its season-finale event, one per tour. The Development tour SHALL hold its championship one week before the Elite tour's, so newly promoted players get a break week before their next season. A Tour Championship SHALL be a normal counted event whose rewards flow into the season standings that drive promotion and relegation.

#### Scenario: Each tour ends with a championship

- **WHEN** a standard-scale season is generated
- **THEN** each tour SHALL have exactly one Tour Championship event, placed at that tour's final scheduled week

#### Scenario: Development finishes before Elite

- **WHEN** a standard-scale season is generated
- **THEN** the Development Tour Championship SHALL be scheduled earlier than the Elite Tour Championship

### Requirement: Small-Season Schedule Degradation

When the configured season is too short to host the structured cadence, the World SHALL fall back to a simple proportional schedule that still satisfies the base schedule contract — each event allocated a tour, course, week, and prestige, with the configured signature and major counts — and remains seed-reproducible. This preserves valid schedules for small (e.g. test-scale) worlds.

#### Scenario: A short season still produces a valid schedule

- **WHEN** a season shorter than the structured-cadence minimum is generated
- **THEN** the World SHALL produce a valid, reproducible schedule via the proportional fallback, with each event carrying a tour, course, week, and prestige
