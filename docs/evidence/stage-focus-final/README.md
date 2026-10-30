# Final premium stage focus and coastal edge

Matched production `Hole2d` and `PhysicalPuttingGreen` component renders. Each before/after pair uses the same generated or checked-in hole, viewBox, viewport, and artwork. The **before** captures have the earlier shared fade and unextended ocean polygon; the **after** captures have the corrected fade and render-only ocean extension. PNGs were rasterized from captured SVGs with Inkscape. For the Parkland/Woodland PNGs, both SVGs' `var(--background)` was resolved to the game's dark-theme `#0e1311` during rasterization.

| Sample | Before | After |
| --- | --- | --- |
| Parkland | [PNG](before/parkland.png) | [PNG](after/parkland.png) |
| Woodland | [PNG](before/woodland.png) | [PNG](after/woodland.png) |
| Heathland | [PNG](before/heathland.png) | [PNG](after/heathland.png) |
| Desert | [PNG](before/desert.png) | [PNG](after/desert.png) |
| Coastal Links, seed 103 hole 7 | [PNG](before/coastal-links.png) | [PNG](after/coastal-links.png) |
| Coastal Links bay, seed 103 hole 9 | [PNG](before/coastal-links-bay.png) | [PNG](after/coastal-links-bay.png) |
| Links putting, 60 ft | [PNG](before/links-putting.png) | [PNG](after/links-putting.png) |

The supplied Old Downs screenshots show a straight gap at the left edge. The renderer now uses its measured SVG aspect ratio after layout and extends only the projected ocean polygon's offshore edge beyond the current viewBox. The canonical shoreline vertices and gameplay geometry are unchanged. The renderer handles either coastal orientation; inland Links holes do not enter the ocean extension path. The exact Docker screenshot state was not reproduced in these static component captures.

The original premium Parkland comparison shows a bright playable center, short dimming shoulders, and consistently dark outer wings. The full-hole shared gradient now remains at 0.92 opacity from the edge through 24% of the stage, falls to 0.82 at 30% and 0.34 at 35%, and is clear from 40–60%; the right half mirrors those stops. The putting profile retains its wider, gentler 35–65% clear center. Family shade colors and vertical lighting remain unchanged.

For the captured full-hole samples, all fairway, green, and bunker vertices project between roughly 44% and 56% of the viewport width, inside the clear center. The fade leaves their geometry and aiming coordinates untouched.

The capture/test source is `frontend/src/components/play/stage-side-focus.test.tsx`; set `SIDE_FOCUS_CAPTURE_DIR` to write SVGs. The two coastal captures use backend-generated seed 103 holes 7 and 9.
