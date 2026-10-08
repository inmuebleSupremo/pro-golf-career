## ADDED Requirements

### Requirement: Resolver wind is signed canonical local flow
The weather-to-shot translation SHALL provide a finite signed wind-flow vector in the active hole's canonical local coordinates, along with existing lie-quality effects. Weather generation MAY retain speed, bearing, exposure, and its deterministic per-hole orientation mapping, but the resolver SHALL preserve wind side rather than reduce crosswind to an unsigned scalar before a shot direction is known.

#### Scenario: Equal opposite crosswinds remain distinguishable
- **WHEN** two otherwise identical shot contexts receive equal-magnitude wind flow on opposite sides of the actual shot axis
- **THEN** their resolver inputs SHALL retain opposite signed cross-axis components.

### Requirement: Wind decomposes against actual aim
For a non-putting canonical shot, the resolver SHALL decompose signed local wind flow against that shot's actual origin-to-`AimPoint` frame. It SHALL not treat a per-hole precomputed head/cross decomposition as the final directional-wind answer for an off-axis aim.

#### Scenario: Re-aiming changes wind components
- **WHEN** one fixed local wind vector is resolved for two different valid aim points from the same origin
- **THEN** the resulting along-axis and cross-axis wind components MAY differ according to the two actual aim frames.

### Requirement: Effective canonical wind remains available for observation
The active playable hole SHALL retain its current effective resolver wind flow as a finite read-only vector in
canonical hole coordinates. The vector SHALL describe the direction air moves toward and its magnitude in the
resolver's effective wind units after the existing weather, exposure, and synthetic per-hole orientation conversion.
It SHALL not claim geographic north or replace the documented synthetic orientation model.

#### Scenario: Wind observation preserves signed flow
- **WHEN** active-hole effective wind has a nonzero signed canonical flow
- **THEN** a read projection SHALL retain both signed components and magnitude without converting it to an unsigned
  crosswind or a geographic compass bearing.
