# Premium Links SVG presentation

Supported Generator 8 Links holes use a top-down full-stage SVG scene in normal play. The existing
canonical projection, inverse aiming transform, and backend shot trace remain in charge of gameplay.
Earlier Links saves and unsupported geometry keep the existing polished renderer.

The approved source is the editable vector and generator in
[`premium_links_codex_handoff`](../course-holes/prototypes/premium_links_codex_handoff/README.md)
(SVG SHA-256 `19dcceb2a1ed615a4985322e2a53533ae9baf2f38755aae80854009be2cb1161`).
`/dev/premium-links-source` renders an identical copy of that SVG at its original viewBox.
The game renderer ports its gradient stops, four-layer sinusoidal dune outlines, nested curved ridges,
contour filaments, sparse grass and low scrub, bunker rims, and horizontal plus vertical stage lighting.
Five tangent-adjusted lateral corridor bands use the source's continuous two-cubic rear cap. Their centres and widths
are fitted to projected canonical fairway, first-cut and playable-boundary samples. A feathered green-end
transition rounds the blunt terminal edges of generated surface polygons; canonical green, bunkers, water and tee
draw above it. Only backend `BUNKER`, `WASTE_AREA`, and `WATER` polygons depict hazards. The Python
generator remains a design artifact and has no runtime role.

On wide stages, partial neighbouring holes use rounded polygons from three real Generator 8 Links holes
(seed 103, holes 3, 2, and 4) at the active hole's pixels-per-yard scale. They are illustrative property
context rather than a literal map of adjacent holes in the current course. Each neighbour reuses the
active Links five-band corridor and two-cubic green-end cap in its own tee-to-green frame. The placement
planner reuses Parkland's generated-neighbour composition and route construction, then accepts a Links
neighbour only after a clear outside route and a physical junction to the active walking track are found.
It checks path strokes against active and decorative playing surfaces, the rounded outer bands, bunkers,
and water. A bounded search tries other positions and orientations before omitting an unconnectable
neighbour. Decorative scenery does not receive pointer events, and aiming outside the active canonical
stage is rejected.

For the seed 103 course at the 2.16 aspect review stage, all 18 holes were checked: 0 views have no
neighbour, 8 have one, and 10 have two. All 28 accepted neighbours have a connected route. Of 362
candidate placements, 19 failed visibility, 228 failed overlap or existing-route clearance, 3 had no
legal neighbour route, and 84 had no legal junction; all 28 accepted placements generated complete
rounded bands. The route-clearance test found no crossing of active or decorative playing surfaces,
bunkers or water.

## Visual review

Run the frontend in development mode and open `/dev/premium-links-preview`. The page shows the unchanged
approved vector beside a real generated hole, then original and Premium Links views, a narrow-stage
toggle, aiming interaction, and all 18 holes of one
deterministic generated Links course (seed 103). Saved evidence:

- [Original versus Premium Links](../course-family-renders/premium-links-comparison.jpg)
- [Approved SVG source](../course-family-renders/premium-links-approved-source.jpg)
- [Approved SVG versus generated hole](../course-family-renders/premium-links-approved-vs-generated.jpg)
- [Straight par 4 with neighbouring holes](../course-family-renders/premium-links-straight-par-4.jpg)
- [Par 3 comparison](../course-family-renders/premium-links-par-3.jpg)
- [18-hole contact sheet](../course-family-renders/premium-links-18-hole-contact.jpg)
- [Narrow stage](../course-family-renders/premium-links-narrow.jpg)
- [Before and after: straight par 4, hole 2](../course-family-renders/premium-links-before-hole-2.jpg) · [after](../course-family-renders/premium-links-after-hole-2.jpg)
- [Before and after: par 3, hole 3](../course-family-renders/premium-links-before-hole-3.jpg) · [after](../course-family-renders/premium-links-after-hole-3.jpg)
- [Before and after: dogleg par 5, hole 4](../course-family-renders/premium-links-before-hole-4.jpg) · [after](../course-family-renders/premium-links-after-hole-4.jpg)
- [One connected neighbour: hole 8](../course-family-renders/premium-links-after-hole-8.jpg)

The review course covers par 3s, straight par 4s, doglegs, and longer par 5s. Its preview uses checked-in
backend-emitted geometry, while normal gameplay uses the live GraphQL hole geometry.
