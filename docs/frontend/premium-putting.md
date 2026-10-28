# Premium Putting Green

Premium Putting is the presentation of the existing `PHYSICAL_V1` putting route in normal play. `StageHole` passes its normal course presentation to `PhysicalPuttingGreen`; that component continues to own the yard-space projector, click-to-direction mapping, trajectory animation, cup and ball positions, and playback completion. `PremiumPuttingTerrain` supplies a shared green and family-configured scenery. The approved reference package in `docs/course-holes/prototypes/premium-putting-four-families-reference/` remains unchanged.

## Artwork and geometry

The React SVG uses the approved Python generators' layered green/fringe bands, gradient stops, radial turf shade, clipped mowing/grass marks, soft side falloff, recessed bunker materials, ball highlight and contact shadow, flag, and deterministic vegetation construction. Catmull–Rom boundaries use the generated canonical polygon vertices with restrained tangents. The production green and fringe paths come from `hole.geometry.regions`; bunkers and water are drawn only from actual regions. No fixed prototype bunker, walking path, green, ball, cup, contour, or slope arrow enters production.

`premium-putting.ts` chooses Parkland, Links, Heathland, or Desert from the same course provenance used by the full-hole presentation. New Woodland with Parkland family uses the denser Parkland scenery. Historical Woodland associated with Heathland retains the existing putting renderer. Decoration is deterministic from the layout seed and remains clear of green, fringe, fairway, bunker and water polygons; it has no gameplay or pointer events. The scene is memoized independently of the aim direction.

The natural view uses low-opacity contours from `physicalGreen.contours`. Read Green uses those same contours with stronger contrast and a sparse subset of `physicalGreen.slopeSamples`, pointing in the returned downhill direction. It never uses the prototype's illustrative guidance. The existing dock retains control of Read Green availability, pace entry, and Roll Putt. Physical trajectory samples drive the same ball animation as before. Premium framing adds visual context at medium and long distances through the existing uniform projector; its inverse transform controls aiming. Unsupported geometry, older policies, and historical families use the original physical putting drawing.

## Verification evidence

`premium-putting-states.json` contains deterministic `ApiMapper.playingHole` captures from generated Java 21 course models with `PHYSICAL_V1`, valid green ball locations, and backend-produced contours/slope samples. The selected captures cover roughly 3, 23, and 60 feet. The renderer test checks the actual React SVG component. The screenshots below were taken in headless Chrome from a temporary local Next production route using that same component and data; the route was removed after capture. No separate client elevation model was used.

- [Four families, natural above Read Green](premium-putting-evidence/four-families-app.png): Parkland, Links, Heathland, Desert from left to right, 60 ft.
- [Short 3 ft](premium-putting-evidence/parkland-short-app.png), [longer 60 ft](premium-putting-evidence/parkland-long-app.png), and [390 px narrow](premium-putting-evidence/parkland-mobile-app.png) framing.
- [Full-size Links Read Green](premium-putting-evidence/links-read-app.png).
- [Approved Parkland SVG beside generated production state](premium-putting-evidence/source-vs-production-app.png): approved source on the left, in-app production renderer on the right. Their green geometry and camera are intentionally different.

The deterministic capture did not find a 92 ft ball/cup pair within the sampled generated greens; the projector has a 92 ft unit case, but a genuine 90 ft in-game screenshot remains to be verified. An authenticated live putting session was not available for click, pace, Roll Putt, result and next-shot browser verification. The component evidence does not establish that a live shot completed.

The approved Parkland SVG has about 6,172 SVG shapes and the approved Heathland Read Green SVG about 8,842. The corresponding generated production captures have about 1,105 and 1,113 shapes, respectively, after batching turf and fescue paths and limiting decoration to the active camera. No performance issue was measured; aim changes keep the static terrain memoized.
