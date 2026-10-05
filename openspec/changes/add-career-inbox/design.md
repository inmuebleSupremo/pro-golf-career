## Context

The World already generates the player-facing state an Inbox needs. At seasonal transition it banks Development Points, creates pending sponsorship offers, staff candidates, equipment upgrades and equipment-deal offers, and generates the next schedule. Those values are stored in the world snapshot and exposed through separate GraphQL reads. The current off-season review reports the completed season but embeds Development and Staff controls, omitting equivalent Finance/Sponsorship and Equipment attention.

`player-control` remains the governing behaviour: decisions are configured before a normal advance, and the world does not pause for management decisions except a player-entered event. The Inbox must not add a pause, a new decision path, or simulation state. The pure `sim.*` / application `app.*` boundary requires any aggregation, DTO, GraphQL resolver, and route to live outside the engine.

## Goals / Non-Goals

**Goals:**

- Surface a small, trustworthy set of current career decisions that a player could otherwise overlook.
- Route every item to its existing authoritative management screen.
- Make new-season attention coherent: complete the retrospective review, then arrive at the action index.
- Keep the feature additive, deterministic from existing state, and save/load-safe without a simulation migration.

**Non-Goals:**

- A general mailbox, social/conversation system, notification history, or replacement for world news.
- Item-level offers, a generic Inbox mutation API, read/unread state, dismissal, archive, or message persistence.
- Urgency levels, artificial deadlines, upcoming-major reminders, health prompts, or event-preparation reminders.
- A new time-advance gate or any change to sponsorship, staff, equipment, development, scheduling, or news rules.

## Decisions

### The MVP is a live, five-item maximum projection

The application shall derive at most one item for each qualifying source, in this fixed source order: Schedule, Development, Finances/Sponsorships, Equipment, Staff. It shall not create one item per offer or candidate. This gives the player a compact agenda rather than a notification feed.

