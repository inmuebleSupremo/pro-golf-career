## Why

Schedule initializes its rest control to playing on each mount, although the world retains the player's blanket rest choice. A returning player can see and submit the opposite decision from the one the simulation will apply.

## What Changes

- Expose the existing standing rest decision through one owner-scoped GraphQL read field.
- Render the Schedule rest control from that field and refresh it after a successful change.
- Cover remount, read-back, and named-save restore.

## Capabilities

### Modified Capabilities

- `web-career`: Schedule displays the authoritative standing rest choice.
- `graphql-api`: an additive read field exposes that choice.

## Impact

No simulation, snapshot, save format, event-entry, or navigation rules change.
