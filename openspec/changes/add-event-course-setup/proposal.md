## Why

Every event today plays the identical course setup — the same pin placements, wind exposure, and green/fairway widths — regardless of tour tier or prestige. Because tour fields differ sharply in skill (a measured ~7-stroke gap between the Development and Elite fields on the same setup), scoring runs *backwards*: the weakest tour posts the **highest** scores (~+5/round) and the strongest the lowest (~−2/round), and majors are no harder than a Development event. Real golf hides the skill gap because **course setups scale with the field** — each tour is set up to play near even par, and only marquee events are brutal. This change gives the simulation that texture: an event's course setup difficulty is derived from its tour tier (to normalize each field to ~even par) and its prestige (to make Signature events tougher and Majors punishing).

Target field-scoring averages: every tier's Regular events ~−0.5 to E; Signature ~+0.5 to +1; Majors ~+2.5 to +4.

## What Changes

- Introduce a per-event **course setup** — three difficulty factors: **pin aggression** (how tucked/deep the flags are), **wind scale** (exposure severity), and **width scale** (effective green/fairway width) — applied when resolving that event's holes and conditions.
- Derive the setup from **(tour tier, event prestige)**: the tier component normalizes for field strength (weaker tours get wider, calmer, centre-pin setups; stronger tours get tighter ones) so every tier's Regular events average ~even par; prestige adds difficulty on top (Signature tougher, Major brutal).
- Apply the setup identically in the automatic resolver and the interactive playable path, so a simmed event stays byte-identical to the played one (fidelity preserved).
- Neutral by construction: a `standard` setup (all factors 1.0) reproduces today's behaviour exactly, so any resolution not given an event setup (raw-course calibration, unit tests) is unchanged.

## Capabilities

### New Capabilities
- `course-setup`: an event's course setup difficulty (pin aggression, wind, effective width) is derived from its tour tier and event prestige to normalize per-tier field scoring and scale marquee-event difficulty.

### Modified Capabilities
- `tournament-play`: each round resolves under the event's course setup (in addition to weather), applied identically on the automatic and interactive paths.
- `event-prestige`: prestige now scales the event's course setup difficulty as well as closing-round pressure (Majors are set up hardest); the base shot mechanics remain prestige-independent.

## Impact

- `com.progolf.sim.course`: new `CourseSetup` record; `Course.holeModel` / `GeneratedHole.forRound` / `pinFor` gain a setup-aware overload; `HoleZones.profileFor` scales effective widths.
- `com.progolf.sim.tournament`: new `SetupDifficulty.forEvent(tier, prestige)` calibration; `Tournament` carries a setup and applies it in round/playoff resolution (default `standard`).
- `com.progolf.sim.play.PlayableEvent`: applies the tournament's setup on the interactive path.
- `com.progolf.sim.world.World`: computes the event setup and hands it to the tournament and the playable event.
- Calibration ripple: world scoring shifts by tier/prestige (the point). Raw-course `ScoringCalibrationTest` uses the `standard` setup and is unaffected; world direction tests remain valid; any absolute-score assertions are re-tuned.
