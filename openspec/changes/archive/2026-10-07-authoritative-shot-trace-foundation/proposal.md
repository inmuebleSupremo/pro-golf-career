# Authoritative Shot Trace Foundation

## Why

Canonical ball state, terrain, `BallStrikeIntent`, sampled contact, and settlement now exist, but a visible shot has no single backend-authored spatial account. The canonical renderer can show settlement markers, while retained fallback playback still fabricates loft, curve, bounce, and roll from client-side profiles. That makes future animation capable of disagreeing with game truth.

## What Changes

- Add an immutable, optional `ShotTrace` for observable shots: factual club identity, origin, intended aim point, actual contact, any non-flight settlement transition, and final legal point.
- Preserve `ShotSettlement` as the authority for legal recovery and `ShotOutcome` as the scoring summary; attach a trace rather than spreading spatial/physics fields across the outcome.
- Materialize trace only for human, visible AI, and other observable shots. Background, calibration, and bulk simulations remain summary-only while sharing the exact resolver and settlement path.
- Project the trace through GraphQL and make canonical SVG playback/feedback consume it, including target-marker lifecycle and reduced-motion behavior.
- Retire client-side synthetic flight authority once canonical trace playback has objective coverage gates.

## Non-Goals

This change does not add ballistic flight, apex, curve, spin, bounce, rollout, terrain-flight intersection, time-of-flight, directional wind, forced carries, tree collision, shot shape, trajectory controls, short-game overhaul, spatial putting, replay/history persistence, or a play-UI redesign.

## Capabilities

### Modified Capabilities

- `shot-resolution`: resolution can emit an observational, semantic trace without changing sampled execution or settlement.
- `playable-round`: visible human and simulated strokes receive the same trace contract; non-observable simulations do not allocate it.
- `graphql-api`: shot results expose the optional trace projection.
- `web-play`: the action/playback lifecycle uses the trace's historical target rather than next-shot guidance.
- `web-hole-visualization`: canonical SVG playback renders authoritative trace points and removes synthetic-flight authority after gates.

## Impact

- **Pure simulation:** compact trace and transition records assembled from existing context/contact/settlement facts, without changing resolution distributions.
- **API:** additive nullable GraphQL fields for immediate shot playback; stale submissions return no outcome and no trace.
- **Frontend:** canonical renderer interpolates only between authoritative endpoints and presents recovery/replay as a non-flight rule transition.
- **Persistence:** none in this change. Pending playable events cannot currently be saved, and completed-event shot traces have no public history/replay consumer.
- **Future sequence:** this establishes the honest playback seam before surface-aware shot families, trajectory/wind controls, flight-terrain response, and spatial putting.
