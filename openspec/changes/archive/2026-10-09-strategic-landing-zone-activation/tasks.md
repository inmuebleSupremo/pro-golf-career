## 1. Decision and model contract

- [x] 1.1 Confirm the additive structured strategic-option GraphQL contract and retain a compatible ordinary PRIMARY path during client migration.
- [x] 1.2 Map the existing V4 `HoleSpatialPlan`, `LandingZone`, `HoleRoute`, `HazardPlan`, canonical geometry, legal club/family, and ground-response seams; keep all new simulation types in `sim.*`.
- [x] 1.3 Define immutable pure option, route-consequence, and qualitative exposure facts. Document the material-distinction thresholds from fixed fixtures rather than as UI-only magic values.
- [x] 1.4 Extract/reuse only the pre-shot reach/dispersion and supported nominal-release facts needed for deterministic screening. Do not consume RNG, duplicate resolver randomness, or create a new settlement path.

## 2. Pure strategic target planning and AI

- [x] 2.1 Implement V4 candidate discovery from current ball/route progress for SAFE, PRIMARY, and AGGRESSIVE zones, including legal club/family selection and canonical valid-landing checks.
- [x] 2.2 Implement deterministic canonical exposure and bounded nominal-ground-response screening. Reject unsupported/false water, out-of-bounds, bunker, tree, recovery, or rollout claims.
- [x] 2.3 Enforce truthful material distinction and ordinary PRIMARY fallback; hide/unpublish duplicate or non-viable roles rather than manufacturing a strategic choice.
- [x] 2.4 Integrate the same planner into `StrategyPolicy` / round simulation. Keep putt, recovery, current short-game, and legacy lay-up protections; preserve existing `ShotResolver` as final authority.
- [x] 2.5 Preserve V1/V2 and V4-without-eligible-options current aim/policy paths. Do not change generator version, persisted strategy data, or historical results.

## 3. Guidance projection and play surface

- [x] 3.1 Extend `ShotGuidance`, application DTOs, schema, and operation selections with additive server-authored strategic options; regenerate frontend GraphQL types.
- [x] 3.2 Update the existing guidance dock to render only distinct backend options with role, suggested club/family, and concise route/exposure explanation.
- [x] 3.3 Ensure an option click changes only the literal target marker. Preserve free pointer/touch/keyboard aiming and deliberate club/family/shape selection; do not add controls or client terrain/risk/roll logic.
- [x] 3.4 Retain accessible non-colour-only labels and the ordinary/no-option fallback state.
- [x] 3.5 Initialize and reconcile club/family/shape from the server-authored availability projection, preserving legal deliberate choices; show safe expected validation feedback and simplify aiming to click/tap plus disclosed X/Y fine adjustment.

## 4. Focused verification and calibration evidence

- [x] 4.1 Add a named deterministic V4 strategic par-5 fixture/test proving valid, distinct SAFE and AGGRESSIVE plans, legal clubs/families, shorter-next-shot versus higher-exposure trade-off, and shared-resolver execution.
- [x] 4.2 Add a named deterministic V4 positional par-4 fixture/test proving route/landing selection instead of an automatic green/pin target, including the honest single-PRIMARY fallback where applicable.
- [x] 4.3 Test deterministic AI selection for all dispositions, no future-RNG consumption, human-guidance/AI-candidate parity, and normal legacy par-5/recovery protections.
- [x] 4.4 Test V1/V2 and ineligible V4 compatibility, fixed-seed repeatability, architecture purity, GraphQL projection, and frontend guidance rendering/free-manual selection. The focused backend selection passed 41 tests; frontend codegen, typecheck, lint, 22 tests, and production build passed.
- [x] 4.5 Run justified existing strategy, course, hazard, scoring, and ground-response calibration suites. The selected scoring/hazard calibration passed with course mean -2.415, role-hazard mean 2.948 (38 water, 10 recovery), routing mean -3.339, and field mean -0.43; no rollout or club-distance retuning occurred. `WorldScoringRealismTest` remains an explicit bounded-run limitation: it did not complete within 30 seconds and was not retried.
- [x] 4.6 Add focused regression coverage for bunker/recovery availability initialization, invalid out-of-envelope aim rejection, legal selection preservation, and the bounded client feedback path. Focused backend tests passed 30/30; frontend tests passed 27/27, with codegen/typecheck and lint passing.

## 5. Manual acceptance and closeout

- [ ] 5.1 Manually play the documented strategic V4 par-5 and positional V4 par-4 paths. Record only scenarios actually played, including option explanation/readability and free-manual override.
- [ ] 5.2 Record any observed scoring, hazard, or long-club-rollout concerns as evidence for the existing deferred calibration work, not an automatic blocker.
- [x] 5.3 Strictly validate the active OpenSpec change and canonical specifications; update the acceptance record honestly before requesting implementation/integration approval. `openspec validate --all --strict` passed 80 items with zero failures.
- [x] 5.4 Overall manual E2E acceptance: gameplay felt good, the simplified click-first aiming interface was accepted, and this implementation is approved for integration. This records overall approval only; it does not claim the unenumerated scenario-specific paths in 5.1 were manually verified.
- [x] 5.5 Preserve the product boundary in the acceptance record: strategic target activation is complete, but generated-hole variety remains inadequate. The separately approved next priority is principles-driven next-generation course and hole architecture, not further tuning of this feature.
