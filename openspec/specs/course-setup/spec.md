# course-setup Specification

## Purpose
An event's course setup difficulty (pin aggression, wind, effective width) is derived from its tour tier and event prestige, to normalize per-tier field scoring toward even par and scale marquee-event difficulty.
## Requirements
### Requirement: Event Course Setup

Every competitive event SHALL have a course setup that scales the difficulty of the course as it is played, expressed as at least three factors: **pin aggression** (how tucked laterally and how deep the flags are cut), **wind scale** (the severity of the event's wind/exposure), and **width scale** (the effective playing width of greens and fairways). A neutral setup (all factors at their identity value) SHALL reproduce the course's baseline difficulty and canonical geometry exactly. A more difficult setup SHALL tuck pins more aggressively, play windier, and narrow the effective greens and fairways; an easier setup SHALL do the reverse.

For any active round or playoff, the setup-specific canonical geometry used to classify shot contact and settlement SHALL be the effective geometry of that hole. Any presentation or API projection of that playable hole SHALL use the same effective geometry, not its unscaled base geometry.

#### Scenario: A harder setup raises scoring

- **WHEN** the same field plays the same course under a harder setup versus a neutral one
- **THEN** the field's scoring average SHALL be higher (more over par) under the harder setup

#### Scenario: Neutral setup is baseline

- **WHEN** a hole is resolved under a neutral course setup
- **THEN** the outcome and its canonical geometry SHALL match the course's baseline behaviour

#### Scenario: Setup geometry is shared by resolution and presentation

- **WHEN** a player event uses a non-neutral width setup
- **THEN** a position's surface in the projected hole geometry SHALL equal the surface used by gameplay resolution for that position

### Requirement: Setup Derived From Tier and Prestige

An event's course setup SHALL be derived deterministically from its tour tier and its event prestige. The **tour-tier** component SHALL normalize for field strength — a weaker-field tour SHALL receive an easier setup and a stronger-field tour a harder one — so that each tier's Regular events produce a field-scoring average near even par despite differing field skill. The **prestige** component SHALL add difficulty on top — Signature events harder than Regular, Majors hardest — so that marquee events play tougher regardless of tier.

#### Scenario: Regular events across tiers normalize to even par

- **WHEN** the Regular events of different tour tiers are each resolved with their own field
- **THEN** every tier's Regular field-scoring average SHALL land near even par, rather than the weaker tours scoring far higher than the stronger ones

#### Scenario: Prestige raises setup difficulty monotonically

- **WHEN** events of the same tour tier but different prestige are compared
- **THEN** the higher-prestige event SHALL have a harder course setup and a higher field-scoring average, with Majors the hardest

### Requirement: Setup Applied Consistently Across Resolution Paths

The event's course setup SHALL be applied identically wherever the event is resolved — the automatic field resolver and the interactive playable path — so that a fully-simmed event remains identical to the played one for the same inputs and seed.

#### Scenario: Simmed and played events agree under a setup

- **WHEN** an event with a non-neutral setup is resolved automatically versus played (then simmed) interactively, for the same field, seed, and setup
- **THEN** the results SHALL be identical competitor-for-competitor

### Requirement: Setup-Specific Pin Authority

Course setup SHALL affect pin derivation and surface settlement through one identical effective canonical geometry. For a V5 pin, the width-scaled/otherwise setup-specific GREEN polygon used to validate the cup SHALL be the exact geometry returned by the active hole model for contact classification and presentation. Pin aggression may increase the frequency and degree of valid tucked locations, but SHALL never place a cup outside GREEN or inside the 2.0-yard excluded edge region.

#### Scenario: Hard setup remains valid and challenging

- **WHEN** an Elite Major or another harder supported setup derives V5 pins
- **THEN** every cup SHALL satisfy the effective-GREEN invariants
- **AND THEN** the setup SHALL retain more challenging valid pin placement than a regular/easier setup

#### Scenario: Presentation and settlement share cup authority

- **WHEN** a current playable hole is projected under a non-neutral setup
- **THEN** its displayed cup and effective GREEN geometry SHALL be the same values used by authoritative shot settlement
