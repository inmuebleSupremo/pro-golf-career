## Why

Career mode already produces meaningful player decisions, especially at a season boundary, but their discovery is fragmented. Pending sponsorship, staff, equipment, development, and schedule decisions live in separate spokes; a player who does not visit each one can miss an opportunity without making an intentional choice.

The current off-season review makes this more visible: it combines a valuable retrospective with embedded Development and Staff controls, while equivalent Finance/Sponsorship and Equipment opportunities remain elsewhere. A personal Inbox can make attention visible without creating a second rules system or turning career mode into a generic messaging product.

## What Changes

- Add a Career Inbox: a personal, action-oriented view of meaningful career decisions and opportunities, with context and a route to the existing section that owns the decision.
- Establish the Inbox as distinct from the world/news feed: news reports simulation events; Inbox highlights the player's current attention items.
- Make the Inbox discoverable from the normal career experience, including a meaningful indication when attention is available.
- Define an intentional relationship between the completed-season/off-season review and next-season attention: the review remains retrospective, while its exit directs the player to Inbox when current attention exists.
- In the MVP, derive one compact Inbox item per qualifying source area: new-season Schedule review, unspent Development Points, actionable Sponsorship offers, affordable Equipment options, and affordable Staff candidates.
- Keep the MVP current-state only: no message history, read/unread state, dismissals, urgency taxonomy, or generic notification framework.
- Preserve the existing player-control model: Inbox directs the player to authoritative decisions; it does not introduce alternate mutations, simulation rules, or new time-advance gates.

## Capabilities

### New Capabilities

- `career-inbox`: a personal, current-state view of meaningful player attention that explains the item and directs the player to the authoritative career-management destination.

### Modified Capabilities

- `web-career`: the career experience gains a discoverable Inbox entry point and a clear handoff from the seasonal flow when personal attention is available.
- `graphql-api`: an additive, authenticated read query exposes the compact career Inbox projection without exposing simulation types.

## Impact

- **Frontend:** a career Inbox route/entry point, attention indication, and possible off-season review simplification. Existing Schedule, Development, Finances, Equipment, and Staff spokes remain the decision surfaces.
- **Backend app/API:** an additive, session-scoped `careerInbox` read model assembled at the `WorldService`/application boundary. It returns compact typed source/count items and has no mutation.
- **Simulation/persistence:** no Inbox simulation state or message persistence. The projection derives from already persisted world state, so save/load behaviour is unchanged.
- **Specifications:** the new capability constrains scope and authority; the existing `player-control` specification remains the source of truth for how offers and standing decisions behave.
