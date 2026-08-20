# 2D hole restyle — biome prototypes

Approved visual prototypes for the parametric 2D hole renderer, one per course biome. Each is a
self-contained HTML file that renders six representative holes to show the biome's look and its
variance. They are the **design source of truth** for the live renderer
(`frontend/src/components/play/hole-2d.tsx`, `frontend/src/lib/play/biomes.ts`,
`frontend/src/lib/play/hole-geometry.ts`).

Open any file in a browser (or serve the folder) to view.

## Shared engine (identical across all six)

- **Frame**: mirrors `projectHole` — but the fairway spine is a **cubic** bezier with the tee offset
  *opposite* the dogleg, so holes swing hard across the frame (pronounced + blind doglegs, L/R, par 4 & 5).
  The live port must switch `hole-geometry.ts` to this cubic and update `ballPosition` to sample the
  same cubic so shot playback stays aligned.
- **`ribbon()`** — organic filled corridor via normal-offset + jitter; `holdTop` variant holds full width
  into the green so the fairway forms a continuous flow (no pinch).
- **`blob()`** — smooth closed organic shapes (greens, bunkers, water).
- **Concentric green complex** — the green is seated *inside* the fairway (pushed down / apron) so the
  fairway flows around and above it. No lollipop heads.
- Deterministic seeded PRNG (`mulberry32`) for reproducible cosmetic placement.

## Global design rules (apply to every biome)

1. Vegetation/scatter fills the frame where appropriate; **drawn over bunkers but under the green**
   (canopies overhang bunkers slightly; greens are never under a tree).
2. **Nothing is ever placed in water** — bunkers and vegetation are gated by the *actual local land
   width* (accounting for ribbon taper/jitter), not a nominal width.
3. **No object ever overlaps another of its kind** — bunkers, trees, rocks, palms all use non-overlap
   rejection.
4. **One tee box per hole, coloured per course type** (variety is *between* courses).
5. **Two-layer greens** (fringe + green), shape-varied — **except Mountain**, which is multi-layer/tiered
   to read the elevation change.
6. Championship bunkering: numerous, **varied in size and shape**, strategically placed.
7. Water hazards present with varied placement; frequency varies by biome (desert rarest, tropical most).
8. Fairway-width variance; **no black/dark rings** anywhere (green collars blend, never a hard border).
9. No cart paths.
10. Per-biome plant sets stay **cohesive** (one colour family); go easy on purple.

## Per-biome signature elements

| Biome | Signature |
|---|---|
| parkland | forest fills the frame, 3-layer fairway, kidney bunkers, cart-path-free |
| links | greener fescue, crinkled edge, clustered pot bunkers, gorse, **coastal margins** (sea along one edge) |
| desert | saguaro/barrel/scrub/boulders, sand grain + dry washes, diagonal mow, burnt dry-green surface |
| mountain | slate walls (snow/facets/striations/scree), dense pines, waterfalls into plunge pools, tiered greens |
| heathland | cohesive olive/brush-brown heather + gorse + sparse birch/pine (between links & parkland) |
| tropical | turquoise ocean, wide manicured islands, thin beach/shallow rims, sleek modern bridges over carries, edge palms |
