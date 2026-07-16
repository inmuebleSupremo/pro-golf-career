## Context

`World.generateSchedule` builds each season's calendar with an even-spread formula: per tier it lays `eventsPerTierPerSeason` events at `week = 1 + e·weeksPerSeason/events`, tags the first `signatureEventsPerTier` of them as Signature (so signatures land at the season's start), and spreads `majorsPerSeason` majors across the Elite tier at even intervals. There are no rest weeks, no season finale, and every tier shares one rhythm. The standard scale is already 30 weeks / 4 majors (`WorldConstants`), but only 6 events/tier, so a tour plays ~6–10 weeks of 30 and the year has no shape.

The engine is sim-pure and deterministic: schedules are generated from the world seed and must be byte-reproducible. Tests run small worlds (`WorldConfig(40,6,3,20,4)` — 6-week seasons) that cannot host a 30-week designed cadence.

## Goals / Non-Goals

**Goals:**
- A designed, tier-specific 30-week cadence: majors anchor fixed chapters, signatures are spotlighted, rest weeks bracket the peaks, each tour ends in a Tour Championship.
- Moderate density (~12–16 events/tier) — roughly every other week plus the structured peaks.
- A `TOUR_CHAMPIONSHIP` prestige weighted between Signature and Major.
- Determinism and cross-tour major fields preserved; small-season configs still produce a valid schedule.

**Non-Goals:**
- A near-weekly full calendar (~27/tier) — explicitly the lighter option.
- A playoff series (points reset / staged finale) — deferred.
- Restricted championship fields (top-N qualification) — the championship uses the normal tour field in this slice; elevated fields can come later.

## Decisions

### 1. Cadence as a declarative template + fill rule, not an imperative formula
The season is described by **anchors** (fixed `{week, tier, prestige}` placements that carry the design intent) plus a **fill rule** that lays Regular events on the remaining "on" weeks until the tier hits its target density. Anchors: the 4 majors (Elite, weeks 7/14/21/27), each tour's Tour Championship finale, the spotlight Signatures (including the mid-season Elite+Development collision at week 17), and the rest weeks that bracket majors. Regulars fill ~every other remaining week.

*Why:* the anchor weeks ARE the realism; a formula can't express chapters, collisions, or rest weeks. A declarative template is readable and directly testable ("major on week 7", "no Elite event week 13"). *Rejected:* a fully hardcoded 30-row table (brittle, no path to degrade); a richer parametric formula (still can't express intentional structure).

Representative standard-scale cadence (E = Elite, D = Development; middle tiers Secondary/Primary follow the Development pattern without majors):

| Wk | Elite | Development | | Wk | Elite | Development |
|----|-------|-------------|-|----|-------|-------------|
| 1  | Reg   | Reg         | | 16 | Reg   | Reg         |
| 2  | —     | Reg         | | 17 | **Sig** | **Sig** (collision) |
| 4  | Sig   | —           | | 19 | Reg   | Reg         |
| 5  | —     | Sig         | | 20 | Rest  | Reg         |
| 7  | **MAJOR** | Rest    | | 21 | **MAJOR** | Rest    |
| 10 | Reg   | Reg         | | 24 | Sig   | Reg         |
| 12 | Reg   | Sig         | | 25 | Reg   | **Sig**     |
| 13 | Rest  | Reg         | | 27 | **MAJOR** | Reg     |
| 14 | **MAJOR** | Reg     | | 29 | Reg (push) | **DEV CHAMPIONSHIP** |
|    |       |             | | 30 | **TOUR CHAMPIONSHIP** | Off-season |

This yields ~14 Elite and ~14 Development events. Blank weeks are simply "no event this tier that week" (rest). The exact table is finalized against the density target in implementation; the anchors above are the contract the tests assert.

