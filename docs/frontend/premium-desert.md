# Premium Desert presentation

Generator 8 Desert courses with canonical Desert provenance select Premium Desert in normal play. Older
courses and incomplete geometry keep the existing renderer. The backend remains authoritative for all
playing surfaces, hazard positions, aiming, shot outcomes, and saves.

The approved V2 source remains unchanged in
[`premium-desert-vector-reference`](../course-holes/prototypes/premium-desert-vector-reference/README.md).
Its SVG SHA-256 is `7be2f520ff4f90297f55214037c28887c8ff1dad88241fa9292971939b13ced4`.
The production artwork ports the source's quadratic organic contours, sandstone and gravel gradients,
layered rock shelves, sparse shrubs and cacti, batched fine ground marks, turf and sand grain, compacted
earth track shoulders, excavated sand treatment, and full-height atmospheric falloff. Python, NumPy,
SciPy, and Shapely remain design-time only.

The fixed illustrative course polygons are replaced by projected Generator 8 geometry. Five continuous
bands sample actual fairway, first-cut, playable-boundary, and green widths, then turn smoothly around
the green. Canonical green, tee, bunker, water, and tree regions draw at their actual coordinates. A
shared mask cuts canonical bunker footprints out of the maintained turf before the recessed sand floors
and contextual shoulders are drawn. A tee-side fade prevents source polygons' rectangular endpoints
from interrupting the continuous bands.

Neighbouring holes come from three genuine Desert Generator 8 holes and keep the active hole's scale.
Placement is accepted only when the outer bands do not overlap and each neighbouring access track has
a clear physical junction with the active track. Route checks cover the whole line stroke against
projected active and neighbouring surfaces and hazards. The routes reserve terrain before shelves,
rocks, shrubs, cacti, and fine marks are placed. A blocked route rejects that placement; canonical
hazards are never moved. All decorative SVG groups ignore pointer input. Aiming remains restricted to
the active 220-unit stage and excludes cosmetic tracks.

## Visual evidence

The development page `/dev/premium-desert-preview` compares the exact approved SVG with generated
artwork, offers a current versus premium comparison and narrow layout, and loads the 18-hole sheet on
request. It uses the checked-in backend-generated Desert seed 103 fixture; normal play uses the live
GraphQL hole.

- [18-hole contact sheet](../course-family-renders/premium-desert-18-hole-contact.jpg)
- [Par 3](../course-family-renders/premium-desert-par3.jpg)
- [Straight par 5](../course-family-renders/premium-desert-straight-par5.jpg)
- [Dogleg par 5 with two connected neighbours](../course-family-renders/premium-desert-dogleg-par5.jpg)
- [Narrow stage](../course-family-renders/premium-desert-mobile.jpg)
- [Current versus Premium](../course-family-renders/premium-desert-current-vs-premium.jpg)
- [Approved source versus generated](../course-family-renders/premium-desert-approved-vs-generated.jpg)
- [Development browser preview](../course-family-renders/premium-desert-browser.png)

The hole 4 desktop scene contains 1,318 SVG elements, including 797 paths and two masks, with two
accepted neighbours and five access paths. Geometry, route, deterministic placement, bunker-cutout,
presentation-selection, typecheck, lint, and production-build checks are part of this change.
The local webpack development preview loaded both the source SVG and generated hole; clicking the
full-size active stage moved the selected aim marker, and the narrow control changed the stage viewBox.
An authenticated Desert career was unavailable, so an actual normal-game shot was not exercised.
