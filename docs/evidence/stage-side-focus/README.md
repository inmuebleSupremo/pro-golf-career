# Earlier gameplay stage side-focus pass

These captures record the first normalization pass. The final premium fade and ocean-edge correction is documented in [`../stage-focus-final/README.md`](../stage-focus-final/README.md).

Matched 1600 × 900 renders from the production `Hole2d` and `PhysicalPuttingGreen` components. Each before/after pair uses the same fixture, viewport, hole geometry, and artwork. Only the horizontal atmosphere gradient changed.

| View | Before | After |
| --- | --- | --- |
| Woodland full hole | [PNG](before/woodland.png) | [PNG](after/woodland.png) |
| Coastal Links hole 7 | [PNG](before/coastal-links.png) | [PNG](after/coastal-links.png) |
| Desert full hole | [PNG](before/desert.png) | [PNG](after/desert.png) |
| Links putting green, 60 ft | [PNG](before/links-putting.png) | [PNG](after/links-putting.png) |

At the time of this capture, the full-hole renderers used the same symmetric side-focus curve. Opacity was 0.92 at each viewport edge, 0.80 at 11.6%, 0.58 at 24.9%, 0.18 at 35.3%, and zero from 43% to 57%. The putting camera used the same normalized curve at 90% strength with a wider clear center from 35% to 65%.

The test in `frontend/src/components/play/stage-side-focus.test.tsx` captures Parkland, Woodland, coastal Links, Heathland, Desert, and Links putting SVGs when `SIDE_FOCUS_CAPTURE_DIR` is set. These PNGs were rasterized from the captured SVGs with Inkscape; the Woodland SVG's `var(--background)` was resolved to the game's dark-theme `#0e1311` for both captures. The coastal sample shows the continuous sea and shoreline beside the fairway; the sea remains visible under the fade.
