## Context

The engine runs fully autonomously: `World.advanceWeek`/`advanceSeason` resolve everything to completion, and every decision the game is about is taken by an AI policy at a seam — `AllocationPolicy` (development, inside `ProgressionEngine.develop` via `evolveGolfer`), availability gating (event entry, in `resolveEvent`), and `AcceptancePolicy` (sponsorship, in `runFinancialSeason`). The engine was deliberately built control-agnostic (REQ-104) so a human can occupy the same seams. This change adds the human: a designated golfer whose standing decisions, configured between advances, are applied at those seams. Scoping locked: configure-then-advance; focused core (development, sponsorship, schedule); control-state in the engine; designate an existing golfer.

## Goals / Non-Goals

**Goals:**
- A single designated, human-controlled golfer per world, with the world otherwise autonomous.
- Player standing decisions applied at the development, schedule, and sponsorship seams — for the player's golfer only.
- Configure-then-advance: decisions are inputs set between advances; the advance stays run-to-completion.
- Full backward compatibility: an unassigned world is byte-identical to today.

**Non-Goals:**
- Player control of staff/equipment (later slice, same pattern); custom golfer creation; mid-advance pause/resume; the GraphQL/UI surface.
- Any change to how AI golfers are handled, or to the engine's determinism when no player is assigned.

## Decisions

### D1. `PlayerControl` holds the player's configured decisions; the World holds generated pending state
`com.progolf.sim.control.PlayerControl` = the designated `golferId` + a `developmentFocus` (ordered `List<Attribute>`) + a `resting` flag. It imports only `core`. The World additionally holds the player's `pendingSponsorshipOffers` — engine-*generated* state awaiting a decision, kept out of `PlayerControl` so the control object stays a pure record of player inputs and free of an economy dependency. *Why:* clean separation (player inputs vs generated state), and it keeps `sim.control` core-only. *Alternative rejected:* putting pending offers in `PlayerControl` — couples it to `sim.economy` and blurs input vs derived state.

### D2. Configure-then-advance: decisions are inputs, the advance is unchanged
The player sets decisions between advances; `advanceWeek`/`advanceSeason` still run to completion, consulting `PlayerControl` at the seams for the player's golfer. No yield/resume. *Why:* the locked model and the smallest change to the autonomous loop (matches the weekly-turn design). *Alternative rejected:* pausing mid-advance — restructures the core loop for no V1 benefit.

### D3. Player branches are strict overrides of the existing AI paths
At each seam: `if (playerControl present && id == player's golfer) apply player decision; else <existing AI path>`. `evolveGolfer` → focus-aware develop; `resolveEvent` field filter → also exclude a resting player; `runFinancialSeason` → generate offers into the pending queue instead of AI choose+sign (paying/evaluating existing agreements is automatic and unchanged). *Why:* guarantees an unassigned world (and every non-player golfer) is unchanged, preserving determinism and the 290-test suite. *Alternative rejected:* a control-type field consulted everywhere — the engine already has no control-type branching in shot/tournament code (REQ-104) and must keep it that way; branching lives only in the World's orchestration.

### D4. Development follows a player focus via a Progression overload
`ProgressionEngine.develop(current, age, supportFactor, List<Attribute> focus)` awards the season's Development Points (scaled by the coach factor) and, when `focus` is non-empty, greedily raises the focus attributes in priority order under the same per-season cap and cost curve — otherwise it delegates to the AI allocation. *Why:* keeps allocation logic (cap, cost curve) in `progression`; the player picks *what* to develop, the engine still enforces *how much* (gradual, capped). *Alternative rejected:* the World computing allocations — duplicates the cost-curve logic outside progression.

### D5. Resting reuses the availability seam
A resting player's golfer is excluded from event fields exactly as an unavailable golfer is, so they sit out and their weekly recovery/rehab proceeds. `resolveEvent` adds `&& !(player resting)` to the field filter. *Why:* REQ-222 workload management is already modeled via availability; resting is the player's explicit version of it. *Alternative rejected:* a separate "did not enter" code path — duplicates availability gating.

### D6. Sponsorship becomes pending-then-accept
For the player's golfer, `runFinancialSeason` generates offers (as the AI path does) but stores them in `pendingSponsorshipOffers` instead of signing. `acceptSponsorship(index)` signs the chosen offer to the account (respecting the max-concurrent-agreements cap) and removes it from the queue; unaccepted offers expire when the next season regenerates the queue. *Why:* gives real per-offer accept/decline under configure-then-advance (the player reviews and accepts between advances). *Alternative rejected:* a standing "acceptance appetite" policy — less agency; the player chose per-offer accept/decline.

### D7. Player operations are surfaced on `WorldService`
`WorldService` gains `assignPlayer`, `setDevelopmentFocus`, `setResting`, `pendingSponsorships`, and `acceptSponsorship`, delegating to the session's `World`. *Why:* the service is the app's single seam to the engine (app-shell design D2); GraphQL resolvers will wire these to the UI. *Alternative rejected:* provisional REST gameplay endpoints now — GraphQL is the primary API and would replace them.

## Risks / Trade-offs

- **[First human input into an autonomous engine]** → contained to one designated golfer via strict override branches; an unassigned world is unchanged; a test asserts assigning no player leaves outcomes identical, and that a non-player golfer near the player is still AI-driven.
- **[Determinism now depends on player inputs]** → expected for a game; with no inputs the world is unchanged; persistence (later) captures `PlayerControl` + pending offers so save/load stays observably identical.
- **[Player golfer diverges from AI on assignment]** → correct: once assigned, the golfer stops auto-signing sponsors and develops per the player's focus. Documented; existing tests assign no player so are unaffected.
- **[Focus/rest with no configuration]** → sensible defaults: empty focus falls back to AI development; not resting means playing; no accepted offers means no new sponsors (the player must act) — passive agency is a valid, if suboptimal, way to play.

## Migration Plan

Additive, backward-compatible. Sequencing: (1) `com.progolf.sim.control.PlayerControl`; (2) `ProgressionEngine.develop(current, age, factor, focus)` overload (modifies player-development); (3) `World`: optional `PlayerControl` + `pendingSponsorshipOffers`, `assignPlayer`, player-action methods, and player branches at `evolveGolfer` / `resolveEvent` field filter / `runFinancialSeason` (modifies world-progression); (4) `WorldService` player operations; (5) tests — focus-driven development beats/differs from AI; resting excludes from fields and recovers; offers go pending and accept signs them under the cap; an unassigned world is identical and a non-player golfer stays AI-driven; a service-level "play a turn" integration test. Each layer testable before the next.

## Open Questions

- Whether development focus is a single attribute or a priority list — a priority list in V1 (more expressive, same cost/cap enforcement).
- Whether resting is a standing flag or per-event — a standing flag in V1; per-event scheduling can refine it later.
- How the player is designated at world creation vs mid-run — an explicit `assignPlayer` op for now; a create-with-player convenience and custom-golfer creation come with the user/account model.
- Where accepted-offer signing takes its date — the current calendar date; fine for V1.
