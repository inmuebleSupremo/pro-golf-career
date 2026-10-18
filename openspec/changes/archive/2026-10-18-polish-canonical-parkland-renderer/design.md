## Context

`CanonicalHole2d` already projects the active event's canonical polygons, tee, cup, ball, aim point, and
backend-authored trace into one SVG. `layoutSeed` is a stable, backend-supplied cosmetic seed. The parkland
reference uses layered greens, a clear rough/fairway relationship, natural deciduous canopy silhouettes, soft
water and sand treatment, and restrained texture rather than a replacement landform model.

## Decisions

### Preserve canonical polygons as the only terrain geometry

Every surface remains a path directly projected from `PlayingGeometry.regions`; styling may add gradients,
patterns, or outlines to that same path but may not alter its boundary. Terrain order continues to follow the
authoritative region order. The playable boundary remains the sole outer course extent.

### Constrain vegetation to authoritative tree regions

The frontend derives deterministic deciduous-canopy placements from `layoutSeed` and a tree-region identity.
It accepts points only inside that region's supplied polygon, rejects overlapping placements, and clips the
finished canopies to the same polygon. It therefore cannot show blocking trees in a fairway, rough, water,
bunker, fringe, or green.

### Treat texture and lighting as non-mechanical material cues

Parkland surface gradients, mower stripes, sand grain, water highlights, and subtle grass texture are fills
or overlays clipped to their actual canonical surface. They do not imply elevation, carries, collision, new
hazards, or a different lie. Existing target, ball, cup, and trace elements remain above terrain and retain
their authoritative coordinates and labels.

### Keep the change renderer-local and reusable

A pure helper contains stable seed, point-in-polygon, bounds, and bounded non-overlap placement logic. JSX
only renders its result with the existing biome palette/symbol definitions. This allows a second actual
geometry to demonstrate reuse without creating a new local course or hole generator.

## Risks and mitigations

- **Dense ornament can hide gameplay facts** — use subtle material detail and retain high-contrast markers;
  clip all decoration to the owning surface.
- **Seeded details could shift after a rerender** — derive them exclusively from `layoutSeed` and supplied
  polygons, with no ambient randomness.
- **Tree canopies could suggest false obstacles** — only generate for `TREES` regions and clip canopies to their
  polygon; do not decorate generic rough as trees.

## Non-goals

- V6 landscape work, generator redesign, physics, hazard/collision mechanics, GraphQL changes, new gameplay
  controls, or hand-authored playable templates.