### 2. Degrade to the proportional generator for small seasons
The structured cadence is used only when the config has room for it — heuristic: `weeksPerSeason >= STRUCTURED_MIN_WEEKS` (≈20) and the standard major/championship counts fit. Otherwise the existing even-spread logic runs (kept as the fallback path), so the 6-week test worlds and any custom small config still satisfy the `world-schedule` contract (tour/course/week/prestige per event, configured signature + major counts, determinism).

*Why:* a 6-week season cannot meaningfully host 4 chaptered majors + a finale. *Rejected:* proportionally scaling the template (a compressed 6-week "cadence" is noise, and would churn every existing schedule test).

### 3. `TOUR_CHAMPIONSHIP` ranks between Signature and Major
New `EventPrestige.TOUR_CHAMPIONSHIP`, inserted **above SIGNATURE, below MAJOR** in every monotonic weighting the prestige system already drives — ranking points, prize money, closing-round pressure, and course-setup difficulty. One per tour per season at the tour's final week (Elite 30, Development 29, middle tiers at their last scheduled week). It is a normal counted event, so its heavier points/prize flow into season standings and thus promotion/relegation **without any new tour-movement rule**.

*Why:* a season finale is significant, but the four majors stay the sport's pinnacle. *Rejected:* major-level weighting (dilutes majors); a separate non-prestige "championship" flag (duplicates the weighting machinery the prestige enum already centralizes).

### 4. Majors stay Elite-tagged but cross-tour; tiers get offset rhythms
A major's `ScheduledTournament` keeps `tier = ELITE` (unchanged), while its field is still drawn cross-tour by `majorField` — so a Development standout can still qualify. Development/Secondary/Primary get their own Signature spotlights placed *near but not on* the Elite major weeks (so lower-tier players get their own high-stakes weeks), plus their own championship. *Why:* preserves the existing, tested cross-tour major mechanic while giving each tour a distinct feel.

### 5. Hold per-season reward totals roughly constant (recommended)
Density roughly doubles (6 → ~14 events/tier). To avoid inflating careers (earnings, ranking-point accumulation, HoF scores all key off season totals), scale per-event Regular reward magnitudes so a *season's* total points/prize stays close to today's, with Signature/Championship/Major keeping their relative premiums. *Why:* preserves the existing, already-recalibrated balance (world scoring, HoF thresholds, career length) instead of re-tuning the whole economy. This is the safest default; see Open Questions.

## Risks / Trade-offs

- **Balance drift from higher density** → Hold per-season totals ~constant (Decision 5) and re-run the behavior tests that pin scoring/HoF timing/career length; treat any residual recalibration as an explicit task, not a surprise.
- **Season-sim performance (~2× events → ~0.6s → ~1.2s/season)** → Non-player events already resolve fast via `playToCompletion`; measure a multi-season advance and confirm it stays responsive. Acceptable if a full unattended season stays well under a couple of seconds.
- **Existing schedule tests churn** → The degradation path (Decision 2) keeps small-config tests on today's behavior; only new tests exercise the 30-week cadence.
- **Determinism regressions** → The template is seed-independent placement; courses stay seed-assigned. A round-trip/determinism test asserts same-seed schedules remain identical.

## Migration Plan

Pure engine change, no data migration. Save compatibility: schedules are regenerated per season and archived; existing saves keep their archived (old-cadence) schedules and generate the new cadence for future seasons — no snapshot format change. Rollback is reverting the change; in-flight seasons are unaffected because a season's schedule is fixed once generated.

## Open Questions

- **Reward normalization:** hold per-season totals constant (Decision 5, recommended) vs. let totals grow with density (simpler, but needs full economy/HoF recalibration). Confirm before implementing the weighting tasks.
- **Championship field:** normal tour field (this slice) vs. an elevated top-N-of-standings field (more realistic finale). Deferring to keep scope tight — confirm that's acceptable.
- **Exact fill density:** target 14/tier vs. anywhere in 12–16 — pin one number for deterministic tests.
