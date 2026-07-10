## Context

`RoundResolver.resolveHole` plays a hole by looping `ShotResolver.resolveShot` per shot, deriving each `ShotDecision` from a `StrategyPolicy` (the AI). `resolveShot` is the decision-by-decision entry point built for a human. This change exposes that loop interactively: a `PlayableRound` that lets a human supply the `ShotDecision` each shot (club, target, risk) while reusing every other engine input (attributes, golfer state, hole geometry, per-hole environment, seed coordinates). It is the "playing the golf" core from the player-experience target (`docs/backend/player-experience.md` §3A/§7). Framework-free; the app drives it later.

## Goals / Non-Goals

**Goals:**
- An interactive 18-hole round: see the situation, choose club/target/risk, resolve one shot, repeat.
- Always skippable: sim a shot, a hole, or the rest of the round using the AI policy.
- Faithful to the AI path: a fully-simmed round equals `RoundResolver` for the same inputs and seed.
- Deterministic, pure, no new randomness; standalone and fully testable.

**Non-Goals:**
- Tournament/World integration and "it counts" (the next change).
- Changing `resolveShot`/`resolveRound` or any existing domain.
- Modelling lie-dependent difficulty beyond what the shot engine already consumes (the current surface is shown for context; the engine resolves from remaining distance + zone profile + environment).
- Any UI or service surface.

## Decisions

### D1. `PlayableRound` reproduces `RoundResolver`'s per-shot loop exactly
The interactive loop mirrors `resolveHole`: `preShotRemaining = remaining`; build the `ShotContext` from `(attributes, state, environment, remaining, hole.zoneProfileFor(remaining), decision, coord.withHole(h).withShot(shotNo))`; resolve; on a hazard reset to `preShotRemaining` (stroke-and-distance), otherwise set `remaining = distanceRemaining` and hole out when `remaining <= HOLED_THRESHOLD`; cap at `MAX_SHOTS_PER_HOLE`. The only difference from the AI is where the `ShotDecision` comes from. *Why:* identical mechanics + identical seed coordinates guarantee that simming (policy decisions) equals `RoundResolver`, and that interactive play is the same engine resolving the human's choice. *Alternative rejected:* delegating sim to `resolveHole` per hole — breaks seed continuity when a hole is played partly by hand then simmed, and forks the code path.

### D2. Seed coordinates match the tournament's exactly
The round is constructed with a base `SeedCoordinate(worldSeed, season, tournament, roundNo, fieldIndex, 0, 0)`; per shot it uses `base.withHole(holeNumber).withShot(shotNumber)` — byte-identical to the coordinate `resolveHole` uses (`new SeedCoordinate(..., roundNo, fieldIndex, hole, 0).withShot(shotNo)`). *Why:* the next change injects `PlayableRound` into the real tournament; using the same coordinates means a simmed player round is indistinguishable from the AI-resolved one, and a played round differs only by the human's decisions. *Alternative rejected:* an independent RNG for the player — would make the played and AI worlds diverge for reasons other than the player's choices.

### D3. Decisions are `ShotDecision` (club + target + Strategy); the situation informs them
`situation()` exposes what a human needs to decide: hole number and par, shot number and strokes so far, distance to pin, current lie (`Surface`), and the reachable `ShotZoneProfile` (the surfaces/hazards the shot could find). `playShot(ShotDecision)` takes the human's club/target/risk. *Why:* this is exactly §1.7's club/target/risk selection, and `ShotDecision` is already the engine's decision type — no new decision model. *Alternative rejected:* a bespoke player-decision type — needless translation to `ShotDecision`.

### D4. Skippable at shot / hole / round granularity via the AI policy
`simShot()` plays the current shot with `StrategyPolicy.decide(remaining)`; `simHole()` sims to the end of the current hole; `simRound()` sims all remaining holes. Sim and play share the one per-shot loop (decision source differs), so mixing is seamless and seed-continuous. *Why:* the confirmed "always skippable" decision, at natural granularities, without duplicating logic. *Alternative rejected:* whole-round-only sim — too coarse for "play the moments that matter."

### D5. `HoleToPlay` carries the per-hole inputs; the caller assembles the round
A `HoleToPlay(HoleModel model, int par, Environment environment)` bundles what each hole needs; `PlayableRound` takes the 18 of them plus attributes, state, base coordinate, and a fallback `Strategy`. *Why:* keeps `PlayableRound` decoupled from `Course`/`Tournament`/weather (the next change assembles the list from the real event); standalone tests assemble it from a generated course with `Environment.calm()`. *Alternative rejected:* passing a `Course` + weather — couples `play` to `course`/`weather` and complicates testing.

## Risks / Trade-offs

- **[Interactive loop drifting from `resolveHole`]** → the two share the same constants and rules; an equivalence test asserts a fully-simmed `PlayableRound` equals the sum of `RoundResolver.resolveHole` over the round for the same inputs and seed. Any drift fails that test.
- **[Lie not mechanically penalising]** → the shot engine resolves from remaining distance + zone profile + environment; the current surface is shown for context only. This is an existing engine characteristic, documented, not introduced here; a future refinement can make lie bite.
- **[Determinism with human input]** → deterministic given the decisions and seed; the same decisions replay identically — which is what the next change and persistence rely on.
- **[Shot cap / pathological holes]** → the `MAX_SHOTS_PER_HOLE` cap from the engine bounds a hole; `PlayableRound` completes a hole on holing out or the cap, exactly as `resolveHole` does.

## Migration Plan

Greenfield, additive. Sequencing: (1) `HoleToPlay` and `ShotSituation` value types; (2) `PlayableRound` (state, `situation`, `playShot`, `simShot`/`simHole`/`simRound`, completion + scoring), mirroring `resolveHole`; (3) tests — a round completes and scores; a fully-simmed round equals `RoundResolver` (fidelity); interactive `playShot` resolves the human's decision and advances the situation; determinism (same decisions + seed → same result); sim at shot/hole/round granularity; the architecture-purity test still passes (`play` is framework-free). No existing code changes.

## Open Questions

- Whether `situation()` should also surface a recommended/default decision (the AI's pick) for a "quick play" affordance — easy to add from `StrategyPolicy`; deferred to the UI layer.
- How lie should eventually influence the next shot (rough vs fairway) — an engine refinement tracked separately; not in this change.
- Whether partial-hole sim ("sim the rest of this hole") needs its own affordance beyond `simHole()` — `simHole()` already continues from the current shot, so it covers it.
