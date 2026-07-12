## 1. Gate upset news on an established ranking

- [x] 1.1 `World.feedConsumers`: report an upset only when the ranking is established (`!rankingSnapshots.isEmpty()` — at least one completed season) AND the winner's pre-event rank is worse than the upset threshold.

## 2. Tests

- [x] 2.1 `WorldUpsetNewsTest`: the opening season produces no upset news; upsets appear once the ranking is established (from the second season), all attributed to a season after the first.
- [x] 2.2 Full suite green (442); reproducibility and all other news unaffected.