| Source | Qualifying state | Item meaning | Destination | Relevance ends |
| --- | --- | --- | --- | --- |
| Schedule | Week 1, the player has at least one unplayed eligible event, and the Schedule review has not been acknowledged for this season | Review the new season’s schedule and entry/rest choices | Schedule | The player leaves Schedule after visiting it, week advances beyond 1, or no eligible event remains |
| Development | Banked Development Points can fund at least one permitted attribute raise (respecting the golfer's potential) | Allocate available points | Development | No further permitted raise can be funded after Apply |
| Finances/Sponsorships | One or more pending offers and an available sponsorship slot | Review offers that can be accepted | Finances | No pending offer, or sponsorship book becomes full |
| Equipment | At least one affordable equipment upgrade or a pending equipment brand deal | Review gear choices that can be taken now | Equipment | No qualifying option remains |
| Staff | At least one pending staff candidate is affordable and the current season's Staff review has not been acknowledged | Review the season's optional candidates | Staff | The Staff landing loads successfully, no affordable candidate remains, or a new season replaces the candidate set |

The source conditions deliberately use the state that actually determines whether the player can act. For example, a full sponsorship book or an unaffordable staff candidate is not a current actionable item. Offer expiry remains owned by the existing seasonal-transition logic; the Inbox only stops displaying it once the source no longer qualifies.

Alternative considered: show every offer/candidate and use Inbox as a detailed offer queue. Rejected: the established owning spokes already compare offers and explain affordability; item-level duplication would create noise and another interface to maintain.

### No generic Inbox persistence, read state, dismissal, or urgency in the MVP

Inbox is an answer to “what can I usefully act on now?”, not a historical record. It is recomputed from current world state on every read. A visible count is simply the number of currently qualifying source items. All MVP items use the same visual priority; the actual source state supplies the only timing rule (for example, Schedule at week 1 or offers that the world replaces next season).

This eliminates a generic message-history model, stale per-item read status, artificial deadlines, and a notification framework. Existing season review and world news retain the retrospective story.

Alternative considered: persistent messages with read/archive state and urgency categories. Deferred because no selected source needs it to be discoverable or actionable, and an acknowledged-but-unresolved item should not vanish.

### Schedule and Staff reviews have narrow, seasonal acknowledgements

Schedule differs from the decision-owning sources: reviewing the existing Schedule screen is itself the intended preseason task, rather than an offer or balance that can become zero. When a player enters the Schedule screen and then leaves it, the client SHALL call a narrowly named application mutation that records the current `scheduleReviewAcknowledgedSeason`. The Inbox excludes Schedule only when that stored season equals the world’s current season.

The value belongs to the application session/save wrapper, not `sim.*` or the world snapshot. It is persisted with the save solely so a reviewed Schedule does not reappear after loading the same season; it is not exposed to the player, not an Inbox item record, and does not affect entry, rest, simulation, or any other source. A new season has a different number and therefore automatically qualifies for a fresh Schedule review without needing to clear historical state.

Staff is similarly a review surface, but for a different product reason: hiring, firing, or retaining the current team are optional decisions. When the Staff landing successfully loads the season's candidate context, the client SHALL call a separately named application mutation that records the current `staffReviewAcknowledgedSeason`. The Inbox excludes Staff only when this stored season equals the world’s current season. It does not alter candidates, staffing, funds, or simulation. The World replaces the player’s candidate set at the next season transition, so a new season automatically receives a fresh Staff-review item when affordable candidates exist.

Both values belong to the application session/save wrapper, not `sim.*` or the world snapshot. They are persisted with the save solely so reviewed Schedule or Staff items do not reappear after loading the same season; they are not exposed to the player, are not Inbox item records, and do not affect the owning domain or another source.

Alternative considered: keep these acknowledgements only in browser memory. Rejected because page reload or save/load would cause a reviewed item to reappear during the same season, breaking the expected completion flow. Alternative considered: general Inbox read state. Rejected because acknowledgements remain limited to the two sources whose intended task is review rather than domain resolution.

### Inbox is navigation-led; existing spokes own decisions

An item carries only its typed source and count. The frontend maps that type to the existing route and renders the source-specific title/body. Accepting sponsorship, buying equipment, hiring staff, allocating points, setting rest, and choosing events remain existing mutations in their current screens.

Alternative considered: render action controls in Inbox. Rejected because it would duplicate validation, explanation, error handling, and mutation semantics across the owning screens.

### One additive application-layer GraphQL read model

Add `careerInbox(id): CareerInbox!`, scoped to the authenticated owner like other session queries. Its return is `CareerInbox { items: [CareerInboxItem!]! }`; an item is `{ kind: CareerInboxKind!, count: Int! }`, where `kind` is one of `SCHEDULE`, `DEVELOPMENT`, `SPONSORSHIP`, `EQUIPMENT`, or `STAFF`. `count` is the source quantity in the table above (events, points, actionable offers/options, or candidates). Add separately named, authenticated `acknowledgeScheduleReview(id)` and `acknowledgeStaffReview(id)` mutations for the corresponding review lifecycles. They return success only, do not accept item ids, and do not provide general Inbox controls.

`WorldService` assembles this projection from the current world state. The API controller maps it to DTOs; no engine record appears in GraphQL. One aggregate read keeps the sidebar count, Inbox page, and off-season handoff consistent and avoids the frontend composing five independently stale queries.

### Retain the off-season review, remove its embedded decision ownership

The off-season route remains the existing retrospective review of completed results, ranking movement, development changes, and headlines. It shall remove the embedded Development and Staff decision components. Its exit action becomes “Continue”: when Inbox contains items it goes to Inbox; otherwise it goes to the hub. The Inbox is not a new gate—the player can leave it without resolving anything, and normal time advance remains unchanged.

This gives all source areas equal discoverability and corrects the current timing language: the world has already advanced before the review is shown, so “Begin Season” is misleading.

## Risks / Trade-offs

- **An item can disappear before a player opens it** → This is accurate for a live attention index; no action is lost because the owning domain’s existing expiry rule is unchanged.
- **Schedule prompt is too brief** → It intentionally appears only at week 1, the new-season planning moment. A player can always inspect Schedule directly later; no permanent calendar reminder is added.
- **Affordability changes after the read** → The owning mutation remains authoritative and revalidates the action. Refetch Inbox after relevant mutations.
- **MVP omits health and event timing** → Defer them until evidence shows they are actionable and not redundant with Fitness, Schedule, and Play Event.
- **Source-specific copy moves into the client** → A fixed enum and count is smaller and more stable than server-authored message text; frontend route/copy mappings must be exhaustive.

## Migration Plan

No simulation or snapshot migration is required. Add the API and UI additively, with existing management spokes and current saved worlds unchanged. The optional application save fields for acknowledged Schedule and Staff seasons are null for existing saves and have no effect until a player completes the corresponding review. On save/load, the Inbox recomputes from restored world state plus those narrowly scoped seasonal acknowledgements. Rollback consists of removing the Inbox UI/read use; no player decision or simulation state needs rollback.

## Deferred After MVP

- Persistent message history, read/unread/archived state, dismissal, and acknowledgement for sources other than Schedule and Staff.
- Item-level offer/candidate messages or direct deep-links to a selected offer.
- Urgency, deadlines, and categories.
- Health/recovery, upcoming-event, major, workload, objective, or narrative prompts.
- Any explicit decline action for offers. Existing lapse behaviour remains authoritative unless a separate domain change proposes otherwise.
