## Context

The seams for a support team already exist: `FinancialAccount.spend`/`charge` (Economy) is unused for staff, `HealthSystem.recoverWeek` runs weekly, and `ProgressionEngine.develop` runs each season. This change adds a `sim.staff` domain that owns each golfer's Support Team (roles, quality, costs, effects, history), then wires it into the World: salaries and hiring flow through the Economy, and role effects are applied to Progression (coach → development) and Health (fitness/physio → recovery). Locked model: weekly turn-based world, seasonal seams, deterministic AI policies (same pattern as the economy acceptance policy and progression allocation policy).

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: effects are pure factors; the only randomness (staff generation, hiring choice) draws from the world seed hierarchy (`Seeds`/`SplitMix64Rng`), never `java.util.Random`.

## Goals / Non-Goals

**Goals:**
- A per-golfer Support Team of independent, role-typed staff members with quality, costs, and defined responsibilities.
- Deterministic hiring/firing considering affordability and career stage; continuity (gradual change); permanent appointment/departure history.
- Economic integration: hiring cost + ongoing salary charged through the `FinancialAccount`.
- Role-consistent influence applied by the World: coach → development, fitness/physio → recovery; never modifying tournament results directly.
- Wire into the World deterministically; keep it reproducible.

**Non-Goals:**
- Human-facing hire/fire UI (Presentation; AI policy drives decisions now).
- Caddie (strategic) and Sports Psychologist (mental) shot-level effects — modeled/costed/historical, but their application awaits World-driven shot context.
- A global shared staff market or staff having their own careers (staff are per-golfer entities, not globally unique — REQ-200).
- Staff directly modifying scores, rankings, or attributes (forbidden, REQ-197/202).

## Decisions

### D1. `sim.staff` depends only on `core`; the World applies effects
The Staff domain owns Support Team state and exposes plain costs (`double`) and effect factors (a `StaffEffects` bundle). It imports only `core`. The World feeds it primitives (age, available funds), reads back costs/effects, and does the cross-domain work itself: charging the `FinancialAccount`, scaling `ProgressionEngine.develop`, and boosting `HealthSystem` recovery. *Why:* REQ-202 independence and the established pattern (economy/health depend only on core; the World orchestrates). *Alternative rejected:* staff importing economy/health/progression to apply its own effects — inverts the boundary and couples the domain to its consumers.

### D2. Support Team is a per-golfer aggregate keyed by role; history is append-only
`SupportTeam` holds at most one `StaffMember` per `StaffRole` (current team) plus an append-only `List<StaffRelationship>` (every appointment, closed on departure). `hire(member, season)` and `release(role, season)` mutate the current team and record history. *Why:* REQ-191/193/196 — current team accurately maintained, history preserved, changing staff never invalidates it. *Alternative rejected:* multiple members per role — V1 defines one relationship per role; simpler and matches the responsibilities list.

### D3. Effects are pure, role-consistent factors aggregated across the team
Each `StaffMember` has a `quality` in [0,1]; `SupportTeam.effects()` aggregates employed roles into a `StaffEffects` — `developmentBonus` (coach), `recoveryBonus` (fitness + physio), and `mentalSupport`/`strategicSupport` (psychologist/caddie, exposed but not yet applied). Effects scale with quality. *Why:* REQ-194/197 — influence consistent with each role, quality-scaled, and read-only data the World applies. *Alternative rejected:* staff mutating attributes/results directly — violates REQ-197/202.

### D4. Deterministic hiring policy gated by affordability and career stage
`HiringPolicy.chooseHire(team, age, availableFunds, rng)` returns an optional role to hire: it targets a team size that grows with career stage (a developing golfer prioritises a Coach; established golfers fill out the team), hires at most one role per season (continuity, REQ-199), and only when the golfer can afford the hiring cost plus a season of salary. `StaffMarket.generate(role, rng)` produces a candidate (quality, costs scaled by quality). *Why:* REQ-195 (affordability, career stage), REQ-199 (gradual continuity). *Alternative rejected:* hiring everything affordable at once — no continuity, unbounded wage bill.

### D5. Economy integration: mandatory salaries, discretionary hiring, release under pressure
Each season the World charges every employed member's salary (`FinancialAccount.charge` with a new `STAFF_SALARY` type — mandatory, may go into debt), then, if funds fall below a release threshold, releases the most expensive members (recorded departures) until sustainable; then the hiring policy may sign one new member if the hiring cost is affordable (`FinancialAccount.spend` with `STAFF_HIRING` — discretionary, declined if unaffordable). *Why:* REQ-198 (hiring costs + ongoing commitments integrate with the Economy), REQ-196 (staff changes), and the Economy's integrity rules (mandatory vs discretionary). *Alternative rejected:* all-discretionary salaries — a golfer could simply never pay wages; mandatory salaries create the real cost pressure.

