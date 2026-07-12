## ADDED Requirements

### Requirement: Injury Impairment

Shot resolution SHALL accept an injury-impairment condition input in [0,1] (0 = uninjured, neutral) alongside the existing condition inputs (fatigue, pressure). A positive impairment SHALL degrade the outcome — widening dispersion, shortening carry, and lowering putt make-rate — with the degradation growing as the impairment rises. Because the impairment is physical, staff mental support SHALL NOT relieve it (unlike fatigue and pressure). An impairment of zero SHALL reproduce prior shot behaviour exactly, so uninjured play is unchanged.

#### Scenario: Impairment degrades the shot

- **WHEN** the same shot is resolved at a higher injury impairment versus zero, all else equal
- **THEN** the higher-impairment outcome SHALL be worse on average — wider dispersion and reduced expected proximity/putting

#### Scenario: Zero impairment is neutral

- **WHEN** a shot is resolved at zero injury impairment
- **THEN** its outcome SHALL match the outcome the model produced before the impairment input existed

#### Scenario: Mental support does not relieve impairment

- **WHEN** two otherwise-identical shots are resolved at the same positive impairment, one with staff mental support and one without
- **THEN** the injury impairment's effect SHALL be identical in both (mental support relieves fatigue and pressure, not physical injury)
