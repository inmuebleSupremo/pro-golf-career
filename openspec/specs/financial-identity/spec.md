# financial-identity Specification

## Purpose
TBD - created by archiving change add-economy. Update Purpose after archive.
## Requirements
### Requirement: Financial Identity

Every Professional Golfer SHALL possess a financial identity that persists throughout the Career, exposing available funds, career earnings, sponsorship income, tournament earnings, and career expenses.

#### Scenario: A golfer has a persistent financial identity

- **WHEN** a Professional Golfer exists in the world
- **THEN** they SHALL have a financial identity exposing available funds, career earnings, sponsorship income, tournament earnings, and career expenses that persists for the life of the Career

### Requirement: Tournament Earnings

Prize money SHALL be awarded automatically according to Tournament results, credited to the golfer's available funds and tournament earnings, and preserved as part of permanent Career financial history.

#### Scenario: Prize money is awarded from a result

- **WHEN** a golfer finishes a Tournament in a paying position
- **THEN** their prize money SHALL be credited automatically to available funds and tournament earnings, and recorded

#### Scenario: Non-paying finishes credit nothing

- **WHEN** a golfer finishes outside the paying positions
- **THEN** no prize SHALL be credited, and their earnings SHALL be unchanged

### Requirement: Career Expenses

Professional Golfers SHALL incur Career-related expenses (for example tournament entry and travel), debited from their available funds and accumulated into career expenses.

#### Scenario: Competing incurs expenses

- **WHEN** a golfer competes in a Tournament
- **THEN** the associated career expenses SHALL be debited from available funds and added to career expenses

### Requirement: Economic Integrity

The Economy SHALL remain internally consistent: every financial transaction SHALL produce an auditable record; a discretionary spend SHALL be declined unless sufficient funds are available; and negative balances arising from mandatory obligations SHALL be handled by a defined rule rather than being silently discarded.

#### Scenario: Every transaction is recorded

- **WHEN** any credit or debit is applied to an account
- **THEN** it SHALL append an auditable transaction record (type, amount, date, resulting balance)

#### Scenario: Discretionary spending requires available funds

- **WHEN** a discretionary spend exceeds available funds
- **THEN** it SHALL be declined and the balance SHALL be unchanged

#### Scenario: Mandatory obligations follow the negative-balance rule

- **WHEN** a mandatory expense exceeds available funds
- **THEN** the expense SHALL still be recorded and the balance SHALL follow the defined negative-balance rule

### Requirement: Financial History

The simulation SHALL permanently preserve financial history — including career and seasonal earnings, sponsorship history, and major financial milestones — contributing to Career legacy.

#### Scenario: Financial history is preserved

- **WHEN** financial activity occurs over a Career
- **THEN** the account SHALL preserve the transaction history and major financial milestones for the life of the Career

