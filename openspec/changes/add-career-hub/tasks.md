## 1. Typed operations & label data

- [x] 1.1 Author the `load` mutation and a combined `CareerOverview` query (`world` + `careerGoals` + `playerSchedule` for one `$id`); run codegen and confirm they type-check against the schema
- [x] 1.2 Add a labels module mapping `GoalType`, `TourTier`, and `EventPrestige` enum names to display labels, plus a helper to classify a goal as boolean-style vs targeted and format its progress (money for `CAREER_EARNINGS`)

## 2. Resume action & data hooks

- [x] 2.1 Implement a `useLoadCareer` TanStack Query mutation wrapping `load(saveId)`; on success navigate to `/career/[returned session id]`; map failures to a friendly message
- [x] 2.2 Implement a `useCareerOverview(id)` query hook returning the combined overview; expose loading/error and detect the not-found (NOT_FOUND classification) case

## 3. Career hub (impeccable)

- [x] 3.1 Design the career-hub layout with the `impeccable` skill (world-status header, goals with progress, upcoming schedule) in the editorial/premium token direction; note the flagged no-golfer-profile gap
- [x] 3.2 Build the `/career/[id]` route under the protected `(app)/` group (await the async route params) rendering the overview via `useCareerOverview`
- [x] 3.3 Build the world-status header (season · week · active field) and the career-goals section (boolean vs targeted rendering, achieved treatment, empty state), reusing token-driven primitives
- [x] 3.4 Build the upcoming-schedule section (next events: week, tour-tier label, prestige label, entered/skipped), with an empty state
- [x] 3.5 Implement the expired/unknown-session state (NOT_FOUND error) explaining the session is gone with a link back to `/saves`; plus loading and error states

## 4. Entry point from saves

- [x] 4.1 Add a "Resume" control to each save row on the saves screen that triggers `useLoadCareer`; show a pending state on the acting row and keep the player on saves if it fails

## 5. Verification

- [x] 5.1 Run typecheck, lint, and codegen clean
- [x] 5.2 Verify end to end against a running backend: create a career (with goals via the API where needed), resume it from saves, and confirm the hub shows the correct season/week, goals with progress, and upcoming schedule
- [x] 5.3 Verify the read-only guarantee (no advance/play/edit controls), the empty-goals state, and the expired-session state (open `/career/<bogus-id>` → guided back to saves); confirm a resume failure keeps the player on saves
- [x] 5.4 Confirm the impeccable design hook is clean across the new UI files; verify keyboard navigation, focus states, and reduced-motion behaviour
- [x] 5.5 Run `openspec validate add-career-hub` and resolve any issues
