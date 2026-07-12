## Why

The decision policy always advanced the ball maximally toward the pin — it never faced golf's most iconic strategic fork: on a long par-5 second shot, **go for the green** (an eagle/birdie chance, but the long forced shot risks trouble) or **lay up** to a comfortable full wedge. And it ignored the golfer's power: a bomber who can reach a par 5 in two plays it completely differently from a short hitter. This is the first of the deferred situational-strategy refinements: the lay-up-or-go decision, folding in playing-to-strengths.

## What Changes

- **Lay-up vs go-for-it on a par-5 approach (policy).** On a long approach (not a tee shot) to a par 5, the golfer decides: an **aggressive** disposition always fires at the green; **any** golfer who can reach it **comfortably** goes too (playing to a long hitter's strength); otherwise the golfer **lays up** to leave a full wedge (`LAYUP_LEAVE_DISTANCE`), aimed safely at the green centre. Restricted to par 5s — a par 4/3 has no stroke to spare, and a tee shot always goes — using the hole's par, now exposed on `HoleModel`.
- **Playing to strengths (policy).** "Comfortably reachable" is computed from the golfer's own reach (their distance attribute for the reaching club), so a long hitter goes for greens a short hitter must lay up short of — the golfer's power finally shapes the decision. The policy now receives the golfer's attributes.

Measured: aggressive golfers make markedly more par-5 birdies (~58% vs ~47% for conservative lay-up play) and carry the widest score variance, while the three dispositions stay mean-neutral (within ~0.2 strokes) — a real par-5 risk/reward and a distinct long-hitter style. World scoring drifts up modestly (live strategic play is a touch harder); the calibration guard holds.

Explicitly out of scope: front-of-green par-5 hazards (the model has none, so laying up trades proximity/birdies for a controlled wedge rather than *avoiding* a hazard — a consistency choice, not a break-even; a fuller model would add the water/bunker that makes going-for-it genuinely perilous); lay-up on long par-4 approaches; scoreboard-aware or pin-depth decisions (separate refinements).

## Capabilities

### Modified Capabilities
- `shot-resolution`: the decision policy now makes a lay-up-vs-go-for-it choice on a long par-5 approach — an aggressive or comfortably-reaching (long-hitting) golfer goes for the green, otherwise it lays up to a full wedge — so par-5 strategy and the golfer's power shape play.

## Impact

- **Codebase**: `HoleModel.par()` (default 4) exposed and overridden by `RoundHole`; `StrategyPolicy.decide` gains the golfer's attributes and the hole par and the lay-up/go-for-it branch; `SimConstants` gains lay-up tunables (`LAYUP_MIN_DISTANCE`, `LAYUP_LEAVE_DISTANCE`, `LAYUP_COMFORTABLE_MARGIN`); `RoundResolver`/`PlayableRound`/`PlayableHole` pass the attributes + par. New `LayUpStrategyTest`.
- **Determinism / fidelity**: the decision is a pure, deterministic function of distance/lie/pin/attributes/par, computed identically on the auto and interactive paths, so reproducibility and playable-event fidelity hold. Scores shift by design (a new decision layer); non-par-5 and tee-shot behaviour is unchanged.
- **DAG**: unchanged — all within `sim.shot` / `sim.course` / `sim.play`.
