## Context

The tournament engine produces `TournamentResult` (finishing order + positions), each event carrying a `tier` and a `scheduledDate`. Golfers are `ProfessionalGolfer`/`Player` with stable ids. Nothing yet measures standing across events. This change adds `sim.ranking`: a pure, deterministic World Ranking computed from a ledger of dated ranking awards. Season Standings / Tours and the World calendar stay deferred; time is taken from tournament dates and an as-of date.

Framework-free, Java 21 / Spring Boot 3, consistent with existing `sim.*`.

## Goals / Non-Goals

**Goals:**
- Award ranking points from a completed tournament by position × tier × field strength.
- Decay points over a rolling ~2-year window; ranking value = sum of decayed points; ranking = order by value.
- Deterministic and reproducible from the same results + as-of date; identical for all control types.
- Preserve history (career-high, weeks at #1, snapshots, movement) append-only, and recognise Top 100/50/10/#1.
- Analytical only — never mutates gameplay; single authoritative source of standing.

**Non-Goals:**
- Season Standings, tour promotion/relegation (Tour domain).
- A real calendar/season loop (dates come from results; "weeks" derive from dated snapshots).
- Any write-back to players or tournaments.
- Monetary/awards logic (consumers read the ranking later).

## Decisions

### D1. A ledger of immutable dated awards; standings computed on demand
`RankingLedger` holds `RankingAward(golferId, date, points, tournamentId)` records appended as tournaments complete. `rankingAsOf(date)` computes each golfer's decayed value and orders them. *Why:* keeps the source of truth append-only (REQ-144), makes any historical ranking reproducible by choosing an as-of date, and separates raw awards (stored) from standings (derived) — mirroring the Player derived-statistics rule. *Alternative rejected:* storing a mutable current ranking — loses reproducibility and history.

### D2. Points = base(tier) × positionCurve(position) × fieldStrengthFactor
A tournament's award for a finish is `basePoints(tier) * positionWeight(position) * fieldStrengthFactor(field)`. `positionWeight` is a decreasing curve (winner most, decaying by place). *Why:* matches REQ-143 (position, tier, field strength) and keeps better-finish-more-points monotone by construction. All coefficients live in a single `RankingConstants` surface (like `SimConstants`), tunable later.

### D3. Field strength via a bootstrap proxy that upgrades to real ranking value
`fieldStrengthFactor` uses the field's current ranking values when available, falling back to a tier-based proxy when rankings have not converged (early events). *Why:* solves the chicken-and-egg (points need field strength; field strength needs points) without iteration, and REQ-143 asks for a bootstrap. *Alternative rejected:* iterating to a fixed point — unnecessary complexity for V1; the proxy converges as history accumulates.

### D4. Rolling decay is a pure function of age in days
`decay(points, ageDays)` returns full value within a full-value period then declines linearly to zero at the window edge (~730 days), à la a simplified OWGR. Age = `as-of date − tournament date` in days. *Why:* deterministic, needs only dates already present, and expired points drop out naturally (REQ-143). Constants (full-value days, window days) live in `RankingConstants`.

### D5. History and snapshots are derived by evaluating the ledger at chosen dates
"Career-high", "weeks at #1", season-ending ranking, and snapshots are computed by evaluating `rankingAsOf(date)` at the relevant dates and folding the results; a `RankingSnapshot` is an immutable capture. Movement/milestone crossings compare two snapshots. *Why:* one engine (the ledger) drives everything; snapshots are immutable by being plain value records (REQ-148). "Weeks at #1" is counted over dated snapshots — a documented proxy until the World calendar provides real weeks.

### D6. Recognition and consumers are read-only over the standing
`RankingRecognition` derives the best milestone a golfer has reached from their history; dependent systems receive read-only standing/among value types and cannot reach the ledger's mutation surface. *Why:* REQ-146/149 — single authoritative source, consumed without modification.

## Risks / Trade-offs

- **[Field-strength proxy could rank early events oddly]** → Proxy is tier-based and monotone; a test asserts stronger fields/tiers award more; accept early-season approximation, documented.
- **[Points/decay constants uncalibrated]** → Isolate in `RankingConstants`; assert structural properties (monotonicity, decay, expiry) now; tune magnitudes later against desired ranking dynamics.
- **["Weeks at #1" without a real calendar]** → Derive from dated snapshots as a proxy; the World/calendar change refines it; keep the API stable so the refinement is additive.
- **[Ledger growth over a long career]** → Awards are small records; decay lets old ones be pruned by the window when computing, and pruning storage is a later optimisation. O(golfers × awards-in-window) per computation is fine at Standard scale.
- **[Golfer identity vs eligibility]** → Rank by golfer id; eligibility filters the active ranking while history keys stay valid, so a retired golfer's records survive (REQ-142).

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) `RankingConstants`, `RankingAward`, `RankingLedger`; (2) points model (base × position × field strength) + bootstrap proxy; (3) rolling decay + `rankingAsOf(date)` ordering with deterministic tie-break; (4) eligibility filtering; (5) history (`RankingSnapshot`, career-high, weeks-at-#1, season-ending, movement); (6) recognition (Top 100/50/10/#1) + read-only standing types; (7) test suites (monotonicity, decay/expiry, reproducibility, control-type equality, eligibility, history immutability, milestone recognition, and an end-to-end run feeding several `TournamentResult`s through the ledger). Each layer testable before the next.

## Open Questions

- Exact base points per tier and the position curve shape — placeholder constants now; tune against desired ranking spread and turnover later.
- Full-value period vs. decline shape of the decay window — start with a simple full-then-linear model; revisit if ranking dynamics feel wrong.
- Whether ranking snapshots are taken by this domain or requested by the (future) World/season loop — expose snapshot-on-demand now; the World domain decides cadence later.
- Ingestion point: whether the ledger observes tournament completion directly or is fed results by a coordinator — model it as an explicit `record(TournamentResult, tier, date)` call now; the World coordinator wires it later.
