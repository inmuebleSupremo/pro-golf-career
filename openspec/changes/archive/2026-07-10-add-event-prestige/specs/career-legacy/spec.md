## ADDED Requirements

### Requirement: Majors Won

A Career SHALL track the number of majors it has won, folded from the golfer's own tournament results whenever a win occurs in a major. Majors won SHALL be a permanent, retrievable part of a career's legacy.

#### Scenario: Winning a major increments majors won

- **WHEN** a golfer's finish in a major is a win
- **THEN** their career majors-won count SHALL increase by one

#### Scenario: A non-major win does not count as a major

- **WHEN** a golfer wins a regular or signature event
- **THEN** their career majors-won count SHALL NOT change

## MODIFIED Requirements

### Requirement: Hall-of-Fame Eligibility Evaluation

On retirement a Career SHALL automatically evaluate Hall-of-Fame eligibility and permanently record the result. The evaluation SHALL be derived from the career's recorded achievements — **including majors won, which SHALL provide a path to eligibility** — and SHALL NOT alter any historical record. The detailed eligibility criteria are a placeholder that a later Legacy Systems specification refines.

#### Scenario: Evaluation runs automatically on retirement

- **WHEN** a Career becomes RETIRED
- **THEN** a Hall-of-Fame eligibility result SHALL be evaluated and permanently recorded

#### Scenario: Majors provide a path to eligibility

- **WHEN** a career has won at least the majors threshold
- **THEN** it SHALL be evaluated as Hall-of-Fame eligible

#### Scenario: Evaluation is derived and non-destructive

- **WHEN** Hall-of-Fame eligibility is evaluated
- **THEN** the result SHALL be a function of the career's recorded achievements, and no historical record SHALL be modified

#### Scenario: Result is available after retirement

- **WHEN** a retired Career is inspected
- **THEN** its recorded Hall-of-Fame eligibility result SHALL be retrievable
