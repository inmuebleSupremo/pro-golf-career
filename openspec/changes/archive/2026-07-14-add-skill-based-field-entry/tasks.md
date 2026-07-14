## 1. Implement

- [x] 1.1 In `World#buildEvent`, add an `abilityOf(id)` helper (mean of the golfer's attributes — NOT the live form rating, which starts uniform) and a field-priority comparator (season points desc, then ability desc, then id asc); apply it to the regular/signature draw before `limit(fieldSize)`
- [x] 1.2 Keep the major (cross-tier) field draw unchanged
- [x] 1.3 Raise the created-golfer starting build (`CREATION_BASELINE` 50 → 56, created humans only) so a prospect makes entry-tier fields on merit

## 2. Test

- [x] 2.1 Add a test proving a created golfer of competitive entry-tier ability reaches a pending event within a season (a fresh world, `createPlayer`, advance until `hasPendingEvent`) — the scenario that previously failed
- [x] 2.2 Add/confirm a determinism assertion (same state → same field/order)
- [x] 2.3 Run the full backend test suite; update any assertions that legitimately changed because fields are now skill-ordered on equal points

## 3. Verify end to end

- [x] 3.1 Against the running backend at default scale: onboard a golfer through the frontend (or `createWorld` default + `createPlayer`), advance, and confirm a pending event is reached and playable
- [x] 3.2 Run `openspec validate add-skill-based-field-entry`
