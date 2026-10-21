# Physical Putting V1

## Status and activation

Physical putting is a playable, **explicit event opt-in**, not the default game mode. A scheduled event has a `PuttingPolicy`: `LEGACY` preserves the probability-based putting route, while `PHYSICAL_V1` enables physical putting on green lies. New and historical events without a stored policy resolve as `LEGACY`; an unstarted eligible event may be switched deliberately. The policy is carried with event provenance, so a started event does not silently change rules.

This boundary preserves historical saves and ordinary gameplay. It does not claim that physical putting is globally enabled, fully calibrated to professional performance, or merged into `develop`.

## Authoritative simulation

`GreenSurface V1` is a deterministic height field derived from the active canonical green geometry and its recorded provenance. It provides the elevation and gradient samples used by the physical solver; it is not a separately authored frontend terrain model. Reconstructing the same event provenance regenerates the same surface, cup, and effective pin placement.

For `PHYSICAL_V1`, the player supplies only a starting direction and intended roll distance. The pure backend solver applies execution variation from golfer state and attributes, rolling resistance, downhill/uphill slope effects, and physical cup-capture rules. It returns the final position, terminal condition, holed state, and an ordered trajectory. A ball is holed only by that authoritative capture outcome: proximity to the cup or display rounding cannot create a hole-out.

Human and simulated play use the same physical resolution path. AI supplies its planner intent to that solver; it does not receive a separate cup-capture rule or a hidden green model. The integrated round/event path retains the solver's trajectory and final `BallState` for scoring, completion, and the next playable stroke.

## API and presentation boundary

The GraphQL read model exposes the event putting policy and, for a physical hole, a read-only `physicalGreen` projection with solver-derived contours and slope samples. The physical mutation accepts direction, intended roll feet, and the expected shot revision. It returns the authoritative physical result and trajectory.

The frontend uses that data for the dedicated overhead putting view, optional green-reading overlays, physical-scale cup/ball markers with zoom-aware locators, and trajectory playback. It starts with a cup-directed line and distance-based pace, but players may adjust both independently. It does not calculate a roll, capture, final position, or future trajectory; playback reveals only the portion already traversed before presenting the final state or hole completion.

## Compatibility and remaining work

Legacy `playPutt` remains the route for `LEGACY` events. Existing saves with no putting-policy field deserialize to `LEGACY`; explicit `PHYSICAL_V1` policy and event provenance survive save/load. No duplicate mutable GreenSurface payload is stored solely for presentation.

The frontend and automatic paths select the policy-appropriate route, and `playPhysicalPutt` rejects non-physical events. The retained legacy `playPutt` endpoint is a compatibility route and does not itself reject a physical event, so a non-UI client must select the route from the reported policy. This is a limited API-boundary caveat, not a different solver for the shipped physical interface.

The current milestone intentionally leaves these concerns open: evidence-based putting-difficulty and golfer-attribute calibration, broader green footprint/contour diversity, and any visual polish warranted by gameplay evidence. Manual testing observed occasional apparent cup-edge/remaining-distance ambiguity; focused investigation found no demonstrated physical-solver defect.
