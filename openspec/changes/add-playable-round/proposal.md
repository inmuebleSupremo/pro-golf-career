## Why

The player-experience target commits to **strategic shot selection** — the player playing their golfer's rounds shot-by-shot, choosing club, target, and risk each shot, with the sim resolving the outcome (§1.7; no dexterity). The engine was built for exactly this: `resolveShot` is the per-shot, decision-by-decision entry point that sits beside the AI's `resolveRound`. But nothing exposes it — the player's rounds still auto-resolve. This change builds the missing interactive core: a **`PlayableRound`** that lets a human play an 18-hole round one shot at a time on a real course in real conditions, with the option to sim any part of it. It is the first step of "playing the golf" — the thing that makes an end-to-end session feel like a game rather than a spectated simulation. This slice is standalone and fully testable; wiring the played round into a live tournament so it *counts* is the immediately following change.

## What Changes

- Add a framework-free **`com.progolf.sim.play`** package with **`PlayableRound`**: an interactive, stateful session that plays an 18-hole round for a golfer, hole by hole, shot by shot, through the existing shot engine.
- Expose the decision surface: `situation()` returns the current shot's context (hole, par, shot number, distance to pin, current lie, and the reachable zone profile of surfaces/hazards ahead); `playShot(ShotDecision)` resolves one shot via `resolveShot` — the human choosing **club, target, and risk (Strategy)**.
- Make it **always skippable** (confirmed decision): `simShot()`, `simHole()`, and `simRound()` auto-play the remaining shots using the same `StrategyPolicy` the AI uses.
- Guarantee **fidelity to the AI path**: a fully-simmed `PlayableRound` uses the same per-shot seed coordinates and strategy policy as `RoundResolver`, so its score is identical to the AI's `resolveRound` for the same golfer, course, conditions, and seed — the human simply substitutes their own decisions where they choose to play.
- Report the round result: total strokes, per-hole scores, and score relative to par.

Explicitly out of scope: any World/tournament integration (the next change wires this into a live event so it counts — cut, leaderboard, prizes, ranking); no change to `resolveShot`, `resolveRound`, or any existing domain; no UI (the app/GraphQL layer drives it later). This change adds the interactive round *engine* only.

## Capabilities

### New Capabilities
- `playable-round`: An interactive, stateful 18-hole round a human plays shot-by-shot through the shot engine — choosing club, target, and risk each shot from the current situation — that is always skippable (sim a shot, a hole, or the round) and whose simmed play is faithful to the AI's round resolution (identical score for the same inputs and seed).

## Impact

- **Codebase**: new `com.progolf.sim.play` package (`PlayableRound`, a `HoleToPlay` input, a `ShotSituation` view). It depends only on `core`, `shot`, and `spatial` — the engine pieces it drives. No existing file changes.
- **Fidelity/determinism**: `PlayableRound` reproduces `RoundResolver`'s per-shot loop (coordinates, hazard/holed rules, shot cap) so simmed rounds equal the AI path exactly; interactive play is the same engine resolving the human's decisions. All deterministic given decisions + seed.
- **DAG**: `play` depends only on `core`/`shot`/`spatial`; nothing lower imports it. The World/app will drive it (the next change and the service layer).
- **Downstream (next change)**: `add-playable-event` injects `PlayableRound` into the player's real tournament — the field auto-resolves around the player's rounds, the cut/leaderboard/prizes/ranking apply, and the world advance yields to the player's event and resumes when done.
- **Boundary/risk**: pure engine, no side effects, no new randomness beyond the existing shot RNG; the AI-equivalence test is the guardrail that the interactive path did not perturb resolution.
