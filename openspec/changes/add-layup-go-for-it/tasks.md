## 1. Expose hole par

- [x] 1.1 `HoleModel.par()` (default 4); `RoundHole` overrides it to the generated hole's par.

## 2. Lay-up vs go-for-it (+ strengths)

- [x] 2.1 `SimConstants`: `LAYUP_MIN_DISTANCE` (215), `LAYUP_LEAVE_DISTANCE` (95), `LAYUP_COMFORTABLE_MARGIN` (10).
- [x] 2.2 `StrategyPolicy.decide`: accept the golfer's attributes and the hole par; on a long par-5 approach (not a tee shot), go for it if aggressive or comfortably reachable (from the golfer's reach — playing to strengths), else lay up to a full wedge aimed at centre. Keep the aim-only convenience overloads (neutral attrs, par 4).
- [x] 2.3 Thread attributes + par into `decide` from `RoundResolver`/`PlayableRound`/`PlayableHole`.

## 3. Tests

- [x] 3.1 `LayUpStrategyTest`: aggression goes for the green / conservatism lays up; a long hitter goes for it where a short hitter lays up; only par 5s offer a lay-up; a tee shot never lays up.
- [x] 3.2 Full suite green (428); reproducibility, playable-event fidelity, and the calibration guard hold.

## 4. Verify

- [x] 4.1 Measure: aggressive golfers make ~58% par-5 birdies vs ~47% for conservative lay-up play, with the widest score variance; the three dispositions stay mean-neutral (within ~0.2 strokes); world scoring drifts up modestly and stays believable.
