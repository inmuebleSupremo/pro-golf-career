# equipment-influence Specification

## Purpose
TBD - created by archiving change add-equipment. Update Purpose after archive.
## Requirements
### Requirement: Equipment Characteristics

Equipment MAY possess gameplay characteristics (for example forgiveness, workability, launch profile, spin profile, or feel). The simulation SHALL define the existence of equipment characteristics on items; their detailed implementation is not prescribed.

#### Scenario: Items carry characteristics

- **WHEN** an equipment item exists
- **THEN** it SHALL expose gameplay characteristics

### Requirement: Characteristics Influence Play Within the Boundary

The active Golf Bag's characteristics SHALL be able to influence shot resolution — forgiveness reduces dispersion, power extends reach, **workability improves ball-flight control in wind, and feel improves distance control** — consistently for all golfers. The Equipment domain SHALL NOT itself perform shot calculations, financial transactions, tournament scoring, or player progression, and SHALL NOT directly modify player attributes or tournament results.

#### Scenario: Better equipment measurably helps play

- **WHEN** a golfer plays with a bag of stronger characteristics versus a baseline bag
- **THEN** their shot dispersion SHALL be no worse and their reach no shorter, so equipment influence is felt

#### Scenario: All four characteristics are felt

- **WHEN** a golfer plays with a bag stronger in workability or feel versus a baseline bag
- **THEN** workability SHALL improve their control in wind and feel SHALL improve their distance control, so no characteristic is inert

#### Scenario: Equipment stays within its responsibilities

- **WHEN** equipment influences play
- **THEN** the influence SHALL be applied by the shot engine reading the bag's characteristics, and the Equipment domain SHALL not compute shots or modify attributes, scores, rankings, or finances

