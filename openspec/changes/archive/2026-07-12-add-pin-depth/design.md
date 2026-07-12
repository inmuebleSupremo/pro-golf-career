## Context

`HoleZones.profileFor` built the green complex symmetrically around the pin distance (`greenStart = R − greenHalfDepth − fringe`, `greenEnd = R + greenHalfDepth + fringe`), so the pin always sat at the green's depth-centre and the over-green trouble was equally far for every pin. The per-round pin already carries a depth offset (`PinPosition.depthOffset`) — used to set the hole's playing length — but it never shaped the green.

## Goals / Non-goals

- **Goals**: make front/back pins play differently — a back pin punishes going long, a front pin punishes coming up short — deterministically, keeping the green's size and a symmetric centre pin.
- **Non-goals**: a safe-aim *decision* in depth (fire-at-pin still), green contouring/slope.

## Decisions

### D1 — Shift the green off-centre from the pin by a fraction of the pin depth
The green keeps its total depth but splits it asymmetrically: `frontExtent = greenHalfDepth + shift`, `backExtent = greenHalfDepth − shift`, where `shift = pinDepthOffset × PIN_DEPTH_ASYMMETRY`, clamped so each side keeps `PIN_DEPTH_MIN_SIDE` (the pin is never off its own green). A back pin (positive offset) gets more green in front and less behind, so `greenEnd` — and the over-green trouble beyond it — sits closer; a front pin the reverse. Reusing the existing depth offset means a back pin is consistently both longer (its established effect on playing length) and riskier long (its new effect on the green) — two facets of the same pin placement, not double-counting. `PIN_DEPTH_ASYMMETRY = 0.6` keeps the effect meaningful but moderate (GIR dips only ~2 points on extreme pins).

### D2 — Geometry only; the safe-aim decision is deferred
Making the green asymmetric already makes pin depth matter: the shot's natural distance dispersion now meets a pin-dependent penalty (a wide-dispersion player is punished more by a demanding pin). The depth analog of the lateral pin-attack aim — a conservative golfer deliberately aiming short of a back pin — would need the pin depth threaded into the decision policy (a sixth `decide` input) and a target-distance adjustment; it is deferred to avoid bloating the decision signature, and noted. So the policy still fires at the pin; pin depth shapes the *risk*, not yet the *choice*.

## Risks / Trade-offs

- **Modest scoring drift** (extreme pins are a touch harder): the calibration guard holds (field mean ~−0.4 in calm). World field-wide scoring has drifted up cumulatively across the situational-strategy changes (now ~+4.9/round across all four tiers plus weather) — realistic (harder, more strategic golf), but a compensating base-dispersion recalibration is worth considering if it later reads too hard.
- **Passive, not a decision** (D2): pin depth affects difficulty and interacts with dispersion, but is not yet a strategic choice; the safe-aim decision is the deferred completion.
- **Reuses the hole-length depth offset** for the green shift (D1) — intentional and consistent, but couples the two effects of pin depth to one generated value.
