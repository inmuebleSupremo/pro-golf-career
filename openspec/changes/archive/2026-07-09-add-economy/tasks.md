## 1. Financial identity & ledger

- [x] 1.1 Create framework-free package `com.progolf.sim.economy` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `EconomyConstants` (starting funds, entry/travel expense magnitudes by tier, sponsorship base values/bonuses, reputation tier thresholds, max concurrent agreements, agreement duration, offer counts, economy seed salt) as the single tunables surface.
- [x] 1.3 Define `TransactionType` (PRIZE_MONEY, SPONSORSHIP_INCOME, SPONSORSHIP_BONUS, ENTRY_FEE, TRAVEL, …) with an earning/expense category, and an immutable `Transaction` (sequence, date, type, amount, resulting balance, description).
- [x] 1.4 Implement `FinancialAccount`: available funds + folded totals (career earnings, tournament earnings, sponsorship income, career expenses) + append-only ledger; guarded `award` / `charge` / `spend` (see 1.5); a read-only history and `FinancialMilestone` set.
- [x] 1.5 Economic integrity: `award` credits; `charge` is a mandatory obligation that always records and may drive funds negative (defined negative-balance rule); `spend` is discretionary and declined (recorded) when it exceeds available funds. All amounts non-negative; totals always equal the ledger folded by type.

## 2. Sponsorship

- [x] 2.1 Define `ObjectiveType` (PARTICIPATION, RANKING, WINS, CONSISTENCY, MILESTONE), `SponsorshipObjective` (type, target, reward), and `PerformanceSnapshot` (season events/wins/best ranking/consistency/milestones) supplied by the World.
- [x] 2.2 Define `SponsorshipAgreement` (sponsor, per-season payment, duration, objectives, signing bonus, start season) with `evaluate(snapshot)` → objectives met/unmet, bonuses earned, and a renewal verdict; and helpers for active/expired.
- [x] 2.3 Define `SponsorshipOffer` and `CommercialReputation.fromCompetitive(competitive, rng)` — a bounded deterministic perturbation to a commercial reputation/tier that broadly reflects but is not identical to competitive reputation.
- [x] 2.4 Implement `SponsorshipMarket.generateOffers(commercialReputation, season, rng)` — offer count and value scale with reputation tier (reputation-gated); deterministic.
- [x] 2.5 Implement `AcceptancePolicy.choose(account, offers, currentAgreements)` — deterministic AI selecting offers under a max-concurrent cap with a trade-off (prefer value, discount unmeetable objectives); same rules for all golfers.

## 3. World wiring (modified: world-progression)

- [x] 3.1 In `admit`, create a `FinancialAccount` (starting funds) for each golfer; add a read-only `financialAccountOf(id)` accessor.
- [x] 3.2 In `resolveEvent`, for each field golfer: `charge` entry + travel expenses and `award` the finish prize from the result.
- [x] 3.3 In `seasonalTransition`, for each active golfer: pay active sponsorships, evaluate objectives (bonuses + renewal), conclude expired/failed agreements, then generate reputation-gated offers (reputation from `ranking.rankingValue` + career wins) and run `AcceptancePolicy` to sign new ones.
- [x] 3.4 Derive the economy seed stream from the world hierarchy with a dedicated salt (isolated from shot/weather streams); keep retirees' accounts preserved (no new activity).

## 4. Verification

- [x] 4.1 Identity/ledger tests: every golfer has an account; award/charge/spend each append an auditable transaction; category totals always equal the ledger folded by type; funds and history persist.
- [x] 4.2 Integrity tests: a discretionary `spend` beyond funds is declined (balance unchanged, recorded); a mandatory `charge` beyond funds records and goes negative per the rule.
- [x] 4.3 Prize/expense tests: a paying finish credits prize to funds + tournament earnings; a non-paying finish credits nothing; competing charges expenses.
- [x] 4.4 Sponsorship tests: offers are reputation-gated (higher reputation ⇒ more/richer offers); objectives evaluate met/unmet from a `PerformanceSnapshot`, paying bonuses; agreements conclude or renew; sponsorship history preserved; commercial reputation correlates with but is not identical to competitive.
- [x] 4.5 Decision tests: `AcceptancePolicy` is deterministic and selects under the cap with a trade-off (does not accept every offer).
- [x] 4.6 Boundary test: the Economy changes no Player Attribute, Tournament outcome, Ranking, or shot resolution (REQ-185); `sim.economy` imports only `core`.
- [x] 4.7 World tests: events award prize and charge expenses; the seasonal cycle runs; winners profit over a season while some lower golfers run at a loss (progression/sustainability); two worlds with the same seed produce identical financial state (reproducible).
- [x] 4.8 Run `openspec validate add-economy --type change --strict` and resolve findings.
