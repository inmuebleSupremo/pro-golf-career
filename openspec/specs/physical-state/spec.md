# physical-state Specification

## Purpose
TBD - created by archiving change add-health. Update Purpose after archive.
## Requirements
### Requirement: Physical State

Every Professional Golfer SHALL possess a Physical State representing current physical readiness for competition, including at least Fitness and Fatigue. Physical State SHALL be tracked continuously, persist throughout the Career, and be available to dependent systems.

#### Scenario: A golfer has a persistent physical state

- **WHEN** a Professional Golfer exists in the world
- **THEN** they SHALL have a Physical State exposing fitness and fatigue that persists for the life of the Career and is readable by dependent systems

### Requirement: Fitness

Professional Golfers SHALL possess a measurable level of Fitness representing long-term physical preparedness. Fitness MAY influence recovery, competitive readiness, and long-term durability.

#### Scenario: Fitness influences durability

- **WHEN** two golfers of differing fitness undergo the same physical demands
- **THEN** the fitter golfer SHALL, broadly, wear down less and recover faster

### Requirement: Fatigue Accumulation

Professional Golfers MAY accumulate Fatigue over a Career from tournament participation, travel, and intensive scheduling. Fatigue SHALL recover naturally over time through appropriate recovery opportunities.

#### Scenario: Competing accumulates fatigue

- **WHEN** a golfer competes in an event
- **THEN** their fatigue SHALL increase

#### Scenario: Fatigue recovers over time

- **WHEN** a golfer rests over subsequent periods without competing
- **THEN** their fatigue SHALL decrease gradually toward rested

### Requirement: Gradual Recovery

Recovery SHALL restore aspects of the golfer's Physical State over time and SHALL remain gradual and believable rather than instantaneous.

#### Scenario: Recovery is gradual

- **WHEN** a fatigued golfer recovers over time
- **THEN** the restoration SHALL accrue gradually across periods rather than resetting in a single step

### Requirement: Equal Simulation

Human-controlled and simulation-controlled Professional Golfers SHALL use the same health, fitness, and recovery systems, with no hidden protection or penalty based on control type.

#### Scenario: Control type does not change the physical model

- **WHEN** physical state is updated for any golfer
- **THEN** the same rules SHALL apply regardless of control type

### Requirement: Long-Term Resilience Varies

Professional Golfers SHALL experience varying levels of physical resilience over a Career, emerging from the interaction of physical factors (such as fitness and age) rather than scripted events.

#### Scenario: Resilience differs across golfers and ages

- **WHEN** golfers of differing fitness and age accumulate the same demands over time
- **THEN** their physical resilience SHALL differ, emerging from those factors rather than being scripted

