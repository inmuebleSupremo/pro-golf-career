## 1. Pure pin-placement and effective geometry

- [x] 1.1 Add a pure, explicit `PinPlacementVersion`/policy seam with exact `LEGACY_V1` compatibility and a separately selected V5 policy; do not alter V1–V4 course-generator implementations or setup constants.
- [x] 1.2 Add reusable pure GREEN-polygon utilities for containment, true point-to-segment clearance, local-frame intent, bounded deterministic inset projection, and deterministic unsupported-inset validation.
- [x] 1.3 Route round, automatic, interactive, and playoff cup construction through the selected policy and the exact setup-specific canonical geometry used for settlement.
- [x] 1.4 Preserve depth/lateral presentation semantics while ensuring the resolved cup coordinate, pin metadata, and active effective geometry cannot disagree.

## 2. Tournament provenance, world state, and migration

- [x] 2.1 Add immutable pin-policy provenance to scheduled events and active tournament definitions; initialise new careers/schedules to V5 and retain a persisted future-schedule default.
- [x] 2.2 Extend world snapshots and save persistence with backward-compatible missing-field defaults to legacy; retain policy provenance in archived/current schedules needed for historical reproduction.
- [x] 2.3 Implement an atomic engine/application migration that is allowed only with no pending playable event, upgrades only future unstarted scheduled events plus the future default, preserves completed/started events, and is idempotent without consuming entropy.
- [x] 2.4 Add a narrow owner-authorized GraphQL status/mutation seam and a minimal existing career-management affordance to make explicit V5 adoption accessible without a new save-management experience.

## 3. Deterministic geometry and compatibility tests

- [x] 3.1 Add fixed-seed V5 corpora across every supported tier/prestige setup, including Elite Major, proving every cup is `GREEN`, has at least 2.0 yards true polygon-edge clearance, and reproduces exactly by seed, course-generator version, policy version, setup, and round.
- [x] 3.2 Add adversarial rotated/narrow-green fixtures proving local-frame interpretation, true Euclidean rather than radial clearance, deterministic projection, and deterministic failure when the eligible inset is absent.
- [x] 3.3 Prove front/back and left/right variety survives projection; prove hard setups remain more tucked and more difficult than regular/easier setups without crossing the clearance boundary.
- [x] 3.4 Lock V1–V4 pin/cup outputs and V4 geometry to historical fixtures; prove selecting V5 changes only pin placement rather than course terrain.

## 4. Lifecycle, save, and completion tests

- [x] 4.1 Test legacy snapshot loading, new-career V5 defaults, explicit future-only migration, active-event rejection, completed/archive immutability, atomic failure behaviour, repeated-migration idempotence, and future schedule generation after migration.
- [x] 4.2 Test automatic, human, and playoff/AI paths against deterministic near-edge V5 cups: putting remains available from GREEN/FRINGE as today, valid cups do not force a non-puttable near-cup loop, and normal hole-out/short-game rules remain unchanged.
- [x] 4.3 Run GIR, putts, hole scoring, tournament setup, and world-scoring calibration corpora. Document any justified V5 distribution updates without weakening historical-version fixtures or unrelated scoring tests.
- [x] 4.4 Record the two pre-existing `WaterDropTest` failures as excluded baseline defects; do not modify them.

## 5. Validate

- [x] 5.1 Run the focused and full backend suite, reporting the known WaterDrop baseline separately.
- [x] 5.2 Run `openspec validate valid-effective-pin-placement --strict --no-interactive`, then manually check that course generation, setup, tournament lifecycle, persistence, and GraphQL deltas make one consistent versioning and migration contract.

## Deferred manual follow-up

Manual legacy-career migration UI E2E is intentionally deferred. Automated migration coverage and the accepted
new-career/combined-gameplay E2E checks do not constitute this separate manual acceptance.
