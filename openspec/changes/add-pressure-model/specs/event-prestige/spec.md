## MODIFIED Requirements

### Requirement: Event Prestige Levels

Every tournament SHALL carry an event prestige — one of Regular, Signature, or Major — distinct from and orthogonal to its tour tier. Prestige SHALL weight the event's rewards (ranking points and prize money) and its career significance, with Major weighted above Signature above Regular for every reward it affects. Prestige SHALL additionally scale the **situational closing-round pressure** of the event — a Major's closing rounds carry the most pressure — so higher-prestige events are harder to close. Prestige SHALL NOT enter the base shot mechanics directly; it affects play only through that situational pressure.

#### Scenario: Prestige is independent of tour tier

- **WHEN** an event is defined
- **THEN** it SHALL have both a tour tier and an event prestige, and either may vary without the other

#### Scenario: Higher prestige means greater rewards, monotonically

- **WHEN** two events of the same tour tier and field strength are compared
- **THEN** the higher-prestige event SHALL award more ranking points and a larger purse for the same finishing position

#### Scenario: Higher prestige raises closing-round pressure

- **WHEN** the same competitive situation is evaluated at different prestige levels
- **THEN** the higher-prestige event SHALL impose more closing-round pressure on the golfers in contention, so its closing play is harder — while the opening rounds and the base shot mechanics remain prestige-independent
