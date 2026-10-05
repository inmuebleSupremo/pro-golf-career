# Verification evidence and migration boundary

## Manual gameplay E2E

Status: **PASS**, reported by the user after the final manual gameplay run.

The reported pass covers the canonical geometry and visible lies, the ball state persisting across shots and holes,
hazards and recoveries, green/fringe distinction, SVG coherence, and the expected foundation-level course variation.
This evidence records the user's result only; it does not claim an additional automated browser run or details that
were not supplied.

## Automated contracts and calibration guardrails

The backend suite includes deterministic geometry generation for every `EnvironmentClassification`, polygon and
precedence validation, direct normal/water/out-of-bounds settlement coverage, human/automatic resolution coverage,
and GraphQL DTO/off-event coverage. The frontend geometry unit test verifies that renderable terrain paths are derived
from the API-provided canonical regions and change only when that canonical input changes. Existing scoring,
fairway, GIR, putts, and strategy calibration harnesses remain the regression gate; no separate course-generation or
shot-model expansion is introduced by this change.

Final focused calibration run: the 80-player generated-field score guard produced mean **+0.05**, best **-9**, and
worst **+16** to par; the scoring-realism, shot-statistics, and putting calibration tests also passed.

## Persistence scope

`World.snapshot()` rejects a pending playable event. Consequently, the current save format has no in-progress
`PlayableRound`, `BallState`, or unresolved `ShotSettlement` to serialize or migrate. Course geometry is regenerated
from the persisted world seed, configuration, and generator version; it is not persisted as a mutable presentation
copy. A future save format that admits a pending playable event must persist the complete legal `BallState` and any
unresolved settlement needed to resume from the exact next-shot origin. That future format is deliberately outside
this change.

## Legacy adapter status and retirement gates

`CourseGeometry.surfaceAt` is the final terrain authority for generated production holes. `HoleZones` /
`ShotZoneProfile` remains a bounded compatibility read for the existing carry/lateral sampler and reachable-preview
data: production `RoundHole`, `PlayableRound`, and `PlayableHole` still call it while `ShotContext` requires that
input. The canonical spatial resolver overwrites that sampled profile surface with the geometry-derived result; the
adapter cannot add a canonical terrain region or drive frontend terrain rendering.

It is therefore retained, not misrepresented as fixture-only. It may be removed only after the sampler and reachable
preview no longer read a zone profile, generated holes and both round paths retain their canonical behavior,
public current-hole rendering remains geometry-only, calibration gates pass, and an architecture check confines any
residual adapter code to test support. Removing it earlier would be a behavior change, not cleanup.

## Deferred questions (intentionally out of scope)

The next change, if approved, may separately consider richer course generation, club/loft/spin systems, shot
types/shapes/aiming, flight or terrain-response physics, spatial putting, and paused-event persistence. None is
implemented, implied, or required here.
