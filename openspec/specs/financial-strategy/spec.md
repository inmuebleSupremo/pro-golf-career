# financial-strategy Specification

## Purpose
TBD - created by archiving change add-economy. Update Purpose after archive.
## Requirements
### Requirement: Meaningful Financial Decisions

Professional Golfers SHALL make meaningful financial decisions throughout their Careers (for example accepting sponsorship offers and managing expenses), involving strategic trade-offs rather than automatic optimisation. Simulation-controlled golfers SHALL make these decisions through a deterministic policy, by the same rules used for any golfer.

#### Scenario: Decisions involve trade-offs, not auto-optimisation

- **WHEN** a golfer is presented with more offers than they may hold
- **THEN** the decision SHALL select among them under a defined limit and trade-off rather than accepting every available offer

#### Scenario: AI decides deterministically

- **WHEN** a simulation-controlled golfer faces the same offers, account, and agreements
- **THEN** a deterministic policy SHALL always make the same decision

### Requirement: Financial Progression Reflects Success

Financial opportunity SHALL broadly reflect competitive success — higher competitive achievement SHALL generally provide access to greater earning potential — and financial progression SHALL emerge naturally from Career development rather than being granted arbitrarily. **In particular, event purses SHALL scale with the tour tier (and event prestige), so higher tiers award larger purses and climbing the ladder unlocks greater earnings.**

#### Scenario: Greater success unlocks greater earning potential

- **WHEN** a higher-achieving golfer is compared with a lower-achieving one over time
- **THEN** the higher achiever SHALL, broadly, have access to greater earning potential

#### Scenario: Higher tiers pay larger purses

- **WHEN** events on different tour tiers are compared
- **THEN** a higher-tier event SHALL award a larger purse than a lower-tier event of the same prestige

### Requirement: Career Sustainability

The Economy SHALL support long-term Career sustainability, so that a golfer must balance competitive ambition with financial stability and financial management becomes increasingly relevant over a longer Career. **Competing SHALL NOT guarantee income: prize money SHALL be paid to the golfers who make the cut, so missing the cut leaves a golfer out of the money while still incurring the costs of competing.**

#### Scenario: Costs and income must be balanced over time

- **WHEN** a Career runs across many seasons
- **THEN** ongoing expenses SHALL be weighed against earnings, so financial stability is not guaranteed independent of results

#### Scenario: Missing the cut earns nothing

- **WHEN** a golfer misses the cut of an event
- **THEN** they SHALL earn no prize money from it while still bearing the entry and travel costs of competing

#### Scenario: Making the cut is the pay line

- **WHEN** prize money is awarded for an event
- **THEN** the golfers who made the cut SHALL be the paid positions, and those who missed it SHALL earn nothing

### Requirement: Economic Independence

The Economy SHALL NOT directly modify Player Attributes, Tournament outcomes, Rankings, or Shot resolution. Financial success SHALL provide opportunities but SHALL NOT purchase competitive success directly. The Economy domain SHALL be responsible for finances, prize money, sponsorship agreements, financial history, and commercial opportunities, and SHALL NOT be responsible for staff management, equipment systems, tournament scoring, player progression, or rankings.

#### Scenario: Finances buy opportunity, not competitive outcomes

- **WHEN** the Economy processes earnings, expenses, and sponsorships
- **THEN** it SHALL not modify any Player Attribute, Tournament outcome, Ranking, or shot resolution

#### Scenario: The domain stays within its responsibilities

- **WHEN** the Economy operates
- **THEN** it SHALL manage only finances, prize money, sponsorships, financial history, and commercial opportunities, delegating staff, equipment, scoring, progression, and rankings to their domains

