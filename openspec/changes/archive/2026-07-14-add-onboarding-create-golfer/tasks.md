## 1. Typed operations & option data

- [x] 1.1 Author the `createWorld`, `createPlayer`, and `save` GraphQL operations (via the generated `graphql()`); run codegen and confirm they type-check against the schema
- [x] 1.2 Add a frontend constants module for onboarding options: the 16 nationalities (with display labels) and the 5 playing-style archetypes (enum name + label + one-line play-style description from strengths/weaknesses); include the age bounds (16–22)

## 2. Career-creation orchestration

- [x] 2.1 Implement a `useCreateCareer` TanStack Query mutation that runs `createWorld(random seed)` → `createPlayer(worldId, …)` → `save(worldId, generated saveId)` in sequence and returns the created golfer summary
- [x] 2.2 Generate the random seed (Long-as-string) and a unique `saveId` (`crypto.randomUUID()`); map backend BAD_REQUEST/other failures to friendly messages; on success, invalidate the `saves` query

## 3. Onboarding flow (impeccable)

- [x] 3.1 Design the create-your-golfer form and the confirmation moment with the `impeccable` skill (archetype selection with trade-offs, the "your golfer is ready" reveal), in the editorial/premium token direction
- [x] 3.2 Build the onboarding route under the protected `(app)/` group (e.g. `/new`) using React Hook Form + Zod: identity (first/last name), nationality select, archetype selection, starting-age input constrained to 16–22
- [x] 3.3 Reuse the existing token-driven shadcn/Radix primitives (Button, Input, Label, Field); add only new primitives if genuinely required (e.g. a Select/RadioGroup) and re-skin them to the tokens
- [x] 3.4 Implement the confirmation state showing the created golfer (name, archetype, nationality, age) with a "View your saves" action that navigates to `/saves` and refreshes it
- [x] 3.5 Handle submission states: pending on submit, field-level validation errors, and a form-level error for backend failures with retry

## 4. Entry point from saves

- [x] 4.1 Add a "Start a new career" CTA to the saves screen — primary action in the empty state, secondary action on the populated list — linking to the onboarding route

## 5. Verification

- [x] 5.1 Run typecheck, lint, and codegen clean
- [x] 5.2 Verify end to end against a running backend: from an empty saves list, create a golfer (each of a couple of archetypes), confirm the created-golfer screen, return to saves and see the new "Career in progress" entry; confirm a second creation makes a distinct save
- [x] 5.3 Verify validation (missing name, out-of-range age, no archetype) blocks submission, and that a simulated backend failure surfaces a friendly error with retry (no orphan save persists)
- [x] 5.4 Confirm the impeccable design hook is clean across the new UI files; verify keyboard navigation, focus states, and reduced-motion behaviour on the form and archetype selection
- [x] 5.5 Run `openspec validate add-onboarding-create-golfer` and resolve any issues
