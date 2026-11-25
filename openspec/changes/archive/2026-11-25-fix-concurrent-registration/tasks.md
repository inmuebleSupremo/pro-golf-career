## 1. Reproduce

- [x] 1.1 Add a deterministic simultaneous-registration test using one username and verify that the existing implementation creates two accounts.

## 2. Correct

- [x] 2.1 Add atomic new-account creation to the user-store port and filesystem adapter, keeping other usernames independent.
- [x] 2.2 Route registration through that operation without changing existing validation, storage, or HTTP errors.

## 3. Verify

- [x] 3.1 Confirm one success, one conflict, and one persisted account for the same username; confirm distinct usernames can register concurrently.
- [x] 3.2 Run focused filesystem, authentication flow, JWT, and HTTP error-status tests.
- [x] 3.3 Validate the OpenSpec change.
