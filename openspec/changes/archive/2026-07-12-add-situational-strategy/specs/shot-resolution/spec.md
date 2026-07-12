## ADDED Requirements

### Requirement: Distance Measured to the Pin

The distance remaining after a shot SHALL be measured to the actual pin — including the pin's lateral offset from the green centre — not to the green centre. A tucked pin therefore plays harder (a centre-aimed shot leaves a longer approach to it), and finishing near the pin, not merely on the green, is what leaves a short putt. A centre pin (zero lateral offset) SHALL reproduce prior behaviour exactly.

#### Scenario: A tucked pin is farther from a centre-aimed shot

- **WHEN** the same centre-aimed shots are resolved to a tucked pin versus a centre pin
- **THEN** the distance remaining SHALL be greater to the tucked pin

#### Scenario: A centre pin is neutral

- **WHEN** a shot is resolved to a pin with zero lateral offset
- **THEN** the distance remaining SHALL match the pre-pin behaviour (measured to the green centre)

### Requirement: Situational Shot Decisions

The simulation decision policy SHALL choose each shot from the situation — the remaining distance, the ball's lie, and the pin placement — not from a fixed per-golfer strategy alone. From a difficult lie (deep rough, bunker, recovery, trees) the golfer SHALL play conservatively to recover, regardless of disposition; from a clean lie it SHALL keep its disposition. On a scoring approach the golfer SHALL aim a fraction of the way toward the pin determined by its disposition (an aggressive golfer attacks a tucked flag, a conservative one plays the green centre) and by shot confidence (the fraction fades to zero as the shot lengthens, so a long approach plays the centre). The policy SHALL remain pure and deterministic.

#### Scenario: A difficult lie forces conservative recovery

- **WHEN** the policy decides a shot from a bunker or deep rough
- **THEN** it SHALL play conservatively regardless of the golfer's disposition

#### Scenario: Aggression attacks the pin, conservatism plays the centre

- **WHEN** an aggressive and a conservative golfer decide the same confident approach to a tucked pin
- **THEN** the aggressive golfer SHALL aim toward the pin and the conservative golfer SHALL aim at the green centre

#### Scenario: Pin attack fades with distance

- **WHEN** the same aggressive golfer decides a short approach versus a long approach to a tucked pin
- **THEN** it SHALL aim more toward the pin on the short approach, and play the centre on a long approach

#### Scenario: Attacking a pin is a real risk/reward

- **WHEN** approaches aimed at a tucked pin are compared with approaches aimed at the green centre
- **THEN** the pin-aimed approaches SHALL finish closer to the hole on average (more birdie-range looks) but hold the green less often (the flanking hazard)
