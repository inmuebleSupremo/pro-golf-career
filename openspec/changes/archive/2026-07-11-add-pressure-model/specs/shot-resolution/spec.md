## ADDED Requirements

### Requirement: Shots Consume Situational Pressure

A shot SHALL additionally consume a situational **pressure** value in [0,1] carried on the golfer's temporary state. Pressure SHALL worsen the shot — widening dispersion and lowering putt make probability — and its effect SHALL be resisted by the golfer's **Composure** attribute, so a more composed golfer is less affected by the same pressure. Mental support (a sports psychologist) SHALL relieve pressure as well as fatigue. Pressure SHALL be neutral at zero: a shot at zero pressure SHALL be resolved identically regardless of Composure, so calm and opening-round play is unchanged and reproduces prior behaviour exactly.

#### Scenario: Pressure worsens a shot, resisted by composure

- **WHEN** the same golfer's shots are resolved at high pressure versus zero pressure
- **THEN** the high-pressure outcomes SHALL be worse on average, and a golfer with higher Composure SHALL be less affected than one with lower Composure

#### Scenario: A psychologist relieves pressure

- **WHEN** a golfer under pressure is resolved with above-zero mental support
- **THEN** the pressure's effect on the shot SHALL be no greater than the same shot with no mental support

#### Scenario: Zero pressure is neutral

- **WHEN** a shot is resolved at zero pressure
- **THEN** its outcome SHALL NOT depend on the Composure attribute, and SHALL match the outcome the model produced before pressure was activated
