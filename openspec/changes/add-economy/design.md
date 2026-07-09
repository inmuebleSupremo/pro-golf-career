## Context

Prize money is already computed (`PrizeStructure.amountForPosition` → `TournamentResult.Finish.prize`) and tallied read-only in `CareerStatistics.totalEarnings`, but there is no spendable financial identity, no expenses, and no commercial layer. This change adds a `sim.economy` domain: a per-golfer `FinancialAccount` with an auditable ledger, career expenses, and a sponsorship system (reputation-gated offers, objective-bearing agreements, deterministic AI decisions), then wires it into the World's event resolution and seasonal transition. The locked world model is weekly turn-based with clean seasonal seams, and AI is driven by deterministic policies (same pattern as `player-development`'s allocation policy).

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: amounts are pure; the only randomness (offer generation, commercial-reputation variation) draws from the world seed hierarchy (`Seeds`/`SplitMix64Rng`), never `java.util.Random`.

## Goals / Non-Goals

**Goals:**
- A per-golfer `FinancialAccount` (funds, career/tournament/sponsorship earnings, expenses) with an auditable transaction ledger and economic integrity (no discretionary overspend; defined negative-balance rule for mandatory obligations).
- Automatic prize awards and career-expense charges per event; permanent financial history and milestones.
- Sponsorship: reputation-gated offers, agreements carrying performance objectives that are evaluated (bonuses / renewal), and agreements that begin and conclude across a career.
- Deterministic AI acceptance/management with real trade-offs; financial opportunity broadly reflecting competitive success.
- Wire into the World deterministically; keep it reproducible.

**Non-Goals:**
- Human-facing money/offer UI (Presentation; AI policy drives decisions now).
- Staff, equipment, or other spendable systems (separate deferred domains) — the account is built to receive their debits later, but this change adds none.
- Investment/expansion systems and real-currency/tax modelling (REQ-181 "future expansion").
- Any economic effect on attributes, outcomes, rankings, or shot resolution (forbidden, REQ-185).

## Decisions

### D1. `sim.economy` depends only on `core`; the World feeds it primitives
The Economy owns financial machinery and imports only `core` (RNG/seeds). It never imports `tournament`, `ranking`, `career`, or `world`. The World passes in primitives: a prize amount (`double`), a competitive-reputation score (`double`), and a `PerformanceSnapshot` (a plain record it builds from career/ranking). *Why:* REQ-185/190 independence — the Economy is a consumer of results, not an authority over them, and cannot reach into other domains. *Alternative rejected:* Economy importing `ranking`/`career` to read reputation/performance — inverts the boundary and couples the domain to consumers it must not own.

### D2. `FinancialAccount` is the single spendable aggregate; the ledger is the source of truth
`FinancialAccount` holds `availableFunds` and an append-only `List<Transaction>`; the category totals (career earnings, tournament earnings, sponsorship income, career expenses) are folded as transactions post. Every mutation goes through one guarded path that records a `Transaction` (sequence, date, type, signed amount, resulting balance, description). *Why:* REQ-184 "every transaction recorded / auditable" and REQ-177 categories; confines spendable mutation to one aggregate. *Alternative rejected:* free setters on funds — unauditable and easy to desync from the ledger.

### D3. Economic integrity: award / charge / spend with a defined negative-balance rule
Three operations: `award(type, amount, date)` credits; `charge(type, amount, date)` is a *mandatory* obligation (entry/travel) that always records and MAY drive funds negative — the defined negative-balance rule (debt), which the season cycle lets winnings recover; `spend(type, amount, date)` is *discretionary* and is declined (recorded as declined, no balance change) when `amount > availableFunds`. All amounts are non-negative; type carries the sign/category. *Why:* REQ-184 "funds cannot be spent unless available" (discretionary) while "negative balances handled according to defined rules" (mandatory obligations). *Alternative rejected:* a single overdraw-forbidding debit — can't model unavoidable costs that outstrip a struggling golfer's funds (the intended sustainability tension, REQ-187).

### D4. Sponsorship: reputation-gated offers, objective-bearing agreements, deterministic evaluation
A `SponsorshipMarket.generateOffers(commercialReputation, season, rng)` yields `SponsorshipOffer`s whose count and value scale with reputation tier (REQ-186 gated). An accepted offer becomes a `SponsorshipAgreement` (per-season payment, duration, `List<SponsorshipObjective>`, signing bonus). Each season the World supplies a `PerformanceSnapshot`; `agreement.evaluate(snapshot)` marks objectives met/unmet, paying success bonuses and producing a renewal verdict (met enough → renewable; failed → concludes). *Why:* REQ-179/180/188 — independent commercial relationships with objectives that evolve and conclude. *Alternative rejected:* flat, objective-free sponsorship income — loses the strategic and narrative core of REQ-180/181.

