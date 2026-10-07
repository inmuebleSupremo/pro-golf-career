# Design: Spatial Shot Intent and Club Selection

## Core Contract

Introduce pure immutable `ShotIntent`. Its first variant is:

```text
BallStrikeIntent { club: ClubId, aimPoint: AimPoint }
AimPoint { x: double, y: double }
```

`AimPoint` is an absolute position in the current hole's canonical yard-space: tee-side origin, positive `y` toward green, positive `x` golfer-right. It is intended carry/first contact, not final resting position, a relative carry/lateral vector, a selected zone, or a flight instruction.

The compatibility resolver derives direction and requested travel from current ball position to point, then retains the existing reach cap, dispersion, conditions, and canonical surface settlement. It does not add bounce, roll, path intersection, forced carries, or a trace.

## Clubs and Equipment

A static stable catalogue supplies driver, fairway woods, hybrids, individual numbered irons, named wedges, and putter. A spec has a stable ID, display label, equipment family, nominal carry calibration, and resolver characteristics. IDs are durable API/save contracts.

Each club maps to existing driver/fairway-wood/hybrid/iron/wedge/putter equipment families. Current `GolfBag` and loadout aggregate effects still modify execution. There is no per-club ownership, equip/unequip, rarity, or progression redesign.

## Human / AI Boundary

Humans submit only a club and literal point. Strategy is removed from manual UI and public mutation. The initial compatibility slice uses a documented neutral legacy-dispersion adaptation for human strikes; it cannot reintroduce a hidden human risk setting.

AI retains `Strategy` as internal tournament/score-state policy. It picks a catalogue club, resolves SAFE/PRIMARY/AGGRESSIVE route guidance into an `AimPoint`, and emits the same `BallStrikeIntent`. `ShotAim` is AI/guidance only and cannot redirect a human submitted point.

## Guidance, Legality, and UI

`ShotGuidance` is a read model: current ball position, clubs and approximate reach, finite aim envelope, and literal SAFE/PRIMARY/AGGRESSIVE defaults. A default resolves to an ordinary `AimPoint`; players may freely select points inside the envelope.

Reach is advice, not legality. Hazard, tree, out-of-bounds, and beyond-reach points within the envelope are legal poor-golf choices. Reject only non-finite coordinates, unknown clubs, stale revisions, and points outside a server-derived planning envelope based on hole boundary plus a documented margin.

Canonical SVG targeting supplies invertible `project`/`unproject` transforms within tolerance. Desktop supports click/drag; mobile tap and touch-friendly adjustment; keyboard users get labelled coarse/fine nudges, coordinate/distance/reach feedback, and non-colour warnings. The client does not manufacture terrain truth.

## API, Concurrency, and Retirement

The public mutation takes `BallStrikeIntentInput` (catalogue club, `AimPointInput`, opaque expected shot revision). A coherent planning projection provides the same pending-shot revision, canonical state, envelope, clubs/reach, and guidance. The service atomically checks revision before mutation; stale or replayed requests return typed retryable conflict and do not change ball, stroke count, or round state.

`ShotDecision`, target distance, and target lateral become compatibility adapters only. Delete them after all human and AI paths use `ShotIntent`, no public strategy/carry/lateral survives, `ShotAim` is absent from human direction, and parity/calibration gates pass. No old public mutation remains authoritative beside the new one. Legacy adapters may serve fixtures or noncanonical compatibility, never select a human direction.

## Saves and Calibration

Existing saves load unchanged. Pending playable shots are not currently persisted, so this adds no in-flight migration. Catalogue IDs are stable; any future persisted pending intent/revision requires an explicit versioned migration.

Gate removal on deterministic intent/validation tests, hazardous-but-legal and envelope tests, club-family tests, shared human/AI intent tests, stale concurrency tests, GraphQL/security, save, mapping/accessibility tests, and full V1-V4 scoring/hazard calibration comparison.
