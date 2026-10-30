# Golf audio v1

The gameplay scene owns one `GolfAudioController`. It prefetches the approved short recordings, unlocks a Web Audio context from a Play Shot or audio-control gesture, and decodes effects without blocking gameplay. The independent SVG shot clock cues the selected club sample on its first movement frame; the physical putting trajectory cues one quiet putter sample on its first movement frame. The backend `holed` result gates cup Foley at the displayed terminal frame. A per-category shuffle bag avoids immediate repeat, and shot identity prevents duplicate cues after a remount.

One HTML media element loops the course-family ambience through shot and hole transitions. It changes source only when the family changes, fades at scene entry, family switch and exit, and is stopped when gameplay unmounts. Browser audio refusal or missing media is silent and does not change shot submission or animation.

The compact gameplay header control stores master mute, effects volume and ambience volume locally under `progolf.audio.v1`. The default ambience gain is intentionally low. There is no sound on page load before a user gesture.

The 15 approved files in `frontend/public/audio/golf-v1/` are exact copies from the approved trimmed palette. [The source manifest](golf-audio-source-manifest.csv) records their original URLs, recorded licenses and SHA-256 values. This is internal-development approval, not public-release rights clearance. Wood Golf 3 may contain applause, the floorball wedge has indoor character, the edited Desert track may contain later animal sounds, and the supplied ambience tracks were not authored as seamless loops. Those recordings and long-loop seams need listening review before release.
