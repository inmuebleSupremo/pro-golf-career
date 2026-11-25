## Why

After a player event completed, the career overview refreshed while cached profile and career-record reads could still show the previous bankroll, rank, and last result. The shared query client treats those reads as fresh for 30 seconds, and returning to the hub does not guarantee a refetch. Reopening the saved career creates new query keys and hides the discrepancy.

## What Changes

- Invalidate the current session's profile and career-record queries after successful event completion.
- Invalidate the current session's profile after successful week advancement, which can change its ranking or finances.
- Add focused cache tests that reproduce the stale reads and guard session scoping.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `web-data-access`: successful career progression must refresh the affected cached player summary reads.

## Impact

- Frontend query invalidation only. No simulation, backend, GraphQL, save format, navigation, or season-operation changes.
- Active affected queries refetch; inactive affected queries are marked stale for their next use. An in-flight refetch may briefly show previous values.
