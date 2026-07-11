## ADDED Requirements

### Requirement: Shots Consume Support Characteristics

A shot SHALL additionally consume the golfer's active staff-support characteristics, carried on the golfer's temporary state: strategic support SHALL reduce the likelihood of a mishit, and mental support SHALL reduce the effect of fatigue on the shot. These characteristics SHALL be neutral by default, so that a golfer with no support reproduces prior shot behaviour exactly, and they SHALL never persist into permanent attributes.

#### Scenario: No support is neutral

- **WHEN** a shot is resolved with neutral (zero) staff-support characteristics
- **THEN** the outcome SHALL match the outcome with no staff-support influence

#### Scenario: Strategic support reduces mishits

- **WHEN** shots are resolved with above-zero strategic support
- **THEN** the mishit likelihood SHALL be no greater than with no strategic support

#### Scenario: Mental support softens fatigue

- **WHEN** a fatigued golfer's shot is resolved with above-zero mental support
- **THEN** its dispersion SHALL be no larger than the same fatigued shot with no mental support
