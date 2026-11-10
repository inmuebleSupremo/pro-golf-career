# Physical Putting V1

## Status and activation

**Current status (2026-11-10):** physical putting is integrated into `develop` and is the default and only mode for player-playable events. A scheduled event still carries a `PuttingPolicy` for save compatibility: older records without a stored policy deserialize as `LEGACY`, while `PHYSICAL_V1` identifies the physical solver. When a player-entered event becomes playable, the world upgrades its effective event policy to `PHYSICAL_V1` without rewriting the saved schedule record.

Historical save compatibility remains important. This milestone does not claim finished professional-performance calibration or green-contour variety.

## Authoritative simulation

`GreenSurface V1` is a deterministic height field derived from the active canonical green geometry and its recorded provenance. It provides the elevation and gradient samples used by the physical solver; it is not a separately authored frontend terrain model. Reconstructing the same event provenance regenerates the same surface, cup, and effective pin placement.

For `PHYSICAL_V1`, the player supplies only a starting direction and intended roll distance. The pure backend solver applies execution variation from golfer state and attributes, rolling resistance, downhill/uphill slope effects, and physical cup-capture rules. It returns the final position, terminal condition, holed state, and an ordered trajectory. A ball is holed only by that authoritative capture outcome: proximity to the cup or display rounding cannot create a hole-out.

Human and simulated play use the same physical resolution path. AI supplies its planner intent to that solver; it does not receive a separate cup-capture rule or a hidden green model. The integrated round/event path retains the solver's trajectory and final `BallState` for scoring, completion, and the next playable stroke.

## API and presentation boundary

The GraphQL read model exposes the event putting policy and, for a physical hole, a read-only `physicalGreen` projection with solver-derived contours and slope samples. The physical mutation accepts direction, intended roll feet, and the expected shot revision. It returns the authoritative physical result and trajectory.

The frontend uses that data for the dedicated overhead putting view, optional green-reading overlays, physical-scale cup/ball markers with zoom-aware locators, and trajectory playback. It starts with a cup-directed line and distance-based pace, but players may adjust both independently. It does not calculate a roll, capture, final position, or future trajectory; playback reveals only the portion already traversed before presenting the final state or hole completion.

## Compatibility and remaining work

Legacy `playPutt` remains a compatibility route for `LEGACY` events. Existing saves with no putting-policy field deserialize to `LEGACY`; explicit `PHYSICAL_V1` policy and event provenance survive save/load. No duplicate mutable GreenSurface payload is stored solely for presentation.

The frontend and automatic paths select the policy-appropriate route. The retained legacy `playPutt` endpoint rejects physical events; `playPhysicalPutt` rejects non-physical green states. A non-UI client must select the route from the reported policy.

The current milestone intentionally leaves these concerns open: evidence-based putting-difficulty and golfer-attribute calibration, broader green footprint/contour diversity, and any visual polish warranted by gameplay evidence. Manual testing observed occasional apparent cup-edge/remaining-distance ambiguity; focused investigation found no demonstrated physical-solver defect.