### D5. Commercial reputation broadly reflects competitive reputation, not identically
`CommercialReputation.fromCompetitive(double competitive, Rng rng)` maps the World's competitive-reputation score to a commercial score with a small bounded deterministic perturbation, then to a tier. *Why:* REQ-186 "broadly reflect competitive reputation without being identical." *Alternative rejected:* commercial == competitive — the spec explicitly wants correlation, not identity.

### D6. Deterministic AI acceptance policy with trade-offs
`AcceptancePolicy.choose(account, offers, currentAgreements)` deterministically selects which offers to sign under a max-concurrent-agreements cap, preferring higher expected value while discounting offers whose objectives the golfer is unlikely to meet (a simple heuristic from recent performance). Same rules for every golfer; no external input. *Why:* REQ-181 meaningful, non-auto-optimising decisions; REQ-163-style "AI by the same rules." *Alternative rejected:* accept-everything — no trade-off, unbounded income.

### D7. World wiring: per-event prize/expense, seasonal financial cycle
`admit` creates a `FinancialAccount` (starting funds). In `resolveEvent`, for each field golfer: `charge` entry + travel expenses, then `award` the finish prize. In `seasonalTransition`, for each active golfer, in order: pay active sponsorship installments (`award`), evaluate objectives (bonuses + renewal), conclude expired/failed agreements, then generate reputation-gated offers and run `AcceptancePolicy` to sign new ones. Reputation is derived by the World from `ranking.rankingValue(id, date)` + career wins; the `PerformanceSnapshot` from career/ranking. All deterministic. *Why:* REQ-178/182/183/187/188 and the living-world goal; REQ-299 reproducibility. *Alternative rejected:* generating economy inside `Tournament`/`Career` — those must not own finances (REQ-190).

### D8. Isolated economy seed stream; amounts are pure
Offer generation and reputation variation derive a per-golfer/season seed via `Seeds.deriveSeed(...)` with a dedicated economy salt, isolated from shot and weather streams. Prize and expense amounts are pure functions (no RNG). *Why:* reproducibility (REQ-299) and no perturbation of play. *Alternative rejected:* a shared/global RNG — breaks isolation and reproducibility.

## Risks / Trade-offs

- **[Economy leaking into a competitive actor]** → `economy` imports only `core`; it returns financial state and reads only primitives passed in; an architecture/boundary test asserts it never touches attributes/outcomes/rankings/shot resolution (REQ-185).
- **[Ledger/aggregate desync]** → All mutation flows through one guarded path that updates funds and totals and appends the transaction together; a test asserts category totals always equal the ledger folded by type.
- **[Determinism across the world]** → Amounts are pure; offer/reputation randomness uses an isolated seeded stream; the acceptance policy is deterministic; the existing world reproducibility test (two seeds → identical world) covers it end to end, extended to financial state.
- **[Uncalibrated magnitudes — everyone bankrupt or everyone rich]** → All amounts isolated in `EconomyConstants`; tests assert structural properties (winners profit, prize > 0 credited, expenses recorded, high reputation ⇒ richer offers, no discretionary overspend), not magnitudes; tune later.
- **[Negative-balance semantics]** → Explicit: mandatory `charge` may go negative (debt, recovered by winnings/sponsors); discretionary `spend` is declined when unaffordable; both always recorded — asserted by tests.
- **[Financial history growth]** → Transactions are small immutable records kept as the golfer's financial history (REQ-189); acceptable, prunable later.

## Migration Plan

Greenfield `sim.economy` + one additive modification (World financial wiring). Sequencing: (1) `EconomyConstants`, `TransactionType`, `Transaction`; (2) `FinancialAccount` (award/charge/spend, totals, ledger, milestones, history); (3) `ObjectiveType`, `SponsorshipObjective`, `SponsorshipAgreement` (+ `evaluate`), `SponsorshipOffer`, `PerformanceSnapshot`; (4) `CommercialReputation`, `SponsorshipMarket` (reputation-gated offer generation), `AcceptancePolicy` (deterministic AI); (5) wire the World — accounts at `admit`, prize/expense in `resolveEvent`, financial cycle in `seasonalTransition`, a read-only `financialAccountOf` accessor; (6) tests — identity/ledger/integrity/history; prize awarded + expenses charged; sponsorship offers gated by reputation, objectives evaluated, agreements conclude/renew; AI acceptance deterministic with trade-offs; independence boundary; world awards/charges and stays reproducible (financial state identical across two same-seed worlds). Each layer testable before the next.

## Open Questions

- Exact magnitudes (starting funds, entry/travel costs by tier, sponsorship values and bonuses, reputation tier thresholds, max concurrent agreements, agreement duration) — placeholder `EconomyConstants`, tuned later against desired career economics.
- Whether financial milestones live in `sim.economy` or fold into `Career`/legacy — recorded in economy now; a later Media/legacy change can surface them.
- The precise consistency metric for the "seasonal consistency" objective — a simple made-cut/top-finish rate from the `PerformanceSnapshot` now; refine later.
- How the human player's accept/decline and spending choices are captured — the engine exposes the operations; the AI policy fills in until Presentation adds player choice.
