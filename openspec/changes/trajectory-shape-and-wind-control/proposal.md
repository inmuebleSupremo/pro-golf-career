# Trajectory Shape and Wind Control

## Why

The current resolver owns canonical origin, intent, contact, settlement, and visible trace endpoints, but it does not compute airborne travel. Wind is reduced to an unsigned crosswind uncertainty and a per-hole headwind value before an actual shot direction is known. A fade or draw would therefore be cosmetic unless the engine becomes the single authority for both a curved airborne path and its actual first contact.

This change introduces the smallest deterministic flight representation that makes shot shape and directional wind real while preserving `AimPoint` as the intended first-contact target, existing settlement authority, human/AI parity, and summary-only background simulation. It deliberately establishes—rather than implements—the future `flight-terrain-response` intersection seam.

## What Changes

- Add persistent `Handedness` (`RIGHT`/`LEFT`) to player identity, limited to interpreting fade/draw direction; golfer creation accepts nullable handedness/defaults it to `RIGHT`, profile reads return it, and existing/generated players remain compatible.
- Add `ShotShape` (`STRAIGHT`, `FADE`, `DRAW`) to non-putting `BallStrikeIntent`; human and AI submit the same intent.
- Replace resolver-facing unsigned crosswind with a signed canonical local wind vector. The resolver decomposes it against the actual origin-to-`AimPoint` axis.
- Add a pure deterministic parametric `FlightSolution`. It is the source of both canonical first contact and observable sampled airborne points; samples are a playback projection, never future collision authority.
- Add resolver-derived height and a small authoritative sampled `airbornePath` to trace-materialized `ShotTrace` values.
- Separate ordinary execution dispersion, deterministic signed wind displacement, and wind-related uncertainty in the execution model and calibration surface. Calm `STRAIGHT` shots preserve established behavior.
- Permit all three shapes for eligible FULL and CONTROLLED shots; PITCH, CHIP, and BUNKER materialize a straight airborne path only; putts remain non-airborne.
- Add minimal shape controls/guidance and trace-driven playback. The frontend never derives curve, wind, apex, contact, or path samples.
- Correct the observable handedness mapping: for a right-handed golfer DRAW begins golfer-right and returns left to
  `AimPoint`; FADE begins golfer-left and returns right. The mapping inverts for left-handed golfers.
- Restore canvas `AimPoint` interaction after a non-holing result finishes authoritative playback, without changing
  the settled backend ball state or losing textual feedback.
- Sequence existing authoritative roll playback after first contact, and expose a minimal read-only effective-wind
  vector/arrow in canonical hole coordinates; neither addition changes resolver authority or rollout physics. Positive
  rollout must be perceptible as an explicit `Rolling` result phase even when its authoritative endpoint separation is
  visually small, and wind flow must use an unambiguous symmetric arrowhead and visible `Wind toward`/unit label.
- Record the independently verified effective-cup-placement defect as an integration-blocking dependency. Its course
  generation, putting, save-compatibility, and calibration correction belongs to a separate OpenSpec change and is not
  implemented by this trajectory feature.

## Non-Goals

- Flight-terrain, tree, water, or out-of-bounds intersection before first contact.
- Forced carries, bounce, spin, slope, aerodynamic spin response, speed, multi-surface ground traversal, or richer rollout.
- Player trajectory-height controls, cut/hook variants, spatial putting, green contours, geographic course orientation,
  or visual redesign.
- A new player skill, equipment progression system, or AI-only shot-shape advantage.
- Course generation, active-pin placement, putting eligibility/mechanics, existing saves, or any correction for the
  separate invalid effective-cup-placement defect.

## Capabilities

### Modified Capabilities

- `player-entity`: player identity gains narrowly scoped handedness.
- `save-persistence`: handedness restores safely from new and pre-existing snapshots.
- `playing-conditions`: wind is represented for resolution as signed canonical local flow rather than an unsigned crosswind scalar.
- `shot-resolution`: intent shape, parametric flight, first-contact authority, calibrated wind channels, and trace airborne samples.
- `playable-round`: human/AI shape parity, eligibility/guidance, and observable-vs-summary flight materialization.
- `graphql-api`: additive shape input/guidance, airborne trace projection, and read-only effective-wind projection.
- `web-play`: minimal club → technique → shape → landing-target control flow plus informational wind readout, with no client flight mechanics.
- `web-hole-visualization`: render backend-authored airborne samples, sequence authoritative roll/recovery distinctions,
  and restore target interaction after result playback.

## Impact

- **Simulation:** pure and deterministic; `FlightSolution` derives contact without an extra random stream or framework dependency.
- **Scoring:** calm straight behavior is a regression baseline; wind behavior is deliberately recalibrated and guarded by scoring/distribution tests.
- **Persistence:** only player handedness becomes durable state. Shot traces and flight samples remain immediate observable results and are not replay/save history; missing historical handedness restores as `RIGHT`.
- **API/UI:** additive schema fields and generated operation types; the browser plays back backend samples only.
- **Future work:** `flight-terrain-response` will evaluate the parametric flight function, not infer collision from sampled UI points.
- **Integration dependency:** do not integrate this feature until the separate effective-cup-placement change proves that
  every active cup is safely GREEN under its effective event setup and preserves practical hole completion.
