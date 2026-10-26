# 2D hole restyle — biome prototypes

Historical visual studies for the 2D hole renderer, one per course biome. Each is a self-contained
HTML file that renders six representative holes. Their local landform generator and cart-path-free
Parkland design do not describe normal play: the live renderer uses backend canonical geometry, and
supported Generator 8 Parkland holes use Premium Parkland V2. See `docs/frontend/technology.md` and
`openspec/specs/web-hole-visualization/spec.md` for current behaviour.

Open any file in a browser (or serve the folder) to view.

## Shared prototype engine (identical across all six)

- **Frame**: the studies use a **cubic** bezier fairway spine with the tee offset *opposite* the
  dogleg. This is prototype-only geometry; live playable holes use the backend's canonical polygons
  and shot traces.
- **`ribbon()`** — organic filled corridor via normal-offset + jitter; `holdTop` variant holds full width
  into the green so the fairway forms a continuous flow (no pinch).
- **`blob()`** — smooth closed organic shapes (greens, bunkers, water).
- **Concentric green complex** — the green is seated *inside* the fairway (pushed down / apron) so the
  fairway flows around and above it. No lollipop heads.
- Deterministic seeded PRNG (`mulberry32`) for reproducible cosmetic placement.

## Historical prototype design rules (apply to every HTML study)

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
