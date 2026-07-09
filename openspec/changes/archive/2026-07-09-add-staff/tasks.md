## 1. Staff model

- [x] 1.1 Create framework-free package `com.progolf.sim.staff` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `StaffConstants` (quality distribution, hiring cost + seasonal salary by role/quality, development + recovery bonus per quality, target team size by career stage, release threshold, staff seed salt) as the single tunables surface.
- [x] 1.3 Define `StaffRole` (COACH, CADDIE, FITNESS_COACH, PHYSIOTHERAPIST, SPORTS_PSYCHOLOGIST) each with a defined responsibility, and an immutable `StaffMember` (role, name, quality, hiring cost, seasonal salary).
- [x] 1.4 Define `StaffEffects` (developmentBonus, recoveryBonus, mentalSupport, strategicSupport) as the aggregated, quality-scaled, read-only influence of a team.

## 2. Support team & relationships

- [x] 2.1 Define `StaffRelationship` (role, staff name, quality, start season, optional end season) for history.
- [x] 2.2 Implement `SupportTeam` — at most one member per role; `hire(member, season)`, `release(role, season)`, `has(role)`, `size()`, `members()`, `effects()`, and an append-only `history()` that records every appointment and departure and is never invalidated by changes.

## 3. Market & hiring policy

- [x] 3.1 Implement `StaffMarket.generate(role, rng)` — a candidate with quality and costs scaled by quality; deterministic.
- [x] 3.2 Implement `HiringPolicy.chooseHire(team, age, availableFunds, rng)` — an optional role to hire: target team size grows with career stage, at most one hire per season (continuity), only when the hiring cost plus a season of salary is affordable; and `chooseRelease(team, availableFunds)` — the member(s) to release under financial pressure.

## 4. Progression overload & economy types (modified)

- [x] 4.1 Add `ProgressionEngine.develop(current, age, supportFactor)` scaling awarded Development Points; make `develop(current, age)` delegate with factor `1.0` (modifies player-development).
- [x] 4.2 Add `TransactionType.STAFF_HIRING` and `STAFF_SALARY` (Category.EXPENSE) for the ledger.

## 5. World wiring (modified: world-progression)

- [x] 5.1 In `admit`, create an empty `SupportTeam` for each golfer; add a read-only `supportTeamOf(id)` accessor.
- [x] 5.2 In `seasonalTransition` (per surviving golfer), run the staff cycle: charge each employed member's salary (`STAFF_SALARY`, mandatory), release the most expensive members while funds are below the release threshold (recorded departures), then hire one member per `HiringPolicy` if affordable (`STAFF_HIRING`, discretionary `spend`).
- [x] 5.3 In `evolveGolfer`, scale development by the team's development bonus via `ProgressionEngine.develop(attrs, age, 1 + bonus)`.
- [x] 5.4 In `recoverHealth`, apply the team's recovery bonus as extra weekly fatigue reduction via `PhysicalState.withFatigue`.
- [x] 5.5 Derive the staff seed stream from the world hierarchy with a dedicated salt (isolated from other domain streams); age from `Career.age()`.

## 6. Verification

- [x] 6.1 Support-team tests: a golfer starts with an empty team; hiring adds a member; at most one per role; roles carry defined responsibilities; a member belongs to one team.
- [x] 6.2 Relationship tests: appointments and departures are recorded; replacing a member keeps the former relationship in history and updates the current team; a multi-season relationship's duration is recoverable.
- [x] 6.3 Effect tests: `StaffEffects` aggregate by role and scale with quality (coach ⇒ development bonus; fitness/physio ⇒ recovery bonus).
- [x] 6.4 Policy tests: hiring is declined when unaffordable and deterministic given the same inputs; releases occur under financial pressure; at most one hire per season.
- [x] 6.5 Progression test: `develop(current, age, factor>1)` develops more than the unsupported baseline; `develop(current, age)` equals `develop(current, age, 1.0)`.
- [x] 6.6 Boundary test: the Staff domain changes no score, ranking, or attribute state (REQ-197/202); `sim.staff` imports only `core`.
- [x] 6.7 World tests: golfers acquire staff over seasons, salaries are charged through the account, staff effects apply (development/recovery), history is preserved, and two worlds with the same seed produce identical support teams and history (reproducible).
- [x] 6.8 Run `openspec validate add-staff --type change --strict` and resolve findings.
