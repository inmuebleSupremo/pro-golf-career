# availability Specification

## Purpose
TBD - created by archiving change add-health. Update Purpose after archive.
## Requirements
### Requirement: Availability

Every Professional Golfer SHALL possess a current Availability status, derived from their Physical State, that determines whether they may enter competitive events. Availability SHALL distinguish at least Available, Resting, Recovering, and Injured.

#### Scenario: Availability is derived from physical state

- **WHEN** a golfer's Physical State indicates an active injury or excessive fatigue
- **THEN** their Availability SHALL reflect it (Injured, Recovering, or Resting) rather than Available

#### Scenario: Availability gates event entry

- **WHEN** a field is formed for an event
- **THEN** only golfers whose Availability permits competition SHALL be entered

### Requirement: Workload Management

The simulation SHALL create a workload-management tension: frequent competition increases physical demands, and appropriate recovery supports sustained performance, so that competing without rest can render a golfer temporarily unable to compete.

#### Scenario: Overwork forces recovery

- **WHEN** a golfer competes frequently without recovery until fatigue is excessive
- **THEN** their Availability SHALL become non-competing until they have recovered

### Requirement: Physical State Dependency Boundary

Other domains MAY consume Physical State information (for example tournament eligibility, scheduling, or the world simulation) but SHALL NOT become its authoritative source. The Health, Fitness &amp; Recovery domain SHALL be responsible for Physical State, Fitness, Fatigue, Recovery, Injury, Rehabilitation, and Availability, and SHALL NOT be responsible for tournament scoring, rankings, financial management, player progression, or shot resolution.

#### Scenario: Dependents reference but do not own physical state

- **WHEN** another domain uses Physical State
- **THEN** it SHALL read the Health domain's state without modifying scoring, rankings, finances, progression, or shot resolution as part of the Health domain

