# Surface-Aware Shot Families and Short Game

## Why

Individual clubs and canonical `AimPoint` now make the player's intended landing point explicit, but most non-putting shots still use one carry-and-dispersion model regardless of technique or the surface the ball is played from. A chip, pitch, controlled swing, and bunker shot can therefore be labelled differently without becoming different gameplay decisions.

## What Changes

- Add the first intentional `ShotFamily` set: `FULL`, `CONTROLLED`, `PITCH`, `CHIP`, and `BUNKER`.
- Extend `BallStrikeIntent` so human and AI submit club, intended first-contact `AimPoint`, and family through the same execution seam.
- Make the existing putting route explicit through a minimal `PuttIntent` / `playPutt` path that preserves present make-or-leave behavior without spatial putting controls.
- Add a pure, authoritative lie/club/family eligibility and execution-profile layer. It derives bounded reach, dispersion, mishit, and ground-response behavior from existing inputs; it exposes no execution knobs to the player.
- Add a deliberately narrow landing-and-roll response for pitch and chip. It occurs only after playable canonical contact, is bounded and deterministic, and never claims spin, bounce, slope, speed, or arbitrary terrain traversal.
- Preserve the distinction between contact surface and final settled ball lie. Settlement remains the sole legal-ball authority; water and out-of-bounds recovery remain non-flight rules transitions.
- Extend `ShotTrace` with an explicit optional roll phase, separate from recovery/replay transition, and project the new truthful facts through GraphQL and canonical playback.
- Replace the automatic "fringe within ten yards is a putt" rule with deliberate fringe choice while preserving the existing green putting model.
- Provide family guidance, validation, AI parity, calibration, compatibility, and statistical regression coverage.

## Non-Goals

This change does not add bump-and-run, flop, punch, stinger, shot shape, directional wind, apex, full trajectory simulation, true spin, bounce physics, slope, firmness simulation, arbitrary ground traversal, forced carries, tree collision, spatial putting, green contours, equipment progression redesign, save-format redesign, or a play-screen visual redesign.

## Capabilities

### Modified Capabilities

- `shot-resolution`: intentional family selection, lie-aware execution, bounded post-contact roll, and explicit contact-versus-final semantics.
- `playable-round`: human and AI family intent parity, authoritative eligibility, putting/fringe routing, and summary-safe bulk simulation.
- `graphql-api`: additive family input, family guidance, validation, and roll/settlement result projection.
- `web-play`: club → technique → landing-target controls and result feedback with no client-derived ground response.
- `web-hole-visualization`: canonical rendering of authoritative roll phases, distinct from recovery/replay.

## Impact

- **Simulation:** remains deterministic and pure; family and lie profiles derive execution using existing club and player attributes.
- **Scoring/statistics:** rollout can change final lie and proximity, so GIR, fairways, scrambling, bunker outcomes, and calibration must use documented final-settlement semantics.
- **API:** `shotFamily` is additive and can default to `FULL` at the GraphQL boundary for compatibility; internal ball-strike intent is non-null. `playPutt` remains a dedicated operation.
- **Persistence:** pending shot submission is transient. Existing saves require compatibility testing, not a speculative migration.
- **Future sequence:** richer bump-and-run, flop, ground traversal, trajectory/shape/wind, and spatial putting remain separate decisions after evidence from this vertical slice.
