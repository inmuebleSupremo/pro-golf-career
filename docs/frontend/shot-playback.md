# Non-putt shot playback

The canonical `Hole2d` SVG remains the gameplay renderer for every course family. It plays the backend `ShotTrace` after a completed shot mutation; contact, roll endpoint, recovery, final ball, lie, and score remain backend-owned. Physical putting has its own trajectory renderer and is unchanged.

`tracePlaybackTiming` derives bounded flight, first-contact, release, and recovery durations from each returned trace. Longer carries receive more flight time; longer ground releases receive more roll time. Short running shots are recognized by their release relative to carry, so a chip can finish briskly without club-name timing presets. Ground release uses a decelerating easing curve between the exact returned endpoints. Recovery remains a rules transition, not ball roll.

The moving ball stays on the sampled yard-space track. During flight, the SVG uses interpolated backend height samples for a small screen-space lift, slightly larger ball, and ground-plane shadow. The ball returns to the exact contact point before release. Historical traces without height samples use a zero-height fallback. Reduced-motion mode shows the final result directly.

The trace does not provide physical flight time, impact speed, bounce, spin, or timed rollout samples. These visual cues must not be interpreted as new simulation physics. The non-putt sequence leaves a short terminal frame after the SVG duration before unlocking the next shot.
