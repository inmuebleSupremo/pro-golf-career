## 1. Reproduce and correct

- [x] 1.1 Trace creation, week advancement, event scheduling, pending-event state and navigation against the canonical play flow.
- [x] 1.2 Reproduce direct gameplay navigation for Week 1 and a later pending event with focused tests.
- [x] 1.3 Keep the player on the Hub after a same-season advance; retain the existing Play Event action and Season Review transition.

## 2. Verify

- [x] 2.1 Run focused frontend tests, typecheck, targeted lint and strict OpenSpec validation.
- [ ] 2.2 Owner browser acceptance: create a career, advance Week 1 to an entered event, confirm the Hub remains visible with Play Event, enter it deliberately, and repeat at a later event. Confirm the existing Continue Event, Finish Event and Season Review routes still work.

Focused verification: 20 tests passed across Hub progression, Hub dashboard, sidebar and career progression cache. The Season Review transition fixture now stubs its unrelated operation-status read, and its 3 tests pass. Typecheck, targeted lint and strict OpenSpec validation passed. The owner browser step remains open.
