## Context

`GeneratedHole.pinFor(round, setup)` currently derives a depth and lateral offset from two seeded draws. `RoundHole` separately obtains setup-scaled canonical geometry and constructs its cup by adding those raw global offsets to the green centre. This assumes a rectangular, unscaled, axis-aligned green. It is false for narrowed and rotated V3/V4 GREEN polygons. The legacy `HoleZones` depth clamp affects a compatibility read model, not the authoritative cup or canonical surface classification.

The course-generator version must remain a course-geometry provenance pin. Reusing it for a pin-only correction would either reinterpret legacy tournaments or make explicit future-only adoption impossible without changing a world's historical course identity.

## Goals

- Give every V5 cup a mechanically valid, playable position in the actual effective GREEN.
- Preserve deterministic intent, front/back and left/right semantics, and hard-setup challenge.
- Keep V1–V4 course and pin reproduction exact.
- Let an existing career explicitly adopt corrected flags for future unstarted events without changing completed tournaments or an event already underway.

## Non-Goals

- General polygon/green redesign, terrain changes, putting-rule changes, fallback short-game rules, or score-cap changes.
- A broad save-format or frontend redesign.

## Decisions

### A pin policy is distinct from course generation

Introduce a pure `PinPlacementVersion` (or equivalently named explicit policy) with at least `LEGACY_V1`, which invokes the current historical pin derivation exactly, and `V5_EFFECTIVE_GREEN`, which invokes the constrained algorithm below.

`CourseGenerator` remains at its current V4 geometry version. A `Course`, `HoleModel`, and/or tournament factory shall receive the selected pin policy explicitly rather than inferring it from the latest code. A tournament's definition carries that selected policy immutably, and automatic, interactive, and playoff paths obtain their round models through that same definition.

### V5 pin derivation and geometric invariant

V5 begins with exactly the existing two `SplitMix64` draws derived from the hole seed and round identity; it adds no ambient entropy and does not use unbounded rejection sampling. The draws express intended signed depth and lateral placement. Their axes are the effective green's local approach direction and golfer-right direction, including setup scaling, rather than global x/y coordinates.

The policy obtains the exact setup-specific `CourseGeometry` used by `RoundHole` settlement, locates its GREEN polygon, and forms the intended point from its local-frame intent. It accepts the point only when:

1. `effectiveGeometry.surfaceAt(cup) == GREEN`; and
2. the shortest Euclidean distance from the cup to every GREEN-polygon edge is at least 2.0 yards.

Otherwise V5 uses a bounded deterministic projection of that intended direction into the eligible inset region. The projection must evaluate true point-to-segment distance; radial distance, nominal dimensions, rectangular clamps, and `HoleZones` are not substitutes. A fixed precision/iteration bound is permitted only when it is part of the versioned algorithm and the resulting point is verified against both invariants.

The generator or setup validation boundary must prove that the effective GREEN has a non-empty 2.0-yard eligible region before V5 placement is available. It shall fail deterministically rather than reducing clearance or falling back to legacy placement. Current supported setup combinations are required to pass this validation.

Projection preserves the signs of depth and lateral intent and therefore front/back and left/right semantics. Difficulty remains expressed by existing pin aggression, width, and wind inputs: hard setups may reach the eligible inset boundary but may never cross it.

### Policy provenance, scheduling, and migration

Each `ScheduledTournament` (or its equivalent immutable event provenance) stores a pin-placement version. The world also stores the default policy used to generate future schedules. New careers initialise both to V5. Existing deserialized snapshots whose fields are absent initialise them to `LEGACY_V1`.

An explicit owner-authorized migration is available only when no playable event is pending. It atomically validates the clean event boundary, changes the world's future-schedule default to V5, replaces only unstarted scheduled events with otherwise identical entries pinned to V5, and leaves completed schedule/archive records, results, player progression, seed streams, course-generator version, and any begun event unchanged.

Repeated adoption when V5 is already the default and all eligible events already use V5 is a no-op. The command does not consume random draws. Historical schedule/archive entries retain their legacy policy so historical reproduction is not rewritten.

The app exposes a narrowly named authenticated mutation, with a read/status projection sufficient for an existing career-management surface to explain the current policy and offer the one-time adoption action. It is not a generic save editor or a new management screen.

## Alternatives considered

### Change `GeneratedHole.pinFor` globally

Rejected: it silently changes V1–V4 historical replay despite unchanged generator provenance.

### Increment only the course-generator version

Rejected: geometry is unchanged, and one world-level version cannot preserve historical events while selectively adopting corrected future pins for an existing career.

### Deterministic rejection sampling

Rejected: bounded attempts require a distribution-changing fallback; unbounded attempts violate the fixed deterministic workload requirement. Projection directly preserves the intended direction.

### A 0.4-yard, 3-yard, or 4-yard inset

0.4 yards is only the current mechanical putting-leave floor. Three yards fits the current corpus but projects a large majority of Elite Major flags onto the boundary. Four yards is unsupported by some real effective greens. Two yards fits every audited supported setup, materially exceeds the mechanical floor, and retains meaningful hard-pin variation. It is a game policy for current simplified geometry, not a claim of official course-setting standards.

## Risks and validation

- **Incorrect polygon metric:** direct fixtures must distinguish true segment distance from a radial proxy.
- **Policy leak between resolution and presentation:** derive cup and effective geometry from one round model; assert API cup, settlement geometry, human play, AI play, and playoff play agree.
- **Calibration drift:** preserve all legacy fixtures, compare V5 regular/hard setups, GIR, putts, scoring, and tournament distributions before changing any unrelated calibration constant.
- **Migration history rewrite:** test archive/current schedule separation, active-event rejection, atomicity, and idempotence.

## Migration and rollback

No existing save is silently modified on load. A pre-policy snapshot restores with `LEGACY_V1`; new careers use V5. Explicit adoption writes version provenance at the next normal save. Rollback preserves an already migrated career and its pinned future-event choices; it must not reinterpret them as legacy. Supporting rollback therefore means retaining V5 as a supported policy, not deleting its implementation.