### D6. Coach boosts development via a Progression overload
`ProgressionEngine.develop(current, age, supportFactor)` scales the season's awarded Development Points by `supportFactor` (`develop(current, age)` delegates with `1.0`). The World passes `1.0 + effects.developmentBonus()`. *Why:* REQ-194/197 — the coach's defined responsibility is long-term development; scaling awarded DP is the sanctioned Progression mechanism (attributes still change only via `Player.evolveAttributes`). *Alternative rejected:* the World hand-rolling an extra allocation — duplicates the allocation policy; staff writing attributes — violates REQ-202.

### D7. Fitness/physio boost recovery via an extra weekly fatigue reduction
In `recoverHealth`, after `HealthSystem.recoverWeek`, the World applies `effects.recoveryBonus()` as additional fatigue reduction via `PhysicalState.withFatigue` (a public op). *Why:* REQ-194/197 — physical preparation and recovery support; keeps the Health API unchanged (no health capability modified). *Alternative rejected:* changing `recoverWeek`'s signature — a wider modification than reusing the public state update.

### D8. World wiring: team at admit, seasonal staff cycle, effect application
`admit` creates an empty `SupportTeam`. In `seasonalTransition`, per surviving golfer (in the existing financial loop): run the staff cycle (pay/ release/ hire) through the account, deterministically seeded per golfer/season. `evolveGolfer` reads the team's development bonus; `recoverHealth` reads the recovery bonus. Career stage/age from `Career.age()`. *Why:* REQ-195/196/198 and the living-world goal; REQ-299 reproducibility. *Alternative rejected:* a staff step inside `Career`/`Economy` — those must not own staff (REQ-202).

## Risks / Trade-offs

- **[Staff becoming a rules actor]** → `staff` imports only `core`; it returns costs and effect factors; the World applies them through public ops of the owning domains; an architecture/boundary test asserts staff touches no score/ranking/attribute, and effects never modify tournament results (REQ-197/202).
- **[Determinism across the world]** → generation and hiring draw from an isolated seeded stream; effects are pure; the existing world reproducibility test (two seeds → identical world) covers it, extended to staff state.
- **[Runaway wage bills / everyone broke]** → salaries are mandatory but the hiring policy only signs when affordable and releases under pressure; magnitudes isolated in `StaffConstants`; tests assert structural properties (afford → hire, unaffordable → skip/release, history preserved), not magnitudes.
- **[Two roles without mechanical effect]** → Caddie/Psychologist are costed, historical, and expose effect channels not yet applied (no World-driven shot context); documented as a V1 limitation, spec-legal (REQ-194 implementation not prescribed).
- **[Modifying Progression]** → the `develop` overload is additive and backward-compatible (`develop(current, age)` delegates with factor 1.0); existing progression tests are unaffected.

## Migration Plan

Greenfield `sim.staff` + additive modifications (`ProgressionEngine.develop` overload; two `TransactionType`s; World staff wiring). Sequencing: (1) `StaffConstants`, `StaffRole` (with responsibility), `StaffMember`, `StaffEffects`; (2) `StaffRelationship` + `SupportTeam` (hire/release/effects/history); (3) `StaffMarket` (generate candidate) + `HiringPolicy` (affordability + stage, one-per-season); (4) `ProgressionEngine.develop(current, age, factor)` overload (modifies player-development); add `STAFF_HIRING`/`STAFF_SALARY` types; (5) wire the World — team at `admit`, seasonal staff cycle through the account, coach→development in `evolveGolfer`, fitness/physio→recovery in `recoverHealth`, a read-only `supportTeamOf` accessor; (6) tests — team composition/independence; hire/release/history; effects aggregate by role and quality; deterministic policy gated by affordability/stage; economy integration (salary charged, hiring spent/declined); boundary; progression overload scales development; world runs the cycle and stays reproducible. Each layer testable before the next.

## Open Questions

- Exact magnitudes (quality distribution, hiring cost and salary by role/quality, development and recovery bonus per quality, target team size by stage, release threshold) — placeholder `StaffConstants`, tuned later.
- Whether staff quality should correlate with a golfer's reputation/means — random in V1; a later change can gate better staff behind success.
- Whether staff history folds into `Career` history or stays in `SupportTeam` — kept in the team now; a later legacy/Media change can surface it.
- When Caddie/Psychologist effects attach — when the World drives per-shot context (pressure/strategy); the effect channels already exist.
