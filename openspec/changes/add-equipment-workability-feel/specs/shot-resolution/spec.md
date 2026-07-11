## MODIFIED Requirements

### Requirement: Shots Consume Equipment Characteristics

A shot SHALL additionally consume the golfer's active equipment characteristics, carried on the golfer's temporary state: forgiveness SHALL reduce dispersion, power SHALL extend reach, **workability SHALL improve ball-flight control in wind (raising effective wind resistance), and feel SHALL improve distance control (tightening distance dispersion)**. These characteristics SHALL be neutral by default, so that standard (baseline) equipment reproduces prior shot behaviour exactly and never persists into permanent attributes.

#### Scenario: Standard equipment is neutral

- **WHEN** a shot is resolved with neutral (baseline) equipment characteristics
- **THEN** the outcome SHALL match the outcome with no equipment influence

#### Scenario: Stronger equipment tightens and extends

- **WHEN** a shot is resolved with above-baseline forgiveness and power
- **THEN** its dispersion SHALL be no larger and its reach no shorter than with neutral equipment

#### Scenario: Workability helps in wind and feel controls distance

- **WHEN** a shot is resolved in wind with above-baseline workability, or with above-baseline feel
- **THEN** the wind's effect SHALL be no greater with workability, and the distance dispersion SHALL be no larger with feel, than with neutral equipment
