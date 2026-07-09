## ADDED Requirements

### Requirement: Shots Consume Equipment Characteristics

A shot SHALL additionally consume the golfer's active equipment characteristics, carried on the golfer's temporary state: forgiveness SHALL reduce dispersion and power SHALL extend reach. These characteristics SHALL be neutral by default, so that standard (baseline) equipment reproduces prior shot behaviour exactly and never persists into permanent attributes.

#### Scenario: Standard equipment is neutral

- **WHEN** a shot is resolved with neutral (baseline) equipment characteristics
- **THEN** the outcome SHALL match the outcome with no equipment influence

#### Scenario: Stronger equipment tightens and extends

- **WHEN** a shot is resolved with above-baseline forgiveness and power
- **THEN** its dispersion SHALL be no larger and its reach no shorter than with neutral equipment
