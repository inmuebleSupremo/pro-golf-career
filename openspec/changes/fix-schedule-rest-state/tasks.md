## 1. Regression

- [x] 1.1 Reproduce the Schedule remount mismatch with a focused frontend test.
- [x] 1.2 Cover GraphQL decision read-back and named-save restore in isolated backend tests.

## 2. Correction

- [x] 2.1 Add the owner-scoped read field for the existing world decision.
- [x] 2.2 Bind Schedule to that read field and refresh it after a successful mutation.

## 3. Verify

- [x] 3.1 Run focused tests, frontend codegen/typecheck, backend tests, and strict OpenSpec validation. The full backend suite also ran but failed on assertions outside the changed rest-state path; see the final verification report.
- [x] 3.2 Record minimum browser acceptance for navigation, refresh, and reopening a save.

Browser acceptance: in an isolated test career, select “Rest this season” on Schedule, navigate away and back, refresh Schedule, then save and reopen that career. Each time, the control should read “Resume playing” and the text should state the golfer is resting. Select “Resume playing” and confirm the playing state survives the same navigation and refresh. No live career data is required.
