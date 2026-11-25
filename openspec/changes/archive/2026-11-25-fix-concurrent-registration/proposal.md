## Why

Two requests can both pass the registration availability check before either writes its account file. The service then persists two different account ids with the same username, contrary to the existing authentication requirement.

## What Changes

- Make new-account creation an atomic check-and-write operation in the filesystem user-store adapter for one backend instance.
- Keep the existing availability check as an early rejection, while the store performs the authoritative check.
- Add a deterministic concurrent regression test and verify the existing registration HTTP contract.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `authentication`: clarify that username uniqueness also holds for simultaneous registration requests.

## Impact

- Application-layer auth storage only; no API, account file format, simulation, dependency, or authentication-architecture change.
- Coordination is limited to contenders for the same username within the existing single-instance backend. Multiple backend processes sharing one user directory remain outside this change.
