# Tasks

## 1. Pure Contract

- [x] 1.1 Add pure immutable `AimPoint`, `ShotIntent`, and `BallStrikeIntent` validation.
- [x] 1.2 Add stable individual `ClubCatalogue`/`ClubSpec` mapped to current equipment families.
- [x] 1.3 Adapt intent into the existing resolver without adding physics or a flight trace.
- [x] 1.4 Make AI policy emit `BallStrikeIntent`; restrict `ShotAim` to AI/guidance.
- [x] 1.5 Isolate legacy decision adapters to fixtures/noncanonical compatibility.

## 2. Round and API

- [x] 2.1 Add current-shot planning projection: revision, ball state, envelope, clubs, guidance.
- [x] 2.2 Replace public GraphQL decision input with `BallStrikeIntentInput` and stale conflict.
- [x] 2.3 Remove human strategy, target distance, and target lateral from UI/API/controllers.
- [x] 2.4 Atomically reject stale/replayed revisions before any stroke mutation.
- [x] 2.5 Verify existing saves and specify future pending-action versioning.

## 3. Interaction

- [x] 3.1 Add club selection, target marker, defaults, and reach/legality messages.
- [x] 3.2 Add SVG unprojection and project/unproject tolerance tests.
- [x] 3.3 Add desktop, touch, keyboard, and accessible target controls.
- [x] 3.4 Refresh planning state after stale-shot conflict.

## 4. Verification and Retirement

- [x] 4.1 Add deterministic semantics, clipping, envelope, hazardous-point, and club tests.
- [x] 4.2 Add shared AI/human intent and `ShotAim`-boundary tests.
- [x] 4.3 Add GraphQL, concurrency, save, mapping/accessibility, and focused manual E2E tests.
- [x] 4.4 Run V1-V4 calibration baselines and document acceptance bands.
- [ ] 4.5 Delete `ShotDecision` and production carry/lateral/human-strategy adapters after gates.

## Manual E2E evidence

Manual E2E completed: **PASS**. Individual club selection, canonical spatial targeting, and
human AimPoint authority worked. Guidance markers remained suggestions rather than authority;
dogleg targeting followed selected spatial intent; AI/simulation and putting remained functional.
No blocking spatial-shot-intent regression was found.

UI/UX issues were observed on play event/hole screens. They are explicitly deferred as outside
this feature's gameplay-logic scope and should be revisited in a future frontend
design/styling/visuals phase.
