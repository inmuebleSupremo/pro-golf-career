## Why

An entered event in Week 1 is ready as soon as the first Advance Week pauses the world, but the client immediately navigates from the Hub to gameplay. The same redirect applies to any same-season pending event. The established play flow calls for the Hub to surface a Play Event entry so the player chooses when to enter.

## What Changes

- Keep the player on the Career Hub after Advance Week creates a pending event in the current season.
- Use the existing Play Event action to enter gameplay deliberately.
- Cover Week 1 and later-week pending events while preserving the Season Review transition.

## Capabilities

### Modified Capabilities

- `web-play`: clarify the Hub handoff when Advance Week reaches a tournament.

## Impact

Frontend navigation only. Career creation, backend week and event state, GraphQL, saves, simulation, event completion, and Season Review behavior are unchanged.
