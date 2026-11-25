## 1. Investigate and reproduce

- [x] 1.1 Trace event completion, week advancement, query keys, navigation, and backend player-profile/record contracts.
- [x] 1.2 Reproduce the stale profile and career-record keys with a focused frontend cache test before correction.

## 2. Correct

- [x] 2.1 Invalidate only the active session's affected queries after successful completion or advancement.
- [x] 2.2 Verify an independently loaded session's cache remains unaffected.

## 3. Verify

- [x] 3.1 Run focused progression, event-completion, and season-control frontend tests.
- [x] 3.2 Run frontend typecheck and targeted lint.
- [x] 3.3 Validate the OpenSpec change.
