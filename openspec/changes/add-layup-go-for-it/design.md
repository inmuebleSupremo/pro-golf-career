## Context

`StrategyPolicy.decide` always advanced the ball maximally (`target = min(remaining, club.baseDistance())`), so it never laid up, and it had no access to the golfer's attributes or the hole's par — so it could neither make the par-5 lay-up decision nor play to a long hitter's strength.

## Goals / Non-goals

- **Goals**: a par-5 lay-up-or-go decision keyed on disposition and the golfer's reach (playing to strengths); pure and deterministic; auto/interactive fidelity preserved.
- **Non-goals**: front-of-green par-5 hazards, lay-up on par-4 approaches, scoreboard/pin-depth decisions.

## Decisions

### D1 — Restrict lay-up to par 5s, using the hole par
The lay-up-or-go decision only makes sense with a stroke to spare, i.e. a par 5; on a par 4/3 the golfer must go for the green, and a tee shot always goes. The policy needs the hole par, so `HoleModel.par()` (default 4) is exposed and `RoundHole` returns the real par; `decide` takes it. Without par, laying up on long par-4 approaches would manufacture bogeys — the drift this restriction removes. The tee shot is excluded via `lie == TEE_BOX`.

### D2 — Go/lay decision by disposition and reach (playing to strengths)
`goForIt = aggressive || comfortablyReachable`, where `comfortablyReachable = maxReach(reachClub, attributes) ≥ remaining + LAYUP_COMFORTABLE_MARGIN` and `maxReach` mirrors the resolver's reach formula from the golfer's distance attribute. So an aggressive golfer always fires; a long hitter reaches and goes even when conservative; a short hitter lays up. Laying up selects the club for `remaining − LAYUP_LEAVE_DISTANCE` and aims at the centre (no pin attack). The policy now receives the golfer's `Attributes` (threaded through the resolvers); a neutral default keeps the aim-only convenience overloads working for tests.

### D3 — Accept a modest scoring drift; frame lay-up as a consistency choice
Because the hole model has no front-of-green par-5 hazard, "going for it" that comes up short simply leaves the ball near the green (an easy up-and-down), so laying up never *avoids* a hazard — it trades proximity (and birdies) for a controlled wedge. Laying up is therefore a small scoring cost, and world scoring drifts up modestly as non-aggressive golfers lay up on unreachable par 5s. This is framed honestly: aggressive golfers are birdie-productive on par 5s (~58% vs ~47%) and higher-variance, the three dispositions stay mean-neutral (within ~0.2 strokes), and conservative play is steady-but-fewer-birdies. A fuller model (front-green water/bunker that punishes the miss) would make lay-up a break-even decision; it is deferred.

## Risks / Trade-offs

- **World scoring drift** (calibration field mean ~−0.3 in calm, up from ~−1): live strategic play is a touch harder; the `ScoringCalibrationTest` guard holds. Reproducibility and playable-event fidelity are preserved (the decision is deterministic and identical across paths).
- **Lay-up is a consistency trade, not a break-even** (no front-green hazard) — documented; the fix is a hole-model addition, out of scope here.
- **Reach heuristic ignores equipment** (the policy has only attributes) — a small mismatch with the resolver's actual reach; acceptable for a decision heuristic.
