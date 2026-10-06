## 1. Effective geometry seam

- [x] 1.1 Expose the exact active setup-specific `CourseGeometry` from playable event/hole presentation paths without duplicating generator logic.
- [x] 1.2 Route `WorldService` and `ApiMapper` to project that effective geometry while retaining stable overview metadata and active pin behavior.
- [x] 1.3 Preserve neutral setup identity/equivalence and reuse the existing deterministic setup-geometry cache.

## 2. Contract and regression coverage

- [x] 2.1 Add engine tests proving a non-neutral width setup's effective geometry agrees with canonical surface classification used by resolution.
- [x] 2.2 Add API/GraphQL tests proving `PlayingHole.geometry` is the active setup geometry for current and explicitly requested holes, and remains unchanged for neutral setup.
- [x] 2.3 Add frontend coverage proving returned canonical regions, including a setup-specific width difference, are rendered without local terrain synthesis.
- [x] 2.4 Run course setup/calibration, API, frontend typecheck/lint/build.
- [x] 2.5 Manual gameplay E2E completed — PASS (user confirmation). No blocking gameplay or visual regressions were identified; both changes are approved for integration into `develop`.

## 3. Deferred compatibility record

- [ ] 3.1 Carry the version-pinned generator regeneration decision, save-policy design, and compatibility tests into the later `course-design-foundations` change before any generator-version increase.
