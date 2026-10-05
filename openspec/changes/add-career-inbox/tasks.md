## 1. Application Inbox projection and API

- [x] 1.1 Add a small application-layer Inbox projection that derives the five specified source items from the current `World` state, without changing `sim.*` rules or adding persisted Inbox state.
- [x] 1.2 Add application DTOs for `CareerInbox` and its typed, counted items; add the authenticated, read-only `careerInbox` GraphQL query and schema types.
- [x] 1.3 Add backend tests for every source qualification and relevance end condition, source aggregation (one item per area), empty Inbox, ownership, read-only behaviour, and no engine-type leakage.
- [x] 1.4 Verify that Inbox recomputes correctly after save/load from existing persisted world state; no snapshot migration is introduced.
- [x] 1.5 Derive Development eligibility from allocatable applied state, and add the narrowly scoped, save/load-safe Schedule-review acknowledgement in the application layer that excludes only the acknowledged current season from Schedule Inbox eligibility.
- [x] 1.6 Add a narrowly scoped, save/load-safe Staff-review acknowledgement that excludes only the acknowledged current season from Staff Inbox eligibility.

## 2. Career Inbox experience

- [x] 2.1 Add the Career Inbox route, query binding, source-specific copy, routes to existing owning spokes, and loading/error/empty states using the established frontend design system.
- [x] 2.2 Add an Inbox entry to normal career navigation with the current item count only when the Inbox is non-empty.
- [x] 2.3 Refetch or invalidate Inbox after the existing relevant mutations and time advances so its count and page stay current.
- [x] 2.4 Record Schedule review completion when the player leaves Schedule, then refresh the shared Inbox result used by its page and navigation count.
- [x] 2.5 Record Staff review completion after the Staff landing successfully loads, then refresh the shared Inbox result used by its page and navigation count.

## 3. Off-season handoff

- [x] 3.1 Preserve the completed-season recap, remove its embedded Development and Staff decision components, and replace the misleading post-transition "Begin Season" wording with a continuation action.
- [x] 3.2 On continuation, route to Inbox when it contains items and otherwise route to the career hub; do not require a player to resolve Inbox items.

## 4. Verify

- [x] 4.1 Run backend tests, including architecture-purity and save/load regression coverage; run frontend codegen, typecheck, lint, and build.
- [x] 4.2 Manually verify a week-1 Inbox with all qualifying source areas, each source becoming non-qualifying after its state changes, an empty Inbox, normal navigation/count behaviour, seasonal handoff, and separation from world news.
- [x] 4.3 Add and run refinement coverage for applied Development completion, seasonal Schedule acknowledgement (including save/load), and synchronized Inbox/page navigation counts.
- [x] 4.4 Add and run coverage for Staff acknowledgement, its seasonal save/load scope, and shared Inbox/sidebar count consistency.
