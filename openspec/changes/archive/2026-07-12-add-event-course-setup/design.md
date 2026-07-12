## Context

Course holes are produced by `Course.holeModel(hole, round)` → `GeneratedHole.forRound(round)` → `pinFor(round)` (pin lateral/depth from `holeSeed+round`), and the reachable surfaces by `HoleZones.profileFor(hole, remaining, pinDepth)` (green/fairway half-widths flanked by rough/hazard). Wind reaches a shot via `PlayingConditions.environmentForHole(hole, exposure)`, where `exposure = course.classification().exposure()`. Both the automatic resolver (`Tournament.playRound` / playoff) and the interactive path (`PlayableEvent` rounds / playoff) call these with the raw course — no tier/prestige input — so every event plays identically. `Tournament` already holds `definition.tier()` and `definition.prestige()` (it uses prestige for `PressureModel`), and `PlayableEvent` holds the `Tournament`.

Measured baseline (identical setup, calm): Elite −2.0, Primary −0.0, Secondary +2.3, Development +5.0 per round — a ~7-stroke field-skill spread running opposite to the target. The pin lever alone moves ~2.5 strokes; width is the strong lever for the full range.

## Goals / Non-Goals

**Goals:**
- Give each event a course setup (pin aggression, wind scale, effective width scale) derived from tier + prestige.
- Normalize each tier's Regular events to ~even par; add prestige difficulty (Signature +, Major ++).
- Preserve auto↔playable fidelity: the same setup applies on both paths.
- Neutral by construction: a `standard` setup reproduces today's course behaviour exactly.

**Non-Goals:**
- No change to the core shot-resolution math (dispersion/attribute mapping). Difficulty comes from what the field faces (pins/width/wind), not from re-tuning the shot engine.
- No per-golfer or per-round setup variation beyond what pins/weather already provide — the setup is one descriptor per event.
- No regeneration of `Course` geometry; the setup scales presentation at resolution time, so one physical course can host events of any tier/prestige.

## Decisions

**Decision: a pure `CourseSetup(pinAggression, windScale, widthScale)` record in `sim.course`, with `standard()` = (1,1,1).** It is a presentation-scaling descriptor consumed by the course (pins, width) and the tournament (wind). Living in `sim.course` keeps it free of any tournament dependency; the tournament (which already depends on course) supplies it.

**Decision: the tier+prestige→setup mapping lives in `sim.tournament.SetupDifficulty.forEvent(Tier, EventPrestige)`.** This is where tier and prestige are first-class. It returns a `CourseSetup`. Keeping the *policy* in tournament and the *factors* in course respects the existing dependency direction (tournament → course).

**Decision: one difficulty scalar per (tier, prestige), mapped to the three factors.** `forEvent` computes a single `difficulty` (tier base that rises with field strength + a prestige bump), then maps it monotonically to `widthScale` (wide→narrow), `pinAggression` (central→tucked), and `windScale` (calm→windy). One knob per event class keeps calibration tractable; the three factors move together toward "harder."

**Decision: thread the setup as an explicit, defaulted input — not recomputed ad hoc.** `Course.holeModel(hole, round, setup)`, `GeneratedHole.forRound(round, setup)`, `pinFor(round, setup)`, and `HoleZones.profileFor(hole, remaining, pinDepth, widthScale)` gain setup-aware overloads; the existing no-setup overloads delegate with `standard()`. `Tournament` gains a `(def, weather, setup)` constructor (older constructors default to `standard`) and exposes `setup()`; `PlayableEvent` reads `tournament.setup()`. The World computes `SetupDifficulty.forEvent(tier, prestige)` once per event and passes it to both. This makes non-event resolution (raw-course calibration, unit tests) byte-identical and keeps the auto/playable setup provably the same object.

**Decision: pins scale within the (width-scaled) green.** `pinFor` derives lateral as `uniform × greenHalfWidth × widthScale × PIN_LATERAL_FACTOR × pinAggression` and depth as `uniform × PIN_DEPTH_RANGE × pinAggression`, so a narrowed hard setup tucks pins toward the tighter green edge while a wide easy setup keeps them central — the levers compound in the same direction.

**Decision: calibrate `forEvent` empirically.** A throwaway per-(tier,prestige) diagnostic over generated fields tunes the difficulty table to the targets (Regular ~E each tier, Signature ~+1, Major ~+3) before finalizing constants, mirroring how the putting/per-club calibrations were tuned.

## Risks / Trade-offs

- **Calibration ripple across world tests** → world direction tests (rankings, promotion, longevity) stay valid; only absolute-score assertions need re-tuning. Raw-course `ScoringCalibrationTest` uses `standard()` and is untouched. Budget for a coupled fix-up pass.
- **Fidelity regression if the two paths diverge** → the setup is one object computed by the World and handed to both the Tournament and the PlayableEvent; a fidelity test resolves a non-neutral event both ways and asserts equality.
- **Width scaling could distort hole geometry** (e.g. a green wider than its fairway) → scale factors are bounded to a sane range and applied to the core widths only, letting the existing contiguous-band construction absorb them; the `ShotZoneProfile` invariants (gap-free, contiguous) still hold.
- **Over-normalization masking real skill** → intended: the field-skill gap is expressed as *course difficulty scaling with the field* (realistic — weaker tours play easier courses), not by flattening the shot engine's skill sensitivity, so within an event stronger golfers still separate.
