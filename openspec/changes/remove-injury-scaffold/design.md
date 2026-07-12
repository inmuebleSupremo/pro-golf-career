## Context

Two injury models coexist. `sim.health` (Health, Fitness & Recovery, spec: `injury-recovery` / `physical-state`) is the live one: `World` drives `HealthSystem.afterParticipation`/`recoverWeek` over each golfer's `PhysicalState`, which holds zero-or-one `sim.health.Injury` and derives `Availability`. `sim.player` carries an older, parallel injury scaffold (spec: `player-entity` "Injury State") — `Injury` with a `performancePenalty`, `PlayerState`/`Player` apply/advance methods, and a `CareerStatus.INJURED` status. A repo-wide search shows the `sim.player` path has no `main/` caller; only its own tests exercise it. The two `InjuryType` enums are identical and the two severity enums are near-identical.

## Goals / Non-Goals

**Goals:**
- Delete the dead `sim.player` injury scaffold so `sim.health` is the single injury model.
- Remove `CareerStatus.INJURED` (orphaned once nothing applies an injury at the player-entity layer) so the career-status machine reflects reality: injury is a transient availability, not a lifecycle stage.
- Keep the change behaviour-neutral for the live World (no `sim.health`, `World`, or serialization change).

**Non-Goals:**
- Any new injury behaviour — the play-through-injury performance-penalty idea is a **separate** follow-on change in `sim.health`.
- Touching `Player.deriveFormRating`'s existence — it stays as a derived-statistics demonstration; only its injury term is dropped.
- Consolidating the two `InjuryType` enums (only one survives; nothing to consolidate).

## Decisions

**Decision: delete `sim.player.Injury` outright** rather than deprecate. It has no production caller and a live equivalent exists; a deprecation shim would just prolong the duplicate.

**Decision: remove `CareerStatus.INJURED` from the enum and transition table.** Nothing sets it after `Player.applyInjury` is gone, and the health domain already expresses "cannot compete" via `Availability`. Keeping an unreachable status is a latent wart. `ACTIVE` transitions become `{RETIRED, DECEASED}`; the `INJURED` row is dropped. Alternative (keep INJURED as a defined-but-unused constant) was rejected by the scoping decision in favour of the cleaner machine. The `Career.retire()` comment that mentions the ACTIVE/INJURED→RETIRED pair is updated to ACTIVE-only.

**Decision: keep `Player.deriveFormRating`, drop only its injury term.** It is a spec'd derived-statistics illustration keyed on rating + fatigue; those still work. `PlayerEntityTest.derivedFormRatingIsRecalculatedFromCurrentState` (fatigue lowers form) stays green.

**Decision: prove behaviour-neutrality by the full suite.** There is no runtime behaviour to observe (dead code removal), so verification is: the whole suite compiles and passes with the injury references excised, and a repo search confirms no lingering `sim.player` injury reference.

## Risks / Trade-offs

- **A hidden reflective/serialization reference to `CareerStatus.INJURED`** → search the repo (main + tests + any JSON) before deleting; there is no persistence layer yet, so no saved enum names to break.
- **The play-through-injury feature might later want a player-level injury signal** → it will live in `sim.health` (its natural home) and use `Availability`; it does not need the `sim.player` scaffold or a `CareerStatus.INJURED`. If a player-level status is ever wanted, re-adding an enum constant is trivial and reversible.
- **`player-entity` spec churn** → the "Injury State" requirement is removed with an explicit migration note pointing at `injury-recovery`, so the capability is not lost, only relocated to its real owner.
