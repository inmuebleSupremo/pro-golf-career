# Non-putt shot playback

The canonical `Hole2d` SVG remains the gameplay renderer for every course family. It plays the backend `ShotTrace` after a completed shot mutation; contact, roll endpoint, recovery, final ball, lie, and score remain backend-owned. Physical putting has its own trajectory renderer and is unchanged.

`tracePlaybackTiming` derives bounded flight, first-contact, release, and recovery durations from each returned trace. Longer carries receive more flight time; longer ground releases receive more roll time. Short running shots are recognized by their release relative to carry, so a chip can finish briskly without club-name timing presets. Ground release uses a decelerating easing curve between the exact returned endpoints. Recovery remains a rules transition, not ball roll.

The moving ball stays on the sampled yard-space track. During flight, the SVG uses interpolated backend height samples for a small screen-space lift, slightly larger ball, and ground-plane shadow. The ball returns to the exact contact point before release. Historical traces without height samples use a zero-height fallback. Reduced-motion mode shows the final result directly.

The animation clock and moving marks live in a small child of the canonical SVG, so frame updates do not rerender the premium scenery. The shot-time dock, target, hole and viewport remain displayed until the trace has painted its final frame; only then do the refreshed controls and physical putting view become available. A genuine resize is measured immediately but applied to the course projection after playback. Reduced motion and historical traces without flight samples keep their fallbacks.

The clock starts after the origin frame and caps a delayed frame's progress step, so a missed frame cannot skip an unseen launch segment. Normal frame intervals keep the approved timing. The screen-space height cue has a finite slope at launch; the authoritative airborne samples and yard-space positions are unchanged. A valid non-putt submission hides the planning target and locks aiming while the result is pending. The resolved trace shows its flight path without the selected target marker; failed submissions restore the editable target, and successful playback releases the next shot's target.

The trace does not provide physical flight time, impact speed, bounce, spin, or timed rollout samples. These visual cues must not be interpreted as new simulation physics.
