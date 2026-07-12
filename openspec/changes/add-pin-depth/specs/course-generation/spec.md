## MODIFIED Requirements

### Requirement: Per-Round Pin Positions

Every Hole SHALL have exactly one active pin position, which SHALL remain fixed for the duration of a completed round. Pin positions MAY differ between rounds of the same tournament, derived deterministically per round. The pin's **depth** (front-to-back placement) SHALL position the green complex off-centre from the pin so that front and back pins play differently: a back pin SHALL leave less green behind it (the over-green trouble closer, so going long is punished), and a front pin SHALL leave less green in front of it (the run-up shorter, so coming up short is punished). A centre pin SHALL leave the green symmetric about it.

#### Scenario: Pin is fixed within a round

- **WHEN** a round is in progress on a hole
- **THEN** that hole SHALL expose exactly one active pin position that does not change until the round completes

#### Scenario: Pins may vary across rounds deterministically

- **WHEN** pin positions are derived for two different rounds of the same tournament
- **THEN** they MAY differ, and each SHALL be reproducible from its round's seed

#### Scenario: A back pin brings the over-green trouble closer

- **WHEN** the green complex is emitted for a back pin versus a front pin
- **THEN** the back pin SHALL have less green behind it (over-green trouble nearer), and the front pin SHALL have less green in front of it (a shorter safe run-up)

#### Scenario: A centre pin is symmetric

- **WHEN** the green complex is emitted for a centre pin
- **THEN** the green SHALL extend equally in front of and behind the pin
