## Why

A tour pro is never alone — a coach shapes their development, a physio keeps them on the course, a caddie reads the greens. The sim has all the seams for this now (the Economy account has an unused `spend`, Health has a weekly recovery step, Progression has a development step) but no one to fill them: golfers improve and recover on their own, and money only ever leaves for entry fees. Support Team & Professional Staff is the layer that turns earnings into an investment decision and gives a career a human supporting cast: hire well and develop faster and stay healthier; overspend and the wages bite. It makes the money mean something and gives long careers a narrative — the long-serving coach, the trusted caddie, the new team after a promotion.

## What Changes

- Give every Professional Golfer a **Support Team** (REQ-191/192/200): zero or more independent `StaffMember`s across the five V1 roles — Coach, Caddie, Fitness Coach, Physiotherapist, Sports Psychologist — whose composition changes over a Career; the same staff member is not shared between golfers.
- Define **role responsibilities** (REQ-194) and **apply role influence** (REQ-197): a **Coach boosts long-term development** (scaling seasonal Development Points), and a **Fitness Coach / Physiotherapist speed recovery** (extra weekly fatigue recovery) — influence stays consistent with each role and **never directly modifies tournament results**. Caddie (strategic) and Sports Psychologist (mental) are modeled, costed, and historical; their shot-level effect is deferred until shot context is World-driven.
- Model **hiring, changes, and continuity** (REQ-195/196/199): a deterministic AI hiring policy hires staff considering **affordability and career stage**, replaces or releases staff under financial pressure, and favours continuity (gradual, one appointment at a time) — all effective only once the relationship is established.
- **Integrate with the Economy** (REQ-198): employing staff costs a one-off **hiring cost** and an ongoing **seasonal salary**, charged through the golfer's `FinancialAccount`, so staff are a real financial commitment.
- Preserve **staff relationship history** for **career narrative** (REQ-193/196/201): every appointment and departure is recorded permanently, and changing staff never invalidates history.
- **Wire staff into the World** (modifies `world-progression` and `player-development`): the World gives each golfer a Support Team, runs the seasonal staff cycle (pay salaries, release under pressure, hire per policy) through the Economy, applies the coach's development boost when developing attributes and the fitness/physio recovery boost each week — deterministically, keeping the world reproducible.

Explicitly out of scope, per REQ-197/202: staff **never directly modify tournament results, shot resolution, rankings, or player attributes** — they provide *influence* that the owning domains (Progression, Health) apply through their sanctioned mechanisms; and the Staff domain is **not responsible for tournament scoring, rankings, shot resolution, financial management, or player attributes**. Also deferred: the human-facing hire/fire UI (Presentation; a deterministic AI policy drives decisions for now), a global shared staff market / staff careers of their own, and the Caddie/Psychologist shot-level effects (no World-driven shot context yet). Staff decisions and generation are deterministic — drawn from an isolated seed stream — so a seeded world stays reproducible.

## Capabilities

### New Capabilities
- `support-team`: A per-golfer Support Team of zero or more independent staff members across the five V1 roles, whose composition changes over a Career; staff are independent entities not shared between golfers, each role carrying clearly defined responsibilities (REQ-191/192/194/200).
- `staff-relationships`: Ongoing professional relationships that begin and end over a Career, with every appointment and departure recorded permanently so changing staff never invalidates history — contributing to career narrative and continuity (REQ-193/196/199/201).
- `staff-influence`: Role-consistent influence on long-term career development and recovery/preparation that never directly modifies tournament results, hired by a deterministic policy considering affordability and career stage, with hiring costs and ongoing salaries integrated into the Economy (REQ-194/195/197/198).

### Modified Capabilities
- `world-progression`: The World gives each golfer a Support Team, runs the seasonal staff cycle (pay salaries and hiring costs through the Economy, release under financial pressure, hire per a deterministic policy), and applies staff influence — coach-boosted development and fitness/physio-boosted recovery — deterministically, keeping the world reproducible.
- `player-development`: Seasonal development MAY be enhanced by development support (a coach), scaling the Development Points awarded that season; without support, development is unchanged.

## Impact

- **Codebase**: New framework-free `com.progolf.sim.staff` package (`SupportTeam`, `StaffMember`, `StaffRole`, `StaffEffects`, `StaffRelationship`, `StaffMarket`, `HiringPolicy`, `StaffConstants`). `ProgressionEngine.develop` gains a support-factor overload. `World` gains a `Map<String, SupportTeam>` created at `admit`, a seasonal staff cycle charging the Economy, and application of staff effects to development and recovery. Two `TransactionType`s (`STAFF_HIRING`, `STAFF_SALARY`) are added for the ledger.
- **Determinism**: staff generation and hiring decisions draw from an isolated per-golfer/season seed stream (via `Seeds`/`SplitMix64Rng`); effects are pure multipliers/bonuses, so worlds stay reproducible.
- **DAG**: `staff` depends only on `core`. The World feeds it primitives (age, available funds) and reads back plain costs and effect factors, applying them to Economy/Progression/Health itself, so `staff` imports no other domain; only `world` imports `staff`.
- **Downstream consumers (future changes)**: the Caddie/Psychologist effects attach once shot context is World-driven; Media/legacy can surface long-serving-staff narratives; Presentation adds the hire/fire UI.
- **Boundary/risk**: staff are an influence layer, not a rules actor — they never touch scores/rankings/attributes directly; the only competitive effects are applied by the owning domains (Progression development, Health recovery) through public ops, and the seasonal cycle touches the World, whose full-run reproducibility test guards against regressions.
