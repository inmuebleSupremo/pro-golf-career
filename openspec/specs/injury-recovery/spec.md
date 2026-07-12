# injury-recovery Specification

## Purpose
TBD - created by archiving change add-health. Update Purpose after archive.
## Requirements
### Requirement: Injury

Professional Golfers MAY sustain injuries that temporarily reduce physical capability. Injuries SHALL arise believably from physical strain rather than being scripted, and significant injuries SHALL become part of permanent Career health history. While injured, a golfer's physical capability SHALL be reduced; in the early stage of a significant injury the golfer SHALL be unable to compete, but once the injury reaches its final rehabilitation stage (Recovering) a controlled golfer MAY choose to **play through** it — competing at a severity-scaled impairment to shot performance rather than sitting out.

#### Scenario: An injury reduces capability and is recorded when significant

- **WHEN** a golfer sustains an injury
- **THEN** their physical capability SHALL be reduced, and a significant injury SHALL be recorded in health history

#### Scenario: Early-stage injury prevents competition

- **WHEN** a golfer is in the early (non-Recovering) stage of an injury
- **THEN** the golfer SHALL be unable to compete

#### Scenario: A recovering golfer may play through at an impairment

- **WHEN** a golfer's injury has reached its final rehabilitation stage (Recovering) and the golfer chooses to compete
- **THEN** the golfer SHALL be permitted to enter, and their shots SHALL be impaired by an amount scaled to the injury's severity

### Requirement: Gradual Rehabilitation

A golfer recovering from injury SHALL undergo Rehabilitation supporting a gradual return to full competitive participation. Recovery from injury SHALL NOT be represented as an instantaneous event. Rehabilitation SHALL advance only in periods during which the golfer does NOT compete: competing while injured (playing through) SHALL freeze rehabilitation for that period, so grinding through an injury prolongs it while resting heals it.

#### Scenario: Rehabilitation takes time

- **WHEN** a golfer is recovering from an injury and rests
- **THEN** the injury SHALL heal over multiple periods of rehabilitation, not clear in a single step

#### Scenario: Competing freezes rehabilitation

- **WHEN** an injured golfer competes in an event during a rehabilitation period
- **THEN** that period SHALL NOT advance the injury's rehabilitation (only non-competing periods do)

#### Scenario: Return follows full rehabilitation

- **WHEN** an injury's rehabilitation completes
- **THEN** the injury SHALL clear and the golfer SHALL become able to compete again unimpaired

