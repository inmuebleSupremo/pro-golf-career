## Context

`HallOfFame.score` weighted only wins. The ranking domain already derives the needed signals over the world's season-ending snapshot history — `RankingHistory.careerHighPosition` (best position ever) and `weeksAtNumberOne` (seasons finishing #1) — and `CareerStatistics` already tracks earnings. Only the plumbing into the credentials + score was missing (it was called out as the extension point when the election shipped).

## Goals / Non-goals

- **Goals**: reward sustained ranking dominance (peak #1, seasons at #1) and earnings in the HoF score; keep the hard baseline floor unchanged; stay deterministic.
- **Non-goals**: a ranking-based eligibility path (the user kept the win/major floor); weekly (vs season-ending) #1 tracking; ranking effects on any non-HoF behaviour.

## Decisions

### D1 — Add ranking + earnings to the credentials as primitives; score-only
`HallOfFameCredentials` gains `careerHighRanking`, `seasonsAtNumberOne`, `careerEarnings`. `HallOfFame` stays in the career domain and consumes them as primitives (the World supplies the ranking, computed from `ranking` snapshots — the same core-only boundary the statistics domain uses). `meetsBaseline` is untouched, so ranking/earnings never change eligibility. Backward-compatible constructors default the new fields, so `Career.retire`'s baseline evaluation and every existing test compile unchanged.

### D2 — Peak bonus linear to a cap; per-season-#1 weight; small earnings term
Peak bonus = `HOF_SCORE_RANK_PEAK × (cap − careerHigh + 1) / cap` for `careerHigh ≤ HOF_RANK_PEAK_CAP` (10), else 0 — most at #1, fading out by #11, nothing for the never-ranked (`HOF_UNRANKED`). Plus `HOF_SCORE_SEASON_AT_ONE` per season at #1 (sustained reign) and `HOF_SCORE_EARNINGS_PER_MILLION` per $1M. Weights are chosen so dominance is a meaningful minority of a strong score (a multi-year #1 adds ~40–55, versus ~12 per major and ~50+ for a bare baseline career) — it can tip the ballot between similar careers without dwarfing majors.

### D3 — Season-ending snapshots as the reign proxy
The world stores one ranking snapshot per season, so "seasons at #1" (not literal weeks) is the reign measure — coarse but the only signal available until a weekly ranking calendar exists. Noted as a limitation; the weights are set against this proxy.

## Risks / Trade-offs

- **Inductions can change** vs the wins-only score (a reigning #1 now beats a compiler): intended, and every inductee still clears the unchanged baseline, so the world HoF test holds. Reproducibility is preserved (score is a deterministic function of deterministic inputs).
- **Coarse reign measure** (season-ending, D3) — a golfer who was #1 mid-season but not at season-end scores no reign credit; acceptable until weekly rankings exist.
- **Earnings scale coupling**: the earnings term assumes the current purse scale; if purses are recalibrated the weight may need revisiting. Kept small so it never dominates.
